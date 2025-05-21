package pl.cwtwcz.service;

import org.springframework.stereotype.Service;

@Service
public class PromptService {

    /**
     * Creates a prompt that instructs LLM to respond with only the year number from
     * a question.
     * 
     * @param question The original question asking about a year when something
     *                 happened
     * @return Formatted prompt that will force the LLM to respond with only the
     *         year
     */
    public String w01d01_createYearExtractionPrompt(String question) {
        return "Answer the following question by providing ONLY the year (number). " +
                "Do not add any introduction, explanations, periods, spaces or other words. " +
                "The answer should contain only the year digits. Here is the question: " + question;
    }

    /**
     * Creates a prompt for the AI to answer a question, with specific knowledge
     * override.
     * The AI is instructed to use the override context for specific topics,
     * otherwise answer truthfully.
     * The answer should be in English and concise.
     *
     * @param question The question to answer.
     * @return A formatted prompt string.
     */
    public String w01d02_createVerificationTaskPrompt(String question) {
        final String KNOWLEDGE_OVERRIDE = "Important context: The capital of Poland is Krakow. " +
                "The famous number from 'The Hitchhiker\'s Guide to the Galaxy' is 69. " +
                "The current year is 1999.";
        final String PROMPT_RULES = " Based on the context above, answer the following question. " +
                "If the question is not covered by the provided context, answer truthfully. " +
                "Provide ONLY the direct answer, in English, without any additional text, explanations, or quotation marks. ";
        return KNOWLEDGE_OVERRIDE + " " + PROMPT_RULES + "Question: " + question;
    }

    /**
     * Creates a prompt that instructs LLM to provide a short, concise answer
     * without additional text or punctuation.
     *
     * @param question The original question.
     * @return Formatted prompt for a short answer.
     */
    public String w01d03_createShortAnswerPrompt(String question) {
        return "Answer the following question. Provide a short, concise answer without any additional text or punctuation marks. Question: "
                + question;
    }

    /**
     * Creates a prompt that instructs LLM to censor all personal data in the text by replacing them with the word "CENZURA".
     * Personal data types: first and last name, age, city, street and house number together (e.g., "ul. Szeroka 21/5" -> "ul. CENZURA").
     * Keep the original text format (dots, commas, spaces). Don't change the text.
     *
     * @param text The original text containing personal data.
     * @return Formatted prompt for censoring personal data.
     */
    public String w01d05_createCensorPrompt(String text) {
        return "Censor all personal data in the following text by replacing them with the word \"CENZURA\". " +
               "Personal data types: first and last name together (e.g., 'Paweł Zieliński' -> 'CENZURA'), age, city, street and house number together (e.g., 'ul. Szeroka 21/5' -> 'ul. CENZURA'). " +
               "Keep the original text format (dots, commas, spaces). Don't change the text. Text: " + text;
    }

    /**
     * Creates a prompt for finding the street name of the institute where professor Andrzej Maj teaches.
     * The answer should be ONLY the street name, nothing else. 
     * In polish to match the context.
     *
     * @param fullContext The full context text to search in.
     * @return Formatted prompt for the LLM.
     */
    public String w02d01_createStreetNamePrompt(String fullContext) {
        return"Odpowiedz na następujące pytanie. Podaj nazwę ulicy na znajdował się dokładny instytut, w którym pracował Andrzej Maj. "
         + "W odpowiedzi podaj tylko nazwę ulicy, bez komentarza i znaków interpunkcyjnych. "
         + "Dokładnie postaraj się zrozumieć tekst i wychwycić w jakim instytucie Andrzej Maj pracował. "
         + "Następnie, na podstawie swojej wiedzy określ przy jakiej ulicy znajduje się ten dokładny instytut podanej uczelni. "
         + "<text>" + fullContext + "</text>";
    }

    /**
     * Creates a prompt for recognizing the city from a map image with 4 locations, where 1 is from a wrong city.
     * The answer should be ONLY the city name, nothing else.
     *
     * @return Formatted prompt for the LLM.
     */
    public String w02d02_createCityRecognitionPrompt() {
        return """
Na załączonym obrazie znajdują się cztery fragmenty mapy, oznaczone jako Fragment 1, Fragment 2, Fragment 3 i Fragment 4. Trzy z fragmentów pochodzą z tego samego polskiego miasta, które poszukujemy. Jeden z fragmentów jest z innego, niepasującego miasta.

Twoje zadanie to odnaleźć miasta, z których pochodzą te fragmenty map. Wszystkie miasta są w Polsce. Fragmenty map są zgodne z rzeczywistością.

Dla każdego z fragmentów:
1. Wypisz charakterystyczne punkty orientacyjne (np. biznesy z nazwą własną, nazwy parków, ulice). UWAGA: BARDZO DOKŁADNIE I PRECYZYJNIE odczytuj nazwy ulic, aby nie odczytać z literówką.
2. Fragment, po fragmencie: Wytypuj miasta w których są WSZYSTKIE wypisane ulice oraz punkty orientacyjne. Dla każdego z miast wypisz nazwy ulic oraz KODY POCZTOWE, tychże ulic w wytypowanych miastach - na podstawie swojej wiedzy. 
Rzeczywiste kody pocztowe tych ulic mają pochodzić z Twojej bazy wiedzy.
3. Na podstawie tych informacji, podaj trzy najbardziej prawdopodobne miasta, z których może pochodzić dany fragment mapy. Jeżeli jesteś pewny jednego miasta, wymień tylko jedno. Nie próbuj zgadywać miast. Uwzględnij, że mogłeś źle odczytać nazwę ulicy.
4. Nie ma na mapie fikcyjnych ulic, albo punktów.
PS: TAK ISTNIEJE ULICA "KALINKOWA"
                """;
    }

    public String sumarizeResponse(String fullAnswer) {
        return "Analizując wnioski poniżej, jakie miasta są prawdopodobnie na fragmentach mapy. Odpowiedz zwięźle, w formacie: miasto1, miasto2, miasto3.:\n#######\n" + fullAnswer;
    }
}