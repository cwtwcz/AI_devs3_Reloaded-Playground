package pl.cwtwcz.dto.week5;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class W05D05CentralaResponseDto {
    private int code;
    private String message;
    private List<String> ok;
    private List<String> failed;
} 