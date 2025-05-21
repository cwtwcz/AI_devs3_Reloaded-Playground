package pl.cwtwcz.service.weeks.week2;

import org.springframework.stereotype.Service;
import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@RequiredArgsConstructor
@Service
public class W02D02Service {

    @Value("${custom.w02d02.image-file.path}")
    private String imagePath;

    private final OpenAiAdapter openAiAdapter;
    private final PromptService promptService;
    private final FileService fileService;

    // slow and very expensive, but only one which got correct answer. Prompt needs
    // to be optimized to work on cheaper/dumber models.
    private static final String IMAGE_PROCESSING_MODEL_NAME = "gpt-4.5-preview";

    public String w02d02() {
        String prompt = promptService.w02d02_createCityRecognitionPrompt();
        String base64Image = fileService.readFileAsBase64(imagePath);
        OpenAiImagePromptRequestDto requestDto = OpenAiImagePromptRequestDto.build(IMAGE_PROCESSING_MODEL_NAME, prompt,
                base64Image);
        String fullAnswer = openAiAdapter.getAnswerWithImageRequestPayload(requestDto, IMAGE_PROCESSING_MODEL_NAME);
        String sumarizedResponse = promptService.sumarizeResponse(fullAnswer);
        return openAiAdapter.getAnswer(sumarizedResponse);
    }
}
