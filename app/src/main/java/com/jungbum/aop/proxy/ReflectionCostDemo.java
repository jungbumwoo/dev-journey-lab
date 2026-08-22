package com.jungbum.aop.proxy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.function.IntFunction;

/**
 * 직접 호출, Method.invoke, MethodHandle 호출의 비용을 관찰하는 작은 실험이다.
 *
 * <p>정확한 벤치마크가 필요하면 JMH를 사용해야 한다. 이 코드는 워밍업과 결과 소비를 넣어
 * 가장 흔한 측정 오류만 줄인 학습용 데모이며 실행 환경에 따라 순위와 수치가 달라질 수 있다.</p>
 */
public final class ReflectionCostDemo {

    private static volatile long blackhole;

    private ReflectionCostDemo() {
    }

    public static void main(String[] args) throws Throwable {
        int iterations = args.length == 0 ? 1_000_000 : Integer.parseInt(args[0]);
        CalculatorService target = new CalculatorService();
        Method method = CalculatorService.class.getMethod("sumLeftRight", int.class, int.class);
        MethodHandle methodHandle = MethodHandles.lookup().findVirtual(
                CalculatorService.class,
                "sumLeftRight",
                MethodType.methodType(int.class, int.class, int.class));

        for (int i = 0; i < 5; i++) {
            runDirect(target, iterations / 10);
            runReflection(target, method, iterations / 10);
            runMethodHandle(target, methodHandle, iterations / 10);
        }

        measure("direct call", iterations, count -> runDirect(target, count));
        measure("Method.invoke", iterations, count -> runReflection(target, method, count));
        measure("MethodHandle", iterations, count -> runMethodHandle(target, methodHandle, count));

        System.out.println("blackhole = " + blackhole);
        System.out.println("주의: 수치는 설명용입니다. 신뢰할 수 있는 비교에는 JMH를 사용하세요.");
    }

    private static void measure(String name, int iterations, IntFunction<Long> action) {
        long start = System.nanoTime();
        action.apply(iterations);
        long elapsed = System.nanoTime() - start;
        System.out.printf("%-14s %8.2f ns/op%n", name, (double) elapsed / iterations);
    }

    private static long runDirect(CalculatorService target, int iterations) {
        long result = 0;
        for (int i = 0; i < iterations; i++) {
            result += target.sumLeftRight(i, 1);
        }
        return blackhole = result;
    }

    private static long runReflection(CalculatorService target, Method method, int iterations) {
        long result = 0;
        try {
            for (int i = 0; i < iterations; i++) {
                // int 인자는 Object[]에 담기며 Integer로 boxing되고, 반환값도 Integer로 boxing된다.
                result += (Integer) method.invoke(target, i, 1);
            }
        }
        catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return blackhole = result;
    }

    private static long runMethodHandle(CalculatorService target, MethodHandle methodHandle, int iterations) {
        long result = 0;
        try {
            for (int i = 0; i < iterations; i++) {
                // invokeExact는 (CalculatorService, int, int)int 타입을 호출 지점에서 정확히 맞춘다.
                result += (int) methodHandle.invokeExact(target, i, 1);
            }
        }
        catch (Throwable ex) {
            throw new IllegalStateException(ex);
        }
        return blackhole = result;
    }
}
