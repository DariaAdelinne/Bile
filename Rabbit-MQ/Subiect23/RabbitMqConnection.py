import pika
from retry import retry


class RabbitMqInterface:
    """
    Clasa de baza pentru conexiunile RabbitMQ.
    Principiul D (DIP): componentele depind de aceasta abstractizare,
    nu de pika direct.
    """
    def __init__(self):
        self.config = {
            'host': 'localhost',
            'port': 5672,
            'username': 'student',
            'password': 'student'
        }
        self.credentials = pika.PlainCredentials(
            self.config['username'], self.config['password']
        )
        self.parameters = pika.ConnectionParameters(
            host=self.config['host'],
            port=self.config['port'],
            credentials=self.credentials
        )


class RabbitMqProducer(RabbitMqInterface):
    """
    Producator RabbitMQ.
    Principiul S (SRP): singura responsabilitate este trimiterea mesajelor.
    """
    def __init__(self, exchange: str, routing_key: str):
        super().__init__()
        self.config['exchange'] = exchange
        self.config['routing_key'] = routing_key

    def send_message(self, message: str):
        with pika.BlockingConnection(self.parameters) as connection:
            with connection.channel() as channel:
                channel.basic_publish(
                    exchange=self.config['exchange'],
                    routing_key=self.config['routing_key'],
                    body=message.encode('utf-8') if isinstance(message, str) else message
                )


class RabbitMqDynamicProducer(RabbitMqInterface):
    """
    Producator RabbitMQ cu routing key dinamic — folosit de master pentru
    a trimite in inbox-urile create dinamic per utilizator.
    Principiul O (OCP): nu modifica RabbitMqProducer, ci extinde comportamentul.
    """
    def __init__(self, exchange: str):
        super().__init__()
        self.config['exchange'] = exchange

    def send_to(self, routing_key: str, message: str):
        with pika.BlockingConnection(self.parameters) as connection:
            with connection.channel() as channel:
                channel.basic_publish(
                    exchange=self.config['exchange'],
                    routing_key=routing_key,
                    body=message.encode('utf-8') if isinstance(message, str) else message
                )


class RabbitMqConsumer(RabbitMqInterface):
    """
    Consumator RabbitMQ.
    Principiul S (SRP): singura responsabilitate este primirea mesajelor.
    """
    def __init__(self, rabbit_queue: str, purge: bool = True):
        super().__init__()
        self.config['queue'] = rabbit_queue
        self.connection = pika.BlockingConnection(self.parameters)
        self.channel = self.connection.channel()
        if purge:
            self.channel.queue_purge(self.config['queue'])

    @retry(Exception, delay=1, tries=15)
    def receive_message(self) -> str:
        result_msg = self.channel.basic_get(self.config['queue'])
        if result_msg[2]:
            self.channel.basic_ack(result_msg[0].delivery_tag)
            return result_msg[2].decode('utf-8')
        else:
            raise Exception("Niciun mesaj disponibil")

    @retry(Exception, delay=1, tries=-1)
    def receive_message_infinite_tries(self) -> str:
        result_msg = self.channel.basic_get(self.config['queue'])
        if result_msg[2]:
            self.channel.basic_ack(result_msg[0].delivery_tag)
            return result_msg[2].decode('utf-8')
        else:
            raise Exception("Niciun mesaj disponibil, reincerc...")


class RabbitMqAdmin(RabbitMqInterface):
    """
    Clasa utilitara pentru crearea dinamica de cozi si binding-uri.
    Folosita de ChatMasterProcessor la inregistrarea unui nou utilizator.
    Principiul S (SRP): singura responsabilitate este administrarea topologiei.
    """
    def __init__(self):
        super().__init__()

    def create_queue_and_bind(self, exchange: str, queue_name: str, routing_key: str):
        with pika.BlockingConnection(self.parameters) as connection:
            channel = connection.channel()
            channel.queue_declare(queue=queue_name, durable=False)
            channel.queue_purge(queue=queue_name)
            channel.queue_bind(
                exchange=exchange,
                queue=queue_name,
                routing_key=routing_key
            )
