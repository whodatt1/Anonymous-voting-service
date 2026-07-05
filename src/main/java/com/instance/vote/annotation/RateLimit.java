package com.instance.vote.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 해당 어노테이션은 메서드에만 붙일 수 있다
// 실행 중에도 이 어노테이션 정보를 읽을 수 있다
// 기본값인 CLASS는 컴파일 후 .class 파일에는 남지만 JVM 실행할 때는 버림
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit(); // 허용 횟수
    int windowSeconds(); // 시간 윈도우(초)
}
