import pika
from retry import retry


class RabbitMqInterface:
    """
    Clasa de baza pentru conexiunile RabbitMQ.
    Respecta principiul D din SOLID: componentele depind de aceasta
    abstractizare, nu de pika direct.
    """
    def __init__(self):
        self.config = {
            'host': 'localhost',
            'port': 5672,
            'username': 'student',
            'password': 'student'
        }
        self.credentials = pika.PlainCredentials(self.config['username'], self.config['password'])
        self.parameters = pika.ConnectionParameters(
            host=self.config['host'],
            port=self.config['port'],
            credentials=self.credentials
        )


class RabbitMqProducer(RabbitMqInterface):
    """
    Producator RabbitMQ.
    Respecta principiul S: singura responsabilitate este trimiterea mesajelor.
    """
    def __init__(self, exchange: str, routing_key: str):
        super().__init__()
        self.config["exchange"] = exchange
        self.config["routing_key"] = routing_key

    def send_message(self, message: str):
        with pika.BlockingConnection(self.parameters) as connection:
            with connection.channel() as channel:
                channel.basic_publish(
                    exchange=self.config['exchange'],
                    routing_key=self.config['routing_key'],
                    body=message.encode('utf-8') if isinstance(message, str) else message
                )


class RabbitMqConsumer(RabbitMqInterface):
    """
    Consumator RabbitMQ.
    Respecta principiul S: singura responsabilitate este primirea mesajelor.
    """
    def __init__(self, rabbit_queue: str):
        super().__init__()
        self.config["queue"] = rabbit_queue
        self.connection = pika.BlockingConnection(self.parameters)
        self.channel = self.connection.channel()
        self.channel.queue_purge(self.config["queue"])

    @retry(Exception, delay=1, tries=15)
    def receive_message(self) -> str:
        """Citeste un mesaj, ridica exceptie daca nu exista (max 15 incercari)."""
        result_msg = self.channel.basic_get(self.config['queue'])
        if result_msg[2]:
            self.channel.basic_ack(result_msg[0].delivery_tag)
            return result_msg[2].decode("utf-8")
        else:
            raise Exception("Niciun mesaj disponibil in coada")

    @retry(Exception, delay=1, tries=-1)
    def receive_message_infinite_tries(self) -> str:
        """Asteapta un mesaj la infinit."""
        result_msg = self.channel.basic_get(self.config['queue'])
        if result_msg[2]:
            self.channel.basic_ack(result_msg[0].delivery_tag)
            return result_msg[2].decode("utf-8")
        else:
            raise Exception("Niciun mesaj, reincerc...")
