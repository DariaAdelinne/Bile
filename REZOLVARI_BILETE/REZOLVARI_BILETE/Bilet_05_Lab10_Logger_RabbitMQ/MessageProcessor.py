from RabbitMqConnection import RabbitMqProducer, RabbitMqConsumer


class MessageProcessor:
    """
    Microserviciu MessageProcessor — primeste ofertele de la Auctioneer,
    elimina duplicatele, sorteaza dupa valoare si le trimite la BiddingProcessor.

    Modificare fata de lab: trimite log-uri catre LoggerProcessor.

    Principii SOLID:
      S - singura responsabilitate: deduplicare + sortare oferte
      D - depinde de abstractizarile RabbitMq, nu de pika direct
    """
    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="message_processor.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="biddingprocessor.routingkey"
        )
        self.logger = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="bidding_logger.routingkey"
        )
        # Dict de forma {identity: amount} — cheia unica elimina duplicatele
        self.bids = dict()

    def get_and_process_messages(self):
        print("[MessageProcessor] Astept ofertele si semnalul de incheiere de la Auctioneer...")
        self.logger.send_message("info:[MessageProcessor] Astept ofertele si semnalul de incheiere de la Auctioneer...")

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception as e:
                self.logger.send_message("except:[MessageProcessor] Timeout asteptand mesaje: {}".format(e))
                break

            if message is None:
                break

            if message == "incheiat":
                print("[MessageProcessor] Licitatie incheiata. Procesez ofertele...")
                self.logger.send_message("info:[MessageProcessor] Licitatie incheiata. Procesez ofertele...")
                break

            # Formatul: "id:<UUID>_amount:<SUMA>"
            parts = message.split("_")
            identity = parts[0].split(":")[1]
            amount = int(parts[1].split(":")[1])

            # Deduplicare: daca identity exista deja, nu il adaugam din nou
            if identity not in self.bids:
                self.bids[identity] = amount
                self.logger.send_message("info:[MessageProcessor] Oferta acceptata: {} -> {}".format(identity, amount))
            else:
                self.logger.send_message("info:[MessageProcessor] Duplicat ignorat: {} -> {}".format(identity, amount))

        # Sortare dupa suma (descrescator - cel mai mare pret primul)
        sorted_bids = sorted(self.bids.items(), key=lambda x: x[1], reverse=True)
        self.finish_processing(sorted_bids)

    def finish_processing(self, sorted_bids):
        print("[MessageProcessor] Procesarea s-a incheiat! Trimit ofertele sortate:")
        self.logger.send_message("info:[MessageProcessor] Procesarea s-a incheiat! Trimit ofertele sortate:")

        for identity, amount in sorted_bids:
            print("[MessageProcessor] {} a licitat {}.".format(identity, amount))
            self.logger.send_message("info:[MessageProcessor] {} a licitat {}.".format(identity, amount))
            self.producer.send_message("id:{}_amount:{}".format(identity, amount))

        # Anunta BiddingProcessor ca procesarea s-a incheiat
        self.producer.send_message("incheiat")
        self.logger.send_message("info:[MessageProcessor] Trimitere finalizata.")

    def run(self):
        self.get_and_process_messages()


if __name__ == '__main__':
    message_processor = MessageProcessor()
    message_processor.run()
