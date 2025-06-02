package pl.cwtwcz.dto.week3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DokumentyRequestDto {
    private String task;
    private String apikey;
    private Map<String, String> answer;
} 