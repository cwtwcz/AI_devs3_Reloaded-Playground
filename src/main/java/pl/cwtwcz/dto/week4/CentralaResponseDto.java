package pl.cwtwcz.dto.week4;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CentralaResponseDto {
    private Integer code;
    private String message;
    private String hint;
    private String debug;
    
    public boolean isSuccess() {
        return code != null && code == 0;
    }
    
    public boolean hasError() {
        return code != null && code != 0;
    }
    
    public boolean hasHint() {
        return hint != null && !hint.trim().isEmpty();
    }
} 