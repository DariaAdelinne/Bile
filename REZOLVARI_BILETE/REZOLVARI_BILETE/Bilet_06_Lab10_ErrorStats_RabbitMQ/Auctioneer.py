from RabbitMqConnection import RabbitMqConsumer, RabbitMqProducer


class Auctioneer:
    """
    Microserviciu Auctioneer — colecteaza ofertele si le trimite la MessageProcessor.
    Trimite erori catre ErrorProcessor cu formatul: "TIP_EROARE|Sursa|Detalii"

    Principii SOLID:
      S - singura responsabilitate: colectare oferte
      D - depinde de abstractizarile RabbitMq
    """
    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidder.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="messageprocessor.routingkey"
        )
        self.err = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="error.routingkey"
        )

    def receive_bids(self):
        print("[Auctioneer] Astept oferte pentru licitatie...")

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception as e:
                # Timeout epuizat -> licitatia s-a incheiat
                self.err.send_message("COADA|Auctioneer|Timeout asteptare oferte: {}".format(str(e)))
                break

            if message is None:
                break

            try:
                parts = message.split("_")
                identity = parts[0].split(":")[1]
                amount = int(parts[1].split(":")[1])
                print("[Auctioneer] {} a licitat {}".format(identity, amount))
                self.producer.send_message(message)
            except Exception as e:
                self.err.send_message("COMUNICARE|Auctioneer|Eroare parsare mesaj '{}': {}".format(message, str(e)))

        self.finish_auction()

    def finish_auction(self):
        print("[Auctioneer] Licitatia s-a incheiat!")
        try:
            self.producer.send_message("incheiat")
        except Exception as e:
            self.err.send_message("COMUNICARE|Auctioneer|Eroare trimitere 'incheiat': {}".format(str(e)))

    def run(self):
        try:
            self.receive_bids()
        except Exception as e:
            self.err.send_message("COMUNICARE|Auctioneer|Eroare generala: {}".format(str(e)))


if __name__ == '__main__':
    auctioneer = Auctioneer()
    auctioneer.run()
