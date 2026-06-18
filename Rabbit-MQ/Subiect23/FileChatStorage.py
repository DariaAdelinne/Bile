from datetime import datetime
from IChatStorage import IChatStorage


class FileChatStorage(IChatStorage):
    """
    Implementare concreta — salveaza istoricul conversatiei in
    fisierul 'chat_history_<session_id>.txt'.

    Principiul S (SRP): singura responsabilitate este persistenta istoricului.
    Principiul O (OCP): alte implementari (DatabaseChatStorage etc.) pot fi
                        adaugate fara a modifica ChatMasterProcessor.
    """

    def save_history(self, session_id: str, participants: list, messages: list) -> None:
        filename = f"chat_history_{session_id}.txt"
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        with open(filename, 'w', encoding='utf-8') as f:
            f.write("=" * 60 + "\n")
            f.write(f"ISTORIC CONVERSATIE PRIVATA\n")
            f.write(f"Sesiune: {session_id}\n")
            f.write(f"Salvat la: {now}\n")
            f.write(f"Participanti: {', '.join(participants)}\n")
            f.write(f"Total mesaje: {len(messages)}\n")
            f.write("=" * 60 + "\n\n")

            if not messages:
                f.write("(niciun mesaj schimbat)\n")
            else:
                for msg in messages:
                    f.write(f"{msg}\n")

            f.write("\n" + "=" * 60 + "\n")

        print(f"[FileChatStorage] Istoricul salvat in: {filename}")
