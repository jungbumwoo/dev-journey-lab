package io.jungbum.nioserver;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 메시지를 소켓으로 비동기 전송
 * 논블로킹 I/O 환경에서 데이터가 일부만 전송되는 상황을 관리.
 * 전송 대기열을 두고 여러 메시지를 순차적으로 처리.
 */
public class MessageWriter {

//    private List<Message> writeQueue   = new ArrayList<>();
    private Deque<Message> writeQueue   = new ArrayDeque<>(); // remove(0) 대신 poll로 변경함
    private Message  messageInProgress = null;
    private int      bytesWritten      =    0;

    public MessageWriter() {
    }

    /**
     * 새로운 메시지를 전송 대기열에 추가
     * 만약 현재 처리 중인 메시지가 없다면 즉시 활성 메시지로 설정하여 전송 준비
     * * @param message 전송할 데이터가 담긴 Message 객체
     */
    public void enqueue(Message message) {
        if(this.messageInProgress == null){
            this.messageInProgress = message;
        } else {
            this.writeQueue.add(message);
        }
    }

    /**
     * 실제로 소켓에 데이터를 쓰는 핵심 method
     * Selector 루프에서 OP_WRITE 이벤트가 발생할 때마다 호출
     * * @param socket 데이터를 전송할 대상 소켓 (NIO SocketChannel 래퍼)
     * @param byteBuffer 데이터 전송을 위한 임시 버퍼 (Intermediary). socket write 시에는 ByteBuffer 를 param으로 전달해야함.
     * @throws IOException 소켓 전송 중 발생할 수 있는 I/O exception
     */
    public void write(Socket socket, ByteBuffer byteBuffer) throws IOException {
        if (this.messageInProgress == null) {
            return;
        }

        // byteBuffer overflow 방지
        // remaining이 작어서 보지 못한 데이터는 어떻게 처리?
        // 처리하지 않는다. write가 다시 호출 될 때 처리 됨
        int amountToPut = Math.min(byteBuffer.remaining(), this.messageInProgress.length - this.bytesWritten);

        // []byte로 socket에 write가 안되서(왜?) ByteBuffer 로 옮겨 write 함.
        byteBuffer.put(this.messageInProgress.sharedArray,
                this.messageInProgress.offset + this.bytesWritten,
                amountToPut);
        byteBuffer.flip();

        this.bytesWritten += socket.write(byteBuffer);
        byteBuffer.clear();

        if(bytesWritten >= this.messageInProgress.length){
            // 메세지 하나가 완료되면 다음 메세지 진행을 위해 bytesWritten을 0으로 설정하는 것을 추가하였음
            this.bytesWritten = 0;

            if(!this.writeQueue.isEmpty()){
//                this.messageInProgress = this.writeQueue.remove(0);
                this.messageInProgress = this.writeQueue.poll();
            } else {
                this.messageInProgress = null;
                // todo unregister from selector
                // 해제하지 않으면 소켓 버퍼가 비어있을 때마다 불필요한 이벤트가 계속 발생하여 CPU를 점유할 수 있음
            }
        } // 여기서 else 문은 필요가 없다. 다 보내지 못하더라도 this.bytesWritten < this.messageInProgress.length 이니 다시 호출 될 때 처리 된다.
    }

    /**
     * 현재 전송 대기 중이거나 처리 중인 메시지가 있는지 확인
     * * @return 전송할 데이터가 전혀 없다면 true
     */
    public boolean isEmpty() {
        return this.writeQueue.isEmpty() && this.messageInProgress == null;
    }

}
