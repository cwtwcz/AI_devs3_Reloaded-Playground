package pl.cwtwcz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.cwtwcz.service.weeks.week2.W02D01Service;

@RestController
@RequestMapping("/weeks/2")
public class Week2RestController {

    private final W02D01Service w02d01Service;

    public Week2RestController(W02D01Service w02d01Service) {
        this.w02d01Service = w02d01Service;
    }

    @GetMapping("/days/1")
    public String w02d01() {
        return w02d01Service.w02d01();
    }
}