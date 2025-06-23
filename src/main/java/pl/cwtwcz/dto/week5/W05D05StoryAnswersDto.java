package pl.cwtwcz.dto.week5;

import lombok.Data;
import java.util.List;

@Data
public class W05D05StoryAnswersDto {
    private List<String> answers;
    
    public W05D05StoryAnswersDto(List<String> answers) {
        this.answers = answers;
    }
} 