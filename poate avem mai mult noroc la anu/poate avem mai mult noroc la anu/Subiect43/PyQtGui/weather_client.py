"""
weather_client.py - clientul TCP al interfetei grafice catre VisualizationService.

Este separat de codul PyQt ca sa poata fi testat si fara display (headless): tot dialogul prin
socket e aici, iar GUI-ul doar il apeleaza. Protocol (text, o linie per mesaj):
    GUI -> VisualizationService:  "GET <oras>"   /  "HISTORY"
    VisualizationService -> GUI:  "OK <oras>|<tara>|<temp>|<vant>|<descriere>|<ora>|<sursa>|<replicaId>"
                                  "ERR <oras>|<motiv>"
                                  (la HISTORY: cate o linie OK, terminat cu "END")
"""
import socket

DEFAULT_HOST = "localhost"
DEFAULT_PORT = 1700


def _send(host, port, message, multiline=False, timeout=8.0):
    """Trimite o comanda si intoarce raspunsul (o linie, sau toate liniile pana la 'END')."""
    with socket.create_connection((host, port), timeout=timeout) as s:
        s.sendall((message + "\n").encode("utf-8"))
        f = s.makefile("r", encoding="utf-8")
        if not multiline:
            line = f.readline()
            return line.strip() if line else ""
        lines = []
        for line in f:
            line = line.strip()
            if line == "END":
                break
            if line:
                lines.append(line)
        return lines


def parse_ok(line):
    """Transforma o linie 'OK a|b|c|...' intr-un dictionar; None daca nu e OK."""
    if not line.startswith("OK "):
        return None
    fields = line[3:].split("|")
    keys = ["city", "country", "temperature", "windspeed", "description", "time", "source", "replica"]
    data = dict(zip(keys, fields))
    return data


def query(city, host=DEFAULT_HOST, port=DEFAULT_PORT):
    """Cere vremea pentru un oras. Intoarce (dict_or_None, raw_line)."""
    raw = _send(host, port, "GET " + city)
    return parse_ok(raw), raw


def history(host=DEFAULT_HOST, port=DEFAULT_PORT):
    """Intoarce lista de elemente citite (dict-uri) din istoricul serviciului de vizualizare."""
    lines = _send(host, port, "HISTORY", multiline=True)
    return [parse_ok(l) for l in lines if parse_ok(l)]


if __name__ == "__main__":
    # mic test din linia de comanda:  python weather_client.py Bucuresti
    import sys
    c = sys.argv[1] if len(sys.argv) > 1 else "Bucuresti"
    data, raw = query(c)
    print("RAW:", raw)
    print("PARSED:", data)
    print("HISTORY:", history())
