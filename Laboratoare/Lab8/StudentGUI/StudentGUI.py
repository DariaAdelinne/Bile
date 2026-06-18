from tkinter import *
from tkinter import ttk
import socket, threading

HOST = "localhost"

# Trimite o comanda catre microserviciul student local si returneaza raspunsul primit.
def send_command(port, cmd):
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.connect((HOST, int(port)))
            sock.sendall((cmd + "\n").encode())
            return sock.recv(4096).decode().strip()
    except OSError as e:
        return f"Eroare conectare StudentMicroservice: {e}"

# Preia datele din formular si trimite o intrebare catre destinatia selectata.
def ask():
    p = port.get().strip() or "1701"
    dest = destination.get().strip() or "teacher"
    vis = visibility.get().strip() or "privat"
    q = question.get().strip()
    def worker():
        response.insert(END, send_command(p, f"intrebare {dest} {vis} {q}") + "\n")
    threading.Thread(target=worker, daemon=True).start()

# Trimite un mesaj public prin studentul local catre ceilalti participanti.
def public_msg():
    def worker(): response.insert(END, send_command(port.get(), f"mesaj_public {message.get()}") + "\n")
    threading.Thread(target=worker, daemon=True).start()

# Trimite un mesaj privat catre destinatia completata in interfata.
def private_msg():
    def worker(): response.insert(END, send_command(port.get(), f"mesaj_privat {destination.get()} {message.get()}") + "\n")
    threading.Thread(target=worker, daemon=True).start()

# Construirea ferestrei grafice pentru student.
root = Tk(); root.title("Student GUI")
content = ttk.Frame(root, padding=10); content.grid(sticky=(N,S,E,W))
response = Text(content, height=14, width=75); response.grid(row=0, column=0, columnspan=4)
ttk.Label(content, text="Port student local (1701/1702/1703):").grid(row=1, column=0)
port = ttk.Entry(content); port.insert(0, "1701"); port.grid(row=1, column=1)
ttk.Label(content, text="Destinatie:").grid(row=1, column=2)
destination = ttk.Entry(content); destination.insert(0, "teacher"); destination.grid(row=1, column=3)
ttk.Label(content, text="Vizibilitate public/privat:").grid(row=2, column=0)
visibility = ttk.Entry(content); visibility.insert(0, "privat"); visibility.grid(row=2, column=1)
ttk.Label(content, text="Intrebare:").grid(row=3, column=0)
question = ttk.Entry(content, width=60); question.grid(row=3, column=1, columnspan=3)
ttk.Button(content, text="Intreaba", command=ask).grid(row=4, column=0)
ttk.Label(content, text="Mesaj:").grid(row=5, column=0)
message = ttk.Entry(content, width=60); message.grid(row=5, column=1, columnspan=3)
ttk.Button(content, text="Mesaj public", command=public_msg).grid(row=6, column=0)
ttk.Button(content, text="Mesaj privat", command=private_msg).grid(row=6, column=1)
ttk.Button(content, text="Iesi", command=root.destroy).grid(row=6, column=3)
root.mainloop()
