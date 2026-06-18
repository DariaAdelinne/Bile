from RabbitMqConnection import RabbitMqConsumer, RabbitMqProducer


class BiddingProcessor:
    """
    Microserviciu BiddingProcessor — decide castigatorul si scrie in result.txt.
    Trimite erori catre ErrorProcessor.
    Semnalizeaza ErrorProcessor sa se opreasca dupa adjudecare.

    Principii SOLID:
      S - singura responsabilitate: decizie castigator
    """
    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidding_processor.queue")
        self.err = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="error.routingkey"
        )
        self.bids = dict()  # {identity: amount}

    def get_processed_bids(self):
        print("[BiddingProcessor] Astept ofertele procesate de MessageProcessor...")

        while True:
            try:
                message = self.consumer.receive_message_infinite_tries()
            except Exception as e:
                self.err.send_message("COADA|BiddingProcessor|Timeout: {}".format(str(e)))
                break

            if message is None:
                break

            if message == "incheiat":
                print("[BiddingProcessor] Am primit toate ofertele.")
                break

            try:
                parts = message.split("_")
                identity = parts[0].split(":")[1]
                amount = int(parts[1].split(":")[1])
                self.bids[identity] = amount
            except Exception as e:
                self.err.send_message("COMUNICARE|BiddingProcessor|Eroare parsare '{}': {}".format(message, str(e)))

        self.decide_auction_winner()

    def decide_auction_winner(self):
        print("[BiddingProcessor] Procesez ofertele...")

        if not self.bids:
            print("[BiddingProcessor] Nu exista oferte.")
            self.err.send_message("COADA|BiddingProcessor|Nicio oferta de procesat la adjudecare")
            # Semnalizeaza ErrorProcessor sa se opreasca
            self.err.send_message("STOP|BiddingProcessor|adjudecare")
            return

        # Castigatorul = oferta maxima
        winner_id = max(self.bids, key=self.bids.get)
        winner_amount = self.bids[winner_id]

        print("[BiddingProcessor] Castigatorul: {} cu oferta {}".format(winner_id, winner_amount))

        # Scrie rezultatul in result.txt (Bidder-ii il citesc de acolo)
        try:
            with open("result.txt", "w") as f:
                f.write("winner:{}".format(winner_id))
        except Exception as e:
            self.err.send_message("COMUNICARE|BiddingProcessor|Eroare scriere result.txt: {}".format(str(e)))

        # Semnalizeaza ErrorProcessor ca adjudecarea s-a incheiat -> poate scrie statisticile
        self.err.send_message("STOP|BiddingProcessor|Licitatie adjudecata. Castigator: {}".format(winner_id))

    def run(self):
        self.get_processed_bids()


if __name__ == '__main__':
    bidding_processor = BiddingProcessor()
    bidding_processor.run()
