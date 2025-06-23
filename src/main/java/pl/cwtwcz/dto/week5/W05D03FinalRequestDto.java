package pl.cwtwcz.dto.week5;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class W05D03FinalRequestDto {
    private String apikey;
    private Long timestamp;
    private String signature;
    private List<String> answer;
} 