package pl.cwtwcz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.cwtwcz.service.weeks.week3.W03D01Service;
import pl.cwtwcz.service.weeks.week3.W03D02Service;
import pl.cwtwcz.service.weeks.week3.W03D03Service;
import pl.cwtwcz.service.weeks.week3.W03D04Service;
import pl.cwtwcz.service.weeks.week3.W03D05Service;

@RequiredArgsConstructor
@RestController
@RequestMapping("/weeks/3")
public class Week3RestController {

    private final W03D01Service w03d01Service;
    private final W03D02Service w03d02Service;
    private final W03D03Service w03d03Service;
    private final W03D04Service w03d04Service;
    private final W03D05Service w03d05Service;

    @GetMapping("/days/1")
    public String w03d01() {
        return w03d01Service.w03d01();
    }

    @GetMapping("/days/2")
    public String w03d02() {
        return w03d02Service.w03d02();
    }

    @GetMapping("/days/3")
    public String w03d03() {
        return w03d03Service.w03d03();
    }

    @GetMapping("/days/4")
    public String w03d04() {
        return w03d04Service.w03d04();
    }

    @GetMapping("/days/5")
    public String w03d05() {
        return w03d05Service.w03d05();
    }
}