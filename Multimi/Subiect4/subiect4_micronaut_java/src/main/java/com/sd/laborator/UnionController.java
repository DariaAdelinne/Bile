package com.sd.laborator;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller("/union")
public class UnionController {
    private final UnionService service;

    public UnionController(UnionService service) {
        this.service = service;
    }

    @Post(uri = "/")
    public UnionResponse execute(@Body UnionRequest request) {
        if (request == null) {
            request = new UnionRequest();
        }

        UnionResponse response = new UnionResponse();

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
        ADTC c = new ADTC();

        a.initializeRandom(request.getSize(), request.getMinValue(), request.getMaxValue());
        b.initializeRandom(request.getSize(), request.getMinValue(), request.getMaxValue());
        service.calculate(a, b, c);

        response.setMessage("Calcul efectuat cu succes.");
        response.setAValues(a.getValues());
        response.setBValues(b.getValues());
        response.setCValues(c.getValues());

        return response;
    }
}
