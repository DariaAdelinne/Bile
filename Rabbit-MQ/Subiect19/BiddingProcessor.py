from RabbitMqConnection import RabbitMqConsumer, RabbitMqProducer
from ErrorEvent import ErrorEvent
from ErrorType import ErrorType


class BiddingProcessor:
    """
    Microserviciu BiddingProcessor — decide castigatorul si notifica bidderii.

    Modificare fata de lab 10: raporteaza erorile de comunicare catre
    ErrorStatisticsProcessor si trimite semnalul 'stop:stop' la finalul
    licitatiei pentru a declansa scrierea statisticilor.

    Principii SOLID:
      S - decizie castigator + notificare
      D - depinde de abstractizarile RabbitMq
    """

    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidding_processor.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="winner.routingkey"
        )
        self.error_reporter = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="error_stats.routingkey"
        )

    def _report_error(self, error_type: ErrorType, details: str):
        event = ErrorEvent(error_type, "BiddingProcessor", details)
        try:
            self.error_reporter.send_message(event.serialize())
        except Exception:
            pass

    def get_processed_bids(self):
        print("[BiddingProcessor] Astept ofertele procesate de MessageProcessor...")
        bids = dict()

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception as e:
                details = f"Timeout asteptand ofertele procesate: {e}"
                print(f"[BiddingProcessor] {details}")
                self._report_error(ErrorType.COMMUNICATION_ERROR, details)
                break

            if message is None or message == "incheiat":
                print("[BiddingProcessor] Am primit toate ofertele.")
                break

            try:
                parts = message.split("_")
                identity = parts[0].split(":")[1]
                amount = int(parts[1].split(":")[1])
                bids[identity] = amount
            except (IndexError, ValueError) as e:
                details = f"Mesaj cu format invalid: '{message}' - {e}"
                print(f"[BiddingProcessor] {details}")
                self._report_error(ErrorType.INVALID_MESSAGE, details)

        self.decide_auction_winner(bids)

    def decide_auction_winner(self, bids: dict):
        if not bids:
            print("[BiddingProcessor] Nu exista nicio oferta.")
            self._report_error(ErrorType.UNKNOWN, "Nicio oferta valida primita la adjudecare.")
            # trimite stop oricum pentru a declansa scrierea statisticilor
            self.error_reporter.send_message("stop:stop")
            return

        winner_id = max(bids, key=bids.get)
        winner_amount = bids[winner_id]
        print(f"[BiddingProcessor] Castigatorul: {winner_id} cu oferta {winner_amount}")

        result_message = f"winner:{winner_id}"
        for _ in range(len(bids)):
            try:
                self.producer.send_message(result_message)
            except Exception as e:
                self._report_error(ErrorType.QUEUE_ERROR,
                                   f"Nu am putut notifica un bidder: {e}")

        # Adjudecare completa — semnalizam ErrorStatisticsProcessor sa scrie statisticile
        print("[BiddingProcessor] Adjudecare completa. Trimit semnal stop catre ErrorStatisticsProcessor.")
        self.error_reporter.send_message("stop:stop")

    def run(self):
        self.get_processed_bids()


if __name__ == '__main__':
    processor = BiddingProcessor()
    processor.run()
