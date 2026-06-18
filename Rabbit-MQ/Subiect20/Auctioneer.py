from RabbitMqConnection import RabbitMqConsumer, RabbitMqProducer
from MessageEvent import MessageEvent


class Auctioneer:
    """
    Microserviciu Auctioneer — colecteaza ofertele bidderilor.

    Modificare fata de lab 10: raporteaza fiecare mesaj primit/trimis
    catre MessageStatisticsProcessor.

    Principii SOLID:
      S - colectare oferte + notificare MessageProcessor
      D - depinde de abstractizarile RabbitMq
    """

    SOURCE = "Auctioneer"

    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidder.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="messageprocessor.routingkey"
        )
        self.stats_reporter = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="msg_stats.routingkey"
        )

    def _report(self, direction: str, details: str):
        event = MessageEvent(self.SOURCE, direction, details)
        try:
            self.stats_reporter.send_message(event.serialize())
        except Exception:
            pass

    def receive_bids(self):
        print(f"[{self.SOURCE}] Astept oferte pentru licitatie...")

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception:
                print(f"[{self.SOURCE}] Timeout - nu mai sunt oferte.")
                break

            if message is None:
                break

            try:
                parts = message.split("_")
                identity = parts[0].split(":")[1]
                amount = parts[1].split(":")[1]
                print(f"[{self.SOURCE}] {identity} a licitat {amount}")
                self._report("RECEIVED", f"Oferta de la {identity}: {amount}")
                self.producer.send_message(message)
                self._report("SENT", f"Oferta redirectionata catre MessageProcessor: {identity}")
            except (IndexError, ValueError) as e:
                self._report("RECEIVED", f"Mesaj invalid ignorat: '{message}'")

        self.finish_auction()

    def finish_auction(self):
        print(f"[{self.SOURCE}] Licitatia s-a incheiat!")
        self.producer.send_message("incheiat")
        self._report("SENT", "Semnal 'incheiat' catre MessageProcessor")

    def run(self):
        self.receive_bids()


if __name__ == '__main__':
    auctioneer = Auctioneer()
    auctioneer.run()
