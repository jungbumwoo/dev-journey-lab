// The Art of Computer Programming Vo1 - Donald Knuth. 1.4.2 coroutine 에서 설명하는 MIX 어셈블리 수준의 코루틴 연결(Coroutine Linkage) 방식을 자바의 상태 코드로 작성

public class MixCoroutineLinkage {

    enum AState { A1, A2, A3, DONE }
    enum BState { B1, B2, B3, DONE }
    enum Cur { A, B }

    public static void main(String[] args) {
        // (AX/BX) — 다음에 돌아올 location
        AState AX = AState.A1;
        BState BX = BState.B1;

        // 현재 PC
        AState aPC = AState.A1;
        BState bPC = BState.B1;

        Cur cur = Cur.A;

        int steps = 0;
        final int MAX_STEPS = 1000;

        while ((aPC != AState.DONE || bPC != BState.DONE) && steps++ < MAX_STEPS) {
            switch (cur) {
                case A -> {
                    switch (aPC) {
                        case A1 -> {
                            log("A1: A 시작");
                            aPC = AState.A2;
                        }
                        case A2 -> {
                            log("A2: B에게 넘긴다 (JMP B)");
                            // JMP B: rJ = A2 다음(A3) → B 엔트리에서 STJ AX
                            AX = AState.A3;  // 돌아올 때 A3에서 재개
                            // BX로 점프 → B 시작/재개점으로 전환
                            // (처음엔 B1, 이후엔 갱신된 값)
                            bPC = BX;
                            cur  = Cur.B;
                        }
                        case A3 -> {
                            log("A3: B에서 돌아와 A 계속 실행");
                            aPC = AState.DONE;
                            log("A_DONE");
                            if (bPC != BState.DONE) cur = Cur.B;
                        }
                        case DONE -> {
                            // 이미 끝났으면 B 쪽으로
                            if (bPC != BState.DONE) cur = Cur.B;
                        }
                        default -> throw new IllegalStateException("Unknown A state: " + aPC);
                    }
                }
                case B -> {
                    switch (bPC) {
                        case B1 -> {
                            log("B1: B 시작");
                            bPC = BState.B2;
                        }
                        case B2 -> {
                            log("B2: A에게 넘긴다 (JMP A)");
                            // JMP A: rJ = B2 다음(B3) → A 엔트리에서 STJ BX
                            BX = BState.B3; // “돌아올 때 B3에서 재개”
                            // AX로 점프 → A 시작/재개점으로 전환
                            aPC = AX;
                            cur  = Cur.A;
                        }
                        case B3 -> {
                            log("B3: A에서 돌아와 B 계속 실행");
                            bPC = BState.DONE;
                            log("B_DONE");
                            if (aPC != AState.DONE) cur = Cur.A;
                        }
                        case DONE -> {
                            if (aPC != AState.DONE) cur = Cur.A;
                        }
                        default -> throw new IllegalStateException("Unknown B state: " + bPC);
                    }
                }
                default -> throw new IllegalStateException("Unknown current side: " + cur);
            }
        }

        if (steps >= MAX_STEPS) {
            System.err.println("Guard triggered: too many steps (possible infinite loop).");
        } else {
            log("둘 다 종료.");
        }
    }

    private static void log(String msg) {
        System.out.println(msg);
    }
}
