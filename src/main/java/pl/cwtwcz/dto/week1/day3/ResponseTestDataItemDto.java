package pl.cwtwcz.dto.week1.day3;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.cwtwcz.dto.common.TestDetailDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseTestDataItemDto {
    private String question;
    private int answer;
    private TestDetailDto test;
} 