package com.sd.laborator;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

import java.util.List;
import java.util.Map;

@Controller("/perechi")
public class PerechiController {
    private final VerificareService verificareService;

    public PerechiController(VerificareService verificareService) {
        this.verificareService = verificareService;
    }

    @Post(uri = "/")
    public PerechiResponse execute(@Body PerechiRequest request) {
        if (request == null) {
            request = new PerechiRequest();
        }

        PerechiResponse response = new PerechiResponse();

        if (request.getSize() <= 0) {
            response.setMessage("Eroare: size trebuie sa fie mai mare decat 0.");
            return response;
        }

        if (request.getMinValue() > request.getMaxValue()) {
            response.setMessage("Eroare: minValue nu poate fi mai mare decat maxValue.");
            return response;
        }

        ADTA a = new ADTA();
        ADTB b = new ADTB();

        a.initializeRandom(request.getSize(), request.getMinValue(), request.getMaxValue());
        b.initializeRandom(request.getSize(), request.getMinValue(), request.getMaxValue());

        List<Map<String, Integer>> perechi = verificareService.gasestePerechile(a.getValues(), b.getValues());

        String mesaj = perechi.isEmpty()
                ? "Nu s-au gasit perechi care satisfac a*b == a + b*3."
                : "S-au gasit " + perechi.size() + " pereche(i) distincte care satisfac a*b == a + b*3.";

        response.setMessage(mesaj);
        response.setAValues(a.getValues());
        response.setBValues(b.getValues());
        response.setPerechi(perechi);

        return response;
    }
}
