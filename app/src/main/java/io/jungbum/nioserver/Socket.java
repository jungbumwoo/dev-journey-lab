package io.jungbum.nioserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Socket {

    public long socketId;

    public SocketChannel socketChannel = null;
    public IMessageReader messageReader = null;
    public MessageWriter messageWriter = null;

    public boolean endOfStreamReached = false;

    public Socket(SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
    }

    public int read(ByteBuffer byteBuffer) throws IOException {
        int bytesRead = this.socketChannel.read(byteBuffer);
        int totalBytesRead = bytesRead;

        // 기존 코드 수정 한 부분
        // 다시 read 했을 때 -1이 나올 수 있음으로 이 경우에는 totalBytesRead에 -1이 되지 않도록 수정함.
        while (bytesRead > 0) {
            bytesRead = this.socketChannel.read(byteBuffer);
            if (bytesRead == -1) {
                this.endOfStreamReached = true;
                break;
            }

            if (bytesRead == 0) break; // 더 이상 읽을 데이터 없음
            totalBytesRead += bytesRead;
        }

        return totalBytesRead;
    }

    public int write(ByteBuffer byteBuffer) throws IOException {

        /*
        * write() 메서드의 bytesWritten > 0 조건:
            SocketChannel.write()는 네트워크 버퍼가 가득 차면 0을 반환
            *  이때 while 루프를 돌면 CPU를 100% 점유하는 Busy Waiting이 발생.
            * OP_WRITE 전략을 써서, 0이 반환되면 루프를 빠져나가고 나중에 다시 시도 필요
        * */
        int bytesWritten = this.socketChannel.write(byteBuffer);
        int totalBytesWritten = bytesWritten;

        while (bytesWritten > 0 && byteBuffer.hasRemaining()) {
            bytesWritten = this.socketChannel.write(byteBuffer);
            totalBytesWritten += bytesWritten;
        }

        return totalBytesWritten;
    }
}
