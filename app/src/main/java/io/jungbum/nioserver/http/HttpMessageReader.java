package io.jungbum.nioserver.http;

import io.jungbum.nioserver.IMessageReader;
import io.jungbum.nioserver.Message;
import io.jungbum.nioserver.MessageBuffer;
import io.jungbum.nioserver.Socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class HttpMessageReader implements IMessageReader {

    private MessageBuffer messageBuffer    = null;

    private List<Message> completeMessages = new ArrayList<Message>();
    private Message       nextMessage      = null;

    public HttpMessageReader() {
    }

    @Override
    public void init(MessageBuffer readMessageBuffer) {
        this.messageBuffer        = readMessageBuffer;
        this.nextMessage          = messageBuffer.getMessage();
        this.nextMessage.metaData = new HttpHeaders();
    }

    @Override
    public void read(Socket socket, ByteBuffer byteBuffer) throws IOException {
        // socket에서 buffer로 write
        int bytesRead = socket.read(byteBuffer);
        // buffer에서 read를 위해 flip.
        byteBuffer.flip();

        if(byteBuffer.remaining() == 0){
            byteBuffer.clear();
            return;
        }

        // buffer -> message로 가져옴
        this.nextMessage.writeToMessage(byteBuffer);

        // loop 돌도록 변경하였음
        while (true) {
            int endIndex = HttpUtil.parseHttpRequest(
                    this.nextMessage.sharedArray,
                    this.nextMessage.offset,
                    this.nextMessage.offset + this.nextMessage.length,
                    (HttpHeaders) this.nextMessage.metaData
            );

            if (endIndex == -1) break;

            Message completeMsg = this.nextMessage;
            Message newNextMsg = this.messageBuffer.getMessage();
            newNextMsg.metaData = new HttpHeaders();

            newNextMsg.writePartialMessageToMessage(nextMessage, endIndex);

            completeMsg.length = endIndex - completeMsg.offset;

            completeMessages.add(completeMsg);
            this.nextMessage = newNextMsg;
        }

        byteBuffer.clear();
    }


    @Override
    public List<Message> getMessages() {
        return this.completeMessages;
    }

}
