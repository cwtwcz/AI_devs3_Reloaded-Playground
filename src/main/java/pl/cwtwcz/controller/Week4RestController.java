package pl.cwtwcz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import pl.cwtwcz.service.weeks.week4.W04D01Service;
import pl.cwtwcz.service.weeks.week4.W04D02Service;
import pl.cwtwcz.service.weeks.week4.W04D03Service;
import pl.cwtwcz.service.weeks.week4.W04D04Service;
import pl.cwtwcz.service.weeks.week4.W04D05Service;
import pl.cwtwcz.dto.week4.DroneInstructionDto;
import pl.cwtwcz.dto.week4.DroneResponseDto;

@RequiredArgsConstructor
@RestController
@RequestMapping("/weeks/4")
public class Week4RestController {

    private final W04D01Service w04d01Service;
    private final W04D02Service w04d02Service;
    private final W04D03Service w04d03Service;
    private final W04D04Service w04d04Service;
    private final W04D05Service w04d05Service;

    @GetMapping("days/1")
    public String w04d01() {
        return w04d01Service.w04d01();
    }

    @GetMapping("days/2")
    public String w04d02() {
        return w04d02Service.w04d02();
    }

    @GetMapping("days/3")
    public String w04d03() {
        return w04d03Service.w04d03();
    }

    @PostMapping("days/4")
    public DroneResponseDto w04d04(@RequestBody DroneInstructionDto instruction) {
        return w04d04Service.w04d04(instruction);
    }

    @GetMapping("days/5")
    public String w04d05() {
        return w04d05Service.w04d05();
    }
}