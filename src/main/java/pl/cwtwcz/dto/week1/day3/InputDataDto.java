package pl.cwtwcz.dto.week1.day3;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InputDataDto {
    private String apikey;
    private String description;
    private String copyright;
    @JsonProperty("test-data")
    private List<TestDataItemDto> testData;
} 