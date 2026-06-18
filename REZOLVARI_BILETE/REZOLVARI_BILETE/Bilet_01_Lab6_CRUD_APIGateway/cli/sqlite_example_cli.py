import pika
from retry import retry


class RabbitMq:
    config = {
        'host': 'localhost',
        'port': 5672,
        'username': 'student',
        'password': 'student',
        'exchange': 'beerapp.direct',
        'routing_key': 'beerapp.routingkey',
        'queue': 'beerapp.queue1'
    }

    credentials = pika.PlainCredentials(config['username'], config['password'])
    parameters = pika.ConnectionParameters(
        host=config['host'],
        port=config['port'],
        credentials=credentials
    )

    def on_received_message(self, blocking_channel, deliver, properties, message):
        result = message.decode('utf-8')
        try:
            print("\n[Raspuns de la server]:")
            print(result)
        except Exception:
            print("Format raspuns invalid.")
        finally:
            blocking_channel.stop_consuming()

    @retry(pika.exceptions.AMQPConnectionError, delay=5, jitter=(1, 3))
    def receive_message(self):
        with pika.BlockingConnection(self.parameters) as connection:
            with connection.channel() as channel:
                channel.basic_consume(self.config['queue'], self.on_received_message)
                try:
                    channel.start_consuming()
                except pika.exceptions.ConnectionClosedByBroker:
                    print("Conexiune inchisa de broker.")
                except pika.exceptions.AMQPChannelError:
                    print("Eroare canal AMQP.")
                except KeyboardInterrupt:
                    print("Aplicatie inchisa.")

    def send_message(self, message):
        with pika.BlockingConnection(self.parameters) as connection:
            with connection.channel() as channel:
                channel.basic_publish(
                    exchange=self.config['exchange'],
                    routing_key=self.config['routing_key'],
                    body=message
                )


def print_menu():
    print("\n========== Beer Manager CLI ==========")
    print("0 --> Iesire")
    print("1 --> Adauga bere (addBeer)")
    print("2 --> Afiseaza toate berile (getBeers)")
    print("3 --> Cauta dupa nume (getBeerByName)")
    print("4 --> Cauta dupa pret maxim (getBeerByPrice)")
    print("5 --> Actualizeaza bere (updateBeer)")
    print("6 --> Sterge bere (deleteBeer)")
    print("======================================")
    return input("Optiune: ")


if __name__ == '__main__':
    rabbit_mq = RabbitMq()
    print("Conectare la RabbitMQ...")

    while True:
        option = print_menu()

        if option == '0':
            print("La revedere!")
            break

        elif option == '1':
            name = input("Nume bere: ")
            price = float(input("Pret bere: "))
            rabbit_mq.send_message("addBeer~name={};price={}".format(name, price))
            rabbit_mq.receive_message()

        elif option == '2':
            rabbit_mq.send_message("getBeers~")
            rabbit_mq.receive_message()

        elif option == '3':
            name = input("Nume bere: ")
            rabbit_mq.send_message("getBeerByName~name={}".format(name))
            rabbit_mq.receive_message()

        elif option == '4':
            price = float(input("Pret maxim: "))
            rabbit_mq.send_message("getBeerByPrice~price={}".format(price))
            rabbit_mq.receive_message()

        elif option == '5':
            beer_id = int(input("ID bere de actualizat: "))
            name = input("Nume nou: ")
            price = float(input("Pret nou: "))
            rabbit_mq.send_message("updateBeer~id={};name={};price={}".format(beer_id, name, price))
            rabbit_mq.receive_message()

        elif option == '6':
            name = input("Nume bere de sters: ")
            rabbit_mq.send_message("deleteBeer~name={}".format(name))
            rabbit_mq.receive_message()

        else:
            print("Optiune invalida!")
