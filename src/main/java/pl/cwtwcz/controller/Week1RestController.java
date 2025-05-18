package pl.cwtwcz.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.cwtwcz.service.weeks.W01D01Service;
import pl.cwtwcz.service.weeks.W01D02Service;
import pl.cwtwcz.service.weeks.W01D03Service;
import pl.cwtwcz.service.weeks.W01D04Service;

@RestController
@RequestMapping("/week1")
public class Week1RestController {

    private final W01D01Service w01d01Service;
    private final W01D02Service w01d02Service;
    private final W01D03Service w01d03Service;
    private final W01D04Service w01d04Service;

    @Autowired
    public Week1RestController(
            W01D01Service w01d01Service,
            W01D02Service w01d02Service,
            W01D03Service w01d03Service,
            W01D04Service w01d04Service) {
        this.w01d01Service = w01d01Service;
        this.w01d02Service = w01d02Service;
        this.w01d03Service = w01d03Service;
        this.w01d04Service = w01d04Service;
    }

    @GetMapping("/w01d01")
    public String w01d01() {
        return w01d01Service.w01d01();
    }

    @GetMapping("/w01d02")
    public String w01d02() {
        return w01d02Service.w01d02();
    }

    @GetMapping("/w01d03")
    public String w01d03() {
        return w01d03Service.w01d03();
    }

    @GetMapping("/w01d04")
    public String w01d04() {
        return w01d04Service.w01d04();
    }
}