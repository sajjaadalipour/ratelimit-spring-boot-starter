package com.github.sajjaadalipour.ratelimit.servlet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("test")
    public String test() {
        return "test";
    }

    @GetMapping("testx")
    public String test1() {
        return "test";
    }

    @PostMapping("testx")
    public String test2() {
        return "test";
    }

    @PutMapping("testx")
    public String test3() {
        return "test";
    }

    @GetMapping("test-block")
    public String testBlock() {
        return "test";
    }
}
