from RabbitMqConnection import RabbitMqConsumer, RabbitMqProducer


class BiddingProcessor:
    """
    Microserviciu BiddingProcessor — primeste ofertele sortate de la MessageProcessor,
    decide castigatorul (oferta maxima) si trimite rezultatul la toti bidderii.

    Modificare fata de lab: trimite log-uri catre LoggerProcessor.

    Principii SOLID:
      S - singura responsabilitate: decizie castigator + notificare
      D - depinde de abstractizarile RabbitMq, nu de pika direct
    """
    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidding_processor.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="winner.routingkey"
        )
        self.logger = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="bidding_logger.routingkey"
        )

    def get_processed_bids(self):
        print("[BiddingProcessor] Astept ofertele procesate de MessageProcessor...")
        self.logger.send_message("info:[BiddingProcessor] Astept ofertele procesate de MessageProcessor...")

        bids = dict()  # {identity: amount}

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception as e:
                self.logger.send_message("except:[BiddingProcessor] Timeout: {}".format(e))
                break

            if message is None:
                break

            if message == "incheiat":
                print("[BiddingProcessor] Am primit toate ofertele.")
                self.logger.send_message("info:[BiddingProcessor] Am primit toate ofertele.")
                break

            # Formatul: "id:<UUID>_amount:<SUMA>"
            parts = message.split("_")
            identity = parts[0].split(":")[1]
            amount = int(parts[1].split(":")[1])
            bids[identity] = amount

        self.decide_auction_winner(bids)

    def decide_auction_winner(self, bids: dict):
        print("[BiddingProcessor] Procesez ofertele...")
        self.logger.send_message("info:[BiddingProcessor] Procesez ofertele...")

        if not bids:
            print("[BiddingProcessor] Nu exista nicio oferta de procesat.")
            self.logger.send_message("info:[BiddingProcessor] Nu exista nicio oferta.")
            return

        # Castigatorul = ofertantul cu suma maxima
        winner_id = max(bids, key=bids.get)
        winner_amount = bids[winner_id]

        print("[BiddingProcessor] Castigatorul este: {} cu oferta {}".format(winner_id, winner_amount))
        self.logger.send_message(
            "info:[BiddingProcessor] Castigatorul: {} - oferta: {}".format(winner_id, winner_amount)
        )

        # Trimite rezultatul catre toti bidderii
        # Fiecare bidder compara winner_id cu propriul my_id
        result_message = "winner:{}".format(winner_id)
        for _ in range(len(bids)):
            self.producer.send_message(result_message)

        self.logger.send_message("info:[BiddingProcessor] Rezultat trimis la {} bidderi.".format(len(bids)))

    def run(self):
        self.get_processed_bids()


if __name__ == '__main__':
    bidding_processor = BiddingProcessor()
    bidding_processor.run()
