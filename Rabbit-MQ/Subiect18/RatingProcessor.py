from RabbitMqConnection import RabbitMqConsumer
from IRatingStorage import IRatingStorage
from FileRatingStorage import FileRatingStorage


class RatingProcessor:
    """
    Microserviciu NOU — RatingProcessor.

    Responsabilitate (SRP): primeste evaluarile utilizatorilor prin RabbitMQ
    si le persista folosind IRatingStorage.

    Functionare:
      - Asculta pe coada 'rating.queue'
      - Fiecare mesaj are formatul: 'username:rating'
        (ex: 'alice:4', 'bob:2')
      - Valideaza ca rating-ul este intre 1 si 5
      - Salveaza in fisier prin IRatingStorage
      - Mesajul 'stop:stop' opreste procesorul

    Principii SOLID:
      S - singura responsabilitate: receptie mesaje + delegare catre storage
      O - comportamentul de stocare se poate extinde fara a modifica aceasta clasa
      D - depinde de IRatingStorage (abstractizare), nu de FileRatingStorage direct
    """

    def __init__(self, storage: IRatingStorage = None):
        self.consumer = RabbitMqConsumer(rabbit_queue="rating.queue")
        # Injectie de dependenta (DIP): storage-ul poate fi orice implementare a IRatingStorage
        self.storage = storage if storage is not None else FileRatingStorage()

    def process_ratings(self):
        print("[RatingProcessor] Pornit. Astept evaluari de la utilizatori...")

        while True:
            received = self.consumer.receive_message_infinite_tries()

            if received is None:
                continue

            # Mesaj de oprire
            if received == "stop:stop":
                print("[RatingProcessor] Semnal stop primit. Inchidere.")
                break

            # Parsare format "username:rating"
            parts = received.split(":", 1)
            if len(parts) != 2:
                print(f"[RatingProcessor] Format invalid ignorat: '{received}'")
                continue

            username = parts[0].strip()
            rating_str = parts[1].strip()

            try:
                rating = int(rating_str)
                if not (1 <= rating <= 5):
                    raise ValueError("Rating in afara intervalului [1, 5]")
            except ValueError as e:
                print(f"[RatingProcessor] Evaluare invalida de la '{username}': {e}")
                continue

            self.storage.save(username, rating)
            print(f"[RatingProcessor] Evaluare procesata: {username} -> {rating}/5")

        print("[RatingProcessor] Evaluarile au fost salvate in ratings.txt")

    def run(self):
        self.process_ratings()


if __name__ == "__main__":
    processor = RatingProcessor()
    processor.run()
