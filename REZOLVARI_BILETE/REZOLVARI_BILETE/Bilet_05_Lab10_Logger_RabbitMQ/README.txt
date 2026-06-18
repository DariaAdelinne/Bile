====================================================================
  BILET 05 - Lab 10: Logger Procesor Flux (Python + RabbitMQ)
====================================================================

CERINTA:
  Pornind de la Okazii din Lab 10, sa se adauge un procesor de flux
  care monitorizeaza TOATE operatiile celorlalte procesoare si le
  scrie intr-un jurnal local. Comunicare prin RabbitMQ.

--------------------------------------------------------------------
MODIFICARE FATA DE LABORATORUL 10 (Kafka -> RabbitMQ + Logger):
--------------------------------------------------------------------

  Laboratorul 10 foloseste Kafka.
  Aceasta solutie foloseste RabbitMQ (pika) - conform cerintei biletului.

  ADAUGAT - LoggerProcessor.py (NOU):
    - Asculta pe coada "bidding_logger.queue"
    - Primeste mesaje de forma "<tip>:<continut>":
        info:    -> scrie ca INFO in application_log.log
        except:  -> scrie ca ERROR in application_log.log
        stop:    -> se opreste (trimis de castigatorul licitatiei)
    - Toate celelalte microservicii au un self.logger = RabbitMqProducer
      care trimite catre bidding_logger.routingkey

  MODIFICATE (adaugat self.logger in fiecare):
    - Bidder.py      - logeaza oferta trimisa, rezultatul, semnalul stop
    - Auctioneer.py  - logeaza ofertele primite, incheierea licitatiei
    - MessageProcessor.py - logeaza deduplicarea, sortarea, trimiterea
    - BiddingProcessor.py - logeaza castigatorul, trimiterea rezultatelor

--------------------------------------------------------------------
ARHITECTURA FLUX MESAJE RABBITMQ:
--------------------------------------------------------------------

  Exchange: bidder.direct (tip: direct)

  [Bidder] --bidder.routingkey--> [bidder.queue] --> [Auctioneer]
                                                            |
                                           messageprocessor.routingkey
                                                            |
                                                            v
                                                [message_processor.queue]
                                                            |
                                                [MessageProcessor]
                                                            |
                                            biddingprocessor.routingkey
                                                            |
                                                            v
                                                [bidding_processor.queue]
                                                            |
                                                [BiddingProcessor]
                                                            |
                                                 winner.routingkey
                                                            |
                                                            v
                                                  [winner.queue] --> [Bidder]

  TOATE microserviciile trimit log-uri:
  [Bidder/Auctioneer/MessageProcessor/BiddingProcessor]
            --bidding_logger.routingkey-->
                  [bidding_logger.queue] --> [LoggerProcessor]
                                                    |
                                                    v
                                           application_log.log

Principii SOLID:
  S - fiecare microserviciu are o singura responsabilitate
      (Bidder: licitare; Logger: scriere jurnal; etc.)
  O - se pot adauga noi tipuri de mesaje ("warn:") in Logger fara modificari
  L - RabbitMqProducer/Consumer pot fi inlocuiti cu orice implementare
  I - RabbitMqInterface, RabbitMqProducer, RabbitMqConsumer sunt separate
  D - toate microserviciile depind de RabbitMqProducer/Consumer (abstractizari)

--------------------------------------------------------------------
DEPENDENTE PYTHON:
--------------------------------------------------------------------

  pip install pika retry

--------------------------------------------------------------------
SETUP RABBITMQ:
--------------------------------------------------------------------

  1. Porneste RabbitMQ:
       sudo service rabbitmq-server start

  2. Creeaza userul student (daca nu exista):
       sudo rabbitmqctl add_user student student
       sudo rabbitmqctl set_permissions -p / student ".*" ".*" ".*"

  3. Ruleaza scriptul de setup:
       python3 setup_rabbitmq.py

--------------------------------------------------------------------
ORDINEA DE PORNIRE (IMPORTANTA):
--------------------------------------------------------------------

  Terminal 1: python3 LoggerProcessor.py       <- PRIMUL (trebuie sa fie gata)
  Terminal 2: python3 BiddingProcessor.py
  Terminal 3: python3 MessageProcessor.py
  Terminal 4: python3 Auctioneer.py
  Terminal 5: python3 Bidder.py               <- pot fi mai multi simultan
  Terminal 6: python3 Bidder.py
  (etc.)

  Nota: Auctioneer are un timeout de 15 incercari x 1 secunda.
  Porneste Bidder-ii inainte ca Auctioneer sa expire!

--------------------------------------------------------------------
EXEMPLU application_log.log:
--------------------------------------------------------------------

  [2021-06-24 00:08:16] [INFO] [LoggerProcessor] Pornit.
  [2021-06-24 00:08:22] [INFO] [Auctioneer] Astept oferte...
  [2021-06-24 00:08:25] [INFO] [Bidder abc] Trimit oferta: 2117
  [2021-06-24 00:08:25] [INFO] [Auctioneer] abc a licitat 2117
  [2021-06-24 00:08:39] [INFO] [Auctioneer] Licitatia s-a incheiat!
  [2021-06-24 00:08:40] [INFO] [MessageProcessor] Procesez ofertele...
  [2021-06-24 00:09:04] [INFO] [BiddingProcessor] Castigatorul: abc - oferta: 2117
  [2021-06-24 00:09:04] [INFO] [Bidder abc] Am CASTIGAT!
  [2021-06-24 00:09:04] [INFO] [LoggerProcessor] Semnal de oprire primit. Inchidere.

====================================================================
