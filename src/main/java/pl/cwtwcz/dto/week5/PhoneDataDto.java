package pl.cwtwcz.dto.week5;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PhoneDataDto {
    private Map<String, PhoneConversationDto> conversations;
    private List<String> reszta;
} 