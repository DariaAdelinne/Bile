====================================================================
  BILET 06 - Lab 10: Statistici Erori Licitatie (Python + RabbitMQ)
====================================================================

CERINTA:
  Pornind de la Okazii din Lab 10, sa se adauge un procesor de flux
  care monitorizeaza ERORILE aparute (comunicare + cozi) pana la
  adjudecare si le scrie cu statistici intr-un fisier local.

--------------------------------------------------------------------
DIFERENTA FATA DE BILET 05 (Logger):
--------------------------------------------------------------------

  Bilet 05: logheza TOATE operatiile (info + erori)
  Bilet 06: logheza DOAR ERORILE + face STATISTICI per tip si sursa

--------------------------------------------------------------------
MICROSERVICIU NOU: ErrorProcessor.py
--------------------------------------------------------------------

  Formatul mesajelor de eroare trimise de celelalte microservicii:
    "TIP_EROARE|Sursa|Detalii"

  Tipuri de erori:
    COMUNICARE  -> erori de retea, RabbitMQ producer/consumer
    COADA       -> timeout, duplicat, coada goala
    STOP        -> semnal de oprire (trimis de BiddingProcessor dupa adjudecare)

  ErrorProcessor:
    1. Asculta pe "errors.queue" la infinit
    2. La primirea "STOP|..." scrie statisticile si se opreste
    3. Statistici in errors.txt: total erori, per tip, per sursa, lista detaliata

  Cine trimite erori (self.err = RabbitMqProducer -> error.routingkey):
    - Auctioneer:        timeout oferte, erori parsare mesaje
    - MessageProcessor:  timeout, duplicat detectat, erori parsare
    - BiddingProcessor:  timeout, erori scriere result.txt, semnal STOP

--------------------------------------------------------------------
FLUX MESAJE RABBITMQ:
--------------------------------------------------------------------

  Exchange: bidder.direct (tip: direct)

  [Bidder] --bidder.routingkey--> [bidder.queue] --> [Auctioneer]
                                                           |
                                          messageprocessor.routingkey
                                                           v
                                               [message_processor.queue]
                                                           |
                                              [MessageProcessor]
                                                           |
                                           biddingprocessor.routingkey
                                                           v
                                               [bidding_processor.queue]
                                                           |
                                              [BiddingProcessor]
                                                           |
                                                  result.txt <- Bidder citeste de aici

  ERORI de la toate microserviciile:
  [Auctioneer / MessageProcessor / BiddingProcessor]
       --error.routingkey--> [errors.queue] --> [ErrorProcessor]
                                                       |
                                                  errors.txt (statistici)

Principii SOLID:
  S - ErrorProcessor: singura responsabilitate = colectare erori + statistici
  O - se pot adauga noi tipuri de erori (ex: "TIMEOUT") fara a modifica ErrorProcessor
  L - RabbitMqProducer/Consumer pot fi inlocuiti cu orice implementare
  I - interfetele RabbitMq sunt clare si separate
  D - toate microserviciile depind de RabbitMqProducer (abstractizare)

--------------------------------------------------------------------
DEPENDENTE:
--------------------------------------------------------------------

  pip install pika retry

--------------------------------------------------------------------
SETUP SI RULARE:
--------------------------------------------------------------------

  1. sudo service rabbitmq-server start
  2. sudo rabbitmqctl add_user student student
     sudo rabbitmqctl set_permissions -p / student ".*" ".*" ".*"
  3. python3 setup_rabbitmq.py

  Ordinea de pornire:
  Terminal 1: python3 ErrorProcessor.py       <- PRIMUL
  Terminal 2: python3 BiddingProcessor.py
  Terminal 3: python3 MessageProcessor.py
  Terminal 4: python3 Auctioneer.py
  Terminal 5: python3 Bidder.py               <- unul sau mai multi

  NOTA: result.txt trebuie sa fie in acelasi director cu toate .py-urile!

--------------------------------------------------------------------
EXEMPLU errors.txt:
--------------------------------------------------------------------

  ============================================================
    RAPORT ERORI LICITATIE - 2021-06-24 00:09:00
  ============================================================

  TOTAL ERORI: 3

  ERORI PE TIP:
    COADA           : 2
    COMUNICARE      : 1

  ERORI PE SURSA:
    Auctioneer                : 1
    MessageProcessor          : 2

  LISTA DETALIATA:
  ------------------------------------------------------------
  [00:08:39] [COADA] [Auctioneer] Timeout asteptare oferte: Niciun mesaj
  [00:08:40] [COADA] [MessageProcessor] Duplicat detectat pentru: abc-123
  [00:08:40] [COMUNICARE] [MessageProcessor] Eroare parsare 'msg_gresit': ...

  (daca nu s-au produs erori, fisierul va contine "nicio eroare inregistrata")

====================================================================
