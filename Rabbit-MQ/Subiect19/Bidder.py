from random import randint
from uuid import uuid4
from RabbitMqConnection import RabbitMqProducer, RabbitMqConsumer
from ErrorEvent import ErrorEvent
from ErrorType import ErrorType


class Bidder:
    """
    Microserviciu Bidder — trimite oferta si asteapta rezultatul.

    Modificare fata de lab 10: raporteaza erorile de comunicare catre
    ErrorStatisticsProcessor (ex: nu poate trimite oferta).

    Principii SOLID:
      S - licitare + primire rezultat
      D - depinde de RabbitMqProducer/Consumer
    """

    def __init__(self):
        self.bid_producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="bidder.routingkey"
        )
        self.winner_consumer = RabbitMqConsumer(rabbit_queue="winner.queue")
        self.error_reporter = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="error_stats.routingkey"
        )
        self.my_bid = randint(1000, 10_000)
        self.my_id = uuid4()

    def _report_error(self, error_type: ErrorType, details: str):
        event = ErrorEvent(error_type, f"Bidder-{str(self.my_id)[:8]}", details)
        try:
            self.error_reporter.send_message(event.serialize())
        except Exception:
            pass

    def bid(self):
        print(f"[Bidder {self.my_id}] Trimit oferta: {self.my_bid}")
        bid_message = f"id:{self.my_id}_amount:{self.my_bid}"

        try:
            self.bid_producer.send_message(bid_message)
        except Exception as e:
            self._report_error(ErrorType.COMMUNICATION_ERROR,
                               f"Nu am putut trimite oferta: {e}")
            return

        # Simulare duplicat: sansa din 2 sa trimita de doua ori
        if randint(0, 1) == 1:
            try:
                self.bid_producer.send_message(bid_message)
                print(f"[Bidder {self.my_id}] Oferta trimisa de 2 ori (simulare duplicat)")
            except Exception as e:
                self._report_error(ErrorType.QUEUE_ERROR,
                                   f"Eroare la retransmiterea ofertei: {e}")

    def get_winner(self):
        print(f"[Bidder {self.my_id}] Astept rezultatul licitatiei...")
        try:
            result = self.winner_consumer.receive_message_infinite_tries()
            winner_id = result.split(":")[1]
            if winner_id == str(self.my_id):
                print(f"[Bidder {self.my_id}] Am CASTIGAT! Oferta: {self.my_bid}")
            else:
                print(f"[Bidder {self.my_id}] Am pierdut.")
        except Exception as e:
            self._report_error(ErrorType.COMMUNICATION_ERROR,
                               f"Nu am putut primi rezultatul licitatiei: {e}")

    def run(self):
        self.bid()
        self.get_winner()


if __name__ == '__main__':
    bidder = Bidder()
    bidder.run()
