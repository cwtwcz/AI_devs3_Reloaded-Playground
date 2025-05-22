package pl.cwtwcz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.cwtwcz.service.weeks.week2.W02D01Service;
import pl.cwtwcz.service.weeks.week2.W02D02Service;
import pl.cwtwcz.service.weeks.week2.W02D03Service;
import pl.cwtwcz.service.weeks.week2.W02D04Service;

@RequiredArgsConstructor
@RestController
@RequestMapping("/weeks/2")
public class Week2RestController {

    private final W02D01Service w02d01Service;
    private final W02D02Service w02d02Service;
    private final W02D03Service w02d03Service;
    private final W02D04Service w02d04Service;

    @GetMapping("/days/1")
    public String w02d01() {
        return w02d01Service.w02d01();
    }

    @GetMapping("/days/2")
    public String w02d02() {
        return w02d02Service.w02d02();
    }

    @GetMapping("/days/3")
    public String w02d03() {
        return w02d03Service.w02d03();
    }

    @GetMapping("/days/4")
    public String w02d04() {
        return w02d04Service.w02d04();
    }
}