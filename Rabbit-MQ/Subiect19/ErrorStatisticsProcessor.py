from RabbitMqConnection import RabbitMqConsumer
from IErrorStorage import IErrorStorage
from FileErrorStorage import FileErrorStorage
from ErrorEvent import ErrorEvent


class ErrorStatisticsProcessor:
    """
    Microserviciu NOU — ErrorStatisticsProcessor.

    Responsabilitate (SRP): colecteaza erorile raportate de celelalte
    microservicii pe parcursul unei licitatii si, la adjudecare
    (semnalul 'stop:stop'), scrie statisticile intr-un fisier local.

    Protocol mesaje primite pe 'error_stats.queue':
      - '<ErrorType.name>|<sursa>|<detalii>|<timestamp>' — o eroare
      - 'stop:stop'                                       — licitatia s-a incheiat

    Principii SOLID:
      S - colectare erori + delegare scriere catre IErrorStorage
      O - noi tipuri de erori (ErrorType) se adauga fara modificari aici
      D - depinde de IErrorStorage (abstractizare), nu de FileErrorStorage direct
    """

    def __init__(self, storage: IErrorStorage = None):
        self.consumer = RabbitMqConsumer(rabbit_queue="error_stats.queue")
        self.storage = storage if storage is not None else FileErrorStorage()
        self.collected_errors = []

    def collect_errors(self):
        print("[ErrorStatisticsProcessor] Pornit. Colectez erori pana la adjudecare...")

        while True:
            received = self.consumer.receive_message_infinite_tries()

            if received is None:
                continue

            if received == "stop:stop":
                print(f"[ErrorStatisticsProcessor] Licitatie incheiata. "
                      f"Total erori colectate: {len(self.collected_errors)}")
                self.storage.write_statistics(self.collected_errors)
                break

            try:
                event = ErrorEvent.deserialize(received)
                self.collected_errors.append(event)
                print(f"[ErrorStatisticsProcessor] Eroare inregistrata: {event}")
            except Exception as e:
                print(f"[ErrorStatisticsProcessor] Eroare la deserializare: {e}")

    def run(self):
        self.collect_errors()


if __name__ == "__main__":
    processor = ErrorStatisticsProcessor()
    processor.run()
