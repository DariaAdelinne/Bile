from flask import Flask, request, jsonify
import sqlite3, os, random

# DatabaseService este serviciul central care tine starea aplicatiei
# El foloseste SQLite, o baza de date simpla stocata intr-un fisier

DB = os.environ.get('DB_PATH', '/data/lab9.db')
os.makedirs(os.path.dirname(DB), exist_ok=True)

app = Flask(__name__)

def con():
    #Deschide o conexiune la baza SQLite si permite accesul la coloane dupa nume
    c = sqlite3.connect(DB)
    c.row_factory = sqlite3.Row
    return c

def init():
    #Creeaza tabelele necesare si introduce produsele default daca nu exista deja
    with con() as db:
        db.executescript('''
                         CREATE TABLE IF NOT EXISTS products(name TEXT PRIMARY KEY, stock INTEGER, min_pending INTEGER DEFAULT 3);
                         CREATE TABLE IF NOT EXISTS pending_input(id INTEGER PRIMARY KEY AUTOINCREMENT, client TEXT, product TEXT, quantity INTEGER, address TEXT);
                         CREATE TABLE IF NOT EXISTS orders(id INTEGER PRIMARY KEY AUTOINCREMENT, client TEXT, product TEXT, quantity INTEGER, address TEXT, status TEXT, created_at TEXT);
                         CREATE TABLE IF NOT EXISTS invoices(id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, client TEXT, product TEXT, quantity INTEGER, created_at TEXT);
                         CREATE TABLE IF NOT EXISTS producer_events(id INTEGER PRIMARY KEY AUTOINCREMENT, product TEXT, quantity INTEGER, reason TEXT, created_at TEXT);
                         ''')

        # Produse initiale: nume, stoc initial, prag minim de comenzi restante.
        defaults = [
            ('Masca protectie', 5, 3),
            ('Vaccin anti-COVID-19', 2, 3),
            ('Combinezon', 4, 3),
            ('Manusa chirurgicala', 3, 3),
        ]

        for row in defaults:
            # INSERT OR IGNORE evita duplicarea produselor la fiecare restart
            db.execute('INSERT OR IGNORE INTO products(name,stock,min_pending) VALUES(?,?,?)', row)

        db.commit()


# Initializam baza cand porneste serviciul
init()


def rowdicts(rows):
    #Transforma randurile SQLite in dictionare JSON serializabile
    return [dict(r) for r in rows]


@app.route('/submit_order', methods=['POST'])
def submit_order():
    #Endpoint folosit de GuiService
    #Primeste o comanda introdusa manual in browser si o pune in tabela pending_input
    #ClientSourceMicroservice o va lua mai tarziu prin /next-order
    data = request.get_json(force=True) if request.is_json else request.form

    with con() as db:
        db.execute(
            'INSERT INTO pending_input(client,product,quantity,address) VALUES(?,?,?,?)',
            (
                data.get('client', 'Client GUI'),
                data.get('product', 'Masca protectie'),
                int(data.get('quantity', 1)),
                data.get('address', 'Adresa GUI'),
            ),
        )
        db.commit()

    return jsonify(ok=True)


@app.route('/next-order')
def next_order():
    #Endpoint folosit de ClientSourceMicroservice.
    #Returneaza urmatoarea comanda in format text:
    #client|produs|cantitate|adresa
    #Daca auto=1 si nu exista comenzi manuale, genereaza o comanda aleatoare.
    auto = request.args.get('auto', '0') == '1'

    with con() as db:
        r = db.execute('SELECT * FROM pending_input ORDER BY id LIMIT 1').fetchone()

        if r:
            # Scoatem comanda din coada pending_input ca sa nu fie procesata de doua ori.
            db.execute('DELETE FROM pending_input WHERE id=?', (r['id'],))
            db.commit()
            return f"{r['client']}|{r['product']}|{r['quantity']}|{r['address']}"

    if auto:
        product = random.choice(['Masca protectie', 'Vaccin anti-COVID-19', 'Combinezon', 'Manusa chirurgicala'])
        qty = random.choice([1, 2, 3, 4, 5, 6])
        return f"Popescu Ion|{product}|{qty}|Codrii Vlasiei nr 14"

    return ''


@app.route('/orders', methods=['POST'])
def orders():
    #Endpoint folosit de ComandaProcessorMicroservice.
    #Salveaza comanda in tabela orders cu status initial REGISTERED.
    #Intoarce id-ul comenzii create.
    d = request.get_json(force=True)

    with con() as db:
        cur = db.execute(
            'INSERT INTO orders(client,product,quantity,address,status,created_at) VALUES(?,?,?,?,?,datetime("now"))',
            (d['client'], d['product'], int(d['quantity']), d['address'], 'REGISTERED'),
        )
        db.commit()
        return jsonify(id=cur.lastrowid)


@app.route('/reserve', methods=['POST'])
def reserve():
    #Endpoint folosit de DepozitProcessorMicroservice
    #Verifica daca produsul exista si daca exista stoc suficient

    #Daca exista stoc:
      #- scade cantitatea din products.stock
      #- pune comanda pe ACCEPTED

    #Daca nu exista stoc:
      #- pune comanda pe PENDING_STOCK
      #- numara cate comenzi sunt in asteptare pentru acel produs
      #- daca numarul trece de min_pending, spune ca trebuie anuntat producatorul
    d = request.get_json(force=True)
    order_id = int(d['order_id'])
    product = d['product']
    qty = int(d['quantity'])

    with con() as db:
        p = db.execute('SELECT * FROM products WHERE name=?', (product,)).fetchone()

        if not p:
            # Daca apare un produs nou, il introducem cu stoc 0.
            db.execute('INSERT INTO products(name,stock,min_pending) VALUES(?,?,?)', (product, 0, 3))
            stock = 0
            min_pending = 3
        else:
            stock = p['stock']
            min_pending = p['min_pending']

        if stock >= qty:
            db.execute('UPDATE products SET stock=stock-? WHERE name=?', (qty, product))
            db.execute('UPDATE orders SET status=? WHERE id=?', ('ACCEPTED', order_id))
            db.commit()
            return jsonify(status='ACCEPTED', producer_needed=False, stock_after=stock - qty)

        db.execute('UPDATE orders SET status=? WHERE id=?', ('PENDING_STOCK', order_id))
        pending = db.execute(
            'SELECT COUNT(*) c FROM orders WHERE product=? AND status=?',
            (product, 'PENDING_STOCK'),
        ).fetchone()['c']
        db.commit()

        return jsonify(
            status='PENDING_STOCK',
            producer_needed=(pending >= min_pending),
            pending_count=pending,
            stock_after=stock,
        )


@app.route('/restock', methods=['POST'])
def restock():
    #Endpoint folosit de ProducerService
    #Mareste stocul unui produs si salveaza evenimentul in producer_events
    d = request.get_json(force=True)

    with con() as db:
        db.execute(
            'UPDATE products SET stock=stock+? WHERE name=?',
            (int(d.get('quantity', 50)), d['product']),
        )
        db.execute(
            'INSERT INTO producer_events(product,quantity,reason,created_at) VALUES(?,?,?,datetime("now"))',
            (d['product'], int(d.get('quantity', 50)), d.get('reason', 'producer event')),
        )
        db.commit()

    return jsonify(ok=True)


@app.route('/invoice', methods=['POST'])
def invoice():
    #Endpoint folosit de FacturareProcessorMicroservice
    #Creeaza factura si modifica statusul comenzii in INVOICED
    d = request.get_json(force=True)

    with con() as db:
        cur = db.execute(
            'INSERT INTO invoices(order_id,client,product,quantity,created_at) VALUES(?,?,?,?,datetime("now"))',
            (int(d['order_id']), d['client'], d['product'], int(d['quantity'])),
        )
        db.execute('UPDATE orders SET status=? WHERE id=?', ('INVOICED', int(d['order_id'])))
        db.commit()
        return jsonify(invoice_id=cur.lastrowid)


@app.route('/state')
def state():
    #Endpoint citit de GuiService
    #Intoarce starea curenta: produse, ultimele comenzi, facturi si evenimente de producator
    with con() as db:
        return jsonify(
            products=rowdicts(db.execute('SELECT * FROM products').fetchall()),
            orders=rowdicts(db.execute('SELECT * FROM orders ORDER BY id DESC LIMIT 25').fetchall()),
            invoices=rowdicts(db.execute('SELECT * FROM invoices ORDER BY id DESC LIMIT 25').fetchall()),
            producer_events=rowdicts(db.execute('SELECT * FROM producer_events ORDER BY id DESC LIMIT 25').fetchall()),
        )


@app.route('/')
def home():
    #Endpoint simplu de health-check, util pentru verificare in browser
    return jsonify(service='DatabaseService', status='ok')


if __name__ == '__main__':
    # host='0.0.0.0' permite accesul din afara containerului Docker.
    app.run(host='0.0.0.0', port=2300)
