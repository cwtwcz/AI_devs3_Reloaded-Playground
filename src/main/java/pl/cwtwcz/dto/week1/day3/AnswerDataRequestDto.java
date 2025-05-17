package pl.cwtwcz.dto.week1.day3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnswerDataRequestDto {
    public String task;
    public String apikey;
    public AnswerDto answer;
} 