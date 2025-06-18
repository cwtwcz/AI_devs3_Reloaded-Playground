package pl.cwtwcz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import pl.cwtwcz.service.weeks.week5.W05D01Service;
import pl.cwtwcz.service.weeks.week5.W05D01ConversationReconstructionService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/weeks/5")
public class Week5RestController {

    private final W05D01Service w05d01Service;
    private final W05D01ConversationReconstructionService conversationReconstructionService;

    @GetMapping("days/1/init-db")
    public String w05d01InitializeDatabase() {
        return conversationReconstructionService.initializeDatabase();
    }

    @GetMapping("days/1/reconstruct")
    public String w05d01Reconstruct() {
        return conversationReconstructionService.reconstructConversationsStep();
    }

    @GetMapping("days/1/analyze")
    public String w05d01Analyze() {
        return w05d01Service.analyzeConversationsStep();
    }
}