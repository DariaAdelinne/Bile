from RabbitMqConnection import RabbitMqConsumer, RabbitMqProducer


class Auctioneer:
    """
    Microserviciu Auctioneer — colecteaza ofertele bidderilor si le trimite
    la MessageProcessor dupa expirarea timpului de licitatie.

    Modificare fata de lab: trimite log-uri catre LoggerProcessor.

    Principii SOLID:
      S - singura responsabilitate: colectare oferte + notificare procesor
      O - logica de logging poate fi extinsa fara a modifica receive_bids()
    """
    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidder.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="messageprocessor.routingkey"
        )
        self.logger = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="bidding_logger.routingkey"
        )

    def receive_bids(self):
        print("[Auctioneer] Astept oferte pentru licitatie...")
        self.logger.send_message("info:[Auctioneer] Astept oferte pentru licitatie...")

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception as e:
                # receive_message a epuizat cele 15 incercari -> timeout, licitatia s-a incheiat
                self.logger.send_message("info:[Auctioneer] Timeout - nu mai sunt oferte.")
                break

            if message is None:
                break

            # Formatul mesajului: "id:<UUID>_amount:<SUMA>"
            parts = message.split("_")
            identity = parts[0].split(":")[1]
            amount = parts[1].split(":")[1]

            print("[Auctioneer] {} a licitat {}".format(identity, amount))
            self.logger.send_message("info:[Auctioneer] {} a licitat {}".format(identity, amount))

            # Trimite oferta mai departe catre MessageProcessor
            self.producer.send_message(message)

        self.finish_auction()

    def finish_auction(self):
        print("[Auctioneer] Licitatia s-a incheiat!")
        self.logger.send_message("info:[Auctioneer] Licitatia s-a incheiat!")

        # Notifica MessageProcessor ca poate incepe procesarea
        self.producer.send_message("incheiat")

    def run(self):
        self.receive_bids()


if __name__ == '__main__':
    auctioneer = Auctioneer()
    auctioneer.run()
