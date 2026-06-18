import os
import random
import socket
import time
import uuid
from shared import SESSION_ID, TOPIC_BIDS, TOPIC_RESULT, AUCTION_TYPE, producer, consumer, metric

# Identificator unic pentru ofertant; daca nu exista in mediu, se construieste automat
BIDDER_ID = os.getenv("BIDDER_ID", f"Bidder-{socket.gethostname()}-{uuid.uuid4().hex[:6]}")
# Intervalul din care se alege suma licitata
MIN_BID = int(os.getenv("MIN_BID", "1000"))
MAX_BID = int(os.getenv("MAX_BID", "10000"))
# Probabilitatea cu care trimitem aceeasi oferta de doua ori, pentru a testa filtrarea duplicatelor
DUPLICATE_CHANCE = float(os.getenv("DUPLICATE_CHANCE", "0.15"))

def main():
    # Asteptare aleatoare ca ofertantii sa nu trimita toti in acelasi moment
    time.sleep(random.uniform(0.5, 7.0))
    # Generam o suma aleatoare si un id unic pentru oferta
    amount = random.randint(MIN_BID, MAX_BID)
    bid_id = str(uuid.uuid4())
    # Payload-ul este mesajul care va fi trimis in Kafka
    payload = {
        "bid_id": bid_id,
        "session_id": SESSION_ID,
        "auction_type": AUCTION_TYPE,
        "identity": BIDDER_ID,
        "amount": amount,
        "message": "licitez",
        "ts": time.time(),
    }
    # Cream producerul Kafka si trimitem oferta in topicul de bids
    p = producer()
    print(f"[{BIDDER_ID}] Trimit oferta {amount} pentru {AUCTION_TYPE}", flush=True)
    p.send(TOPIC_BIDS, key=SESSION_ID, value=payload)
    # Uneori trimitem acelasi payload inca o data, ca sa demonstram ca procesorul elimina duplicatele
    if random.random() < DUPLICATE_CHANCE:
        print(f"[{BIDDER_ID}] Trimit duplicat controlat", flush=True)
        p.send(TOPIC_BIDS, key=SESSION_ID, value=payload)
    # Flush forteaza trimiterea mesajelor, apoi inchidem conexiunea producerului
    p.flush(); p.close()
    # Trimitem o metrica pentru dashboard
    metric("bid_sent", bidder=BIDDER_ID, amount=amount)

    # Dupa ce a licitat, bidder-ul asculta topicul de rezultat ca sa afle daca a castigat
    c = consumer(TOPIC_RESULT, group_id=f"result-{BIDDER_ID}", timeout_ms=60000)
    print(f"[{BIDDER_ID}] Astept rezultatul...", flush=True)
    for msg in c:
        result = msg.value
        # Luam in calcul doar rezultatul pentru sesiunea curenta
        if result.get("session_id") == SESSION_ID:
            # Comparam castigatorul cu identitatea acestui bidder
            if result.get("winner_id") == BIDDER_ID:
                print(f"[{BIDDER_ID}] Am castigat! Plata={result.get('paid_amount')}; regula={result.get('rule')}", flush=True)
            else:
                print(f"[{BIDDER_ID}] Am pierdut. Castigator={result.get('winner_id')} suma={result.get('winner_amount')}", flush=True)
            c.close()
            return
    # Daca nu apare rezultat in timeout, inchidem consumatorul si afisam mesajul
    print(f"[{BIDDER_ID}] Nu am primit rezultat in timp util.", flush=True)
    c.close()

if __name__ == "__main__":
    main()
