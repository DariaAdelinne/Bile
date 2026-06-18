from RabbitMqConnection import RabbitMqProducer, RabbitMqConsumer
from MessageEvent import MessageEvent


class MessageProcessor:
    """
    Microserviciu MessageProcessor — deduplicare + sortare oferte.

    Modificare fata de lab 10: raporteaza fiecare mesaj primit/trimis
    catre MessageStatisticsProcessor.
    """

    SOURCE = "MessageProcessor"

    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="message_processor.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="biddingprocessor.routingkey"
        )
        self.stats_reporter = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="msg_stats.routingkey"
        )
        self.bids = dict()

    def _report(self, direction: str, details: str):
        event = MessageEvent(self.SOURCE, direction, details)
        try:
            self.stats_reporter.send_message(event.serialize())
        except Exception:
            pass

    def get_and_process_messages(self):
        print(f"[{self.SOURCE}] Astept ofertele si semnalul de incheiere...")

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception as e:
                self._report("RECEIVED", f"Timeout - nu mai sunt mesaje: {e}")
                break

            if message is None or message == "incheiat":
                self._report("RECEIVED", "Semnal 'incheiat' de la Auctioneer")
                print(f"[{self.SOURCE}] Licitatie incheiata. Procesez ofertele...")
                break

            try:
                parts = message.split("_")
                identity = parts[0].split(":")[1]
                amount = int(parts[1].split(":")[1])

                self._report("RECEIVED", f"Oferta de la {identity}: {amount}")

                if identity not in self.bids:
                    self.bids[identity] = amount
                    print(f"[{self.SOURCE}] Oferta acceptata: {identity} -> {amount}")
                else:
                    print(f"[{self.SOURCE}] Duplicat ignorat: {identity} -> {amount}")

            except (IndexError, ValueError):
                self._report("RECEIVED", f"Mesaj invalid: '{message}'")

        sorted_bids = sorted(self.bids.items(), key=lambda x: x[1], reverse=True)
        self.finish_processing(sorted_bids)

    def finish_processing(self, sorted_bids):
        print(f"[{self.SOURCE}] Trimit ofertele sortate catre BiddingProcessor:")
        for identity, amount in sorted_bids:
            print(f"  {identity} -> {amount}")
            self.producer.send_message(f"id:{identity}_amount:{amount}")
            self._report("SENT", f"Oferta sortata catre BiddingProcessor: {identity} -> {amount}")
        self.producer.send_message("incheiat")
        self._report("SENT", "Semnal 'incheiat' catre BiddingProcessor")

    def run(self):
        self.get_and_process_messages()


if __name__ == '__main__':
    processor = MessageProcessor()
    processor.run()
