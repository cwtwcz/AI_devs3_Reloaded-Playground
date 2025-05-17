package pl.cwtwcz.dto.week1.day3;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnswerDto {
    private String apikey;
    private String description;
    private String copyright;
    @JsonProperty("test-data")
    private List<ResponseTestDataItemDto> testData;
} 