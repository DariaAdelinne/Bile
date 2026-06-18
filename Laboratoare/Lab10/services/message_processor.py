import time
from shared import SESSION_ID, TOPIC_BIDS, TOPIC_FINISHED, TOPIC_PROCESSED, consumer, producer, metric

def main():
    # Mai intai asteptam semnalul ca licitatia s-a terminat
    print("[MessageProcessor] Astept notificarea de final de licitatie...", flush=True)
    finish_consumer = consumer(TOPIC_FINISHED, group_id="message-processor-finish", timeout_ms=120000)
    got_finish = False
    for msg in finish_consumer:
        # Acceptam doar notificarea pentru sesiunea curenta
        if msg.value.get("session_id") == SESSION_ID:
            got_finish = True
            print(f"[MessageProcessor] Notificare de la {msg.value.get('auctioneer')}", flush=True)
            break
    finish_consumer.close()
    # Daca nu avem mesaj de final, nu procesam ofertele ca sa evitam rezultate incomplete
    if not got_finish:
        print("[MessageProcessor] Nu a sosit notificarea de final.", flush=True)
        return

    # Colecteaza toate ofertele persistente din topic, de la inceput
    bids_consumer = consumer(TOPIC_BIDS, group_id=None, timeout_ms=5000)
    # Dictionarul retine o singura oferta pentru fiecare identity, deci elimina duplicatele
    bids_by_identity = {}
    duplicates = 0
    for msg in bids_consumer:
        bid = msg.value
        # Ignoram ofertele care nu apartin acestei sesiuni de test
        if bid.get("session_id") != SESSION_ID:
            continue
        ident = bid.get("identity")
        # Daca acelasi bidder apare deja, consideram mesajul duplicat
        if ident in bids_by_identity:
            duplicates += 1
            continue
        # Salvam si pozitia mesajului in Kafka pentru trasabilitate
        bid["partition"] = msg.partition
        bid["offset"] = msg.offset
        bids_by_identity[ident] = bid
    bids_consumer.close()

    # Sortam ofertele dupa timestamp, ca procesarea sa respecte ordinea in care au fost trimise
    sorted_bids = sorted(bids_by_identity.values(), key=lambda b: float(b["ts"]))
    print(f"[MessageProcessor] Oferte unice={len(sorted_bids)}, duplicate filtrate={duplicates}", flush=True)
    # Trimitem fiecare oferta unica in topicul de oferte procesate
    p = producer()
    for bid in sorted_bids:
        p.send(TOPIC_PROCESSED, key=SESSION_ID, value=bid)
        print(f"[MessageProcessor] Trimit oferta procesata: {bid['identity']} -> {bid['amount']}", flush=True)
    p.flush(); p.close()
    # Metrica arata cate oferte au ramas si cate duplicate au fost eliminate
    metric("messages_processed", unique=len(sorted_bids), duplicates=duplicates)

if __name__ == "__main__":
    main()
