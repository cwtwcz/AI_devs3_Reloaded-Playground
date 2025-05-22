package pl.cwtwcz.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportW02D04RequestDto {
    private String task;
    private String apikey;
    private Answer answer;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Answer {
        private List<String> people;
        private List<String> hardware;
    }
} 