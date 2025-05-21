package pl.cwtwcz.dto.common;

import lombok.Data;

@Data
public class DallEImageRequestDto {
    private String model;
    private String prompt;

    public DallEImageRequestDto(String model, String prompt) {
        this.model = model;
        this.prompt = prompt;
    }
} 