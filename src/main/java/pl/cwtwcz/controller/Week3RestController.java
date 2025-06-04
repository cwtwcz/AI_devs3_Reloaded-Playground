package pl.cwtwcz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.cwtwcz.service.weeks.week3.W03D01Service;
import pl.cwtwcz.service.weeks.week3.W03D02Service;

@RequiredArgsConstructor
@RestController
@RequestMapping("/weeks/3")
public class Week3RestController {

    private final W03D01Service w03d01Service;
    private final W03D02Service w03d02Service;

    @GetMapping("/days/1")
    public String w03d01() {
        return w03d01Service.w03d01();
    }

    @GetMapping("/days/2")
    public String w03d02() {
        return w03d02Service.w03d02();
    }
}