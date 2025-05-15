package pl.cwtwcz.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.cwtwcz.service.weeks.Week1Service;

@RestController
@RequestMapping("/week1")
public class Week1RestController {

    private final Week1Service week1Service;

    @Autowired
    public Week1RestController(Week1Service week1Service) {
        this.week1Service = week1Service;
    }

    @GetMapping("/w01d01")
    public String w01d01() {
        return week1Service.w01d01();
    }

    @GetMapping("/w01d02")
    public void w01d02() {
        week1Service.w01d02();
    }

} 