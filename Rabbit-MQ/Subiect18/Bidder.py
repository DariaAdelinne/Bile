from random import randint
from uuid import uuid4
from RabbitMqConnection import RabbitMqProducer, RabbitMqConsumer


class Bidder:
    """
    Microserviciu Bidder — trimite o oferta la licitatie, asteapta rezultatul,
    apoi solicita utilizatorului o evaluare (1-5) si o trimite la RatingProcessor.

    Modificare fata de lab: dupa aflarea rezultatului, bidderul cere utilizatorului
    sa evalueze serviciul si trimite evaluarea la 'rating.routingkey' -> RatingProcessor.

    Principii SOLID:
      S - licitare + primire rezultat + trimitere evaluare (flux natural al unui bidder)
      D - depinde de RabbitMqProducer/Consumer, nu de pika direct
    """

    def __init__(self):
        self.bid_producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="bidder.routingkey"
        )
        self.winner_consumer = RabbitMqConsumer(rabbit_queue="winner.queue")
        self.rating_producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="rating.routingkey"
        )
        self.my_bid = randint(1000, 10_000)
        self.my_id = uuid4()
        # Numele utilizatorului — in practica ar fi citit din config/env
        self.username = input(f"[Bidder] Introduceti numele vostru: ").strip() or f"user_{str(self.my_id)[:8]}"

    def bid(self):
        print(f"[Bidder {self.username}] Trimit oferta: {self.my_bid}")
        bid_message = f"id:{self.my_id}_amount:{self.my_bid}"
        self.bid_producer.send_message(bid_message)

        # Simulare duplicat: sansa din 2 sa trimita de doua ori
        if randint(0, 1) == 1:
            self.bid_producer.send_message(bid_message)
            print(f"[Bidder {self.username}] Oferta trimisa de 2 ori (simulare duplicat)")

    def get_winner(self):
        print(f"[Bidder {self.username}] Astept rezultatul licitatiei...")
        result = self.winner_consumer.receive_message_infinite_tries()

        winner_id = result.split(":")[1]

        if winner_id == str(self.my_id):
            print(f"[Bidder {self.username}] Am CASTIGAT! Oferta: {self.my_bid}")
            won = True
        else:
            print(f"[Bidder {self.username}] Am pierdut...")
            won = False

        self._send_rating(won)

    def _send_rating(self, won: bool):
        """Solicita utilizatorului o evaluare si o trimite la RatingProcessor."""
        print(f"\n[Bidder {self.username}] Va rugam evaluati serviciul de licitatie (1-5):")
        print("  1 = Foarte slab  2 = Slab  3 = Mediu  4 = Bun  5 = Excelent")

        while True:
            try:
                rating = int(input("  Evaluare: ").strip())
                if 1 <= rating <= 5:
                    break
                print("  Introduceti un numar intre 1 si 5.")
            except ValueError:
                print("  Introduceti un numar valid.")

        # Trimite evaluarea la RatingProcessor: format "username:rating"
        message = f"{self.username}:{rating}"
        self.rating_producer.send_message(message)
        print(f"[Bidder {self.username}] Evaluare trimisa: {rating}/5. Multumim!")

        # Ultimul bidder care a castigat trimite semnalul de oprire
        # (in practica ar fi nevoie de coordonare; aici il trimite castigatorul)
        if won:
            self.rating_producer.send_message("stop:stop")

    def run(self):
        self.bid()
        self.get_winner()


if __name__ == '__main__':
    bidder = Bidder()
    bidder.run()
