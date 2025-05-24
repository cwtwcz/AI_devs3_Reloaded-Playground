package pl.cwtwcz.dto.week2.day5;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArxivRequestDto {
    private String task;
    private String apikey;
    private Map<String, String> answer;
} 