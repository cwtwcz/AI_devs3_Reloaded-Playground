package pl.cwtwcz.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ReportRequestDto {
    private String task;
    private String apikey;
    private String answer;
} 