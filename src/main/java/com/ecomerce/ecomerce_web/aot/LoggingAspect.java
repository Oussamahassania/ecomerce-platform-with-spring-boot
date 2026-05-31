package com.ecomerce.ecomerce_web.aot;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    @Around("execution(* com.ecomerce.ecomerce_web.services.*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint jp)throws Throwable {
        String methodeName = jp.getSignature().getName();
        String className = jp.getTarget().getClass().getSimpleName();
        String user = getCurrentUser();
        log.info("[{}] Calling {}.{}() with args: {}",
                user,className,methodeName, safeArgs(jp.getArgs()));
        long start = System.currentTimeMillis();
        Object result = jp.proceed();
        long duration = System.currentTimeMillis() - start;
        if (duration > 2000) {
            log.warn(" slow methode : {}.{}() took {}ms", className, methodeName, duration);
        } else {
            log.info(" [{}] {}.{}() finished in {}ms", user, className, methodeName, duration);
        }
        return result;
    }
    @Before("execution(* com.ecomerce.ecomerce_web.services.OrderService.updateOrderStatus(..))")
    public void logOrderStatusChange(JoinPoint jp){
        Object [] args = jp.getArgs();
        log.info("Order status update requested - orderId: {}, newStatus: {} ",args[0],args[1]);
    }
    @Before("execution(* com.ecomerce.ecomerce_web.services.OrderService.cancelOrder(..))")
    public void logOrderCancellation(JoinPoint jp){
      Object [] args = jp.getArgs();
      log.info("Order cancellation requested — orderId: {}", args[0]);
    }
    @AfterThrowing(
            pointcut = "execution(* com.ecomerce.ecomerce_web.services.*.*(..))",
            throwing = "ex"
    )
    public void logException(JoinPoint jp,Exception ex){
        log.error("Exception in {}.{}() — message: {}",
                jp.getTarget().getClass().getSimpleName(),
                jp.getSignature().getName(),ex.getMessage());
    }
    @AfterReturning(
            pointcut = "execution(* com.ecomerce.ecomerce_web.services.OrderService.createOrder(..))",
            returning = "result"
    )
    public void logOrderCreated(JoinPoint jp,Object result){
        log.info(" [{}] New order created successfully — result: {}",getCurrentUser(),result);
    }
    @Before("execution(* com.ecomerce.ecomerce_web.services.ProductService.delete(..))")
    public void logProductDeletion(JoinPoint jp){
        log.warn("[{}] Product delete requested — productId: {}", getCurrentUser(),jp.getArgs()[0]);
    }

    @Before("execution(* com.ecomerce.ecomerce_web.services.RoleService.changeRole(..))")
    public void logRoleChange(JoinPoint jp) {
        Object[] args = jp.getArgs();
        log.warn("[{}] Role change requested — userId: {}, newRole: {}",
                getCurrentUser(), args[0], args[1]);
    }
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }
    private String safeArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (Object arg : args) {
            try {
                if (arg == null) {
                    sb.append("null");
                } else {
                    sb.append(arg.getClass().getSimpleName());
                }
            } catch (Exception e) {
                sb.append("?");
            }
            sb.append(", ");
        }
        if (sb.length() > 1) sb.setLength(sb.length() - 2);
        sb.append("]");
        return sb.toString();
    }
}
