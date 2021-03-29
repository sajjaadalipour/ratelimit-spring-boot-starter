package com.github.sajjaadalipour.ratelimit.servlet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("noLimit")
    public String noLimit() {
        return "Hello World!";
    }

    @GetMapping("firstTime")
    public String firstTime() {
        return "Hello World!";
    }

    @GetMapping("diffMethod")
    public String getMethod() {
        return "Hello World!";
    }

    @PostMapping("diffMethod")
    public String postMethod() {
        return "Hello World!";
    }

    @GetMapping("exceed")
    public String exceed() {
        return "Hello World!";
    }

    @GetMapping("notExceed")
    public String notExceed() {
        return "Hello World!";
    }

    @GetMapping("block")
    public String block() {
        return "Hello World!";
    }

    @GetMapping("diffPolicy")
    public String diffPolicy() {
        return "Hello World!";
    }

    @GetMapping("/global/policy1")
    public String globalPolicy1() {
        return "Hello World!";
    }

    @GetMapping("/global/excluded")
    public String globalExcludedPolicy() {
        return "Hello World!";
    }
}
