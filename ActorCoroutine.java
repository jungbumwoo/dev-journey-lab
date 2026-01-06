import java.util.LinkedList;
import java.util.Queue;

public class ActorCoroutine {

    // 메시지 정의
    record Message(String content, Actor sender) {}

    enum ActorState { IDLE, RECEIVING, DONE }

    static class Actor {
        String name;
        Queue<Message> mailbox = new LinkedList<>();
        ActorState pc = ActorState.IDLE;
        
        Actor(String name) { this.name = name; }

        // 다른 액터에게 메시지 보내기 (우체통에 넣기)
        void send(Actor target, String content) {
            System.out.println("[" + this.name + "] -> [" + target.name + "] : " + content);
            target.mailbox.add(new Message(content, this));
        }

        // 액터의 실행 로직 (코루틴)
        void run() {
            switch (pc) {
                case IDLE -> {
                    // 처음 시작할 때의 상태
                    pc = ActorState.RECEIVING;
                }
                case RECEIVING -> {
                    if (mailbox.isEmpty()) {
                        // 메시지가 없으면 그냥 Yield (상태 유지)
                        return;
                    }
                    
                    Message msg = mailbox.poll();
                    processMessage(msg);
                }
                case DONE -> { /* 종료 */ }
            }
        }

        private void processMessage(Message msg) {
            System.out.println("[" + this.name + "] 가 메시지 수신: " + msg.content);
            
            if (msg.content.equals("PING")) {
                send(msg.sender, "PONG");
            } else if (msg.content.equals("PONG")) {
                send(msg.sender, "PING");
            }
            
            // 실습을 위해 5번만 하고 멈추게 할 수도 있지만, 
            // 여기서는 계속 RECEIVING 상태를 유지하도록 합니다.
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Actor ping = new Actor("PING-ACTOR");
        Actor pong = new Actor("PONG-ACTOR");

        // 초기 메시지 투척
        ping.send(pong, "PING");

        // 스케줄러 루프 (코루틴들을 돌리며 실행)
        int rounds = 0;
        while (rounds < 10) {
            ping.run();
            pong.run();
            
            rounds++;
            Thread.sleep(500); // 눈으로 보기 위해 약간의 지연
        }
        
        System.out.println("시뮬레이션 종료.");
    }
}