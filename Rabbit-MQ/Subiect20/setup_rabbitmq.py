"""
Script de configurare RabbitMQ pentru Subiect 20 — MessageStatisticsProcessor.

Cozi:
  bidder.queue             <- bidder.routingkey          Bidder -> Auctioneer
  message_processor.queue  <- messageprocessor.routingkey Auctioneer -> MessageProcessor
  bidding_processor.queue  <- biddingprocessor.routingkey MessageProcessor -> BiddingProcessor
  winner.queue             <- winner.routingkey           BiddingProcessor -> Bidder
  msg_stats.queue          <- msg_stats.routingkey        TOATE -> MessageStatisticsProcessor (NOU)

Ruleaza INAINTE de pornirea oricarui microserviciu.
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
    ('msg_stats.queue',          'msg_stats.routingkey'),       # NOU
]

if __name__ == '__main__':
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    params = pika.ConnectionParameters(
        host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials
    )

    with pika.BlockingConnection(params) as conn:
        ch = conn.channel()
        ch.exchange_declare(exchange=EXCHANGE, exchange_type='direct', durable=False)
        print(f"Exchange creat: {EXCHANGE}")

        for queue_name, routing_key in QUEUES:
            ch.queue_declare(queue=queue_name, durable=False)
            ch.queue_purge(queue=queue_name)
            ch.queue_bind(exchange=EXCHANGE, queue=queue_name, routing_key=routing_key)
            label = " <-- NOU" if queue_name == "msg_stats.queue" else ""
            print(f"  {queue_name:<30s} <- {routing_key}{label}")

    print("\nSetup complet! Poti porni microserviciile.")
