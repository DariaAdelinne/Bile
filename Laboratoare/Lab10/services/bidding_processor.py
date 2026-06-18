from shared import SESSION_ID, AUCTION_TYPE, TOPIC_PROCESSED, TOPIC_RESULT, consumer, producer, choose_winner, metric

def main():
    # Acest serviciu asteapta ofertele deja curatate de MessageProcessor
    print(f"[BiddingProcessor] Astept oferte procesate. Tip licitatie={AUCTION_TYPE}", flush=True)
    c = consumer(TOPIC_PROCESSED, group_id="bidding-processor", timeout_ms=45000)
    # Lista in care se aduna toate ofertele procesate pentru sesiunea curenta
    bids = []
    for msg in c:
        bid = msg.value
        # Ignoram mesajele din alte sesiuni
        if bid.get("session_id") == SESSION_ID:
            bids.append(bid)
            print(f"[BiddingProcessor] Oferta: {bid.get('identity')} -> {bid.get('amount')}", flush=True)
    c.close()
    # Alegem castigatorul dupa regula licitatiei: english, dutch, swedish sau candle
    result = choose_winner(bids, AUCTION_TYPE)
    # Completam rezultatul cu informatii utile pentru ceilalti consumatori
    result.update({"session_id": SESSION_ID, "auction_type": AUCTION_TYPE, "total_bids": len(bids)})
    print(f"[BiddingProcessor] Rezultat: {result}", flush=True)
    # Publicam rezultatul final in topicul dedicat
    p = producer()
    p.send(TOPIC_RESULT, key=SESSION_ID, value=result)
    p.flush(); p.close()
    # Trimitem metrica finala pentru monitorizare
    metric("winner_decided", **result)

if __name__ == "__main__":
    main()
