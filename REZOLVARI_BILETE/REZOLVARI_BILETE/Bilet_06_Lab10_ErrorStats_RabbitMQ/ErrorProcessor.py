from datetime import datetime
from RabbitMqConnection import RabbitMqConsumer


class ErrorProcessor:
    """
    ErrorProcessor - MICROSERVICIU NOU adaugat pentru bilet.

    Responsabilitate (principiul S din SOLID):
      Monitorizeaza erorile aparute in toate microserviciile pe durata
      unei licitatii (pana la adjudecare) si scrie statistici in errors.txt.

    Formatul mesajelor primite: "TIP_EROARE|Sursa|Detalii"
      TIP_EROARE poate fi:
        COMUNICARE  -> erori de retea/socket/RabbitMQ producer/consumer
        COADA       -> erori legate de sistemul de cozi (timeout, duplicat, coada goala)
        STOP        -> semnal de oprire trimis de BiddingProcessor dupa adjudecare

    Statistici scrise in errors.txt:
      - Total erori
      - Erori per tip (COMUNICARE, COADA)
      - Erori per sursa (Auctioneer, MessageProcessor, etc.)
      - Lista detaliata a tuturor erorilor cu timestamp

    Principii SOLID:
      S - singura responsabilitate: colectare erori + scriere statistici
      O - se pot adauga noi tipuri de erori fara a modifica logica de baza
      D - depinde de RabbitMqConsumer (abstractizare)
    """

    ERRORS_FILE = "errors.txt"

    def __init__(self):
        self.consumer = RabbitMqConsumer(rabbit_queue="errors.queue")
        self.errors = []  # lista de tuple: (timestamp, tip, sursa, detalii)

    def collect_errors(self):
        print("[ErrorProcessor] Pornit. Colectez erorile din licitatie...")

        while True:
            message = self.consumer.receive_message_infinite_tries()

            if message is None:
                continue

            parts = message.split("|", 2)
            if len(parts) < 3:
                print("[ErrorProcessor] Mesaj cu format invalid: {}".format(message))
                continue

            tip, sursa, detalii = parts[0], parts[1], parts[2]

            if tip == "STOP":
                print("[ErrorProcessor] Semnal STOP primit: {}. Scriu statisticile...".format(detalii))
                break

            timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            self.errors.append((timestamp, tip, sursa, detalii))
            print("[ErrorProcessor] Eroare inregistrata [{}] {}/{}: {}".format(timestamp, tip, sursa, detalii))

    def write_statistics(self):
        """Scrie statisticile erorilor in errors.txt."""
        counts_by_type = {}
        counts_by_source = {}

        for _, tip, sursa, _ in self.errors:
            counts_by_type[tip] = counts_by_type.get(tip, 0) + 1
            counts_by_source[sursa] = counts_by_source.get(sursa, 0) + 1

        with open(self.ERRORS_FILE, "w") as f:
            f.write("=" * 60 + "\n")
            f.write("  RAPORT ERORI LICITATIE - {}\n".format(
                datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            ))
            f.write("=" * 60 + "\n\n")

            f.write("TOTAL ERORI: {}\n\n".format(len(self.errors)))

            f.write("ERORI PE TIP:\n")
            for tip, count in sorted(counts_by_type.items()):
                f.write("  {:15s} : {}\n".format(tip, count))

            f.write("\nERORI PE SURSA:\n")
            for sursa, count in sorted(counts_by_source.items()):
                f.write("  {:25s} : {}\n".format(sursa, count))

            f.write("\nLISTA DETALIATA:\n")
            f.write("-" * 60 + "\n")
            for ts, tip, sursa, detalii in self.errors:
                f.write("[{}] [{}] [{}] {}\n".format(ts, tip, sursa, detalii))

            if not self.errors:
                f.write("  (nicio eroare inregistrata)\n")

        print("[ErrorProcessor] Statistici scrise in: {}".format(self.ERRORS_FILE))

    def run(self):
        self.collect_errors()
        self.write_statistics()
        print("[ErrorProcessor] Incheierea activitatii.")


if __name__ == "__main__":
    processor = ErrorProcessor()
    processor.run()
