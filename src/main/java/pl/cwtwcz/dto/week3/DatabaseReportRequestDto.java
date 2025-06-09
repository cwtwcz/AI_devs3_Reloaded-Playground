package pl.cwtwcz.dto.week3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseReportRequestDto {
    private String task;
    private String apikey;
    private List<Integer> answer;
} 