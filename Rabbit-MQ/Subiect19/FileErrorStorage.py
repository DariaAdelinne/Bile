from datetime import datetime
from IErrorStorage import IErrorStorage
from ErrorType import ErrorType


class FileErrorStorage(IErrorStorage):
    """
    Implementare concreta a IErrorStorage — scrie statisticile in error_statistics.txt.

    Principiul S (SRP): singura responsabilitate este scrierea fisierului de statistici.
    Principiul O (OCP): alte implementari (DatabaseErrorStorage etc.) pot fi adaugate
                        fara a modifica aceasta clasa sau ErrorStatisticsProcessor.
    """

    def __init__(self, filepath: str = "error_statistics.txt"):
        self.filepath = filepath

    def write_statistics(self, errors: list) -> None:
        """Scrie un raport complet de statistici pentru licitatia incheiata."""
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        with open(self.filepath, 'w', encoding='utf-8') as f:
            f.write("=" * 60 + "\n")
            f.write(f"RAPORT ERORI LICITATIE\n")
            f.write(f"Generat la: {now}\n")
            f.write(f"Total erori: {len(errors)}\n")
            f.write("=" * 60 + "\n\n")

            if not errors:
                f.write("Nu au aparut erori in aceasta licitatie.\n")
            else:
                # Statistici pe tip de eroare
                f.write("--- Distributie pe tipuri de erori ---\n")
                for error_type in ErrorType:
                    count = sum(1 for e in errors if e.error_type == error_type)
                    if count > 0:
                        f.write(f"  {error_type.value:<30s}: {count}\n")
                f.write("\n")

                # Statistici pe sursa (microserviciu)
                f.write("--- Distributie pe microservicii ---\n")
                sources = {}
                for e in errors:
                    sources[e.source] = sources.get(e.source, 0) + 1
                for source, count in sorted(sources.items()):
                    f.write(f"  {source:<25s}: {count}\n")
                f.write("\n")

                # Lista detaliata a erorilor
                f.write("--- Detalii erori ---\n")
                for i, event in enumerate(errors, start=1):
                    f.write(f"  {i}. {event}\n")

            f.write("\n" + "=" * 60 + "\n")

        print(f"[FileErrorStorage] Statistici scrise in: {self.filepath}")
