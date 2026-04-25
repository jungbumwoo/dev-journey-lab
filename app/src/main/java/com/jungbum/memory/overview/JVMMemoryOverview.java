package com.jungbum.memory.overview;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;

/**
 * [JVM 메모리 구조 전체 개요]
 *
 * JVM 메모리 구조:
 *
 *  ┌──────────────────────────────────────────────────────────┐
 *  │                    JVM Runtime Data Areas                 │
 *  │                                                          │
 *  │  ┌──────────────────────────────────────────────────┐   │
 *  │  │              스레드 공유 영역                       │   │
 *  │  │  ┌───────────────────────┐ ┌──────────────────┐  │   │
 *  │  │  │        Heap           │ │   Method Area    │  │   │
 *  │  │  │ (객체, 배열)           │ │  (Metaspace)     │  │   │
 *  │  │  │                       │ │ (클래스 메타정보)  │  │   │
 *  │  │  │  Young  │  Old        │ │ (Runtime         │  │   │
 *  │  │  │  Gen    │  Gen        │ │  Constant Pool)  │  │   │
 *  │  │  └───────────────────────┘ └──────────────────┘  │   │
 *  │  └──────────────────────────────────────────────────┘   │
 *  │                                                          │
 *  │  ┌──────────────────────────────────────────────────┐   │
 *  │  │              스레드 개별 영역                       │   │
 *  │  │  ┌──────────┐ ┌──────────┐ ┌───────────────────┐│   │
 *  │  │  │ PC       │ │ VM       │ │ Native Method     ││   │
 *  │  │  │ Register │ │ Stack    │ │ Stack             ││   │
 *  │  │  │(명령어   │ │(스택     │ │(네이티브 메서드    ││   │
 *  │  │  │ 주소)    │ │ 프레임)  │ │ 스택)             ││   │
 *  │  │  └──────────┘ └──────────┘ └───────────────────┘│   │
 *  │  └──────────────────────────────────────────────────┘   │
 *  │                                                          │
 *  │  ┌──────────────────────────────────────────────────┐   │
 *  │  │         Direct Memory (네이티브 메모리)            │   │
 *  │  │         (NIO DirectByteBuffer)                    │   │
 *  │  └──────────────────────────────────────────────────┘   │
 *  └──────────────────────────────────────────────────────────┘
 *
 * 실행 방법:
 *   java -Xms64m -Xmx64m -XX:MetaspaceSize=32m \
 *        com.jungbum.memory.overview.JVMMemoryOverview
 */
public class JVMMemoryOverview {

    // Method Area: static 변수
    static final String APP_NAME = "JVM Memory Explorer";
    static int instanceCount = 0;

    // Heap: 인스턴스 변수
    int id;

    JVMMemoryOverview() {
        this.id = ++instanceCount;
    }

    public static void main(String[] args) {
        System.out.println("=== " + APP_NAME + " ===\n");

        // 전체 메모리 영역 정보 출력
        printMemoryOverview();

        // 각 영역에 데이터 할당
        demonstrateAllAreas();

        // MXBean을 통한 상세 정보
        printDetailedMemoryInfo();
    }

    static void printMemoryOverview() {
        System.out.println("[ JVM 메모리 영역 요약 ]\n");
        System.out.println("1. Heap (힙)");
        System.out.println("   - 용도: 객체 인스턴스, 배열");
        System.out.println("   - 스레드 공유: O");
        System.out.println("   - GC 대상: O");
        System.out.println("   - 설정: -Xms, -Xmx\n");

        System.out.println("2. Method Area (메서드 영역 / Metaspace)");
        System.out.println("   - 용도: 클래스 메타데이터, static 변수, 상수 풀");
        System.out.println("   - 스레드 공유: O");
        System.out.println("   - GC 대상: O (클래스 언로딩 시)");
        System.out.println("   - 설정: -XX:MetaspaceSize, -XX:MaxMetaspaceSize\n");

        System.out.println("3. VM Stack (가상 머신 스택)");
        System.out.println("   - 용도: 스택 프레임 (지역 변수, 오퍼랜드 스택, 반환 주소)");
        System.out.println("   - 스레드 공유: X (스레드별 독립)");
        System.out.println("   - 설정: -Xss\n");

        System.out.println("4. PC Register (프로그램 카운터)");
        System.out.println("   - 용도: 현재 실행 중인 바이트코드 주소");
        System.out.println("   - 스레드 공유: X (스레드별 독립)");
        System.out.println("   - OOM 발생: X (유일하게 OOM이 없는 영역)\n");

        System.out.println("5. Native Method Stack (네이티브 메서드 스택)");
        System.out.println("   - 용도: native 메서드 (C/C++) 실행");
        System.out.println("   - 스레드 공유: X (스레드별 독립)\n");

        System.out.println("6. Direct Memory (직접 메모리)");
        System.out.println("   - 용도: NIO DirectByteBuffer");
        System.out.println("   - JVM 관리: X (네이티브 메모리)");
        System.out.println("   - 설정: -XX:MaxDirectMemorySize\n");
        System.out.println("─".repeat(50));
        System.out.println();
    }

    static void demonstrateAllAreas() {
        System.out.println("[ 각 메모리 영역에 데이터 할당 ]\n");

        // Heap: 객체 생성
        JVMMemoryOverview obj = new JVMMemoryOverview();
        int[] array = new int[100];
        System.out.println("Heap: new JVMMemoryOverview() -> id=" + obj.id);
        System.out.println("Heap: new int[100] 배열 할당");

        // Method Area: static 변수 접근
        System.out.println("Method Area: APP_NAME = " + APP_NAME);
        System.out.println("Method Area: instanceCount = " + instanceCount);

        // VM Stack: 지역 변수
        int localVar = 42;
        System.out.println("VM Stack: localVar = " + localVar + " (현재 스택 프레임)");

        // Direct Memory: NIO 버퍼
        ByteBuffer directBuf = ByteBuffer.allocateDirect(1024);
        directBuf.putInt(999);
        directBuf.flip();
        System.out.println("Direct Memory: ByteBuffer에 " + directBuf.getInt() + " 저장");

        System.out.println();
    }

    /**
     * MemoryMXBean과 MemoryPoolMXBean으로 실제 메모리 사용량을 확인한다.
     */
    static void printDetailedMemoryInfo() {
        System.out.println("[ 상세 메모리 정보 (MXBean) ]\n");

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        // Heap 메모리
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        System.out.println("Heap Memory:");
        System.out.println("  Init: " + toMB(heapUsage.getInit()) + " MB");
        System.out.println("  Used: " + toMB(heapUsage.getUsed()) + " MB");
        System.out.println("  Committed: " + toMB(heapUsage.getCommitted()) + " MB");
        System.out.println("  Max: " + toMB(heapUsage.getMax()) + " MB");

        // Non-Heap 메모리 (Metaspace 포함)
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        System.out.println("\nNon-Heap Memory (Metaspace 등):");
        System.out.println("  Used: " + toMB(nonHeapUsage.getUsed()) + " MB");
        System.out.println("  Committed: " + toMB(nonHeapUsage.getCommitted()) + " MB");

        // 개별 메모리 풀 정보
        System.out.println("\nMemory Pools:");
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = pool.getUsage();
            System.out.println("  " + pool.getName() + " [" + pool.getType() + "]");
            System.out.println("    Used: " + toMB(usage.getUsed()) + " MB, "
                    + "Max: " + (usage.getMax() == -1 ? "unlimited" : toMB(usage.getMax()) + " MB"));
        }
    }

    static String toMB(long bytes) {
        return String.format("%.2f", bytes / (1024.0 * 1024.0));
    }
}
