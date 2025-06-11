package pl.cwtwcz.dto.week4;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageLinkDto {
    private String url;
    private String text;
    private String title;
} 