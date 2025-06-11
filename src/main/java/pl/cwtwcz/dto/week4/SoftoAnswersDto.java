package pl.cwtwcz.dto.week4;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoftoAnswersDto {
    private String task;
    private String apikey;
    private Map<String, String> answer;
} 