package pl.cwtwcz.dto.week4;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotesAnswerDto {
    private String task;
    private String apikey;
    private Map<String, String> answer;
} 