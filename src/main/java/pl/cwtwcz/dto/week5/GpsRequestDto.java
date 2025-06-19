package pl.cwtwcz.dto.week5;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpsRequestDto {
    private String task;
    private String apikey;
    private Map<String, GpsLocationDto> answer;
} 