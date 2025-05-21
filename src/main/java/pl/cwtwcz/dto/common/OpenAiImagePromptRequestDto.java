package pl.cwtwcz.dto.common;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiImagePromptRequestDto {
    private String model;
    private List<ImagePromptInput> messages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImagePromptInput {
        private String role;
        private List<ContentPart> content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentPart {
        private String type;
        private String text;
        private ImageUrl image_url;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageUrl {
        private String url;
        private String detail;
    }

    public static OpenAiImagePromptRequestDto build(String model, String prompt, String base64Image) {
        List<ContentPart> content = new ArrayList<>();
        content.add(new ContentPart("text", prompt, null));
        ImageUrl imageUrl = new ImageUrl("data:image/png;base64," + base64Image, "high");
        content.add(new ContentPart("image_url", null, imageUrl));
        return new OpenAiImagePromptRequestDto(model, List.of(new ImagePromptInput("user", content)));
    }
}