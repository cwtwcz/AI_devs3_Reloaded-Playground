package pl.cwtwcz.dto.week4;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PhotoAnalysisRequestDto {
    private String photoUrl;
    private String filename;
    private String currentOperation;
    private boolean needsProcessing;
    private String qualityAssessment;
} 