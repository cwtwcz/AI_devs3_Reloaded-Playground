package pl.cwtwcz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import pl.cwtwcz.service.weeks.week5.W05D01Service;
import pl.cwtwcz.service.weeks.week5.W05D02Service;
import pl.cwtwcz.service.weeks.week5.W05D03Service;
import pl.cwtwcz.service.weeks.week5.W05D04Service;
import pl.cwtwcz.service.weeks.week5.W05D05Service;
import pl.cwtwcz.service.weeks.week5.W05D01ConversationReconstructionService;
import pl.cwtwcz.dto.week5.W05D04RequestDto;
import pl.cwtwcz.dto.week5.W05D04ResponseDto;

@RequiredArgsConstructor
@RestController
@RequestMapping("/weeks/5")
public class Week5RestController {

    private final W05D01Service w05d01Service;
    private final W05D01ConversationReconstructionService conversationReconstructionService;
    private final W05D02Service w05d02Service;
    private final W05D03Service w05d03Service;
    private final W05D04Service w05d04Service;
    private final W05D05Service w05d05Service;

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

    @GetMapping("days/2")
    public String w05d02() {
        return w05d02Service.w05d02();
    }

    @GetMapping("days/3")
    public String w05d03() {
        return w05d03Service.w05d03();
    }

    @PostMapping("days/4/webhook")
    public W05D04ResponseDto w05d04Webhook(@RequestBody W05D04RequestDto request) {
        return w05d04Service.handleWebhookRequest(request);
    }

    @GetMapping("days/5")
    public String w05d05() {
        return w05d05Service.w05d05();
    }
}