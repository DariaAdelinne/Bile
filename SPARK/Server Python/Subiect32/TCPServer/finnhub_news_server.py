#!/usr/bin/env python3
"""
finnhub_news_server.py - Server TCP Python.

Preia stirile din ultimele 2 saptamani pentru compania Apple (AAPL)
de la API-ul Finnhub si le trimite prin socket, cate o stire la 5 secunde.

Serializare: json.dumps (o linie JSON per stire)

Rulare:
  python3 finnhub_news_server.py
"""

import json
import socket
import time
import urllib.request
import urllib.parse
from datetime import datetime, timedelta

API_TOKEN = "brmu4j7rh5r90ebn6irg"
FINNHUB_BASE = "https://finnhub.io/api/v1"
SYMBOL = "AAPL"
TCP_HOST = "0.0.0.0"
TCP_PORT = 9999
DELAY_SEC = 5


def fetch_news(symbol: str) -> list:
    """Preia stirile din ultimele 2 saptamani pentru simbolul dat."""
    today = datetime.now()
    two_weeks_ago = today - timedelta(weeks=2)
    date_from = two_weeks_ago.strftime("%Y-%m-%d")
    date_to = today.strftime("%Y-%m-%d")

    params = urllib.parse.urlencode({
        "symbol": symbol,
        "from": date_from,
        "to": date_to,
        "token": API_TOKEN
    })
    url = f"{FINNHUB_BASE}/company-news?{params}"

    print(f"Se preiau stirile pentru {symbol} ({date_from} - {date_to}) ...")
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode("utf-8"))

    print(f"Stiri preluate: {len(data)}")
    return data


def handle_client(conn: socket.socket, addr, articles: list):
    """Trimite stirile catre clientul conectat, cate una la DELAY_SEC secunde."""
    print(f"Client conectat: {addr[0]}:{addr[1]}")
    try:
        for article in articles:
            # serializam stirea ca JSON pe o singura linie
            line = json.dumps(article) + "\n"
            conn.sendall(line.encode("utf-8"))
            print(f"  Trimis: {article.get('headline', '')[:60]}...")
            time.sleep(DELAY_SEC)
    except (BrokenPipeError, ConnectionResetError):
        print("Clientul s-a deconectat prematur.")
    finally:
        conn.close()
        print("Conexiune inchisa.")


def main():
    articles = fetch_news(SYMBOL)
    if not articles:
        print("Nu s-au gasit stiri. Verifica token-ul API sau simbolul.")
        return

    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind((TCP_HOST, TCP_PORT))
    srv.listen(1)
    print(f"Server pornit pe portul {TCP_PORT}. Astept conexiuni...")

    while True:
        conn, addr = srv.accept()
        handle_client(conn, addr, articles)


if __name__ == "__main__":
    main()
