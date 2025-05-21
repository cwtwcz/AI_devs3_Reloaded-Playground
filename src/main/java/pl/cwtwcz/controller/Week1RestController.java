package pl.cwtwcz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import pl.cwtwcz.service.weeks.week1.W01D01Service;
import pl.cwtwcz.service.weeks.week1.W01D02Service;
import pl.cwtwcz.service.weeks.week1.W01D03Service;
import pl.cwtwcz.service.weeks.week1.W01D04Service;
import pl.cwtwcz.service.weeks.week1.W01D05Service;

@RequiredArgsConstructor
@RestController
@RequestMapping("/weeks/1")
public class Week1RestController {

    private final W01D01Service w01d01Service;
    private final W01D02Service w01d02Service;
    private final W01D03Service w01d03Service;
    private final W01D04Service w01d04Service;
    private final W01D05Service w01d05Service;

    @GetMapping("/days/1")
    public String w01d01() {
        return w01d01Service.w01d01();
    }

    @GetMapping("/days/2")
    public String w01d02() {
        return w01d02Service.w01d02();
    }

    @GetMapping("/days/3")
    public String w01d03() {
        return w01d03Service.w01d03();
    }

    @GetMapping("/days/4")
    public String w01d04() {
        return w01d04Service.w01d04();
    }

    @GetMapping("/days/5")
    public String w01d05() {
        return w01d05Service.w01d05();
    }
}