package pl.cwtwcz.dto.week5;

import lombok.Data;
import java.util.List;

@Data
public class PhoneConversationDto {
    private String start;
    private String end; 
    private Integer length;
    private List<String> sentences;
} 