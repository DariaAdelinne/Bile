from RabbitMqConnection import RabbitMqProducer, RabbitMqConsumer


class MessageProcessor:
    """
    Microserviciu MessageProcessor — primeste ofertele de la Auctioneer,
    elimina duplicatele, sorteaza dupa valoare si le trimite la BiddingProcessor.

    Neschimbat fata de lab 10.

    Principii SOLID:
      S - deduplicare + sortare oferte
      D - depinde de abstractizarile RabbitMq
    """

    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="message_processor.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="biddingprocessor.routingkey"
        )
        self.bids = dict()

    def get_and_process_messages(self):
        print("[MessageProcessor] Astept ofertele si semnalul de incheiere de la Auctioneer...")

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception:
                break

            if message is None or message == "incheiat":
                print("[MessageProcessor] Licitatie incheiata. Procesez ofertele...")
                break

            parts = message.split("_")
            identity = parts[0].split(":")[1]
            amount = int(parts[1].split(":")[1])

            if identity not in self.bids:
                self.bids[identity] = amount
                print(f"[MessageProcessor] Oferta acceptata: {identity} -> {amount}")
            else:
                print(f"[MessageProcessor] Duplicat ignorat: {identity} -> {amount}")

        sorted_bids = sorted(self.bids.items(), key=lambda x: x[1], reverse=True)
        self.finish_processing(sorted_bids)

    def finish_processing(self, sorted_bids):
        print("[MessageProcessor] Trimit ofertele sortate catre BiddingProcessor:")
        for identity, amount in sorted_bids:
            print(f"  {identity} -> {amount}")
            self.producer.send_message(f"id:{identity}_amount:{amount}")
        self.producer.send_message("incheiat")

    def run(self):
        self.get_and_process_messages()


if __name__ == '__main__':
    processor = MessageProcessor()
    processor.run()
