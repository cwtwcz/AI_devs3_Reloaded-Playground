package pl.cwtwcz.dto.week5;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class W05D03SignResponseDto {
    private int code;
    private W05D03MessageDto message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class W05D03MessageDto {
        private String signature;
        private Long timestamp;
        private List<String> challenges;
    }
} 