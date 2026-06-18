import pika
import time

connection = pika.BlockingConnection(
    pika.ConnectionParameters('localhost')
)
channel = connection.channel()

channel.queue_declare(queue='library_queue')
channel.queue_declare(queue='library_response_queue')


def clear_response_queue():
    while True:
        method_frame, header_frame, body = channel.basic_get(
            queue='library_response_queue',
            auto_ack=True
        )
        if body is None:
            break


def send_and_receive(msg):
    clear_response_queue()

    channel.basic_publish(exchange='', routing_key='library_queue', body=msg)
    print("Sent:", msg)

    for _ in range(20):
        method_frame, header_frame, body = channel.basic_get(
            queue='library_response_queue',
            auto_ack=True
        )
        if body:
            response = body.decode()
            print("Response:\n", response)
            return response
        time.sleep(0.2)

    print("Nu a venit niciun raspuns.")
    return None


def choose_format():
    print("Alege formatul:")
    print("1 - JSON")
    print("2 - HTML")
    print("3 - Text")
    print("4 - XML")
    opt = input("Format: ")

    if opt == "1":
        return "json"
    if opt == "2":
        return "html"
    if opt == "3":
        return "raw"
    return "xml"


def save_to_file(content, file_format):
    if content is None:
        print("Nu exista continut de salvat.")
        return

    extension = {
        "json": "json",
        "html": "html",
        "raw": "txt",
        "xml": "xml"
    }[file_format]

    filename = input(f"Nume fisier (fara extensie): ")
    full_name = f"{filename}.{extension}"

    with open(full_name, "w", encoding="utf-8") as f:
        f.write(content)

    print(f"Fisier salvat: {full_name}")


while True:
    print("\nAlege comanda:")
    print("1 - vezi toate cartile")
    print("2 - vezi o carte dupa id")
    print("3 - adauga o carte")
    print("4 - cauta dupa autor")
    print("5 - cauta dupa titlu")
    print("6 - cauta dupa editura")
    print("7 - afiseaza toate cartile in format ales")
    print("8 - cauta si salveaza in fisier")
    print("0 - iesire")

    cmd = input("Optiune: ")

    if cmd == "1":
        send_and_receive("get_books")

    elif cmd == "2":
        book_id = input("ID: ")
        send_and_receive(f"get_book:{book_id}")

    elif cmd == "3":
        book_id = input("ID: ")
        title = input("Titlu: ")
        author = input("Autor: ")
        publisher = input("Editura: ")
        text = input("Text: ")
        send_and_receive(f"add_book:{book_id}:{title}:{author}:{publisher}:{text}")

    elif cmd == "4":
        author = input("Autor: ")
        send_and_receive(f"find_author:{author}|json")

    elif cmd == "5":
        title = input("Titlu: ")
        send_and_receive(f"find_title:{title}|json")

    elif cmd == "6":
        publisher = input("Editura: ")
        send_and_receive(f"find_publisher:{publisher}|json")

    elif cmd == "7":
        fmt = choose_format()
        send_and_receive(f"print:{fmt}")

    elif cmd == "8":
        print("Cautare dupa:")
        print("1 - autor")
        print("2 - titlu")
        print("3 - editura")
        search_type = input("Optiune: ")
        value = input("Valoare cautata: ")
        fmt = choose_format()

        if search_type == "1":
            response = send_and_receive(f"find_author:{value}|{fmt}")
        elif search_type == "2":
            response = send_and_receive(f"find_title:{value}|{fmt}")
        elif search_type == "3":
            response = send_and_receive(f"find_publisher:{value}|{fmt}")
        else:
            print("Tip cautare invalid")
            response = None

        save_to_file(response, fmt)

    elif cmd == "0":
        print("Bye")
        break

    else:
        print("Comanda invalida")

connection.close()