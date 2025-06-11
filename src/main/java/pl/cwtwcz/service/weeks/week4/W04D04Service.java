package pl.cwtwcz.service.weeks.week4;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.week4.DroneInstructionDto;
import pl.cwtwcz.dto.week4.DroneResponseDto;
import pl.cwtwcz.service.PromptService;

@RequiredArgsConstructor
@Service
public class W04D04Service {

    private static final Logger logger = LoggerFactory.getLogger(W04D04Service.class);

    private final OpenAiAdapter openAiAdapter;
    private final PromptService promptService;

    public DroneResponseDto w04d04(DroneInstructionDto instructionDto) {
        logger.info(">>> CENTRALA REQUEST: '{}'", instructionDto.getInstruction());

        // Step 1. Create prompt for LLM to interpret the instruction
        String prompt = promptService.w04d04_createDroneNavigationPrompt(instructionDto.getInstruction());

        // Step 2. Get response from LLM
        String aiResponse = openAiAdapter.getAnswer(prompt);
        logger.info(">>> CENTRALA RESPONSE RAW: '{}'", aiResponse);

        // Step 3. Clean and prepare response
        return new DroneResponseDto(cleanDescription(aiResponse));
    }

    private String cleanDescription(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return "punkt startowy";
        }

        // Remove quotes, dots, and extra whitespace
        String cleaned = aiResponse.trim()
                .replaceAll("^\"|\"$", "") // Remove leading/trailing quotes
                .replaceAll("\\.$", "") // Remove trailing dot
                .trim();

        // Take only first 2 words if more than 2
        String[] words = cleaned.split("\\s+");
        if (words.length > 2) {
            return words[0] + " " + words[1];
        }

        return cleaned;
    }
}