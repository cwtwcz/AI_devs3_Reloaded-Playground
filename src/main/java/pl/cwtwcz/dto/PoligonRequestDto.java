package pl.cwtwcz.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PoligonRequestDto {
    private String task;
    private String apikey;
    private List<Integer> answer;
}
