"""
Script de configurare RabbitMQ pentru Subiect 23 — Chat Privat.

Creeaza exchange-ul si cozile statice.
Cozile per utilizator (chat.inbox.<username>) sunt create DINAMIC
de ChatMasterProcessor la inregistrarea fiecarui utilizator.

Cozi statice:
  chat.register.queue  <- chat.register.routingkey   UserCommunicationProcessor -> ChatMasterProcessor
  chat.msg.queue       <- chat.msg.routingkey         UserCommunicationProcessor -> ChatMasterProcessor

Cozi dinamice (create de master):
  chat.inbox.<username> <- chat.inbox.<username>.routingkey   Master -> fiecare User

Ruleaza INAINTE de pornirea oricarui microserviciu.
"""
import pika

RABBITMQ_HOST = 'localhost'
RABBITMQ_PORT = 5672
RABBITMQ_USER = 'student'
RABBITMQ_PASS = 'student'
EXCHANGE = 'chat.direct'

STATIC_QUEUES = [
    ('chat.register.queue', 'chat.register.routingkey'),
    ('chat.msg.queue',      'chat.msg.routingkey'),
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

        for queue_name, routing_key in STATIC_QUEUES:
            ch.queue_declare(queue=queue_name, durable=False)
            ch.queue_purge(queue=queue_name)
            ch.queue_bind(exchange=EXCHANGE, queue=queue_name, routing_key=routing_key)
            print(f"  {queue_name:<30s} <- {routing_key}")

    print("\nCozi dinamice per utilizator vor fi create de ChatMasterProcessor.")
    print("Setup complet! Poti porni microserviciile.")
