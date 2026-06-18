from datetime import datetime
from IMessageStatsStorage import IMessageStatsStorage


class FileMessageStatsStorage(IMessageStatsStorage):
    """
    Implementare concreta — scrie statisticile in message_statistics.txt.

    Principiul S (SRP): singura responsabilitate este scrierea raportului.
    Principiul O (OCP): alte implementari pot fi adaugate fara a modifica
                        MessageStatisticsProcessor.
    """

    def __init__(self, filepath: str = "message_statistics.txt"):
        self.filepath = filepath

    def write_statistics(self, events: list) -> None:
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        total = len(events)

        with open(self.filepath, 'w', encoding='utf-8') as f:
            f.write("=" * 60 + "\n")
            f.write("RAPORT STATISTICI MESAJE LICITATIE\n")
            f.write(f"Generat la: {now}\n")
            f.write(f"Total mesaje procesate: {total}\n")
            f.write("=" * 60 + "\n\n")

            if not events:
                f.write("Nu au fost procesate mesaje in aceasta licitatie.\n")
            else:
                # Statistici pe microserviciu
                f.write("--- Mesaje per microserviciu ---\n")
                sources = {}
                for e in events:
                    sources[e.source] = sources.get(e.source, 0) + 1
                for source, count in sorted(sources.items()):
                    f.write(f"  {source:<25s}: {count} mesaje\n")
                f.write("\n")

                # Statistici SENT vs RECEIVED
                sent_count = sum(1 for e in events if e.direction == "SENT")
                recv_count = sum(1 for e in events if e.direction == "RECEIVED")
                f.write("--- Directie mesaje ---\n")
                f.write(f"  {'SENT':<25s}: {sent_count}\n")
                f.write(f"  {'RECEIVED':<25s}: {recv_count}\n\n")

                # Statistici per microserviciu si directie
                f.write("--- Mesaje per microserviciu si directie ---\n")
                breakdown = {}
                for e in events:
                    key = (e.source, e.direction)
                    breakdown[key] = breakdown.get(key, 0) + 1
                for (source, direction), count in sorted(breakdown.items()):
                    f.write(f"  {source:<20s} {direction:<10s}: {count}\n")
                f.write("\n")

                # Lista cronologica completa
                f.write("--- Cronologie mesaje ---\n")
                for i, event in enumerate(events, start=1):
                    f.write(f"  {i:3d}. {event}\n")

            f.write("\n" + "=" * 60 + "\n")

        print(f"[FileMessageStatsStorage] Statistici scrise in: {self.filepath}")
