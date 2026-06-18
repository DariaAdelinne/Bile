from flask import Flask, request, redirect
import os, requests, html

# GuiService este interfata web simpla a proiectului
# Ea nu proceseaza direct pipeline-ul RabbitMQ
# Rolul ei este:
# 1. Trimite comenzi manuale catre DatabaseService /submit_order
# 2. Afiseaza starea curenta din DatabaseService /state

app = Flask(__name__)

DB_URL = os.environ.get('DB_URL', 'http://database:2300')

@app.route('/', methods=['GET', 'POST'])
def home():
    if request.method == 'POST':
        # Formularul HTML trimite client, product, quantity si address
        # DatabaseService le pune in tabela pending_input
        requests.post(DB_URL + '/submit_order', data=request.form, timeout=5)
        return redirect('/')

    # Pentru afisare, luam intreaga stare a aplicatiei din DatabaseService
    state = requests.get(DB_URL + '/state', timeout=5).json()
    products = state['products']
    orders = state['orders']
    invoices = state['invoices']
    events = state['producer_events']

    # Construim optiunile din <select> pe baza produselor existente
    # html.escape previne stricarea HTML-ului daca valorile contin caractere speciale.
    product_options = ''.join(f"<option>{html.escape(p['name'])}</option>" for p in products)

    def table(rows):
        # Transforma o lista de dictionare intr-un tabel HTML simplu
        if not rows:
            return '<p>Nu exista inregistrari.</p>'

        keys = list(rows[0].keys())
        head = ''.join(f'<th>{html.escape(k)}</th>' for k in keys)
        body = ''.join(
            '<tr>' + ''.join(f'<td>{html.escape(str(r[k]))}</td>' for k in keys) + '</tr>'
            for r in rows
        )
        return f'<table border="1" cellpadding="4"><tr>{head}</tr>{body}</table>'

    # Pagina este scrisa direct ca string HTML
    return f'''
        <h1>Lab 9 - GUI comenzi</h1>

        <form method="post">
            Client: <input name="client" value="Ionescu Maria"><br>
            Produs: <select name="product">{product_options}</select><br>
            Cantitate: <input name="quantity" value="2" type="number"><br>
            Adresa: <input name="address" value="Strada Exemplu 10"><br>
            <button type="submit">Trimite comanda in pipeline</button>
        </form>

        <h2>Stocuri</h2>
        {table(products)}

        <h2>Ultimele comenzi</h2>
        {table(orders)}

        <h2>Facturi</h2>
        {table(invoices)}

        <h2>Evenimente producator</h2>
        {table(events)}

        <p>Refresh dupa 10-20 secunde ca sa vezi pipeline-ul procesand comanda.</p>
    '''


if __name__ == '__main__':
    # Portul 2500 este expus in docker-compose.yml ca 2500:2500.
    app.run(host='0.0.0.0', port=2500)
