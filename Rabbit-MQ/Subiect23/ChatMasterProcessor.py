import uuid
from datetime import datetime
from RabbitMqConnection import RabbitMqConsumer, RabbitMqDynamicProducer, RabbitMqAdmin
from IChatStorage import IChatStorage
from FileChatStorage import FileChatStorage

EXCHANGE = "chat.direct"

# Protocoale mesaje
# Inregistrare:    REGISTER:<username>
# Mesaj:           MSG:<from>:<to>:<text>     (to=ALL pentru broadcast)
# Iesire:          EXIT:<username>
# Master -> User:  SESSION:<session_id>:<participants_csv>
# Master -> User:  INCOMING:<from>:<text>
# Master -> User:  USER_LEFT:<username>
# Master -> User:  SESSION_ENDED


class ChatMasterProcessor:
    """
    Microserviciu MASTER al sistemului de chat privat.

    Responsabilitati (SRP):
      - Primeste inregistrarile utilizatorilor
      - Creeaza sesiuni de chat
      - Ruteaza mesajele intre utilizatori
      - La finalul sesiunii, salveaza istoricul via IChatStorage

    Principii SOLID:
      S - gestionare sesiuni + rutare mesaje (coezive, acelasi domeniu)
      O - noi tipuri de comenzi pot fi adaugate in _dispatch() fara refactorizare majora
      D - depinde de IChatStorage, nu de FileChatStorage direct

    Cozi ascultate:
      chat.register.queue  — inregistrare utilizatori noi
      chat.msg.queue       — mesaje trimise de utilizatori

    Cozi create dinamic per utilizator:
      chat.inbox.<username> — inbox-ul fiecarui utilizator
    """

    def __init__(self, min_users: int = 2, storage: IChatStorage = None):
        self.min_users = min_users
        self.storage = storage if storage is not None else FileChatStorage()

        self.register_consumer = RabbitMqConsumer(rabbit_queue="chat.register.queue")
        self.msg_consumer = RabbitMqConsumer(rabbit_queue="chat.msg.queue")
        self.router = RabbitMqDynamicProducer(exchange=EXCHANGE)
        self.admin = RabbitMqAdmin()

        self.session_id = str(uuid.uuid4())[:8]
        self.participants = []    # lista de username-uri inregistrate
        self.message_history = []

    def _inbox_queue(self, username: str) -> str:
        return f"chat.inbox.{username}"

    def _inbox_routing_key(self, username: str) -> str:
        return f"chat.inbox.{username}.routingkey"

    def _send_to_user(self, username: str, message: str):
        self.router.send_to(self._inbox_routing_key(username), message)

    def _broadcast(self, message: str, exclude: str = None):
        for user in self.participants:
            if user != exclude:
                self._send_to_user(user, message)

    def _log(self, entry: str):
        timestamp = datetime.now().strftime("%H:%M:%S")
        line = f"[{timestamp}] {entry}"
        self.message_history.append(line)
        print(f"[ChatMasterProcessor] {line}")

    def wait_for_registrations(self):
        """Asteapta pana cand min_users utilizatori s-au inregistrat."""
        print(f"[ChatMasterProcessor] Astept {self.min_users} utilizatori sa se inregistreze...")

        while len(self.participants) < self.min_users:
            try:
                raw = self.register_consumer.receive_message_infinite_tries()
            except Exception:
                continue

            # Protocol: REGISTER:<username>
            if not raw.startswith("REGISTER:"):
                continue

            username = raw.split(":", 1)[1].strip()
            if username in self.participants:
                print(f"[ChatMasterProcessor] {username} deja inregistrat, ignorat.")
                continue

            # Creeaza coada inbox dinamica pentru acest utilizator
            self.admin.create_queue_and_bind(
                exchange=EXCHANGE,
                queue_name=self._inbox_queue(username),
                routing_key=self._inbox_routing_key(username)
            )
            self.participants.append(username)
            self._log(f"{username} s-a inregistrat. ({len(self.participants)}/{self.min_users})")

        # Notifica toti participantii ca sesiunea a inceput
        participants_str = ",".join(self.participants)
        for user in self.participants:
            self._send_to_user(user, f"SESSION:{self.session_id}:{participants_str}")
        self._log(f"Sesiunea {self.session_id} pornita cu: {participants_str}")

    def run_session(self):
        """Ruteaza mesajele intre utilizatori pana cand toti au iesit."""
        active = set(self.participants)

        while active:
            try:
                raw = self.msg_consumer.receive_message_infinite_tries()
            except Exception:
                continue

            if raw.startswith("MSG:"):
                # MSG:<from>:<to>:<text>
                parts = raw.split(":", 3)
                if len(parts) < 4:
                    continue
                sender = parts[1]
                recipient = parts[2]
                text = parts[3]

                self._log(f"{sender} -> {recipient}: {text}")

                if recipient == "ALL":
                    self._broadcast(f"INCOMING:{sender}:{text}", exclude=sender)
                elif recipient in self.participants:
                    self._send_to_user(recipient, f"INCOMING:{sender}:{text}")

            elif raw.startswith("EXIT:"):
                # EXIT:<username>
                username = raw.split(":", 1)[1].strip()
                if username in active:
                    active.discard(username)
                    self._log(f"{username} a parasit conversatia. ({len(active)} raman)")
                    self._broadcast(f"USER_LEFT:{username}", exclude=username)

                if not active:
                    self._log("Toti utilizatorii au iesit. Sesiunea s-a incheiat.")

        # Notifica pe toti (inclusiv cei deja iesiti) ca sesiunea s-a terminat
        self._broadcast("SESSION_ENDED")
        self.storage.save_history(self.session_id, self.participants, self.message_history)

    def run(self):
        self.wait_for_registrations()
        self.run_session()


if __name__ == "__main__":
    import sys
    min_u = int(sys.argv[1]) if len(sys.argv) > 1 else 2
    master = ChatMasterProcessor(min_users=min_u)
    master.run()
