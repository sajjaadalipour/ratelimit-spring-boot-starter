package com.github.sajjaadalipour.ratelimit.servlet;

import org.springframework.web.bind.annotation.*;

@RestController
public class TestController {

    private final ServletApplication.RT rt;

    public TestController(ServletApplication.RT rt) {
        this.rt = rt;
    }

    @PostMapping("test")
    public String test(@RequestBody User user) {
        rt.test();
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


    static class  User {
        private String name;
        private String family;

        public String getName() {
            return name;
        }

        public User setName(String name) {
            this.name = name;
            return this;
        }

        public String getFamily() {
            return family;
        }

        public User setFamily(String family) {
            this.family = family;
            return this;
        }
    }
}
