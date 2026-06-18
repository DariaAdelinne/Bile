from flask import Flask, request, jsonify
import os, requests

# ProducerService simuleaza producatorul
# El este apelat de DepozitProcessorMicroservice atunci cand nu mai exista suficient stoc
# Cand primeste o cerere de productie, apeleaza DatabaseService /restock pentru a mari stocul

app = Flask(__name__)

DB_URL = os.environ.get('DB_URL', 'http://database:2300')

@app.route('/produce', methods=['POST'])
def produce():
    #Primeste un eveniment de productie
      #"product": "Masca protectie",
      #"quantity": 50,
      #"reason": "minim 3 comenzi neonerate pentru acelasi produs"

    data = request.get_json(force=True)
    product = data.get('product', 'Produs necunoscut')
    qty = int(data.get('quantity', 50))
    reason = data.get('reason', 'cerere depozit')

    print(f'[PRODUCATOR] Eveniment primit: produc {qty} bucati din {product}. Motiv: {reason}', flush=True)

    try:
        # Anuntam DatabaseService ca stocul produsului trebuie marit
        requests.post(
            DB_URL + '/restock',
            json={'product': product, 'quantity': qty, 'reason': reason},
            timeout=5,
            )
        print(f'[PRODUCATOR] Am actualizat stocul pentru {product}', flush=True)
    except Exception as e:
        print(f'[PRODUCATOR] Nu pot actualiza DB: {e}', flush=True)

    return jsonify(ok=True)


@app.route('/')
def home():
    return jsonify(service='ProducerService', status='ok')


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=2400)
