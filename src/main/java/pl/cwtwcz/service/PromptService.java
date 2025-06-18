package pl.cwtwcz.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * Creates a prompt for OCR analysis of notebook page 19 for W04D05.
     *
     * @return Formatted prompt for OCR analysis
     */
    public String w04d05_createOcrPrompt(String previousNotes) {
        return """
                Przeanalizuj ten obraz notatki i wyciągnij z niego CAŁY widoczny tekst.
                To jest skan strony z notatkami Rafała, który miał w momencie pisania namieszane w głowie - mogą być chaotyczne i niezrozumiałe.
                Rafał w momencie pisania najpewniej znajdował się w jaskini w Grudziądzu.
                Na obrazku są 3 fragmenty tekstu. Odczytaj tylko te pisany ręcznym pismem. Jeśli słowo jest niewyraźne, staraj się domyśleć jakie słowo powinno być w tym miejscu.
                Zwróć szczególną uwagę na:
                - Nazwy miejscowości (może być rozbite na fragmenty)
                - Daty i numery
                - Wszelkie napisane słowa, nawet jeśli wydają się dziwne

                Odpowiedz TYLKO tekstem bez dodatkowych komentarzy.
                Dla kontekstu przekazuje Tobie poprzednie notatki Rafała (Pomogą one poprawnie określić nierywaźne słowa):
                """
                + previousNotes;
    }

    /**
     * Creates a prompt for analyzing notebook content and answering questions for
     * W04D05.
     *
     * @param question                 The question to answer
     * @param notebookContent          Full notebook content (text + OCR)
     * @param previousIncorrectAnswers Set of previous incorrect answers
     * @param hint                     Optional hint from centrala
     * @return Formatted prompt for question answering
     */
    public String w04d05_createQuestionAnswerPrompt(
            String question, String notebookContent, Set<String> previousIncorrectAnswers, String hint) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(
                "Jesteś ekspertem do analizy dokumentów. Analizujesz notatki należące do chorego psychicznie człowieka o imieniu Rafał. ");
        prompt.append("Notatki są chaotyczne i zawierają urojeń, ale układają się w jedną większą całość.\n\n");

        prompt.append("ZAWARTOŚĆ NOTATNIKA:\n");
        prompt.append(notebookContent);
        prompt.append("\n\n");

        prompt.append("PYTANIE: ").append(question).append("\n\n");

        if (!previousIncorrectAnswers.isEmpty()) {
            prompt.append("POPRZEDNIE BŁĘDNE ODPOWIEDZI (NIE UŻYWAJ ICH!):\n");
            for (String incorrectAnswer : previousIncorrectAnswers) {
                prompt.append("- ").append(incorrectAnswer).append("\n");
            }
            prompt.append("\n");
        }

        if (hint != null && !hint.trim().isEmpty()) {
            prompt.append("PODPOWIEDŹ: ").append(hint).append("\n\n");
        }

        prompt.append("INSTRUKCJE:\n");
        prompt.append("- Znajdź odpowiedź w treści notatnika\n");
        prompt.append("- Jeśli pytanie dotyczy daty względnej, oblicz konkretną datę w formacie YYYY-MM-DD\n");
        prompt.append("- Zwróć uwagę na drobne detale i szary tekst pod rysunkami\n");
        prompt.append("- Pamiętaj, że tekst z OCR może zawierać błędy, szczególnie nazwy miejscowości\n");
        prompt.append(
                "- Gdy odnajdziesz odniesienia do pisma świętego lub innej literatury, odnajdź fragment tekstu do którego prowadzi odniesienie.\n");
        prompt.append(
                "- WAŻNE:Odpowiedz TYLKO zwięzłą odpowiedzią, bez dodatkowych wyjaśnień. Odpowiedź ma być jak najkrótsza. Bez zbędnych znaków interpunkcyjnych.\n");
        return prompt.toString();
    }

    /**
     * Creates a prompt for initial phone conversation reconstruction for W05D01.
     *
     * @param phoneData The phone data containing conversation fragments and
     *                  remaining sentences
     * @return Formatted prompt for initial reconstruction
     */
    public String w05d01_createInitialReconstructionPrompt(Map<String, Object> phoneData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(
                "ZADANIE: Zrekonstruuj 5 rozmów telefonicznych z fragmentów - BARDZO WAŻNE JEST PRECYZYJNE WYKONANIE.\n\n");

        prompt.append("DANE WEJŚCIOWE:\n");
        prompt.append("FRAGMENTY ROZMÓW (każda ma określone zdanie początkowe, końcowe i długość):\n");

        Map<String, Map<String, Object>> conversations = (Map<String, Map<String, Object>>) phoneData
                .get("conversations");
        for (Map.Entry<String, Map<String, Object>> entry : conversations.entrySet()) {
            Map<String, Object> conv = entry.getValue();
            prompt.append(String.format("- %s:\n", entry.getKey()));
            prompt.append(String.format("  * PIERWSZE ZDANIE: \"%s\"\n", conv.get("start")));
            prompt.append(String.format("  * OSTATNIE ZDANIE: \"%s\"\n", conv.get("end")));
            prompt.append(String.format("  * CAŁKOWITA DŁUGOŚĆ: %d zdań (włączając pierwsze i ostatnie)\n",
                    conv.get("length")));
            prompt.append("\n");
        }

        prompt.append("WSZYSTKIE ZDANIA DO WYKORZYSTANIA:\n");
        // Dodaj zdania początkowe i końcowe
        Set<String> usedSentences = new HashSet<>();
        for (Map<String, Object> conv : conversations.values()) {
            usedSentences.add((String) conv.get("start"));
            usedSentences.add((String) conv.get("end"));
        }

        int sentenceNum = 1;
        for (Map<String, Object> conv : conversations.values()) {
            prompt.append(String.format("%d: \"%s\" [ZDANIE POCZĄTKOWE]\n", sentenceNum++, conv.get("start")));
            prompt.append(String.format("%d: \"%s\" [ZDANIE KOŃCOWE]\n", sentenceNum++, conv.get("end")));
        }

        List<String> remainingSentences = (List<String>) phoneData.get("remainingSentences");
        for (String sentence : remainingSentences) {
            if (!usedSentences.contains(sentence)) {
                prompt.append(String.format("%d: \"%s\"\n", sentenceNum++, sentence));
            }
        }

        prompt.append("\n🚨 KRYTYCZNE ZASADY REKONSTRUKCJI (BEZWZGLĘDNIE OBOWIĄZUJĄCE!) 🚨\n");
        prompt.append("1. ⚠️ KAŻDE ZDANIE MUSI WYSTĄPIĆ DOKŁADNIE 1 RAZ - ZERO DUPLIKATÓW!\n");
        prompt.append("2. 🔒 PIERWSZE i OSTATNIE zdanie każdej rozmowy JEST NIEZMIENNE!\n");
        prompt.append("3. 📏 DŁUGOŚĆ każdej rozmowy MUSI być DOKŁADNIE jak określono!\n");
        prompt.append("4. ✅ WSZYSTKIE zdania muszą być wykorzystane - żadne nie może zostać pominięte!\n");
        prompt.append("5. 🚫 KATEGORYCZNY ZAKAZ DUPLIKOWANIA ZDAŃ - każde zdanie tylko raz w całej rekonstrukcji!\n");
        prompt.append("6. 🔍 PRZED ODPOWIEDZIĄ SPRAWDŹ CZY KAŻDE ZDANIE WYSTĘPUJE DOKŁADNIE RAZ!\n\n");

        prompt.append("ALGORYTM REKONSTRUKCJI:\n");
        prompt.append("1. Dla każdej rozmowy zacznij od zdania początkowego (NIEZMIENNEGO!)\n");
        prompt.append("2. Dodawaj kolejne zdania które logicznie pasują do kontekstu\n");
        prompt.append("3. Zakończ zdaniem końcowym (NIEZMIENNYM!)\n");
        prompt.append("4. SPRAWDŹ czy łączna liczba zdań = WYMAGANA DŁUGOŚĆ\n");
        prompt.append("5. SPRAWDŹ czy każde zdanie użyte dokładnie raz\n");
        prompt.append("6. SPRAWDŹ czy wszystkie zdania zostały wykorzystane\n\n");

        prompt.append("FORMAT ODPOWIEDZI - ZWRÓĆ TYLKO JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"rozmowa01\": [\n");
        prompt.append("    \"pierwsze zdanie rozmowy01\",\n");
        prompt.append("    \"kolejne zdania...\",\n");
        prompt.append("    \"ostatnie zdanie rozmowy01\"\n");
        prompt.append("  ],\n");
        prompt.append("  \"rozmowa02\": [...],\n");
        prompt.append("  ...\n");
        prompt.append("}\n\n");

        prompt.append("🔍 OBOWIĄZKOWA LISTA KONTROLNA PRZED ODPOWIEDZIĄ:\n");
        prompt.append("✅ Czy każda rozmowa ma DOKŁADNIE pierwsze i ostatnie zdanie jak określono?\n");
        prompt.append("✅ Czy każda rozmowa ma DOKŁADNIE określoną długość (ani więcej, ani mniej)?\n");
        prompt.append("✅ Czy KAŻDE zdanie zostało wykorzystane DOKŁADNIE RAZ (sprawdź każde z osobna)?\n");
        prompt.append("✅ Czy WSZYSTKIE zdania zostały wykorzystane (żadne nie zostało pominięte)?\n");
        prompt.append("✅ Czy NIE MA ŻADNYCH DUPLIKATÓW zdań w całej rekonstrukcji?\n");
        prompt.append("✅ Czy liczba wszystkich zdań w rekonstrukcji = liczba zdań wejściowych?\n");
        prompt.append("✅ Czy rozmowy mają logiczny sens?\n");
        prompt.append("\n⚠️ INSTRUKCJA SPRAWDZANIA UNIKALNOŚCI:\n");
        prompt.append("1. Policz wszystkie zdania w swojej rekonstrukcji\n");
        prompt.append("2. Sprawdź czy każde zdanie występuje tylko raz\n");
        prompt.append("3. Sprawdź czy liczba zdań = liczba zdań wejściowych\n");
        prompt.append("4. JEŚLI KTÓRYKOLWIEK Z PUNKTÓW NIE JEST SPEŁNIONY - POPRAW REKONSTRUKCJĘ!\n");

        return prompt.toString();
    }

    /**
     * Creates a prompt for improving phone conversation reconstruction with
     * feedback for W05D01.
     *
     * @param currentConversations Current reconstruction to improve
     * @param phoneData            Original phone data with requirements
     * @param validationReport     Report showing current validation issues
     * @param iteration            Current iteration number
     * @return Formatted prompt for feedback improvement
     */
    public String w05d01_createFeedbackImprovementPrompt(List<Map<String, Object>> currentConversations,
            Map<String, Object> phoneData, String validationReport, int iteration) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("ZADANIE: Sprawdź i popraw rekonstrukcję rozmów telefonicznych - ITERACJA ").append(iteration)
                .append("/3\n\n");

        prompt.append("AKTUALNA REKONSTRUKCJA DO SPRAWDZENIA I POPRAWY:\n");
        for (Map<String, Object> conv : currentConversations) {
            prompt.append(String.format("=== %s ===\n", conv.get("name")));
            List<String> sentences = (List<String>) conv.get("sentences");
            if (sentences != null) {
                prompt.append(String.format("AKTUALNA DŁUGOŚĆ: %d zdań\n", sentences.size()));
                for (int i = 0; i < sentences.size(); i++) {
                    prompt.append(String.format("%d. \"%s\"\n", i + 1, sentences.get(i)));
                }
            }
            prompt.append("\n");
        }

        prompt.append("🚨 WYMAGANIA KTÓRE MUSZĄ BYĆ SPEŁNIONE (KRYTYCZNE!) 🚨\n");
        Map<String, Map<String, Object>> conversations = (Map<String, Map<String, Object>>) phoneData
                .get("conversations");

        // Dodaj szczegółowe informacje o każdej rozmowie
        int totalRequiredSentences = 0;
        for (Map.Entry<String, Map<String, Object>> entry : conversations.entrySet()) {
            Map<String, Object> conv = entry.getValue();
            int requiredLength = (Integer) conv.get("length");
            totalRequiredSentences += requiredLength;
            prompt.append(String.format(
                    "🔒 %s: PIERWSZE=\"%s\" (NIEZMIENNE!), OSTATNIE=\"%s\" (NIEZMIENNE!), WYMAGANA DŁUGOŚĆ=%d zdań (DOKŁADNIE!)\n",
                    entry.getKey(), conv.get("start"), conv.get("end"), requiredLength));
        }

        // Dodaj informację o łącznej liczbie zdań
        prompt.append(String.format("\n📊 ŁĄCZNA LICZBA ZDAŃ WE WSZYSTKICH ROZMOWACH: %d\n", totalRequiredSentences));
        prompt.append("📋 KAŻDE ZDANIE MUSI WYSTĄPIĆ DOKŁADNIE RAZ W CAŁEJ REKONSTRUKCJI!\n");

        prompt.append("\nRAPORT WALIDACJI:\n");
        prompt.append(validationReport);

        prompt.append("\n🚨 ZASADY POPRAWIANIA (BEZWZGLĘDNIE OBOWIĄZUJĄCE!) 🚨\n");
        prompt.append("1. 🔒 PIERWSZE i OSTATNIE zdanie każdej rozmowy MUSI pozostać DOKŁADNIE niezmienione!\n");
        prompt.append("2. 📏 DŁUGOŚĆ każdej rozmowy MUSI być DOKŁADNIE taka jak wymagana - ani więcej, ani mniej!\n");
        prompt.append("3. 🔄 Możesz PRZESTAWIAĆ zdania wewnątrz rozmów (między pierwszym a ostatnim)\n");
        prompt.append(
                "4. ↔️ Możesz PRZENOSIĆ zdania między rozmowami TYLKO jeśli to nie naruszy wymaganych długości\n");
        prompt.append("5. 🚫 KATEGORYCZNY ZAKAZ DUPLIKOWANIA - każde zdanie DOKŁADNIE RAZ w całej rekonstrukcji!\n");
        prompt.append("6. ✅ WSZYSTKIE zdania muszą być wykorzystane - żadne nie może zostać pominięte!\n");
        prompt.append("7. 🎯 PRIORYTET: Najpierw spełnij wymagania długości, potem poprawiaj logikę!\n");
        prompt.append("8. ⚠️ KAŻDA ZMIANA MUSI ZACHOWAĆ ZASADĘ: 1 zdanie = 1 użycie (ZERO DUPLIKATÓW)!\n");
        prompt.append("9. 🔍 PRZED KAŻDĄ ZMIANĄ SPRAWDŹ CZY ZDANIE JUŻ GDZIEŚ NIE WYSTĘPUJE!\n\n");

        prompt.append("📋 INSTRUKCJE KROK PO KROK:\n");
        prompt.append("1. 📊 Sprawdź długość każdej rozmowy względem wymagań (DOKŁADNIE!)\n");
        prompt.append(
                "2. ➕ Jeśli rozmowa ma za mało zdań - dodaj zdania (sprawdź czy nie są już użyte gdzie indziej!)\n");
        prompt.append(
                "3. ➖ Jeśli rozmowa ma za dużo zdań - przenieś nadmiarowe zdania (sprawdź czy nie stworzysz duplikatów!)\n");
        prompt.append("4. 🔒 Upewnij się że pierwsze i ostatnie zdanie każdej rozmowy się NIE ZMIENIŁO!\n");
        prompt.append("5. 🧮 Sprawdź czy suma wszystkich zdań się zgadza z wymaganą liczbą\n");
        prompt.append("6. 🚫 SPRAWDŹ CZY KAŻDE ZDANIE WYSTĘPUJE DOKŁADNIE RAZ (nie więcej, nie mniej)!\n");
        prompt.append("7. ✅ Sprawdź czy wszystkie zdania zostały wykorzystane!\n");
        prompt.append("8. 🔍 PRZESKANUJ CAŁĄ REKONSTRUKCJĘ W POSZUKIWANIU DUPLIKATÓW!\n");
        prompt.append("9. 🧠 Dopiero potem poprawiaj logikę i spójność\n\n");

        prompt.append("FORMAT ODPOWIEDZI - ZWRÓĆ POPRAWIONĄ WERSJĘ JAKO JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"rozmowa01\": [\n");
        prompt.append("    \"pierwsze zdanie (NIEZMIENIONE)\",\n");
        prompt.append("    \"zdania środkowe (dokładnie tyle ile potrzeba)\",\n");
        prompt.append("    \"ostatnie zdanie (NIEZMIENIONE)\"\n");
        prompt.append("  ],\n");
        prompt.append("  \"rozmowa02\": [...],\n");
        prompt.append("  ...\n");
        prompt.append("}\n\n");

        prompt.append("🚨 UWAGI KRYTYCZNE - WARUNKI ODRZUCENIA:\n");
        prompt.append("❌ Jeśli nie spełnisz wymagań długości, rekonstrukcja będzie ODRZUCONA!\n");
        prompt.append("❌ Jeśli jakiekolwiek zdanie wystąpi więcej niż raz, rekonstrukcja będzie ODRZUCONA!\n");
        prompt.append("❌ Jeśli jakiekolwiek zdanie zostanie pominięte, rekonstrukcja będzie ODRZUCONA!\n");
        prompt.append("❌ Jeśli zmienisz pierwsze lub ostatnie zdanie, rekonstrukcja będzie ODRZUCONA!\n");
        prompt.append("❌ Jeśli znajdę JAKIKOLWIEK DUPLIKAT zdania, rekonstrukcja będzie ODRZUCONA!\n");
        prompt.append("❌ Jeśli liczba zdań nie będzie się zgadzać, rekonstrukcja będzie ODRZUCONA!\n");
        prompt.append("✅ TYLKO PERFEKCYJNA REKONSTRUKCJA BEZ DUPLIKATÓW ZOSTANIE ZAAKCEPTOWANA!\n\n");

        prompt.append("🔍 FINALNA KONTROLA PRZED ODPOWIEDZIĄ:\n");
        prompt.append("1. Policz wszystkie zdania w swojej rekonstrukcji\n");
        prompt.append("2. Sprawdź każde zdanie czy nie występuje gdzie indziej\n");
        prompt.append("3. Porównaj liczbę zdań z wymaganą liczbą\n");
        prompt.append("4. Sprawdź długości wszystkich rozmów\n");
        prompt.append("5. Sprawdź pierwsze i ostatnie zdania każdej rozmowy\n");
        prompt.append("6. DOPIERO PO POZYTYWNYM SPRAWDZENIU WSZYSTKICH PUNKTÓW - ODPOWIEDZ!\n");

        return prompt.toString();
    }

    /**
     * Creates a prompt for analyzing reconstructed conversations to identify the
     * liar for W05D01.
     *
     * @param conversations          Reconstructed conversations
     * @param facts                  Available facts for verification
     * @param knownCorrectPersonName Optional known correct person name from
     *                               previous correct answers (can be null)
     * @return Formatted prompt for conversation analysis
     */
    public String w05d01_createConversationAnalysisPrompt(List<Map<String, Object>> conversations,
            Map<String, String> facts, String knownCorrectPersonName) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("ZADANIE: Przeanalizuj rozmowy telefoniczne aby zidentyfikować kto kłamie.\n\n");

        // Add known correct person name information if available
        if (knownCorrectPersonName != null && !knownCorrectPersonName.trim().isEmpty()) {
            prompt.append("⚠️ WAŻNE: Na podstawie poprzednich analiz wiemy, że osoba o imieniu '")
                    .append(knownCorrectPersonName.trim())
                    .append("' jest powiązana z pierwszym pytaniem. Uwzględnij to w analizie i upewnij się, ")
                    .append("że identyfikacja osób jest zgodna z tym ustaleniem.\n\n");
        }

        prompt.append("ZREKONSTRUOWANE ROZMOWY:\n");
        for (Map<String, Object> conv : conversations) {
            prompt.append(String.format("=== %s ===\n", conv.get("name")));
            List<String> sentences = (List<String>) conv.get("sentences");
            if (sentences != null) {
                for (int i = 0; i < sentences.size(); i++) {
                    prompt.append(String.format("%d. %s\n", i + 1, sentences.get(i)));
                }
            }
            prompt.append("\n");
        }

        prompt.append("ZNANE FAKTY:\n");
        for (Map.Entry<String, String> fact : facts.entrySet()) {
            prompt.append(String.format("PLIK %s: %s\n", fact.getKey(), fact.getValue()));
        }

        prompt.append("\nINSTRUKCJE ANALIZY:\n");
        prompt.append("1. Porównaj wypowiedzi w rozmowach ze znanymi faktami\n");
        prompt.append("2. Zidentyfikuj sprzeczności między tym co ludzie mówią a faktami\n");
        prompt.append("3. Określ kto kłamie na podstawie tych sprzeczności\n");
        prompt.append("4. Zidentyfikuj kluczowe informacje: imiona, endpointy, hasła, relacje\n");
        prompt.append(
                "5. ⚠️ KONKRETNE IMIONA: Szukaj konkretnych imion osób w rozmowach i faktach, a NIE opisów zawodowych ('agentka' → znajdź prawdziwe imię)\n");
        prompt.append(
                "6. ⚠️ WAŻNE - ŁĄCZENIE OSÓB: Analizuj charakterystyki osób z rozmów (płeć, zawód, umiejętności, relacje) i dopasowuj je do osób z faktów:\n");
        prompt.append(
                "   - 'agentka' + kobieta + IT/programowanie → sprawdź czy w faktach jest kobieta programistka\n");
        prompt.append(
                "   - 'nauczyciel' + mężczyzna + angielski → sprawdź czy w faktach jest nauczyciel angielskiego\n");
        prompt.append("   - Zwracaj zawsze konkretne imię z faktów, nie opisy zawodowe!\n");
        if (knownCorrectPersonName != null && !knownCorrectPersonName.trim().isEmpty()) {
            prompt.append("10. SZCZEGÓLNIE ZWRÓĆ UWAGĘ na osobę '").append(knownCorrectPersonName.trim())
                    .append("' - jej wypowiedzi są kluczowe dla analizy, skłamał on podczas rozmowy\n");
            prompt.append("11. Zwróć analizę w formacie JSON:\n");
        } else {
            prompt.append("10. Zwróć analizę w formacie JSON:\n");
        }
        prompt.append("{\n");
        prompt.append(
                "  \"liar\": \"" + (knownCorrectPersonName != null ? knownCorrectPersonName : "imie_klamcy") + "\",\n");
        prompt.append("  \"truthful_people\": [\"imie1\", \"imie2\"],\n");
        prompt.append("  \"key_facts\": {\n");
        prompt.append("    \"endpoints\": [\"url1\", \"url2\"],\n");
        prompt.append("    \"passwords\": [\"haslo1\"],\n");
        prompt.append("    \"relationships\": {\"osoba\": \"przezwisko\"},\n");
        prompt.append("    \"conversation_participants\": {\"rozmowa1\": [\"osoba1\", \"osoba2\"]}\n");
        prompt.append("  },\n");
        prompt.append("  \"reasoning\": \"wyjasnienie\"\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    /**
     * Creates a prompt for answering questions based on conversations and facts for
     * W05D01.
     *
     * @param question      The question to answer
     * @param conversations Reconstructed conversations
     * @param facts         Available facts
     * @return Formatted prompt for question answering
     */
    public String w05d01_createQuestionAnswerPrompt(String question,
            List<Map<String, Object>> conversations, Map<String, String> facts) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("ZADANIE: Odpowiedz na pytanie na podstawie analizy rozmów telefonicznych.\n\n");

        prompt.append("PYTANIE: ").append(question).append("\n\n");

        prompt.append("ROZMOWY:\n");
        for (Map<String, Object> conv : conversations) {
            prompt.append(String.format("=== %s ===\n", conv.get("name")));
            List<String> sentences = (List<String>) conv.get("sentences");
            if (sentences != null) {
                for (String sentence : sentences) {
                    prompt.append("- ").append(sentence).append("\n");
                }
            }
        }

        prompt.append("\nFAKTY:\n");
        for (Map.Entry<String, String> fact : facts.entrySet()) {
            prompt.append(String.format("%s: %s\n", fact.getKey(), fact.getValue()));
        }

        prompt.append("\nINSTRUKCJE:\n");
        prompt.append("1. Odpowiedz na pytanie podając TYLKO najważniejsze informacje\n");
        prompt.append("2. Używaj minimalnej liczby słów - maksymalnie 5 słów\n");
        prompt.append("3. Bez pełnych zdań, bez interpunkcji\n");
        prompt.append("4. Dla imion: zwracaj tylko imię lub przezwisko - NIE opisy zawodowe!\n");
        prompt.append(
                "5. ⚠️ KONKRETNE IMIONA: Szukaj konkretnych imion osób (np. 'Barbara', 'Adam'), a NIE opisów typu 'agentka', 'nauczyciel', 'kasjerka'\n");
        prompt.append("6. Dla URL: zwracaj kompletny URL\n");
        prompt.append("7. Dla wielu imion: oddzielaj przecinkiem\n");
        prompt.append("8. ⚠️ ŁĄCZENIE OSÓB: Analizuj charakterystyki osób i dopasowuj do faktów:\n");
        prompt.append("   - 'agentka' + kobieta + IT → Barbara (frontend developer)\n");
        prompt.append("   - 'nauczyciel' + angielski → Aleksander (nauczyciel angielskiego)\n");
        prompt.append("   - Zwracaj konkretne imię z faktów!\n");
        prompt.append("9. Opieraj odpowiedź na wynikach analizy i danych z rozmów\n");

        return prompt.toString();
    }

    /**
     * Creates a prompt for answering questions with feedback from previous
     * incorrect attempts for W05D01.
     *
     * @param question         The question to answer
     * @param questionId       The question ID
     * @param conversations    Reconstructed conversations
     * @param facts            Available facts
     * @param incorrectHistory List of previous incorrect answers
     * @return Formatted prompt for question answering with feedback
     */
    public String w05d01_createQuestionAnswerWithFeedbackPrompt(String question, String questionId,
            List<Map<String, Object>> conversations, Map<String, String> facts,
            List<Map<String, Object>> incorrectHistory) {

        StringBuilder prompt = new StringBuilder();
        prompt.append(
                "ZADANIE: Odpowiedz na pytanie na podstawie analizy rozmów telefonicznych z UCZENIEM NA BŁĘDACH.\n\n");

        prompt.append("PYTANIE: ").append(question).append("\n\n");

        // Add incorrect answers history for learning
        if (!incorrectHistory.isEmpty()) {
            prompt.append("POPRZEDNIE NIEPRAWIDŁOWE PRÓBY (ucz się na tych błędach):\n");
            for (Map<String, Object> incorrect : incorrectHistory) {
                prompt.append(String.format("Próba %d: Odpowiedź='%s' -> Błąd: %s\n",
                        incorrect.get("attempt_number"),
                        incorrect.get("answer"),
                        incorrect.get("error_message")));
            }
            prompt.append("NIE powtarzaj tych nieprawidłowych odpowiedzi!\n\n");
        }

        prompt.append("ROZMOWY:\n");
        for (Map<String, Object> conv : conversations) {
            prompt.append(String.format("=== %s ===\n", conv.get("name")));
            List<String> sentences = (List<String>) conv.get("sentences");
            if (sentences != null) {
                for (String sentence : sentences) {
                    prompt.append("- ").append(sentence).append("\n");
                }
            }
        }

        prompt.append("\nFAKTY:\n");
        for (Map.Entry<String, String> fact : facts.entrySet()) {
            prompt.append(String.format("%s: %s\n", fact.getKey(), fact.getValue()));
        }

        prompt.append("\nKRYTYCZNE INSTRUKCJE:\n");
        prompt.append("1. UCZ SIĘ na poprzednich nieprawidłowych próbach - NIE powtarzaj ich\n");
        prompt.append("2. Odpowiedz podając TYLKO najważniejsze informacje\n");
        prompt.append("3. Używaj minimalnej liczby słów - maksymalnie 5 słów\n");
        prompt.append("4. Bez pełnych zdań, bez interpunkcji\n");
        prompt.append("5. Dla imion: zwracaj tylko imię lub przezwisko - NIE opisy zawodowe!\n");
        prompt.append(
                "6. ⚠️ KONKRETNE IMIONA: Szukaj konkretnych imion osób (np. 'Barbara', 'Adam'), a NIE opisów typu 'agentka', 'nauczyciel', 'kasjerka'\n");
        prompt.append("7. Dla URL: zwracaj kompletny URL\n");
        prompt.append("8. Dla wielu imion: oddzielaj przecinkiem\n");
        prompt.append("9. ⚠️ ŁĄCZENIE OSÓB: Analizuj charakterystyki osób i dopasowuj do faktów:\n");
        prompt.append("   - 'agentka' + kobieta + IT → Barbara (frontend developer)\n");
        prompt.append("   - 'nauczyciel' + angielski → Aleksander (nauczyciel angielskiego)\n");
        prompt.append("   - Zwracaj konkretne imię z faktów!\n");
        prompt.append("10. Opieraj odpowiedź na wynikach analizy i danych z rozmów\n");
        prompt.append("11. Myśl inaczej niż w poprzednich nieudanych próbach\n");

        return prompt.toString();
    }

    /**
     * Creates a prompt for extracting password from conversations for W05D01.
     *
     * @param conversations Reconstructed conversations
     * @return Formatted prompt for password extraction
     */
    public String w05d01_createPasswordExtractionPrompt(List<Map<String, Object>> conversations) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("ZADANIE: Wyciągnij hasło z rozmów telefonicznych.\n\n");

        prompt.append("ROZMOWY:\n");
        for (Map<String, Object> conv : conversations) {
            prompt.append(String.format("=== %s ===\n", conv.get("name")));
            List<String> sentences = (List<String>) conv.get("sentences");
            if (sentences != null) {
                for (String sentence : sentences) {
                    prompt.append("- ").append(sentence).append("\n");
                }
            }
            prompt.append("\n");
        }

        prompt.append("INSTRUKCJE:\n");
        prompt.append("1. Znajdź hasło/password w rozmowach\n");
        prompt.append("2. Hasło może być podane wprost lub zakodowane\n");
        prompt.append("3. Szukaj słów kluczowych: 'hasło', 'password', 'kod dostępu'\n");
        prompt.append("4. Zwróć TYLKO samo hasło, bez dodatkowego tekstu\n");
        prompt.append("5. Jeśli nie znajdziesz hasła, zwróć 'BRAK'\n");
        prompt.append("6. Hasło może być kombinacją liter i cyfr\n");
        prompt.append("7. Może być podane w kontekście dostępu do API lub systemu\n\n");

        prompt.append("ODPOWIEDŹ (tylko hasło):\n");

        return prompt.toString();
    }
}