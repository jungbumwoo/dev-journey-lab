package com.jungbum.aop.proxy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.cglib.proxy.Enhancer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CGLIB 상속 기반 프록시")
class CglibProxyLearningTest {

    @Test
    @DisplayName("CGLIB은 target class를 상속한 서브클래스를 런타임에 만든다")
    void generatedClassExtendsTargetClass() {
        CalculatorService proxy = CglibProxyFactory.create(CalculatorService.class, ignored -> { });

        assertTrue(Enhancer.isEnhanced(proxy.getClass()));
        assertSame(CalculatorService.class, proxy.getClass().getSuperclass());
        assertTrue(proxy instanceof CalculatorService);
        assertTrue(proxy.getClass().getName().contains("EnhancerByCGLIB"));
    }

    @Test
    @DisplayName("오버라이드 메서드는 callback을 거쳐 부모 구현을 호출한다")
    void overriddenMethodIsIntercepted() {
        List<String> trace = new ArrayList<>();
        CalculatorService proxy = CglibProxyFactory.create(CalculatorService.class, trace::add);

        assertEquals(5, proxy.sumLeftRight(2, 3));
        assertEquals(List.of("before:sumLeftRight", "after:sumLeftRight"), trace);
    }

    @Test
    @DisplayName("final 메서드는 오버라이드할 수 없어 callback을 우회한다")
    void finalMethodCannotBeIntercepted() {
        List<String> trace = new ArrayList<>();
        CalculatorService proxy = CglibProxyFactory.create(CalculatorService.class, trace::add);

        assertEquals("final method", proxy.finalOperation());
        assertTrue(trace.isEmpty());
    }

    @Test
    @DisplayName("final 클래스는 상속할 수 없어 CGLIB 프록시를 만들 수 없다")
    void finalClassCannotBeProxied() {
        assertThrows(IllegalArgumentException.class,
                () -> CglibProxyFactory.create(FinalService.class, ignored -> { }));
    }

    @Test
    @DisplayName("캡처한 class 파일에는 target superclass와 오버라이드/부모호출 메서드가 있다")
    void generatedBytecodeShowsSubclassMechanism() {
        CglibBytecodeDemo.GeneratedProxy generated = CglibBytecodeDemo.generateProxyClass();
        byte[] bytecode = generated.bytecode();
        ClassReader reader = new ClassReader(bytecode);
        Set<String> methodNames = new HashSet<>();

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(
                    int access, String name, String descriptor, String signature, String[] exceptions) {

                methodNames.add(name);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        assertArrayEquals(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE},
                java.util.Arrays.copyOf(bytecode, 4));
        assertEquals(CalculatorService.class.getName().replace('.', '/'), reader.getSuperName());
        assertEquals(generated.type().getName().replace('.', '/'), reader.getClassName());
        assertTrue(methodNames.contains("sumLeftRight"), "호출을 가로채는 override 메서드");
        assertTrue(methodNames.stream().anyMatch(name -> name.startsWith("CGLIB$sumLeftRight$")),
                "invokeSuper가 호출할 부모 구현 연결 메서드");
        assertFalse(methodNames.contains("finalOperation"), "final 메서드는 override할 수 없음");
    }

    @Test
    @DisplayName("Spring CglibAopProxy는 callback에서 ReflectiveMethodInvocation chain을 실행한다")
    void springCglibAopProxyUsesReflectiveMethodInvocation() {
        List<String> trace = new ArrayList<>();
        AtomicReference<Class<?>> invocationType = new AtomicReference<>();
        ProxyFactory factory = new ProxyFactory(new CalculatorService());
        factory.setProxyTargetClass(true);
        factory.addAdvice((MethodInterceptor) invocation -> {
            invocationType.set(invocation.getClass());
            trace.add("advice.before");
            Object result = invocation.proceed();
            trace.add("advice.after");
            return result;
        });

        CalculatorService proxy = (CalculatorService) factory.getProxy();

        assertTrue(AopUtils.isCglibProxy(proxy));
        assertSame(CalculatorService.class, proxy.getClass().getSuperclass());
        assertEquals(5, proxy.sumLeftRight(2, 3));
        assertEquals(List.of("advice.before", "advice.after"), trace);
        assertSame(ReflectiveMethodInvocation.class, invocationType.get());
    }

    static final class FinalService {
        public String work() {
            return "done";
        }
    }
}
