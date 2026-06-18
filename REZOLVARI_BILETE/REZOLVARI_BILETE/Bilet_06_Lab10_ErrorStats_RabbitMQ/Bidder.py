import os
from random import randint
from uuid import uuid4
from RabbitMqConnection import RabbitMqProducer


class Bidder:
    """
    Microserviciu Bidder — trimite oferta si asteapta rezultatul din result.txt.
    Trimite erori catre ErrorProcessor prin error.routingkey.

    Principii SOLID:
      S - singura responsabilitate: licitare + citire rezultat
      D - depinde de RabbitMqProducer (abstractizare)
    """
    def __init__(self):
        self.producer = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="bidder.routingkey"
        )
        self.err = RabbitMqProducer(
            exchange="bidder.direct",
            routing_key="error.routingkey"
        )
        self.my_bid = randint(1000, 10_000)
        self.my_id = uuid4()

        # Curata result.txt la start
        open("result.txt", "w").close()
        self.result_file = open("result.txt", "r")

    def bid(self):
        print("[Bidder {}] Trimit oferta: {}".format(self.my_id, self.my_bid))
        try:
            bid_message = "id:{}_amount:{}".format(self.my_id, self.my_bid)
            self.producer.send_message(bid_message)

            # Simulare duplicat: sansa din 2
            if randint(0, 1) == 1:
                self.producer.send_message(bid_message)
                print("[Bidder {}] Oferta trimisa de 2 ori (simulare duplicat)".format(self.my_id))
        except Exception as e:
            self.err.send_message("COMUNICARE|Bidder|{}".format(str(e)))

    def get_winner(self):
        print("[Bidder {}] Astept rezultatul licitatiei...".format(self.my_id))
        try:
            # Asteapta pana cand BiddingProcessor scrie in result.txt
            while os.stat("result.txt").st_size == 0:
                pass

            result = self.result_file.readline().strip()
            winner_id = result.split(":")[1]

            if winner_id == str(self.my_id):
                print("[Bidder {}] AM CASTIGAT! Oferta: {}".format(self.my_id, self.my_bid))
            else:
                print("[Bidder {}] Am pierdut. Castigator: {}".format(self.my_id, winner_id))
        except Exception as e:
            self.err.send_message("COMUNICARE|Bidder|{}".format(str(e)))
        finally:
            self.result_file.close()

    def run(self):
        self.bid()
        self.get_winner()


if __name__ == '__main__':
    bidder = Bidder()
    bidder.run()
