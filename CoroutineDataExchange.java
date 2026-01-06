/*


용도: Lazy Evaluation, Lazy Loading, 소비자가 데이터를 원할 때만 생산자가 일을 하게 만들 수 있음.
*/

public class CoroutineDataExchange {

    // 데이터를 주고받을 공유 메모리 (레지스터 역할)
    static int sharedBuffer = 0;

    enum State { START, YIELD, DONE }

    static class Producer {
        State pc = State.START;
        State resumeAt = State.START;
        int count = 1; // 일반적인 함수라면 리턴 시 지역 변수가 사라지지만, 이 코루틴 구조는 객체 내부에 상태를 유지

        void run() {
            switch (pc) {
                case START -> {
                    System.out.println("[P] 데이터 생성을 시작합니다.");
                    pc = State.YIELD;
                }
                case YIELD -> {
                    sharedBuffer = count * 10; // 데이터 생성 (10, 20, 30...)
                    System.out.println("[P] 데이터 " + sharedBuffer + " 생성 후 Yield");
                    count++;
                    
                    // 돌아올 위치 저장 및 제어권 반납 준비
                    resumeAt = (count > 3) ? State.DONE : State.YIELD;
                }
                case DONE -> System.out.println("[P] 모든 데이터를 생산했습니다.");
            }
        }
    }

    static class Consumer {
        State pc = State.START;
        
        void run() {
            System.out.println("[C] 받은 데이터: " + sharedBuffer);
        }
    }

    public static void main(String[] args) {
        Producer p = new Producer();
        Consumer c = new Consumer();
        
        // 메인 루프 (스케줄러 역할)
        while (p.pc != State.DONE) {
            // 1. Producer 실행 (데이터 생산)
            p.run(); 
            
            // 2. 제어권이 넘어왔을 때 Consumer 실행 (데이터 소비)
            if (p.pc != State.DONE) {
                c.run();
                p.pc = p.resumeAt; // Producer를 다음 상태로 복귀시킴
            }
        }
        
        System.out.println("시스템 종료.");
    }
}