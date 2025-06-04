package pl.cwtwcz.dto.week3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportMetadataDto {
    private String date;
    private String filename;
    private String content;
} 