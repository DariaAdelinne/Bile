from RabbitMqConnection import RabbitMqProducer, RabbitMqConsumer


class MessageProcessor:
    """
    Microserviciu MessageProcessor — deduplicare + sortare oferte.
    Trimite erori catre ErrorProcessor.

    Principii SOLID:
      S - singura responsabilitate: procesare (dedup + sort) oferte
    """
    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="message_processor.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="biddingprocessor.routingkey"
        )
        self.err = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="error.routingkey"
        )
        self.bids = dict()  # {identity: amount} - deduplicare automata prin cheie unica

    def get_and_process_messages(self):
        print("[MessageProcessor] Astept ofertele si semnalul de incheiere...")

        while True:
            try:
                message = self.consumer.receive_message_infinite_tries()
            except Exception as e:
                self.err.send_message("COADA|MessageProcessor|Timeout: {}".format(str(e)))
                break

            if message is None:
                break

            if message == "incheiat":
                print("[MessageProcessor] Licitatie incheiata. Procesez...")
                break

            try:
                parts = message.split("_")
                identity = parts[0].split(":")[1]
                amount = int(parts[1].split(":")[1])

                if identity not in self.bids:
                    self.bids[identity] = amount
                else:
                    # Duplicat detectat - trimite eroare de tip COADA
                    self.err.send_message(
                        "COADA|MessageProcessor|Duplicat detectat si ignorat pentru: {}".format(identity)
                    )
            except Exception as e:
                self.err.send_message("COMUNICARE|MessageProcessor|Eroare parsare '{}': {}".format(message, str(e)))

        # Sortare descrescatoare dupa suma
        sorted_bids = sorted(self.bids.items(), key=lambda x: x[1], reverse=True)
        self.finish_processing(sorted_bids)

    def finish_processing(self, sorted_bids):
        print("[MessageProcessor] Procesarea s-a incheiat! Trimit {} oferte unice:".format(len(sorted_bids)))
        try:
            for identity, amount in sorted_bids:
                print("  {} -> {}".format(identity, amount))
                self.producer.send_message("id:{}_amount:{}".format(identity, amount))
            self.producer.send_message("incheiat")
        except Exception as e:
            self.err.send_message("COMUNICARE|MessageProcessor|Eroare trimitere catre BiddingProcessor: {}".format(str(e)))

    def run(self):
        self.get_and_process_messages()


if __name__ == '__main__':
    message_processor = MessageProcessor()
    message_processor.run()
