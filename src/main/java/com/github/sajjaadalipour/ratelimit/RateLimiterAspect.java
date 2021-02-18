package com.github.sajjaadalipour.ratelimit;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

@Aspect
public class RateLimiterAspect {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    @Pointcut("@annotation(limiter)")
    public void rateLimiter(Limiter limiter) {
        // ignore
    }

    @Pointcut("within(@org.springframework.stereotype.Controller *)")
    public void controllerBean() {
        // ignore
    }

    @Around(value = "rateLimiter(rateLimit) && controllerBean()", argNames = "pjp,rateLimit")
    public Object around(ProceedingJoinPoint pjp, Limiter rateLimit) throws Throwable {
        Method method = this.getMethod(pjp);

        Object[] args = pjp.getArgs();
        EvaluationContext context = this.bindParam(method, args);

        Expression expression = parser.parseExpression(rateLimit.key());
        Object key = expression.getValue(context);

        return pjp.proceed();
    }

    private Method getMethod(ProceedingJoinPoint pjp) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        return pjp.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes());
    }

    private EvaluationContext bindParam(Method method, Object[] args) {
        String[] params = discoverer.getParameterNames(method);

        EvaluationContext context = new StandardEvaluationContext();
        for (int len = 0; len < params.length; len++) {
            context.setVariable(params[len], args[len]);
        }
        return context;
    }
}