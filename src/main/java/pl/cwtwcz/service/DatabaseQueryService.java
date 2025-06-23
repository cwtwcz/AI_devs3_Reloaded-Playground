package pl.cwtwcz.service;

import org.springframework.stereotype.Service;

@Service
public class DatabaseQueryService {

    /**
     * Tworzy tabelę do przechowywania pytań z centrali
     */
    public String createQuestionsTable() {
        return """
            CREATE TABLE IF NOT EXISTS questions (
                id TEXT PRIMARY KEY,
                question TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
    }

    /**
     * Tworzy tabelę do przechowywania odpowiedzi
     */
    public String createAnswersTable() {
        return """
            CREATE TABLE IF NOT EXISTS answers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                question_id TEXT NOT NULL,
                answer TEXT NOT NULL,
                is_correct BOOLEAN DEFAULT 0,
                hint TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (question_id) REFERENCES questions (id)
            )
        """;
    }

    /**
     * Tworzy tabelę do przechowywania zawartości notatnika
     */
    public String createNotebookContentTable() {
        return """
            CREATE TABLE IF NOT EXISTS notebook_content (
                id INTEGER PRIMARY KEY,
                content_type TEXT NOT NULL,
                content TEXT NOT NULL,
                source_info TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
    }

    /**
     * Zapytanie do wstawienia lub aktualizacji pytania
     */
    public String insertOrReplaceQuestion() {
        return "INSERT OR REPLACE INTO questions (id, question) VALUES (?, ?)";
    }

    /**
     * Zapytanie do wstawienia zawartości notatnika
     */
    public String insertNotebookContent() {
        return "INSERT INTO notebook_content (content_type, content, source_info) VALUES (?, ?, ?)";
    }

    /**
     * Zapytanie do wstawienia nowej odpowiedzi (bez nadpisywania)
     */
    public String insertAnswer() {
        return "INSERT INTO answers (question_id, answer, is_correct, hint) VALUES (?, ?, ?, ?)";
    }

    /**
     * Zapytanie do pobrania wszystkich pytań
     */
    public String selectAllQuestions() {
        return "SELECT id, question FROM questions ORDER BY id";
    }

    /**
     * Zapytanie do pobrania poprawnej odpowiedzi
     */
    public String selectCorrectAnswer() {
        return "SELECT answer FROM answers WHERE question_id = ? AND is_correct = 1";
    }

    /**
     * Zapytanie do pobrania niepoprawnych odpowiedzi
     */
    public String selectIncorrectAnswers() {
        return "SELECT answer FROM answers WHERE question_id = ? AND is_correct = 0";
    }

    /**
     * Zapytanie do pobrania najnowszej podpowiedzi
     */
    public String selectLatestHint() {
        return "SELECT hint FROM answers WHERE question_id = ? AND hint IS NOT NULL ORDER BY created_at DESC LIMIT 1";
    }

    /**
     * Zapytanie do pobrania wszystkich podpowiedzi dla pytania
     */
    public String selectAllHints() {
        return "SELECT hint FROM answers WHERE question_id = ? AND hint IS NOT NULL ORDER BY created_at ASC";
    }

    /**
     * Zapytanie do pobrania ostatniej odpowiedzi
     */
    public String selectLatestAnswer() {
        return "SELECT answer FROM answers WHERE question_id = ? ORDER BY created_at DESC LIMIT 1";
    }

    /**
     * Zapytanie do pobrania zawartości notatnika
     */
    public String selectNotebookContent() {
        return "SELECT content_type, content, source_info FROM notebook_content ORDER BY id";
    }

    /**
     * Zapytanie do sprawdzenia czy są dane w tabeli
     */
    public String countRecords(String tableName) {
        return "SELECT COUNT(*) FROM " + tableName;
    }

    // Phone answers queries for W05D01

    /**
     * Zapytanie do pobrania poprawnej odpowiedzi z phone_answers
     */
    public String selectCorrectPhoneAnswer() {
        return "SELECT answer FROM phone_answers WHERE question_id = ? AND is_correct = 1";
    }

    /**
     * Zapytanie do pobrania niepoprawnych odpowiedzi z phone_answers
     */
    public String selectIncorrectPhoneAnswers() {
        return "SELECT answer FROM phone_answers WHERE question_id = ? AND is_correct = 0 ORDER BY rowid DESC";
    }

    /**
     * Zapytanie do sprawdzenia czy odpowiedź już istnieje jako poprawna w phone_answers
     */
    public String selectCorrectPhoneAnswerExists() {
        return "SELECT is_correct FROM phone_answers WHERE question_id = ? AND is_correct = 1";
    }

    /**
     * Zapytanie do wstawienia lub aktualizacji poprawnej odpowiedzi w phone_answers
     */
    public String insertOrReplaceCorrectPhoneAnswer() {
        return "INSERT OR REPLACE INTO phone_answers (question_id, answer, is_correct) VALUES (?, ?, 1)";
    }

    /**
     * Zapytanie do wstawienia niepoprawnej odpowiedzi w phone_answers
     */
    public String insertIncorrectPhoneAnswer() {
        return "INSERT INTO phone_answers (question_id, answer, is_correct) VALUES (?, ?, 0)";
    }

    // Story answers queries for W05D05

    /**
     * Tworzy tabelę do przechowywania odpowiedzi na pytania story
     */
    public String createStoryAnswersTable() {
        return """
            CREATE TABLE IF NOT EXISTS story_answers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                question_index INTEGER NOT NULL,
                answer TEXT NOT NULL,
                is_correct BOOLEAN DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
    }

    /**
     * Zapytanie do pobrania poprawnej odpowiedzi z story_answers
     */
    public String selectCorrectStoryAnswer() {
        return "SELECT answer FROM story_answers WHERE question_index = ? AND is_correct = 1";
    }

    /**
     * Zapytanie do pobrania niepoprawnych odpowiedzi z story_answers
     */
    public String selectIncorrectStoryAnswers() {
        return "SELECT answer FROM story_answers WHERE question_index = ? AND is_correct = 0 ORDER BY created_at DESC";
    }

    /**
     * Zapytanie do wstawienia lub aktualizacji poprawnej odpowiedzi w story_answers
     */
    public String insertOrReplaceCorrectStoryAnswer() {
        return "INSERT OR REPLACE INTO story_answers (question_index, answer, is_correct) VALUES (?, ?, 1)";
    }

    /**
     * Zapytanie do wstawienia niepoprawnej odpowiedzi w story_answers
     */
    public String insertIncorrectStoryAnswer() {
        return "INSERT INTO story_answers (question_index, answer, is_correct) VALUES (?, ?, 0)";
    }

    /**
     * Zapytanie do sprawdzenia czy odpowiedź już istnieje jako poprawna w story_answers
     */
    public String selectCorrectStoryAnswerExists() {
        return "SELECT is_correct FROM story_answers WHERE question_index = ? AND is_correct = 1";
    }

    /**
     * Zapytanie do usunięcia niepoprawnych odpowiedzi po znalezieniu poprawnej
     */
    public String deleteIncorrectStoryAnswers() {
        return "DELETE FROM story_answers WHERE question_index = ? AND is_correct = 0";
    }

    /**
     * Zapytanie do zliczenia poprawnych odpowiedzi story
     */
    public String countCorrectStoryAnswers() {
        return "SELECT COUNT(DISTINCT question_index) FROM story_answers WHERE is_correct = 1";
    }

    /**
     * Zapytanie do zliczenia niepoprawnych odpowiedzi story
     */
    public String countIncorrectStoryAnswers() {
        return "SELECT COUNT(*) FROM story_answers WHERE is_correct = 0";
    }
} 