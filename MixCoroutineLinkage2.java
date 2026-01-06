public class MixCoroutineLinkage2 {
    enum State { S1, S2, S3, DONE }
    
    static class Coroutine {
        String name;
        State pc = State.S1;      // 현재 실행 위치
        State resumeAt = State.S1; // 다음에 돌아올 위치 (AX/BX 역할)
        
        Coroutine(String name) { this.name = name; }
        
        boolean isDone() { return pc == State.DONE; }
    }

    public static void main(String[] args) {
        Coroutine A = new Coroutine("A");
        Coroutine B = new Coroutine("B"); 
        Coroutine cur = A;

        int steps = 0;
        while (!(A.isDone() && B.isDone()) && steps++ < 1000) {
            if (cur.isDone()) { // 현재 루틴이 끝났다면 교체
                cur = (cur == A) ? B : A;
                continue;
            }

            switch (cur.pc) {
                case S1 -> {
                    System.out.println(cur.name + "1: 시작");
                    cur.pc = State.S2;
                }
                case S2 -> {
                    System.out.println(cur.name + "2: 제어권 전환 (JMP)");
                    cur.resumeAt = State.S3; // 복귀 지점 저장
                    
                    // 상대방 루틴으로 전환
                    Coroutine next = (cur == A) ? B : A;
                    next.pc = next.resumeAt; // 상대방은 이전에 멈췄던 곳부터
                    cur = next;
                }
                case S3 -> {
                    System.out.println(cur.name + "3: 복귀 후 계속");
                    cur.pc = State.DONE;
                }
            }
        }
        System.out.println("종료.");
    }
}