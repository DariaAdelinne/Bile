from RabbitMqConnection import RabbitMqConsumer, RabbitMqProducer


class Auctioneer:
    """
    Microserviciu Auctioneer — colecteaza ofertele bidderilor si le trimite
    la MessageProcessor dupa expirarea timpului de licitatie.

    Neschimbat fata de lab 10 (fara modificari legate de rating).

    Principii SOLID:
      S - singura responsabilitate: colectare oferte + notificare MessageProcessor
      D - depinde de abstractizarile RabbitMq
    """

    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidder.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="messageprocessor.routingkey"
        )

    def receive_bids(self):
        print("[Auctioneer] Astept oferte pentru licitatie...")

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception:
                print("[Auctioneer] Timeout - nu mai sunt oferte. Inchid licitatia.")
                break

            if message is None:
                break

            parts = message.split("_")
            identity = parts[0].split(":")[1]
            amount = parts[1].split(":")[1]
            print(f"[Auctioneer] {identity} a licitat {amount}")
            self.producer.send_message(message)

        self.finish_auction()

    def finish_auction(self):
        print("[Auctioneer] Licitatia s-a incheiat!")
        self.producer.send_message("incheiat")

    def run(self):
        self.receive_bids()


if __name__ == '__main__':
    auctioneer = Auctioneer()
    auctioneer.run()
