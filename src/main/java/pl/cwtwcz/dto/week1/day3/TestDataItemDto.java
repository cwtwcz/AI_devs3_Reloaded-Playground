package pl.cwtwcz.dto.week1.day3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestDataItemDto {
    private String question;
    private int answer;
    private TestDetailDto test;
} 