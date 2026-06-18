from RabbitMqConnection import RabbitMqConsumer
from IMessageStatsStorage import IMessageStatsStorage
from FileMessageStatsStorage import FileMessageStatsStorage
from MessageEvent import MessageEvent


class MessageStatisticsProcessor:
    """
    Microserviciu NOU — MessageStatisticsProcessor.

    Responsabilitate (SRP): colecteaza evenimentele de mesaje raportate
    de celelalte microservicii si, la adjudecare (semnal 'stop:stop'),
    scrie statisticile intr-un fisier local.

    Protocol mesaje primite pe 'msg_stats.queue':
      - '<source>|<direction>|<details>|<timestamp>' — un eveniment de mesaj
      - 'stop:stop'                                  — licitatia s-a incheiat

    Principii SOLID:
      S - colectare evenimente + delegare scriere catre IMessageStatsStorage
      O - noi tipuri de directii/surse se adauga fara modificari aici
      D - depinde de IMessageStatsStorage (abstractizare)
    """

    def __init__(self, storage: IMessageStatsStorage = None):
        self.consumer = RabbitMqConsumer(rabbit_queue="msg_stats.queue")
        self.storage = storage if storage is not None else FileMessageStatsStorage()
        self.collected_events = []

    def collect_events(self):
        print("[MessageStatisticsProcessor] Pornit. Colectez statistici pana la adjudecare...")

        while True:
            received = self.consumer.receive_message_infinite_tries()

            if received is None:
                continue

            if received == "stop:stop":
                print(f"[MessageStatisticsProcessor] Adjudecare. "
                      f"Total mesaje colectate: {len(self.collected_events)}")
                self.storage.write_statistics(self.collected_events)
                break

            try:
                event = MessageEvent.deserialize(received)
                self.collected_events.append(event)
                print(f"[MessageStatisticsProcessor] Eveniment: {event}")
            except Exception as e:
                print(f"[MessageStatisticsProcessor] Eroare deserializare: {e}")

    def run(self):
        self.collect_events()


if __name__ == "__main__":
    processor = MessageStatisticsProcessor()
    processor.run()
