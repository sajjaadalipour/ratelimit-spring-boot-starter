package com.github.sajjaadalipour.ratelimit;

import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mehran Behnam
 */
public class RateLimitAnnotationParser implements ApplicationContextAware {

    private final Context context;

    public RateLimitAnnotationParser(Context context) {
        this.context = context;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object obj = applicationContext.getBean(beanName);

            if (org.springframework.aop.support.AopUtils.isAopProxy(obj)) {
                Class<?> objClz = org.springframework.aop.support.AopUtils.getTargetClass(obj);
                if (isTargetClass(objClz))
                    for (Method m : objClz.getDeclaredMethods()) {
                        if (isTargetMethod(m)) {
                            String[] basePaths = new String[]{};
                            if (objClz.isAnnotationPresent(RequestMapping.class)) {
                                var requestMapping = objClz.getAnnotation(RequestMapping.class);
                                basePaths = requestMapping.value();
                            }
                            HashSet<RateLimitProperties.Policy.Route> routes = getRoutes(m, basePaths);
                            parseRateLimitAnnotation(m, routes);
                        }
                    }

            }
        }
    }

    private boolean isTargetClass(Class<?> objClz) {
        return objClz.isAnnotationPresent(RequestMapping.class) || objClz.isAnnotationPresent(Controller.class) || objClz.isAnnotationPresent(RestController.class);
    }

    private boolean isTargetMethod(Method m) {
        return m.isAnnotationPresent(RateLimit.class);
    }

    private HashSet<RateLimitProperties.Policy.Route> getRoutes(Method m, String[] basePaths) {
        var routes = new HashSet<RateLimitProperties.Policy.Route>();
        if (m.isAnnotationPresent(GetMapping.class))
            parseGetMappingAnnotation(basePaths, m, routes);
        if (m.isAnnotationPresent(PostMapping.class))
            parsePostMappingAnnotation(basePaths, m, routes);
        if (m.isAnnotationPresent(PutMapping.class))
            parsePutMappingAnnotation(basePaths, m, routes);
        if (m.isAnnotationPresent(PatchMapping.class))
            parsePatchMappingAnnotation(basePaths, m, routes);
        if (m.isAnnotationPresent(DeleteMapping.class))
            parseDeleteMappingAnnotation(basePaths, m, routes);
        if (m.isAnnotationPresent(RequestMapping.class))
            parseRequestMappingAnnotation(basePaths, m, routes);
        return routes;
    }

    private void parseRateLimitAnnotation(Method m, HashSet<RateLimitProperties.Policy.Route> routes) {
        var demoAnnotation = m.getAnnotation(RateLimit.class);
        var duration = demoAnnotation.duration();
        var count = demoAnnotation.count();
        var generator = "BY_IP";
        var block = demoAnnotation.block();
        var block1 = new RateLimitProperties.Policy.Block();
        block1.setDuration(Duration.ofSeconds(block));
        var policy = new RateLimitProperties.Policy(Duration.ofSeconds(duration), count, generator, routes, block1);
        context.getRateLimitProperties().getPolicies().add(policy);
    }

    private void parseGetMappingAnnotation(String[] basPaths, Method
            m, HashSet<RateLimitProperties.Policy.Route> routes) {
        var getMapping = m.getAnnotation(GetMapping.class);
        String[] result = Stream.of(getMapping.value(), getMapping.path()).flatMap(Stream::of).toArray(String[]::new);
        createRoutes(basPaths, routes, result, HttpMethod.GET);
    }

    private void parsePostMappingAnnotation(String[] basPaths, Method
            m, HashSet<RateLimitProperties.Policy.Route> routes) {
        var postMapping = m.getAnnotation(PostMapping.class);
        String[] result = Stream.of(postMapping.value(), postMapping.path()).flatMap(Stream::of).toArray(String[]::new);
        createRoutes(basPaths, routes, result, HttpMethod.POST);
    }

    private void parsePutMappingAnnotation(String[] basPaths, Method
            m, HashSet<RateLimitProperties.Policy.Route> routes) {
        var putMapping = m.getAnnotation(PutMapping.class);
        String[] result = Stream.of(putMapping.value(), putMapping.path()).flatMap(Stream::of).toArray(String[]::new);
        createRoutes(basPaths, routes, result, HttpMethod.PUT);
    }

    private void parsePatchMappingAnnotation(String[] basPaths, Method
            m, HashSet<RateLimitProperties.Policy.Route> routes) {
        var patchMapping = m.getAnnotation(PatchMapping.class);
        String[] result = Stream.of(patchMapping.value(), patchMapping.path()).flatMap(Stream::of).toArray(String[]::new);
        createRoutes(basPaths, routes, result, HttpMethod.PATCH);
    }

    private void parseDeleteMappingAnnotation(String[] basPaths, Method
            m, HashSet<RateLimitProperties.Policy.Route> routes) {
        var deleteMapping = m.getAnnotation(DeleteMapping.class);
        String[] result = Stream.of(deleteMapping.value(), deleteMapping.path()).flatMap(Stream::of).toArray(String[]::new);
        createRoutes(basPaths, routes, result, HttpMethod.DELETE);
    }

    private void parseRequestMappingAnnotation(String[] basPaths, Method
            m, HashSet<RateLimitProperties.Policy.Route> routes) {
        var annotation = m.getAnnotation(RequestMapping.class);
        Arrays.stream(annotation.method())
                .map(this::requestMethodToHttpMethod)
                .forEach(httpMethod -> createRoutes(basPaths, routes, annotation.path(), httpMethod));
    }

    private void createRoutes(String[] basePaths, HashSet<RateLimitProperties.Policy.Route> routes, String[]
            paths, HttpMethod httpMethod) {
        Set<RateLimitProperties.Policy.Route> collect;
        collect = Arrays.stream(paths)
                .flatMap(s -> Arrays.stream(basePaths)
                        .map(s1 -> s1 + s))
                .map(s -> new RateLimitProperties.Policy.Route(s, httpMethod)).collect(Collectors.toSet());
        if (paths.length == 0) {
            collect = Arrays.stream(basePaths).map(s -> new RateLimitProperties.Policy.Route(s, httpMethod)).collect(Collectors.toSet());
        }
        routes.addAll(collect);

    }

    private HttpMethod requestMethodToHttpMethod(RequestMethod requestMethod) {
        switch (requestMethod) {
            case GET:
                return HttpMethod.GET;
            case PUT:
                return HttpMethod.PUT;
            case HEAD:
                return HttpMethod.HEAD;
            case POST:
                return HttpMethod.POST;
            case PATCH:
                return HttpMethod.PATCH;
            case TRACE:
                return HttpMethod.TRACE;
            case DELETE:
                return HttpMethod.DELETE;
            case OPTIONS:
                return HttpMethod.OPTIONS;
        }
        return null;
    }
}