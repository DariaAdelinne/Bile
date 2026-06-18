from RabbitMqConnection import RabbitMqConsumer, RabbitMqProducer
from ErrorEvent import ErrorEvent
from ErrorType import ErrorType


class Auctioneer:
    """
    Microserviciu Auctioneer — colecteaza ofertele bidderilor.

    Modificare fata de lab 10: raporteaza erorile de comunicare
    (timeout, mesaje cu format invalid) catre ErrorStatisticsProcessor.

    Principii SOLID:
      S - colectare oferte + notificare MessageProcessor
      D - depinde de abstractizarile RabbitMq
    """

    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidder.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="messageprocessor.routingkey"
        )
        self.error_reporter = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="error_stats.routingkey"
        )

    def _report_error(self, error_type: ErrorType, details: str):
        event = ErrorEvent(error_type, "Auctioneer", details)
        try:
            self.error_reporter.send_message(event.serialize())
        except Exception:
            pass  # nu blocam licitatia daca reporterul nu e disponibil

    def receive_bids(self):
        print("[Auctioneer] Astept oferte pentru licitatie...")

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception as e:
                # timeout -> licitatia s-a incheiat natural (nu e eroare critica)
                print("[Auctioneer] Timeout - nu mai sunt oferte.")
                break

            if message is None:
                break

            try:
                parts = message.split("_")
                identity = parts[0].split(":")[1]
                amount = parts[1].split(":")[1]
                print(f"[Auctioneer] {identity} a licitat {amount}")
                self.producer.send_message(message)
            except (IndexError, ValueError) as e:
                details = f"Mesaj cu format invalid: '{message}' - {e}"
                print(f"[Auctioneer] {details}")
                self._report_error(ErrorType.INVALID_MESSAGE, details)

        self.finish_auction()

    def finish_auction(self):
        print("[Auctioneer] Licitatia s-a incheiat!")
        try:
            self.producer.send_message("incheiat")
        except Exception as e:
            self._report_error(ErrorType.QUEUE_ERROR, f"Nu am putut trimite 'incheiat': {e}")

    def run(self):
        self.receive_bids()


if __name__ == '__main__':
    auctioneer = Auctioneer()
    auctioneer.run()
