import json
import os
import threading
import time
from collections import deque
from flask import Flask, jsonify, Response
import socket
from shared import TOPIC_BIDS, TOPIC_PROCESSED, TOPIC_RESULT, TOPIC_METRICS, consumer, SESSION_ID

# Aplicatia Flask afiseaza un dashboard simplu pentru monitorizarea licitatiei
app = Flask(__name__)
# State este starea globala a dashboardului: contoare, evenimente recente, serii pentru grafic si date Docker
state = {
    "bids": 0,
    "processed": 0,
    "results": 0,
    "events": deque(maxlen=200),
    "series": deque(maxlen=120),
    "containers": {},
}


def docker_socket_get(path: str):
    # Deschidem socketul local Docker ca sa putem citi informatii despre containere
    client = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    client.settimeout(2)
    client.connect("/var/run/docker.sock")
    # Construim manual o cerere HTTP catre API-ul Docker
    request = f"GET {path} HTTP/1.1\r\nHost: docker\r\nConnection: close\r\n\r\n"
    client.sendall(request.encode())
    chunks = []
    # Citim raspunsul in bucati pana cand conexiunea se inchide
    while True:
        data = client.recv(65536)
        if not data:
            break
        chunks.append(data)
    client.close()
    # Separarea headerelor HTTP de corpul raspunsului JSON
    raw = b"".join(chunks).decode("utf-8", errors="ignore")
    body = raw.split("\r\n\r\n", 1)[-1]
    return json.loads(body or "[]")


def docker_stats_loop():
    # Bucla ruleaza continuu intr-un thread separat si actualizeaza statisticile containerelor
    while True:
        try:
            containers = docker_socket_get("/v1.40/containers/json")
            result = {}
            for c in containers:
                name = (c.get("Names") or [""])[0].lstrip("/")
                # Afisam doar containerele relevante pentru proiectul de licitatie
                if not any(key in name for key in ["auctioneer", "bidder", "message_processor", "bidding_processor"]):
                    continue
                cid = c.get("Id", "")[:12]
                stats = docker_socket_get(f"/v1.40/containers/{cid}/stats?stream=0")
                mem = stats.get("memory_stats", {}).get("usage", 0)
                cpu = stats.get("cpu_stats", {}).get("cpu_usage", {}).get("total_usage", 0)
                result[name] = {"id": cid, "memory_bytes": mem, "cpu_total_usage": cpu}
            state["containers"] = result or {"info": "nu sunt containere relevante in rulare"}
        except Exception as exc:
            # Daca Docker socket nu este disponibil, afisam motivul in dashboard
            state["containers"] = {"info": f"Docker socket indisponibil: {exc}"}
        time.sleep(2)


def kafka_loop():
    # Pornim cate un thread de consum pentru fiecare topic important
    for topic, label in [(TOPIC_BIDS, "bids"), (TOPIC_PROCESSED, "processed"), (TOPIC_RESULT, "results"), (TOPIC_METRICS, "metrics")]:
        threading.Thread(target=consume_topic, args=(topic, label), daemon=True).start()
    # La fiecare secunda salvam valorile curente pentru graficul din pagina web
    while True:
        state["series"].append({"t": time.strftime("%H:%M:%S"), "bids": state["bids"], "processed": state["processed"], "results": state["results"]})
        time.sleep(1)


def consume_topic(topic, label):
    # Consumatorul asculta un topic Kafka si actualizeaza starea dashboardului
    c = consumer(topic, group_id=f"monitor-{topic}", timeout_ms=None)
    for msg in c:
        val = msg.value
        # Daca mesajul are session_id diferit, il ignoram ca sa nu poluam monitorizarea curenta
        if isinstance(val, dict) and val.get("session_id") not in (SESSION_ID, None):
            continue
        # Pentru bids, processed si results crestem contorul corespunzator
        if label in state:
            state[label] += 1
        # Salvam evenimentul in fata listei, ca cele noi sa apara primele
        state["events"].appendleft({"topic": topic, "value": val, "partition": msg.partition, "offset": msg.offset})


@app.route("/")
def index():
    # Ruta principala returneaza HTML-ul dashboardului
    return Response('''<!doctype html><html><head><meta charset="utf-8"><title>Lab10 Monitor</title>
<style>body{font-family:Arial;margin:25px} .card{border:1px solid #ddd;border-radius:12px;padding:16px;margin:12px 0} svg{width:100%;height:260px;border:1px solid #eee}</style></head><body>
<h1>Monitorizare Lab 10 - Kafka Auction System</h1>
<div class="card"><b>Contoare:</b> <span id="counters"></span></div>
<div class="card"><h3>Grafic în timp real</h3><svg id="chart" viewBox="0 0 600 220"></svg><small>Linie 1=oferte, linie 2=procesate, linie 3=rezultate.</small></div>
<div class="card"><h3>Containere</h3><pre id="containers"></pre></div>
<div class="card"><h3>Evenimente recente</h3><pre id="events"></pre></div>
<script>
function line(points, key, offset){let max=Math.max(1,...points.map(p=>p.bids),...points.map(p=>p.processed),...points.map(p=>p.results)); return points.map((p,i)=>`${i*600/Math.max(1,points.length-1)},${200-(p[key]*180/max)-offset}`).join(' ')}
async function tick(){let r=await fetch('/metrics'); let d=await r.json(); document.getElementById('counters').textContent=`oferte=${d.bids}, procesate=${d.processed}, rezultate=${d.results}`; document.getElementById('containers').textContent=JSON.stringify(d.containers,null,2); document.getElementById('events').textContent=JSON.stringify(d.events.slice(0,10),null,2); let s=d.series; document.getElementById('chart').innerHTML=`<polyline fill="none" stroke="black" points="${line(s,'bids',0)}"/><polyline fill="none" stroke="gray" points="${line(s,'processed',8)}"/><polyline fill="none" stroke="darkgray" points="${line(s,'results',16)}"/>`;}
setInterval(tick,1000); tick();
</script></body></html>''', mimetype="text/html")

@app.route("/metrics")
def metrics():
    # Ruta aceasta este apelata periodic de JavaScript si intoarce starea curenta in format JSON
    return jsonify({
        "bids": state["bids"], "processed": state["processed"], "results": state["results"],
        "events": list(state["events"]), "series": list(state["series"]), "containers": state["containers"]
    })

# Pornim thread-urile de monitorizare si serverul Flask
if __name__ == "__main__":
    threading.Thread(target=kafka_loop, daemon=True).start()
    threading.Thread(target=docker_stats_loop, daemon=True).start()
    app.run(host="0.0.0.0", port=2500)
