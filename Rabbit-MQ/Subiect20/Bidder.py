from random import randint
from uuid import uuid4
from RabbitMqConnection import RabbitMqProducer, RabbitMqConsumer
from MessageEvent import MessageEvent


class Bidder:
    """
    Microserviciu Bidder — trimite oferta si asteapta rezultatul.

    Modificare fata de lab 10: raporteaza mesajele trimise/primite
    catre MessageStatisticsProcessor.
    """

    def __init__(self):
        self.bid_producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="bidder.routingkey"
        )
        self.winner_consumer = RabbitMqConsumer(rabbit_queue="winner.queue")
        self.stats_reporter = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="msg_stats.routingkey"
        )
        self.my_bid = randint(1000, 10_000)
        self.my_id = uuid4()
        self.source = f"Bidder-{str(self.my_id)[:8]}"

    def _report(self, direction: str, details: str):
        event = MessageEvent(self.source, direction, details)
        try:
            self.stats_reporter.send_message(event.serialize())
        except Exception:
            pass

    def bid(self):
        print(f"[{self.source}] Trimit oferta: {self.my_bid}")
        bid_message = f"id:{self.my_id}_amount:{self.my_bid}"
        self.bid_producer.send_message(bid_message)
        self._report("SENT", f"Oferta catre Auctioneer: {self.my_bid}")

        # Simulare duplicat
        if randint(0, 1) == 1:
            self.bid_producer.send_message(bid_message)
            self._report("SENT", f"Oferta duplicata catre Auctioneer: {self.my_bid} (duplicat)")
            print(f"[{self.source}] Oferta trimisa de 2 ori (simulare duplicat)")

    def get_winner(self):
        print(f"[{self.source}] Astept rezultatul licitatiei...")
        result = self.winner_consumer.receive_message_infinite_tries()
        self._report("RECEIVED", f"Rezultat licitatie: {result}")

        winner_id = result.split(":")[1]
        if winner_id == str(self.my_id):
            print(f"[{self.source}] Am CASTIGAT! Oferta: {self.my_bid}")
        else:
            print(f"[{self.source}] Am pierdut.")

    def run(self):
        self.bid()
        self.get_winner()


if __name__ == '__main__':
    bidder = Bidder()
    bidder.run()
