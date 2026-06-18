import threading
from RabbitMqConnection import RabbitMqProducer, RabbitMqConsumer

EXCHANGE = "chat.direct"

# Protocoale primite din inbox:
#   SESSION:<session_id>:<participants_csv>  — sesiunea a inceput
#   INCOMING:<from>:<text>                  — mesaj nou de la alt utilizator
#   USER_LEFT:<username>                    — alt utilizator a iesit
#   SESSION_ENDED                           — sesiunea s-a incheiat


class UserCommunicationProcessor:
    """
    Microserviciu de COMUNICATIE per utilizator — replicat pentru fiecare
    participant la sesiunea de chat.

    Responsabilitati (SRP):
      - Inregistrarea utilizatorului la ChatMasterProcessor
      - Trimiterea mesajelor catre master (care le ruteaza)
      - Receptia mesajelor din inbox-ul propriu si afisarea lor
      - Gestionarea iesirii din sesiune

    Principii SOLID:
      S - comunicatie bidirectionala pentru un singur utilizator
      O - noi tipuri de mesaje pot fi adaugate in _handle_incoming() fara a schimba structura
      D - depinde de RabbitMqProducer/Consumer (abstractizari), nu de pika direct

    Nota: receptia se face intr-un thread separat pentru a nu bloca input-ul.
    """

    def __init__(self, username: str):
        self.username = username
        self.session_id = None
        self.participants = []
        self.running = False

        # Producer: inregistrare la master
        self.register_producer = RabbitMqProducer(
            exchange=EXCHANGE,
            routing_key="chat.register.routingkey"
        )
        # Producer: trimitere mesaje catre master
        self.msg_producer = RabbitMqProducer(
            exchange=EXCHANGE,
            routing_key="chat.msg.routingkey"
        )
        # Consumer: inbox propriu (creat dinamic de master dupa inregistrare)
        # Initializam dupa ce masterul creeaza coada
        self.inbox_consumer = None

    def _register(self):
        """Trimite cererea de inregistrare catre ChatMasterProcessor."""
        self.register_producer.send_message(f"REGISTER:{self.username}")
        print(f"[{self.username}] Cerere de inregistrare trimisa. Astept sesiunea...")

    def _init_inbox_consumer(self):
        """Initializeaza consumatorul pentru inbox-ul propriu."""
        # purge=False: nu stergem mesajele deja existente (SESSION poate fi deja acolo)
        self.inbox_consumer = RabbitMqConsumer(
            rabbit_queue=f"chat.inbox.{self.username}",
            purge=False
        )

    def _handle_incoming(self, raw: str) -> bool:
        """
        Proceseaza un mesaj primit in inbox.
        Returneaza False daca sesiunea s-a incheiat.
        """
        if raw.startswith("SESSION:"):
            # SESSION:<session_id>:<participants_csv>
            parts = raw.split(":", 2)
            self.session_id = parts[1]
            self.participants = parts[2].split(",")
            others = [p for p in self.participants if p != self.username]
            print(f"\n[{self.username}] === Sesiune privata pornita ===")
            print(f"[{self.username}] Participanti: {', '.join(self.participants)}")
            print(f"[{self.username}] Scrie '<destinatar>: <mesaj>' sau 'ALL: <mesaj>' sau 'exit'")
            print(f"[{self.username}] ================================\n")

        elif raw.startswith("INCOMING:"):
            # INCOMING:<from>:<text>
            parts = raw.split(":", 2)
            sender = parts[1]
            text = parts[2]
            print(f"\n  [{sender}]: {text}")

        elif raw.startswith("USER_LEFT:"):
            left_user = raw.split(":", 1)[1]
            print(f"\n[{self.username}] *** {left_user} a parasit conversatia ***")

        elif raw == "SESSION_ENDED":
            print(f"\n[{self.username}] === Sesiunea s-a incheiat ===")
            return False

        return True

    def _receive_loop(self):
        """Thread de receptie — citeste inbox-ul in permanenta."""
        while self.running:
            try:
                raw = self.inbox_consumer.receive_message_infinite_tries()
                should_continue = self._handle_incoming(raw)
                if not should_continue:
                    self.running = False
                    break
            except Exception:
                if not self.running:
                    break

    def _send_loop(self):
        """Thread principal — citeste input-ul utilizatorului si trimite mesaje."""
        # Asteptam sa primim SESSION inainte de a permite trimiterea
        while self.session_id is None and self.running:
            pass

        while self.running:
            try:
                user_input = input()
            except EOFError:
                break

            if not user_input.strip():
                continue

            if user_input.strip().lower() == "exit":
                self.msg_producer.send_message(f"EXIT:{self.username}")
                self.running = False
                break

            # Format input: '<destinatar>: <mesaj>' sau 'ALL: <mesaj>'
            if ":" in user_input:
                parts = user_input.split(":", 1)
                recipient = parts[0].strip()
                text = parts[1].strip()
                self.msg_producer.send_message(f"MSG:{self.username}:{recipient}:{text}")
            else:
                # daca nu e specificat destinatarul, trimite la toti
                self.msg_producer.send_message(f"MSG:{self.username}:ALL:{user_input.strip()}")

    def run(self):
        self._register()
        # Asteptam putin pentru ca masterul sa creeze inbox-ul
        import time; time.sleep(1)
        self._init_inbox_consumer()

        self.running = True

        # Thread de receptie (daemon — se inchide automat cand main thread-ul termina)
        recv_thread = threading.Thread(target=self._receive_loop, daemon=True)
        recv_thread.start()

        # Send loop in thread-ul principal (citeste de la tastatura)
        self._send_loop()

        self.running = False
        recv_thread.join(timeout=2)
        print(f"[{self.username}] Deconectat.")


if __name__ == "__main__":
    import sys
    if len(sys.argv) < 2:
        print("Utilizare: python UserCommunicationProcessor.py <username>")
        sys.exit(1)
    username = sys.argv[1]
    user = UserCommunicationProcessor(username)
    user.run()
