package io.jungbum.nioserver;

public interface IMessageProcessor {
    public void process(Message message, WriteProxy writeProxy);
}
