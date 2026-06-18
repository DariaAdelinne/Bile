from RabbitMqConnection import RabbitMqConsumer, RabbitMqProducer
from MessageEvent import MessageEvent


class BiddingProcessor:
    """
    Microserviciu BiddingProcessor — decide castigatorul si notifica bidderii.

    Modificare fata de lab 10: raporteaza fiecare mesaj primit/trimis
    si trimite semnalul 'stop:stop' dupa adjudecare pentru a declansa
    scrierea statisticilor.
    """

    SOURCE = "BiddingProcessor"

    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidding_processor.queue")
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="winner.routingkey"
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

    def get_processed_bids(self):
        print(f"[{self.SOURCE}] Astept ofertele procesate de MessageProcessor...")
        bids = dict()

        while True:
            try:
                message = self.consumer.receive_message()
            except Exception as e:
                self._report("RECEIVED", f"Timeout: {e}")
                break

            if message is None or message == "incheiat":
                self._report("RECEIVED", "Semnal 'incheiat' de la MessageProcessor")
                print(f"[{self.SOURCE}] Am primit toate ofertele.")
                break

            try:
                parts = message.split("_")
                identity = parts[0].split(":")[1]
                amount = int(parts[1].split(":")[1])
                bids[identity] = amount
                self._report("RECEIVED", f"Oferta procesata: {identity} -> {amount}")
            except (IndexError, ValueError):
                self._report("RECEIVED", f"Mesaj invalid: '{message}'")

        self.decide_auction_winner(bids)

    def decide_auction_winner(self, bids: dict):
        if not bids:
            print(f"[{self.SOURCE}] Nu exista nicio oferta.")
            self.stats_reporter.send_message("stop:stop")
            return

        winner_id = max(bids, key=bids.get)
        winner_amount = bids[winner_id]
        print(f"[{self.SOURCE}] Castigatorul: {winner_id} cu oferta {winner_amount}")

        result_message = f"winner:{winner_id}"
        for _ in range(len(bids)):
            self.producer.send_message(result_message)
            self._report("SENT", f"Rezultat trimis catre un Bidder: winner={winner_id}")

        print(f"[{self.SOURCE}] Adjudecare completa. Trimit stop catre MessageStatisticsProcessor.")
        # Adjudecare completa — semnalizeaza scriera statisticilor
        self.stats_reporter.send_message("stop:stop")

    def run(self):
        self.get_processed_bids()


if __name__ == '__main__':
    processor = BiddingProcessor()
    processor.run()
