package pl.cwtwcz.service.weeks.week2;

import org.springframework.stereotype.Service;
import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.FileService;

@Service
public class W02D02Service {
    private final OpenAiAdapter openAiAdapter;
    private final PromptService promptService;
    private final FileService fileService;
    private static final String IMAGE_PATH = "src/main/resources/inputfiles/w02d02/map.png";
    // slow and very expensive, but only one which got correct answer. Prompt needs to be optimized to work on cheaper/dumber models.
    private static final String IMAGE_PROCESSING_MODEL_NAME = "gpt-4.5-preview";    

    public W02D02Service(OpenAiAdapter openAiAdapter, PromptService promptService, FileService fileService) {
        this.openAiAdapter = openAiAdapter;
        this.promptService = promptService;
        this.fileService = fileService;
    }

    public String w02d02() {
        String prompt = promptService.w02d02_createCityRecognitionPrompt();
        String base64Image = fileService.readFileAsBase64(IMAGE_PATH);
        OpenAiImagePromptRequestDto requestDto = OpenAiImagePromptRequestDto.build(IMAGE_PROCESSING_MODEL_NAME, prompt, base64Image);
        String fullAnswer = openAiAdapter.getAnswerWithImage(requestDto, IMAGE_PROCESSING_MODEL_NAME);
        String sumarizedResponse = promptService.sumarizeResponse(fullAnswer);
        return openAiAdapter.getAnswer(sumarizedResponse);
    }
}
