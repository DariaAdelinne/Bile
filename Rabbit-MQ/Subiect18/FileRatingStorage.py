import os
from datetime import datetime
from IRatingStorage import IRatingStorage


class FileRatingStorage(IRatingStorage):
    """
    Implementare concreta a IRatingStorage care scrie evaluarile intr-un
    fisier local (ratings.txt).

    Principiul S (SRP): singura responsabilitate este persistenta evaluarilor
                        in fisier — nu stie nimic despre RabbitMQ sau retea.
    Principiul O (OCP): putem crea alte implementari (DatabaseRatingStorage,
                        CloudRatingStorage) fara a modifica aceasta clasa.
    """

    def __init__(self, filepath: str = "ratings.txt"):
        self.filepath = filepath
        # Creeaza fisierul cu header daca nu exista
        if not os.path.exists(self.filepath):
            with open(self.filepath, 'w', encoding='utf-8') as f:
                f.write("timestamp,username,rating\n")

    def save(self, username: str, rating: int) -> None:
        """Adauga o linie CSV cu timestamp, username si rating."""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        line = f"{timestamp},{username},{rating}\n"
        with open(self.filepath, 'a', encoding='utf-8') as f:
            f.write(line)
        print(f"[FileRatingStorage] Salvat: {username} -> {rating}/5 ({timestamp})")
