package pl.cwtwcz.dto.week5;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class W05D05StoryRequestDto {
    private String task;
    private String apikey;
    private List<String> answer;
} 