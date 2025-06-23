package pl.cwtwcz.dto.week5;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class W05D03ChallengeDataDto {
    private String task;
    private JsonNode data;
} 