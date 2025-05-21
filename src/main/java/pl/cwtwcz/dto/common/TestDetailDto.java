package pl.cwtwcz.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestDetailDto {
    @JsonProperty("q")
    private String question;
    @JsonProperty("a")
    private String answer;
} 