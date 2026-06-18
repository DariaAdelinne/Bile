package com.sd.laborator;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller("/calculate")
public class SumSquaresController {
    private final SumSquaresService service;

    public SumSquaresController(SumSquaresService service) {
        this.service = service;
    }

    @Post(uri = "/")
    public SumSquaresResponse execute(@Body SumSquaresRequest request) {
        if (request == null) {
            request = new SumSquaresRequest();
        }

        SumSquaresResponse response = new SumSquaresResponse();

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
        service.calculate(a, b);

        response.setMessage("Calcul efectuat cu succes.");
        response.setAValues(a.getValues());
        response.setBValues(b.getResults());

        return response;
    }
}
