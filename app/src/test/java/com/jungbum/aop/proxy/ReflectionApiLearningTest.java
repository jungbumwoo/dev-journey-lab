package com.jungbum.aop.proxy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Reflection API 호출 비용의 원인")
class ReflectionApiLearningTest {

    @Test
    @DisplayName("Method.invoke 경계에서는 primitive 인자와 반환값이 Object로 boxing된다")
    void primitiveValuesAreBoxedAtReflectionBoundary() throws ReflectiveOperationException {
        Method add = CalculatorService.class.getMethod("sumLeftRight", int.class, int.class);

        Object result = add.invoke(new CalculatorService(), 2, 3);

        assertInstanceOf(Integer.class, result);
        assertEquals(5, result);
    }

    @Test
    @DisplayName("컴파일 타임에 알 수 있던 인자 타입 오류를 reflection은 런타임에 검사한다")
    void argumentTypesAreCheckedAtRuntime() throws NoSuchMethodException {
        Method add = CalculatorService.class.getMethod("sumLeftRight", int.class, int.class);

        assertThrows(IllegalArgumentException.class,
                () -> add.invoke(new CalculatorService(), "not a number", 3));
    }

    @Test
    @DisplayName("target 예외를 InvocationTargetException으로 감싸 호출 경계를 보존한다")
    void targetExceptionIsWrapped() throws NoSuchMethodException {
        Method divide = CalculatorService.class.getMethod("divideLeftRight", int.class, int.class);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> divide.invoke(new CalculatorService(), 1, 0));

        assertInstanceOf(ArithmeticException.class, thrown.getTargetException());
    }

    @Test
    @DisplayName("MethodHandle.invokeExact는 호출 지점에 구체적인 메서드 타입을 유지한다")
    void methodHandleHasAnExactCallSiteType() throws Throwable {
        MethodHandle add = MethodHandles.lookup().findVirtual(
                CalculatorService.class,
                "sumLeftRight",
                MethodType.methodType(int.class, int.class, int.class));
        CalculatorService target = new CalculatorService();

        int result = (int) add.invokeExact(target, 2, 3);

        assertEquals(5, result);
        assertThrows(WrongMethodTypeException.class, () -> {
            Object ignored = add.invokeExact(target, 2, 3);
        });
    }
}
