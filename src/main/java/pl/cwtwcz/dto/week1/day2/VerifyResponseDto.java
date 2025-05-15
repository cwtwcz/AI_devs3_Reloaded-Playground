package pl.cwtwcz.dto.week1.day2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class VerifyResponseDto {
    private String text;
    private String msgID;
} 