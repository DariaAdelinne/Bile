"""
Script de configurare RabbitMQ pentru biletul Lab 10 - ErrorStats.
Ruleaza INAINTE de pornirea oricarui microserviciu.

Cozi si routing keys:
  bidder.queue             <- bidder.routingkey          (Bidder -> Auctioneer)
  message_processor.queue  <- messageprocessor.routingkey (Auctioneer -> MessageProcessor)
  bidding_processor.queue  <- biddingprocessor.routingkey (MessageProcessor -> BiddingProcessor)
  errors.queue             <- error.routingkey            (TOATE -> ErrorProcessor)
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
    ('errors.queue',             'error.routingkey'),
]

if __name__ == '__main__':
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    params = pika.ConnectionParameters(host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials)

    with pika.BlockingConnection(params) as conn:
        ch = conn.channel()
        ch.exchange_declare(exchange=EXCHANGE, exchange_type='direct', durable=False)
        print("Exchange creat: {}".format(EXCHANGE))

        for queue_name, routing_key in QUEUES:
            ch.queue_declare(queue=queue_name, durable=False)
            ch.queue_purge(queue=queue_name)
            ch.queue_bind(exchange=EXCHANGE, queue=queue_name, routing_key=routing_key)
            print("  {} <- {}".format(queue_name, routing_key))

        # Creeaza result.txt gol
        open("result.txt", "w").close()

    print("\nSetup complet! Poti porni microserviciile.")
