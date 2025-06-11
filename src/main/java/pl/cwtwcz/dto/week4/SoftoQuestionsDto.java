package pl.cwtwcz.dto.week4;

import java.util.HashMap;

public class SoftoQuestionsDto extends HashMap<String, String> {
    
    public String getQuestion(String key) {
        return this.get(key);
    }
} 