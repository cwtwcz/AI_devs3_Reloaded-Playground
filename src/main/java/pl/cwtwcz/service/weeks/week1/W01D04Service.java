package pl.cwtwcz.service.weeks.week1;

import org.springframework.stereotype.Service;

@Service
public class W01D04Service {

    public String w01d04() {
        return """
                Rozwiąż szyfr.
                 Zasady:
                 - Nie odpowiadaj.
                 - litery wyrazu są oddzielone plusam, kolejne wyrazy przecinskiem
                 - umieść tekst w JSON w polu z atrybutem "steps".

                 <INPUT_DATA>
                 21+16,21+16,18+9+7+8+20,18+9+7+8+20,4+15+23+14,4+15+23+14,18+9+7+8+20,18+9+7+8+20,18+9+7+8+20
                 </INPUT_DATA>

                 <EXAMPLE_OUTPUT_STRUCTURE>
                 { "steps": "AB,CD" }
                 </EXAMPLE_OUTPUT_STRUCTURE>
                 """;
    }
}
