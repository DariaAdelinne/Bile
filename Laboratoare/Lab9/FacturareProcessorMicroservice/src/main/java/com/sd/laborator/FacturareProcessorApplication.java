package com.sd.laborator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.integration.annotation.Transformer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/*
 * FacturareProcessorApplication emite factura pentru comenzile acceptate de depozit
 *
 * Rolul lui:
 * 1. Primeste mesaj de forma:
 *    status|id|client|produs|cantitate|adresa
 * 2. Daca statusul este ACCEPTED, creeaza factura in DatabaseService prin POST /invoice
 * 3. Daca statusul nu este ACCEPTED, nu emite factura
 * 4. Trimite mai departe rezultatul catre LivrareSinkMicroservice
 */
@EnableBinding(Processor.class)
@SpringBootApplication
public class FacturareProcessorApplication {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String dbUrl = System.getenv().getOrDefault("DB_URL", "http://localhost:2300");

    @Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
    public String emitereFactura(String mesaj) {
        // Mesaj primit: status|id|client|produs|cantitate|adresa
        String[] p = mesaj.split("\\|", -1);

        String status = p[0];
        int orderId = Integer.parseInt(p[1]);
        String client = p[2];
        String produs = p[3];
        String cantitate = p[4];
        String adresa = p[5];

        // Daca depozitul nu a acceptat comanda, nu putem factura si livra
        if (!"ACCEPTED".equals(status)) {
            System.out.println("[FACTURARE] Nu emit factura pentru comanda " + orderId + " deoarece este " + status);
            return "NEFACTURAT|" + orderId + "|" + client + "|" + produs + "|" + cantitate + "|" + adresa;
        }

        // Pregatim cererea pentru crearea facturii in baza de date
        Map<String, Object> req = new HashMap<>();
        req.put("order_id", orderId);
        req.put("client", client);
        req.put("product", produs);
        req.put("quantity", Integer.parseInt(cantitate.trim()));

        // DatabaseService intoarce id-ul facturii create
        Map resp = restTemplate.postForObject(dbUrl + "/invoice", req, Map.class);
        Object invoice = resp == null ? "0" : resp.get("invoice_id");

        System.out.println("[FACTURARE] S-a emis factura " + invoice + " pentru comanda " + orderId);

        // Mesajul final merge catre microserviciul de livrare
        return "FACTURAT|" + invoice + "|" + orderId + "|" + client + "|" + produs + "|" + cantitate + "|" + adresa;
    }

    public static void main(String[] args) {
        SpringApplication.run(FacturareProcessorApplication.class, args);
    }
}
