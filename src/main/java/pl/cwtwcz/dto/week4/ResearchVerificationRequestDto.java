package pl.cwtwcz.dto.week4;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ResearchVerificationRequestDto {
    private String task;
    private String apikey;
    private List<String> answer;
} 