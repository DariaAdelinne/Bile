====================================================================
  BILET 01 - Lab 6: CRUD Microservicii separate + API Gateway
====================================================================

CERINTA:
  Pornind de la exemplul 1 din laboratorul 6, sa se modifice astfel
  incat fiecare operatie CRUD sa aiba microserviciul sau separat,
  accesate prin API Gateway (poarta API).

--------------------------------------------------------------------
ARHITECTURA:
--------------------------------------------------------------------

  [Python CLI]
       |
       | RabbitMQ (queue: beerapp.queue1)
       v
  [BeerDAOMicroservice]   <-- orchestrare RabbitMQ, trimite inapoi raspunsul
       |
       | HTTP (khttp)
       v
  [BeerCRUDMicroservice]  <-- API GATEWAY (poarta API)
       |
       +---> POST   /addbeermicroservice    --> [BeerAddMicroservice]
       +---> GET    /getbeersmicroservice   --> [BeerGetMicroservice]
       +---> GET    /getbeerbynamemicro...  --> [BeerGetMicroservice]
       +---> GET    /getbeerbypricemicro... --> [BeerGetMicroservice]
       +---> PUT    /updatebeermicroservice --> [BeerUpdateMicroservice]
       +---> DELETE /deletebeermicroservice --> [BeerDeleteMicroservice]
                                                       |
                                                       | Autowired
                                                       v
                                               [BeerDAOService]   <-- implementeaza BeerDAO
                                                       |
                                                       v
                                                   [SQLite DB]

Principii SOLID respectate:
  S - fiecare microserviciu are o singura responsabilitate (Add / Get / Update / Delete / Gateway / Orchestrare)
  O - gateway-ul este deschis pentru extindere (noi microservicii fara a modifica gateway-ul)
  L - BeerDAOService implementeaza complet interfata BeerDAO
  I - interfata BeerDAO declara clar fiecare operatie
  D - microserviciile depind de interfata BeerDAO, nu de BeerDAOService direct

--------------------------------------------------------------------
DEPENDENTE:
--------------------------------------------------------------------

  Java/Kotlin:
    - JDK 8 sau 11
    - Maven 3.x
    - RabbitMQ pornit (rabbitmq-server start)
      user: student / password: student
      Creare user:
        sudo rabbitmqctl add_user student student
        sudo rabbitmqctl set_permissions -p / student ".*" ".*" ".*"

  Python:
    - Python 3.x
    - pip install pika retry

--------------------------------------------------------------------
RULARE:
--------------------------------------------------------------------

  1. Pornire RabbitMQ:
       sudo service rabbitmq-server start
     (sau: sudo rabbitmq-server -detached)

  2. Creare exchange si queue in RabbitMQ:
     Deschide http://localhost:15672 (user: guest/guest) si:
       - Creeaza exchange: beerapp.direct (tip: direct)
       - Creeaza queue: beerapp.queue1
       - Leaga queue la exchange cu routing key: beerapp.routingkey

     SAU foloseste comenzile rabbitmqadmin:
       rabbitmqadmin declare exchange name=beerapp.direct type=direct
       rabbitmqadmin declare queue name=beerapp.queue1
       rabbitmqadmin declare binding source=beerapp.direct destination=beerapp.queue1 routing_key=beerapp.routingkey

  3. Compilare si rulare aplicatie Kotlin (din folderul BeerCRUDApp/):
       mvn clean package -DskipTests
       java -jar target/BeerCRUDApp-1.0.0.jar

     SAU deschide in IntelliJ si ruleaza BeerCRUDApp.kt

  4. Rulare client Python (din folderul cli/):
       python3 sqlite_example_cli.py

--------------------------------------------------------------------
STRUCTURA FISIERE:
--------------------------------------------------------------------

  BeerCRUDApp/
    pom.xml
    src/main/kotlin/com/sd/laborator/
      BeerCRUDApp.kt                        <- entry point Spring Boot
      model/
        Beer.kt                             <- modelul datelor
      interfaces/
        BeerDAO.kt                          <- interfata CRUD
      services/
        BeerDAOService.kt                   <- implementare SQLite
      components/
        RabbitMqComponent.kt                <- configurare RabbitMQ
      microservices/
        BeerDAOMicroservice.kt              <- asculta RabbitMQ, orchestreaza
        BeerCRUDMicroservice.kt             <- API GATEWAY
        BeerAddMicroservice.kt              <- CREATE
        BeerGetMicroservice.kt              <- READ
        BeerUpdateMicroservice.kt           <- UPDATE
        BeerDeleteMicroservice.kt           <- DELETE
    src/main/resources/
      application.properties
  cli/
    sqlite_example_cli.py                   <- client Python CLI

====================================================================
