package pl.cwtwcz.dto.week4;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PhotoProcessingResponseDto {
    private String code;
    private String message;
} 