from RabbitMqConnection import RabbitMqProducer, RabbitMqConsumer
from ErrorEvent import ErrorEvent
from ErrorType import ErrorType


class MessageProcessor:
    """
    Microserviciu MessageProcessor — deduplicare + sortare oferte.

    Modificare fata de lab 10: raporteaza ofertele duplicate si erorile
    de comunicare catre ErrorStatisticsProcessor.

    Principii SOLID:
      S - deduplicare + sortare
      D - depinde de abstractizarile RabbitMq
    """

    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="message_processor.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="biddingprocessor.routingkey"
        )
        self.error_reporter = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="error_stats.routingkey"
        )
        self.bids = dict()

    def _report_error(self, error_type: ErrorType, details: str):
        event = ErrorEvent(error_type, "MessageProcessor", details)
        try:
            self.error_reporter.send_message(event.serialize())
        except Exception:
            pass

    def get_and_process_messages(self):
        print("[MessageProcessor] Astept ofertele si semnalul de incheiere de la Auctioneer...")

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception as e:
                details = f"Timeout asteptand mesaje de la Auctioneer: {e}"
                print(f"[MessageProcessor] {details}")
                self._report_error(ErrorType.COMMUNICATION_ERROR, details)
                break

            if message is None or message == "incheiat":
                print("[MessageProcessor] Licitatie incheiata. Procesez ofertele...")
                break

            try:
                parts = message.split("_")
                identity = parts[0].split(":")[1]
                amount = int(parts[1].split(":")[1])

                if identity not in self.bids:
                    self.bids[identity] = amount
                    print(f"[MessageProcessor] Oferta acceptata: {identity} -> {amount}")
                else:
                    details = f"Oferta duplicata de la {identity} (suma: {amount})"
                    print(f"[MessageProcessor] {details}")
                    self._report_error(ErrorType.DUPLICATE_BID, details)

            except (IndexError, ValueError) as e:
                details = f"Mesaj cu format invalid: '{message}' - {e}"
                print(f"[MessageProcessor] {details}")
                self._report_error(ErrorType.INVALID_MESSAGE, details)

        sorted_bids = sorted(self.bids.items(), key=lambda x: x[1], reverse=True)
        self.finish_processing(sorted_bids)

    def finish_processing(self, sorted_bids):
        print("[MessageProcessor] Trimit ofertele sortate catre BiddingProcessor:")
        for identity, amount in sorted_bids:
            print(f"  {identity} -> {amount}")
            try:
                self.producer.send_message(f"id:{identity}_amount:{amount}")
            except Exception as e:
                self._report_error(ErrorType.QUEUE_ERROR,
                                   f"Nu am putut trimite oferta {identity}: {e}")
        try:
            self.producer.send_message("incheiat")
        except Exception as e:
            self._report_error(ErrorType.QUEUE_ERROR,
                               f"Nu am putut trimite 'incheiat' catre BiddingProcessor: {e}")

    def run(self):
        self.get_and_process_messages()


if __name__ == '__main__':
    processor = MessageProcessor()
    processor.run()
