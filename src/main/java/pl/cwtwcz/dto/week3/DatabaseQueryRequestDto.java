package pl.cwtwcz.dto.week3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseQueryRequestDto {
    private String task;
    private String apikey;
    private String query;
} 