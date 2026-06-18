from RabbitMqConnection import RabbitMqConsumer, RabbitMqProducer


class BiddingProcessor:
    """
    Microserviciu BiddingProcessor — primeste ofertele sortate de la MessageProcessor,
    decide castigatorul (oferta maxima) si trimite rezultatul la toti bidderii.

    Neschimbat fata de lab 10.

    Principii SOLID:
      S - decizie castigator + notificare bidderi
      D - depinde de abstractizarile RabbitMq
    """

    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidding_processor.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="winner.routingkey"
        )

    def get_processed_bids(self):
        print("[BiddingProcessor] Astept ofertele procesate de MessageProcessor...")
        bids = dict()

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception:
                break

            if message is None or message == "incheiat":
                print("[BiddingProcessor] Am primit toate ofertele.")
                break

            parts = message.split("_")
            identity = parts[0].split(":")[1]
            amount = int(parts[1].split(":")[1])
            bids[identity] = amount

        self.decide_auction_winner(bids)

    def decide_auction_winner(self, bids: dict):
        if not bids:
            print("[BiddingProcessor] Nu exista nicio oferta.")
            return

        winner_id = max(bids, key=bids.get)
        winner_amount = bids[winner_id]
        print(f"[BiddingProcessor] Castigatorul: {winner_id} cu oferta {winner_amount}")

        result_message = f"winner:{winner_id}"
        for _ in range(len(bids)):
            self.producer.send_message(result_message)

    def run(self):
        self.get_processed_bids()


if __name__ == '__main__':
    processor = BiddingProcessor()
    processor.run()
