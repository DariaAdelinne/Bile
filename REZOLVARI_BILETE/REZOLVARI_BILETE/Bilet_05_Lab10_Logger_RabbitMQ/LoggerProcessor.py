import logging
from RabbitMqConnection import RabbitMqConsumer


class LoggerProcessor:
    """
    LoggerProcessor - MICROSERVICIU NOU adaugat pentru bilet.

    Responsabilitate (principiul S din SOLID):
      Monitorizeaza toate operatiile efectuate de celelalte procesoare de flux
      (Auctioneer, MessageProcessor, BiddingProcessor, Bidder) si le scrie
      intr-un jurnal local (application_log.log).

    Functionare:
      - Asculta pe coada "bidding_logger.queue"
      - Fiecare mesaj are formatul: "<tip>:<continut>"
          info:    -> logate ca INFO
          except:  -> logate ca ERROR
          stop:    -> oprire LoggerProcessor
      - Se opreste cand primeste mesajul "stop:stop" (trimis de castigatorul licitatiei)

    Principii SOLID:
      S - singura responsabilitate: receptie mesaje + scriere jurnal
      O - se pot adauga noi tipuri de mesaje (ex: "warn:") fara a modifica logica de baza
      D - depinde de RabbitMqConsumer (abstractizare), nu de pika direct

    Nota: Toate celelalte microservicii au un RabbitMqProducer catre
    "bidding_logger.routingkey" => "bidding_logger.queue" pentru a trimite
    log-uri catre acest procesor.
    """
    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="bidding_logger.queue")

        # Configureaza Python logging catre fisierul local
        logging.basicConfig(
            filename="application_log.log",
            format='[%(asctime)s] [%(levelname)s] %(message)s',
            filemode='w'
        )
        self.logger = logging.getLogger(__name__)
        self.logger.setLevel(logging.DEBUG)

        # Suprima log-urile din bibliotecile externe
        logging.getLogger("pika").setLevel(logging.CRITICAL)
        logging.getLogger("retry").setLevel(logging.CRITICAL)

    def receive_messages(self):
        print("[LoggerProcessor] Pornit. Asteapta mesaje de la toate microserviciile...")
        self.logger.info("[LoggerProcessor] Pornit. Asteapta mesaje de la toate microserviciile...")

        while True:
            received = self.consumer.receive_message_infinite_tries()

            if received is None:
                continue

            try:
                # Parsare format "<tip>:<continut>"
                parts = received.split(':', 1)
                if len(parts) < 2:
                    self.logger.warning("Mesaj cu format invalid: {}".format(received))
                    continue

                msg_type = parts[0]
                content = parts[1]

                if msg_type == "info":
                    self.logger.info(content)
                    print("[LoggerProcessor] LOG INFO: {}".format(content))
                elif msg_type == "except":
                    self.logger.error(content)
                    print("[LoggerProcessor] LOG ERROR: {}".format(content))
                elif msg_type == "stop":
                    self.logger.info("[LoggerProcessor] Semnal de oprire primit. Inchidere.")
                    print("[LoggerProcessor] Semnal stop primit. Jurnal salvat in application_log.log")
                    break
                else:
                    self.logger.warning("Tip mesaj necunoscut '{}': {}".format(msg_type, content))

            except Exception as e:
                self.logger.error("Eroare la procesarea mesajului '{}': {}".format(received, e))

    def run(self):
        self.receive_messages()


if __name__ == "__main__":
    processor = LoggerProcessor()
    processor.run()
