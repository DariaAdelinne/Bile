"""
Script de configurare RabbitMQ pentru Subiect 18 - Rating Processor.

Creeaza exchange-ul si toate cozile necesare, inclusiv cea noua
pentru evaluari: rating.queue <- rating.routingkey

Ruleaza INAINTE de pornirea oricarui microserviciu.
"""
import pika

RABBITMQ_HOST = 'localhost'
RABBITMQ_PORT = 5672
RABBITMQ_USER = 'student'
RABBITMQ_PASS = 'student'
EXCHANGE = 'bidder.direct'

QUEUES = [
    ('bidder.queue',             'bidder.routingkey'),           # Bidder -> Auctioneer
    ('message_processor.queue',  'messageprocessor.routingkey'), # Auctioneer -> MessageProcessor
    ('bidding_processor.queue',  'biddingprocessor.routingkey'), # MessageProcessor -> BiddingProcessor
    ('winner.queue',             'winner.routingkey'),           # BiddingProcessor -> Bidder
    ('rating.queue',             'rating.routingkey'),           # Bidder -> RatingProcessor (NOU)
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
            label = " <-- NOU" if queue_name == "rating.queue" else ""
            print(f"  {queue_name:<30s} <- {routing_key}{label}")

    print("\nSetup complet! Poti porni microserviciile.")
