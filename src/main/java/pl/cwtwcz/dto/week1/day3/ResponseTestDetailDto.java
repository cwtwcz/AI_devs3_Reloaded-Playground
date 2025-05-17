package pl.cwtwcz.dto.week1.day3;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseTestDetailDto {
    @JsonProperty("q")
    private String question;
    @JsonProperty("a")
    private String answer;
} 