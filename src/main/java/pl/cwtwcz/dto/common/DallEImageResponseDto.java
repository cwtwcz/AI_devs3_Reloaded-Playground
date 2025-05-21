package pl.cwtwcz.dto.common;

import lombok.Data;
import java.util.List;

@Data
public class DallEImageResponseDto {
    private long created;
    private List<DataItem> data;

    @Data
    public static class DataItem {
        private String url;
    }
} 