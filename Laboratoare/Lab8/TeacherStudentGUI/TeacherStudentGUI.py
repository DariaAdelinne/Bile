from tkinter import *
from tkinter import ttk
import socket, threading

HOST = "localhost"
TEACHER_PORT = 1600

# Trimite o comanda catre microserviciul profesor local si intoarce raspunsul.
def send_command(cmd):
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.connect((HOST, TEACHER_PORT))
            sock.sendall((cmd + "\n").encode())
            return sock.recv(4096).decode().strip()
    except OSError as e:
        return f"Eroare conectare TeacherMicroservice: {e}"

# Trimite o intrebare din partea profesorului si afiseaza raspunsul primit.
def ask():
    dest = destination.get().strip() or "all"
    vis = visibility.get().strip() or "privat"
    q = question.get().strip()
    def worker():
        response.insert(END, send_command(f"intrebare {dest} {vis} {q}") + "\n")
    threading.Thread(target=worker, daemon=True).start()

# Trimite catre serviciul de note nota introdusa pentru student.
def grade():
    def worker():
        response.insert(END, send_command(f"grade {student.get()} {mark.get()} Nota pusa din GUI") + "\n")
    threading.Thread(target=worker, daemon=True).start()

# Cere calcularea rezultatului final pentru studentul selectat.
def finish():
    def worker():
        response.insert(END, send_command(f"finish {student.get()}") + "\n")
    threading.Thread(target=worker, daemon=True).start()

# Construirea ferestrei grafice pentru profesor.
root = Tk(); root.title("Teacher GUI")
content = ttk.Frame(root, padding=10); content.grid(sticky=(N,S,E,W))
response = Text(content, height=14, width=70); response.grid(row=0, column=0, columnspan=4)
ttk.Label(content, text="Destinatie (all/student1/student2/student3/teacher):").grid(row=1, column=0)
destination = ttk.Entry(content); destination.insert(0, "all"); destination.grid(row=1, column=1)
ttk.Label(content, text="Vizibilitate public/privat:").grid(row=1, column=2)
visibility = ttk.Entry(content); visibility.insert(0, "privat"); visibility.grid(row=1, column=3)
ttk.Label(content, text="Intrebare:").grid(row=2, column=0)
question = ttk.Entry(content, width=60); question.grid(row=2, column=1, columnspan=3)
ttk.Button(content, text="Intreaba", command=ask).grid(row=3, column=0)
ttk.Label(content, text="Student:").grid(row=4, column=0)
student = ttk.Entry(content); student.insert(0, "student1"); student.grid(row=4, column=1)
ttk.Label(content, text="Nota:").grid(row=4, column=2)
mark = ttk.Entry(content); mark.insert(0, "10"); mark.grid(row=4, column=3)
ttk.Button(content, text="Pune nota", command=grade).grid(row=5, column=0)
ttk.Button(content, text="Incheie student", command=finish).grid(row=5, column=1)
ttk.Button(content, text="Iesi", command=root.destroy).grid(row=5, column=3)
root.mainloop()
