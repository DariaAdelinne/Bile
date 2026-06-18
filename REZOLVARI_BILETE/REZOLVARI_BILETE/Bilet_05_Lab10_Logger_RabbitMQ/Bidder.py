from random import randint
from uuid import uuid4
from RabbitMqConnection import RabbitMqProducer, RabbitMqConsumer


class Bidder:
    """
    Microserviciu Bidder — trimite o oferta la licitatie si asteapta rezultatul.

    Modificare fata de lab: trimite log-uri catre LoggerProcessor prin
    exchange-ul bidder.direct, routing key bidding_logger.routingkey.

    Principii SOLID:
      S - singura responsabilitate: licitare + primire rezultat
      D - depinde de RabbitMqProducer/Consumer (abstractizare), nu de pika direct
    """
    def __init__(self):
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="bidder.routingkey"
        )
        self.consumer = RabbitMqConsumer(rabbit_queue="winner.queue")
        self.logger = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="bidding_logger.routingkey"
        )
        self.my_bid = randint(1000, 10_000)
        self.my_id = uuid4()

    def bid(self):
        print("[Bidder {}] Trimit oferta: {}".format(self.my_id, self.my_bid))
        self.logger.send_message("info:[Bidder {}] Trimit oferta: {}".format(self.my_id, self.my_bid))

        bid_message = "id:{}_amount:{}".format(self.my_id, self.my_bid)
        self.producer.send_message(bid_message)

        # Simulare eroare: sansa din 2 sa trimiti de doua ori (duplicat)
        if randint(0, 1) == 1:
            self.producer.send_message(bid_message)
            self.logger.send_message("info:[Bidder {}] Oferta trimisa de 2 ori (simulare duplicat)".format(self.my_id))

    def get_winner(self):
        print("[Bidder {}] Astept rezultatul licitatiei...".format(self.my_id))
        self.logger.send_message("info:[Bidder {}] Astept rezultatul licitatiei...".format(self.my_id))

        result = self.consumer.receive_message_infinite_tries()

        # Formatul rezultatului: "winner:<ID>"
        winner_id = result.split(":")[1]

        if winner_id == str(self.my_id):
            print("[Bidder {}] Am CASTIGAT! Oferta: {}".format(self.my_id, self.my_bid))
            self.logger.send_message("info:[Bidder {}] Am CASTIGAT! Oferta: {}".format(self.my_id, self.my_bid))
            # Semnalizeaza LoggerProcessor sa se opreasca
            self.logger.send_message("stop:stop")
        else:
            print("[Bidder {}] Am pierdut...".format(self.my_id))
            self.logger.send_message("info:[Bidder {}] Am pierdut...".format(self.my_id))

    def run(self):
        self.bid()
        self.get_winner()


if __name__ == '__main__':
    bidder = Bidder()
    bidder.run()
