package com.zerobase.account.service;

import com.zerobase.account.aop.AccountLockIdInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {

    private final LockService lockService;

    // 어떤 경우에 LockAopAspect 를 사용할 것인가?
    @Around("@annotation(com.zerobase.account.aop.AccountLock) && args(request)")
    public Object aroundMethod(ProceedingJoinPoint pjp, AccountLockIdInterface request) throws Throwable {

        // lock 취득 시도
        lockService.lock(request.getAccountNumber());

        try {
            return pjp.proceed();
        } finally {
            // lock 해치
            lockService.unlock(request.getAccountNumber());
        }
    }


}
