"""
Script de configurare RabbitMQ pentru biletul Lab 10 - Logger.
Ruleaza INAINTE de pornirea oricarui microserviciu.

Creeaza:
  - Exchange: bidder.direct (type: direct)
  - Cozi si binding-uri:
      bidder.queue           <- routing key: bidder.routingkey          (Bidder -> Auctioneer)
      message_processor.queue <- routing key: messageprocessor.routingkey (Auctioneer -> MessageProcessor)
      bidding_processor.queue <- routing key: biddingprocessor.routingkey (MessageProcessor -> BiddingProcessor)
      winner.queue           <- routing key: winner.routingkey          (BiddingProcessor -> Bidder)
      bidding_logger.queue   <- routing key: bidding_logger.routingkey  (TOATE -> LoggerProcessor)
"""
import pika

RABBITMQ_HOST = 'localhost'
RABBITMQ_PORT = 5672
RABBITMQ_USER = 'student'
RABBITMQ_PASS = 'student'

EXCHANGE = 'bidder.direct'

QUEUES = [
    ('bidder.queue',             'bidder.routingkey'),
    ('message_processor.queue',  'messageprocessor.routingkey'),
    ('bidding_processor.queue',  'biddingprocessor.routingkey'),
    ('winner.queue',             'winner.routingkey'),
    ('bidding_logger.queue',     'bidding_logger.routingkey'),
]

if __name__ == '__main__':
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    params = pika.ConnectionParameters(host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials)

    with pika.BlockingConnection(params) as conn:
        ch = conn.channel()

        # Creeaza exchange-ul
        ch.exchange_declare(exchange=EXCHANGE, exchange_type='direct', durable=False)
        print("Exchange creat: {}".format(EXCHANGE))

        # Creeaza cozile si le leaga la exchange
        for queue_name, routing_key in QUEUES:
            ch.queue_declare(queue=queue_name, durable=False)
            ch.queue_purge(queue=queue_name)
            ch.queue_bind(exchange=EXCHANGE, queue=queue_name, routing_key=routing_key)
            print("  {} <- {}".format(queue_name, routing_key))

    print("\nSetup complet! Poti porni microserviciile.")
