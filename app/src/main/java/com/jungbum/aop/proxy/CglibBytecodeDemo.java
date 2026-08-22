package com.jungbum.aop.proxy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.asm.ClassReader;
import org.springframework.cglib.core.DefaultGeneratorStrategy;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

/** CGLIB이 ASM으로 만든 class 바이트 배열을 실제 .class 파일로 저장한다. */
public final class CglibBytecodeDemo {

    private CglibBytecodeDemo() {
    }

    public record GeneratedProxy(Class<?> type, byte[] bytecode) {

        public GeneratedProxy {
            bytecode = bytecode.clone();
        }

        @Override
        public byte[] bytecode() {
            return bytecode.clone();
        }
    }

    public static GeneratedProxy generateProxyClass() {
        CapturingGeneratorStrategy strategy = new CapturingGeneratorStrategy();

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(CalculatorService.class);
        enhancer.setCallbackType(MethodInterceptor.class);
        enhancer.setStrategy(strategy);
        // 테스트 실행 순서나 CGLIB 전역 캐시와 무관하게 항상 바이트코드를 캡처한다.
        enhancer.setUseCache(false);

        Class<?> proxyClass = enhancer.createClass();
        return new GeneratedProxy(proxyClass, strategy.generatedBytecode());
    }

    public static void main(String[] args) throws IOException {
        GeneratedProxy generated = generateProxyClass();
        ClassReader reader = new ClassReader(generated.bytecode());
        Path outputRoot = args.length == 0
                ? Path.of("build", "generated-cglib")
                : Path.of(args[0]);
        Path classFile = outputRoot.resolve(reader.getClassName() + ".class");

        Files.createDirectories(classFile.getParent());
        Files.write(classFile, generated.bytecode());

        System.out.println("generated class : " + reader.getClassName().replace('/', '.'));
        System.out.println("super class     : " + reader.getSuperName().replace('/', '.'));
        System.out.println("class file      : " + classFile.toAbsolutePath());
        System.out.println("inspect command : javap -c -p " + classFile.toAbsolutePath());
    }

    private static final class CapturingGeneratorStrategy extends DefaultGeneratorStrategy {

        private byte[] generatedBytecode;

        @Override
        protected byte[] transform(byte[] bytecode) {
            generatedBytecode = bytecode.clone();
            return bytecode;
        }

        byte[] generatedBytecode() {
            if (generatedBytecode == null) {
                throw new IllegalStateException("CGLIB did not generate bytecode");
            }
            return generatedBytecode.clone();
        }
    }
}
