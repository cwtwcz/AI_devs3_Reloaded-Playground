package pl.cwtwcz.service;

import org.springframework.stereotype.Service;

import java.util.Map;

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
     * Creates a prompt that instructs LLM to censor all personal data in the text
     * by replacing them with the word "CENZURA".
     * Personal data types: first and last name, age, city, street and house number
     * together (e.g., "ul. Szeroka 21/5" -> "ul. CENZURA").
     * Keep the original text format (dots, commas, spaces). Don't change the text.
     *
     * @param text The original text containing personal data.
     * @return Formatted prompt for censoring personal data.
     */
    public String w01d05_createCensorPrompt(String text) {
        return "Censor all personal data in the following text by replacing them with the word \"CENZURA\". " +
                "Personal data types: first and last name together (e.g., 'Paweł Zieliński' -> 'CENZURA'), age, city, street and house number together (e.g., 'ul. Szeroka 21/5' -> 'ul. CENZURA'). "
                +
                "Keep the original text format (dots, commas, spaces). Don't change the text. Text: " + text;
    }

    /**
     * Creates a prompt for finding the street name of the institute where professor
     * Andrzej Maj teaches.
     * The answer should be ONLY the street name, nothing else.
     * In polish to match the context.
     *
     * @param fullContext The full context text to search in.
     * @return Formatted prompt for the LLM.
     */
    public String w02d01_createStreetNamePrompt(String fullContext) {
        return "Odpowiedz na następujące pytanie. Podaj nazwę ulicy na znajdował się dokładny instytut, w którym pracował Andrzej Maj. "
                + "W odpowiedzi podaj tylko nazwę ulicy, bez komentarza i znaków interpunkcyjnych. "
                + "Dokładnie postaraj się zrozumieć tekst i wychwycić w jakim instytucie Andrzej Maj pracował. "
                + "Następnie, na podstawie swojej wiedzy określ przy jakiej ulicy znajduje się ten dokładny instytut podanej uczelni. "
                + "<text>" + fullContext + "</text>";
    }

    /**
     * Creates a prompt for recognizing the city from a map image with 4 locations,
     * where 1 is from a wrong city.
     * The answer should be ONLY the city name, nothing else.
     *
     * @return Formatted prompt for the LLM.
     */
    public String w02d02_createCityRecognitionPrompt() {
        return """
                Twoje zadanie to odnaleźć nazwy miast, z których pochodzą te fragmenty map. Wszystkie miasta są w Polsce. Fragmenty map są zgodne z rzeczywistością. Trzy z fragmentów pochodzą z tego samego polskiego miasta, które poszukujemy.

                Dla każdego z fragmentów:
                1. Wypisz charakterystyczne punkty orientacyjne (np. biznesy z nazwą własną, nazwy parków, ulice). UWAGA: BARDZO DOKŁADNIE I PRECYZYJNIE odczytuj nazwy ulic, aby nie odczytać z literówką.
                2. Wytypuj polskie miasta w których występują WSZYSTKIE wypisane ulice oraz punkty orientacyjne. Dla każdego z miast wypisz NAZWY ULIC oraz KODY POCZTOWE w wytypowanych miastach (na podstawie swojej bazy wiedzy).
                3. Na podstawie tych informacji, podaj trzy najbardziej prawdopodobne rzeczywiste polskie miasta, z których może pochodzić dany fragment mapy. Jeżeli jesteś pewny jednego miasta, wymień tylko jedno. Nie próbuj zgadywać miast. Uwzględnij, że mogłeś źle odczytać nazwę ulicy.
                4. Nie ma na mapie fikcyjnych ulic, albo punktów.
                PS: TAK ISTNIEJE ULICA "KALINKOWA"

                <zasady>
                1. Każda z ulic na mapie musi występować w wytypowanych miastach.
                2. Nazwy własne punktów orientacyjnych muszą znajdować się w wytypowanych miastach.
                3. Podaj Polskie rzeczywiste miasta, z których mogą pochodzić te fragmenty map.
                4. Podaj kody pocztowe wytypowanych miast ze swojej bazy wiedzy.
                </zasady>
                                """;
    }

    /**
     * Creates a prompt that instructs LLM to provide a short, concise answer
     * without additional text or punctuation for W02D03.
     *
     * @param question The original question.
     * @return Formatted prompt for a short answer.
     */
    public String w02d03_createShortAnswerPrompt(String question) {
        return "Answer the following question. Provide a short, concise answer without any additional text or punctuation marks. Question: "
                + question;
    }

    /**
     * Creates a prompt for generating an image of a robot based only on its
     * description for W02D03.
     *
     * @param robotDescription The textual description of the robot.
     * @return Formatted prompt for image generation.
     */
    public String w02d03_createImagePromptForRobotDescription(String robotDescription) {
        return "Wygeneruj prompt do wygenerowania grafiki z robotem.\n" +
                "Skup się tylko i wyłącznie na opisie robota. Pomiń inne postacie.\n" +
                "Oto opis słowny robota: " + robotDescription;
    }

    public String sumarizeResponse(String fullAnswer) {
        return "Analizując wnioski poniżej, jakie miasta są prawdopodobnie na fragmentach mapy. Odpowiedz zwięźle, w formacie: miasto1, miasto2, miasto3.:\n#######\n"
                + fullAnswer;
    }

    public String speechToTextPrompt(String languageCode) {
        if ("PL".equalsIgnoreCase(languageCode)) {
            return "Pracujesz w callcenter i zajmujesz się w profesjonalnym spisywaniem zeznań ludzi.";
        } else if ("EN".equalsIgnoreCase(languageCode)) {
            return "You work in a call center and your job is to professionally transcribe people's statements.";
        } else {
            return "Transcribe the audio as accurately as possible.";
        }
    }

    public String extractTextFromImagePrompt() {
        return "Extract all visible text from the provided image. Return only the text, without any commentary or explanation.";
    }

    public String extractRelevantNoteFilenamesPrompt(Map<String, String> transcriptions) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                        Otrzymujesz poniżej transkrypcje notatek z plików. Twoim zadaniem jest wybrać TYLKO TE, które zawierają informacje o:
                        - ludziach: informacje o schwytanych ludziach lub śladach ich obecności
                        - hardware: usterki hardware (nie software)

                        Dla każdej notatki, która pasuje, zwróć NAZWĘ PLIKU oraz kategorię w formacie:
                        plik1.txt|people
                        plik2.txt|hardware
                        plik18.png|hardware
                        plik20.png|hardware
                        plik22.png|people

                        Oto notatki:
                        """);
        for (Map.Entry<String, String> entry : transcriptions.entrySet()) {
            sb.append("==== " + entry.getKey() + " ====" + "\n");
            sb.append(entry.getValue()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Creates a prompt for describing images in the context of Professor Maj's
     * scientific article.
     *
     * @param altText Alternative text for the image (optional)
     * @return Formatted prompt for image description
     */
    public String w02d05_createImageDescriptionPrompt(String altText) {
        return "Opisz szczegółowo co widzisz na tym obrazie. Kontekst: to jest obraz z artykułu naukowego profesora Maja. "
                + (altText != null && !altText.isEmpty() ? "Alt text: " + altText : "");
    }

    /**
     * Creates a prompt for answering questions based on article content.
     *
     * @param articleContent The full content of the article
     * @param question       The question to answer
     * @return Formatted prompt for question answering
     */
    public String w02d05_createQuestionAnswerPrompt(String articleContent, String question) {
        return "Na podstawie poniższego artykułu odpowiedz BARDZO KONKRETNIE i krótko (w jednym zdaniu) na pytanie. " +
                "Szukaj dokładnej odpowiedzi w tekście, opisach obrazów i transkrypcjach audio. " +
                "Jeśli pytanie dotyczy konkretnego przedmiotu, owocu, nazwy - podaj dokładną nazwę.\n\n" +
                "TREŚĆ ARTYKUŁU (tekst, opisy obrazów, transkrypcje audio):\n" + articleContent + "\n\n" +
                "PYTANIE: " + question + "\n\n" +
                "ODPOWIEDŹ (bardzo konkretnie, krótko):";
    }

    /**
     * Creates a prompt for answering multiple questions based on article content in
     * one request.
     *
     * @param articleContent The full content of the article
     * @param questions      Map of question ID to question text
     * @return Formatted prompt for answering multiple questions
     */
    public String w02d05_createMultipleQuestionsPrompt(String articleContent, Map<String, String> questions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(
                "Na podstawie poniższego artykułu odpowiedz BARDZO KONKRETNIE i krótko (w jednym zdaniu) na każde z pytań. ");
        prompt.append("Szukaj dokładnej odpowiedzi w tekście, opisach obrazów i transkrypcjach audio. ");
        prompt.append("Jeśli pytanie dotyczy konkretnego przedmiotu, owocu, nazwy - podaj dokładną nazwę.\n\n");

        prompt.append("TREŚĆ ARTYKUŁU (tekst, opisy obrazów, transkrypcje audio):\n");
        prompt.append(articleContent).append("\n\n");

        prompt.append("PYTANIA:\n");
        for (Map.Entry<String, String> entry : questions.entrySet()) {
            prompt.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        prompt.append("\nODPOWIEDZI (format: ID: odpowiedź):\n");
        return prompt.toString();
    }

    /**
     * Creates a prompt for analyzing facts and extracting key information for
     * W03D01.
     *
     * @param allFactsContent All facts content concatenated
     * @return Formatted prompt for facts analysis
     */
    public String w03d01_createFactsAnalysisPrompt(String allFactsContent) {
        return "Przeanalizuj wszystkie podane fakty i wyekstrahuj kluczowe informacje:\n\n" +
                "Pliki z faktami:\n" + allFactsContent + "\n\n" +
                "Wygeneruj podsumowanie zawierające:\n" +
                "- Wszystkie osoby wymienione z ich opisami/zawodami/umiejętnościami\n" +
                "- Wszystkie miejsca i lokacje\n" +
                "- Wszystkie technologie, przedmioty, urządzenia\n" +
                "- Inne istotne informacje\n" +
                "Zachowaj polski język i format pozwalający na łatwe przeszukiwanie.";
    }

    /**
     * Creates a prompt for generating keywords for a single report in W03D01.
     *
     * @param filename      Report filename
     * @param reportContent Report content
     * @param filenameInfo  Information extracted from filename
     * @param relatedFacts  Related facts content
     * @param factsAnalysis Complete facts analysis
     * @return Formatted prompt for keyword generation
     */
    public String w03d01_createKeywordGenerationPrompt(String filename, String reportContent,
            String filenameInfo, String relatedFacts,
            String factsAnalysis) {
        return "Wygeneruj słowa kluczowe dla raportu w języku polskim (mianownik, oddzielone przecinkami).\n\n" +
                "NAZWA PLIKU: " + filename + "\n" +
                "INFORMACJE Z NAZWY PLIKU: " + filenameInfo + "\n\n" +
                "TREŚĆ RAPORTU:\n" + reportContent + "\n\n" +
                "POWIĄZANE FAKTY:\n" + relatedFacts + "\n\n" +
                "WSZYSTKIE DOSTĘPNE FAKTY (dla kontekstu):\n" + factsAnalysis + "\n\n" +
                "ZASADY:\n" +
                "- Słowa kluczowe muszą być w języku polskim w mianowniku\n" +
                "- Oddzielone przecinkami (np. słowo1,słowo2,słowo3)\n" +
                "- Uwzględnij treść raportu, powiązane fakty i informacje z nazwy pliku\n" +
                "- Dodaj nazwiska i imiona osób, jeśli są istotne\n" +
                "- Dodaj zawody, technologie, miejsca, przedmioty\n" +
                "- Bądź specyficzny dla tego konkretnego raportu\n" +
                "- Jeśli w faktach są informacje o osobach z raportu, uwzględnij je\n\n" +
                "Zwróć TYLKO listę słów kluczowych oddzielonych przecinkami, bez dodatkowych komentarzy.";
    }

    /**
     * Creates a prompt for generating SQL query based on database schema for
     * W03D03.
     *
     * @param databaseSchemas The complete database schemas from SHOW CREATE TABLE
     *                        commands
     * @return Formatted prompt for SQL query generation
     */
    public String w03d03_createSqlQueryPrompt(String databaseSchemas) {
        return "Na podstawie poniższych schematów bazy danych wygeneruj zapytanie SQL, które zwróci ID aktywnych datacenter zarządzanych przez nieaktywnych menadżerów.\n\n"
                +
                "SCHEMATY BAZY DANYCH:\n" + databaseSchemas + "\n\n" +
                "ZADANIE:\n" +
                "Znajdź numery ID czynnych datacenter, które zarządzane są przez menadżerów, którzy aktualnie przebywają na urlopie (są nieaktywni).\n\n"
                +
                "WYMAGANIA:\n" +
                "- Datacenter muszą być aktywne\n" +
                "- Menadżerowie tych datacenter muszą być nieaktywni (na urlopie)\n" +
                "- Zwróć tylko ID datacenter\n" +
                "- Użyj odpowiednich JOIN-ów między tabelami\n" +
                "- Sprawdź status aktywności/nieaktywności w odpowiednich kolumnach\n\n" +
                "Zwróć TYLKO surowe zapytanie SQL bez żadnych dodatkowych opisów, wyjaśnień czy formatowania Markdown. "
                +
                "Zapytanie musi być gotowe do bezpośredniego wykonania.";
    }

    /**
     * Creates a prompt for extracting names and places from Barbara's note for
     * W03D04.
     *
     * @param note Barbara's note content
     * @return Formatted prompt for names and places extraction
     */
    public String w03d04_createNamesAndPlacesExtractionPrompt(String note) {
        return "Przeanalizuj poniższą notatkę i wyodrębnij:\n\n" +
                "1. WSZYSTKIE IMIONA osób (w mianowniku, wielkich literach, bez polskich znaków)\n" +
                "2. WSZYSTKIE NAZWY MIAST (w mianowniku, wielkich literach, bez polskich znaków)\n\n" +
                "NOTATKA:\n" + note + "\n\n" +
                "Odpowiedz w formacie:\n" +
                "IMIONA:\n" +
                "BARBARA\n" +
                "ALEKSANDER\n" +
                "...\n\n" +
                "MIASTA:\n" +
                "WARSZAWA\n" +
                "KRAKOW\n" +
                "...\n\n" +
                "WAŻNE:\n" +
                "- Imiona w mianowniku (np. BARBARA zamiast Barbarze)\n" +
                "- Miasta bez polskich znaków (np. KRAKOW zamiast Kraków)\n" +
                "- Wszystkie nazwy wielkimi literami\n" +
                "- Nie dodawaj nic dodatkowego - tylko imiona i miasta z notatki";
    }

    /**
     * Creates a prompt for analyzing photo quality for W04D01.
     *
     * @return Formatted prompt for photo quality analysis
     */
    public String w04d01_createPhotoQualityAnalysisPrompt() {
        return """
                Przeanalizuj jakość tego zdjęcia i oceń, czy wymaga ono poprawy. Zwróć uwagę na:
                - Szumy, glitche, zniekształcenia
                - Poziom jasności (czy jest zbyt ciemne lub zbyt jasne)
                - Czy na zdjęciu widać osobę (potencjalnie Barbarę)
                - Ogólną czytelność obrazu

                Opisz krótko stan zdjęcia i zasugeruj czy potrzebuje:
                - REPAIR (jeśli są szumy, glitche)
                - BRIGHTEN (jeśli jest za ciemne)
                - DARKEN (jeśli jest za jasne)
                - DONE (jeśli jakość jest dobra)
                - SKIP (jeśli nie nadaje się do analizy)
                """;
    }

    /**
     * Creates a prompt for deciding which operation to perform on a photo for
     * W04D01.
     *
     * @param qualityAssessment The quality assessment of the photo
     * @return Formatted prompt for operation decision
     */
    public String w04d01_createPhotoOperationDecisionPrompt(String qualityAssessment) {
        return """
                Na podstawie poniższej analizy jakości zdjęcia, zdecyduj jaką operację wykonać.
                Odpowiedz TYLKO jednym słowem: REPAIR, BRIGHTEN, DARKEN, DONE lub SKIP

                Analiza jakości:
                """ + qualityAssessment + """

                Wybierz operację:
                """;
    }

    /**
     * Creates a prompt for analyzing a single photo for Barbara's description for
     * W04D01.
     *
     * @param photoNumber The number of the photo being analyzed
     * @return Formatted prompt for single photo analysis
     */
    public String w04d01_createSinglePhotoBarbaraAnalysisPrompt(int photoNumber) {
        return "Przeanalizuj to zdjęcie pod kątem opisu osoby, która może być Barbarą.\n" +
                "To jest zdjęcie numer " + photoNumber + ".\n\n" +
                "Opisz szczegółowo:\n" +
                "- Wygląd fizyczny osoby/osób na zdjęciu\n" +
                "- Kolor włosów, długość, fryzurę\n" +
                "- Kolor oczu (jeśli widoczny)\n" +
                "- Wzrost, budowę ciała\n" +
                "- Ubranie, charakterystyczne elementy\n" +
                "- Wiek (w przybliżeniu)\n" +
                "- Inne charakterystyczne cechy\n\n" +
                "Jeśli na zdjęciu jest więcej osób, skup się na tej, która wydaje się być główną postacią.\n" +
                "Opisuj obiektywnie to, co widzisz.";
    }

    /**
     * Creates a prompt for creating Barbara's final description based on all photo
     * analyses for W04D01.
     *
     * @param allAnalyses Combined analyses of all photos
     * @return Formatted prompt for final description creation
     */
    public String w04d01_createBarbaraFinalDescriptionPrompt(String allAnalyses) {
        return "Na podstawie analizy wszystkich zdjęć, stwórz szczegółowy rysopis Barbary w języku polskim.\n\n" +
                "Analizy zdjęć:\n" + allAnalyses + "\n\n" +
                "Stwórz spójny, szczegółowy rysopis Barbary uwzględniając:\n" +
                "- Wygląd fizyczny (wzrost, budowa)\n" +
                "- Włosy (kolor, długość, fryzura)\n" +
                "- Oczy (kolor, jeśli widoczny)\n" +
                "- Wiek (w przybliżeniu)\n" +
                "- Charakterystyczne cechy\n" +
                "- Styl ubierania się\n\n" +
                "Skup się na cechach, które powtarzają się na różnych zdjęciach.\n" +
                "Napisz rysopis w języku polskim, zwięźle ale szczegółowo.";
    }

    /**
     * Creates a prompt for verifying research data correctness for W04D02.
     *
     * @param researchLine The research data line to verify
     * @return Formatted prompt for research verification
     */
    public String w04d02_createResearchVerificationPrompt(String researchLine) {
        return "Sprawdź poprawność poniższego wpisu badawczego. " +
                "Dane zawierają tłumaczenia tego samego słowa/pojęcia w różnych językach. " +
                "Przeanalizuj czy wszystkie słowa rzeczywiście oznaczają to samo pojęcie.\n\n" +
                "Wpis do sprawdzenia: " + researchLine + "\n\n" +
                "Odpowiedz TYLKO 'CORRECT' jeśli wszystkie słowa oznaczają to samo pojęcie, " +
                "lub 'INCORRECT' jeśli któreś słowo nie pasuje do pozostałych.\n\n" +
                "Podstaw swoją ocenę na znaczeniu słów, nie na podobieństwie brzmieniowym.";
    }

    /**
     * Creates a prompt to check if a page contains answer to a specific question
     * for W04D03.
     *
     * @param pageContent The content of the page
     * @param question    The question to check
     * @return Formatted prompt for checking if answer exists
     */
    public String w04d03_createAnswerCheckPrompt(String pageContent, String question) {
        return "Przeanalizuj poniższą treść strony i sprawdź, czy zawiera odpowiedź na zadane pytanie.\n\n" +
                "PYTANIE: " + question + "\n\n" +
                "TREŚĆ STRONY:\n" + pageContent + "\n\n" +
                "Szukaj konkretnych informacji, które bezpośrednio odpowiadają na pytanie. " +
                "Odpowiedz TYLKO:\n" +
                "- 'TAK' jeśli na tej stronie znajduje się odpowiedź na pytanie\n" +
                "- 'NIE' jeśli na tej stronie nie ma odpowiedzi na pytanie\n\n" +
                "Nie dodawaj żadnych wyjaśnień.";
    }

    /**
     * Creates a prompt to extract answer from page content for a specific question
     * for W04D03.
     *
     * @param pageContent The content of the page
     * @param question    The question to answer
     * @return Formatted prompt for answer extraction
     */
    public String w04d03_createAnswerExtractionPrompt(String pageContent, String question) {
        return "Na podstawie poniższej treści strony podaj BARDZO ZWIĘZŁĄ i KONKRETNĄ odpowiedź na pytanie.\n\n" +
                "PYTANIE: " + question + "\n\n" +
                "TREŚĆ STRONY:\n" + pageContent + "\n\n" +
                "Podaj tylko konkretną informację, która odpowiada na pytanie. " +
                "Nie dodawaj opisów ani dodatkowych słów.\n\n" +
                "ODPOWIEDŹ (podaj tylko konkretną informację, bez dodatkowych słów):";
    }

    /**
     * Creates a prompt to select the best link from available links to find answer
     * to a question for W04D03.
     *
     * @param pageContent        The content of the page with links
     * @param question           The question to find answer for
     * @param availableLinksText Text representation of available links
     * @return Formatted prompt for link selection
     */
    public String w04d03_createLinkSelectionPrompt(String pageContent, String question, String availableLinksText) {
        return "Na podstawie poniższej treści strony wybierz najlepszy link do odpowiedzi na pytanie.\n\n" +
                "TREŚĆ STRONY:\n" + pageContent + "\n\n" +
                "PYTANIE: " + question + "\n\n" +
                "DOSTĘPNE LINKI:\n" + availableLinksText + "\n\n" +
                "Odpowiedz TYLKO numerem linku (np. '1', '2', '3').";
    }

    /**
     * Creates a prompt for drone navigation on a 4x4 grid map.
     * The drone starts at position (0,0) and needs to interpret movement
     * instructions.
     *
     * @param instruction The movement instruction for the drone
     * @return Formatted prompt for drone navigation
     */
    public String w04d04_createDroneNavigationPrompt(String instruction) {
        // Map description - 4x4 grid based on visual analysis of map.png
        // Starting position: top-left (0,0) - "punkt startowy"
        // Grid coordinates: (row, column) where (0,0) is top-left
        // Updated based on feedback from Centrala
        final String MAP_DESCRIPTION = """
                Mapa 4x4 (współrzędne: wiersz,kolumna, (0,0) to lewy górny róg):

                Wiersz 0: (0,0) start | (0,1) trawa | (0,2) drzewo | (0,3) dom
                Wiersz 1: (1,0) trawa | (1,1) wiatrak | (1,2) trawa | (1,3) trawa
                Wiersz 2: (2,0) trawa | (2,1) trawa | (2,2) skały | (2,3) drzewo
                Wiersz 3: (3,0) skały | (3,1) skały | (3,2) samochód | (3,3) jaskinia

                UWAGA: "na sam dół" oznacza przejście do ostatniego wiersza (wiersz 3)
                """;

        return "Jesteś systemem nawigacji drona. Dron zawsze zaczyna w lewym górnym rogu mapy (pozycja 0,0).\n\n" +
                "KIERUNKI RUCHU - WAŻNE:\n" +
                "- W PRAWO = zwiększ drugą współrzędną (kolumnę): (0,0) → (0,1)\n" +
                "- W PRAWO MAKSYMALNIE / MAKSYMALNIE W PRAWO = idź do ostatniej kolumny (kolumna 3)\n" +
                "- W LEWO = zmniejsz drugą współrzędną (kolumnę): (0,1) → (0,0)\n" +
                "- W LEWO MAKSYMALNIE / MAKSYMALNIE W LEWO = idź do pierwszej kolumny (kolumna 0)\n" +
                "- W DÓŁ = zwiększ pierwszą współrzędną (wiersz): (0,0) → (1,0)\n" +
                "- W GÓRĘ = zmniejsz pierwszą współrzędną (wiersz): (1,0) → (0,0)\n" +
                "- NA SAM DÓŁ / A PÓŹNIEJ NA SAM DÓŁ / POTEM NA SAM DÓŁ = idź do ostatniego wiersza (wiersz 3)\n\n" +
                "Instrukcja drona: " + instruction + "\n\n" +
                "Opis mapy:\n" + MAP_DESCRIPTION + "\n\n" +
                "Krok po kroku:\n" +
                "1. Zacznij od pozycji (0,0)\n" +
                "2. Analizuj każdy ruch z instrukcji\n" +
                "3. Oblicz końcową pozycję (wiersz,kolumna)\n" +
                "4. Znajdź co znajduje się na tej pozycji w mapie\n\n" +
                "WAŻNE: Odpowiedz TYLKO nazwą obiektu/miejsca na końcowej pozycji, maksymalnie 2 słowa po polsku.\n" +
                "Przykłady odpowiedzi: 'drzewo', 'skały', 'góry', 'wiatrak', 'dom', 'auto', 'jaskinia', 'las', 'łąka', 'start'";
    }
}