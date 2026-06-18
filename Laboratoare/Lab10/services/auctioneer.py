import os
import socket
import time
from shared import SESSION_ID, TOPIC_BIDS, TOPIC_FINISHED, consumer, producer, metric

# Identificatorul auctioneer-ului este luat din variabila de mediu sau generat din hostname
AUCTIONEER_ID = os.getenv("AUCTIONEER_ID", f"Auctioneer-{socket.gethostname()}")
# Timpul maxim de asteptare fara mesaje noi; dupa acest timeout licitatia se considera terminata
INACTIVITY_MS = int(os.getenv("INACTIVITY_MS", "15000"))


def main():
    # Consumatorul citeste ofertele din topicul de bids, in grupul comun al auctioneer-ilor
    c = consumer(TOPIC_BIDS, group_id="auctioneers", timeout_ms=INACTIVITY_MS)
    # Count retine cate oferte valide pentru sesiunea curenta au fost vazute
    count = 0
    print(f"[{AUCTIONEER_ID}] Astept oferte. Timeout inactivitate={INACTIVITY_MS}ms", flush=True)
    # Parcurgem mesajele Kafka pana cand consumatorul nu mai primeste nimic in intervalul setat
    for msg in c:
        bid = msg.value
        # Ignoram mesajele care apartin altei sesiuni, ca sa nu amestecam rularile intre ele
        if bid.get("session_id") != SESSION_ID:
            continue
        count += 1
        print(f"[{AUCTIONEER_ID}] {bid.get('identity')} a licitat {bid.get('amount')} pe partitia {msg.partition}", flush=True)
        # Trimitem o metrica pentru monitorizare, ca sa se vada ca oferta a fost observata
        metric("bid_seen_by_auctioneer", auctioneer=AUCTIONEER_ID, partition=msg.partition)
    # Inchidem consumatorul dupa terminarea licitatiei
    c.close()
    print(f"[{AUCTIONEER_ID}] Licitatia s-a incheiat dupa inactivitate. Oferte vazute={count}", flush=True)
    # Publicam un mesaj de final, folosit de MessageProcessor ca semnal ca poate procesa ofertele
    p = producer()
    p.send(TOPIC_FINISHED, key=SESSION_ID, value={"session_id": SESSION_ID, "auctioneer": AUCTIONEER_ID, "count": count, "ts": time.time(), "message": "incheiat"})
    p.flush(); p.close()
    # Inregistram si metrica de finalizare pentru dashboard
    metric("auctioneer_finished", auctioneer=AUCTIONEER_ID, count=count)

if __name__ == "__main__":
    main()
