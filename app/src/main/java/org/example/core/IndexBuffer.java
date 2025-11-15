package org.example.core;

public class IndexBuffer {
    public int[] position = null;
    public int[] length = null;
    // assuming there can be max ...
    public byte[] type = null;

    public int count = 0; // the number of tokens / elements in the IndexBuffer.

    public IndexBuffer() {}

    public IndexBuffer(int capacity, boolean useTypeArray) {
        this.position = new int[capacity];
        this.length = new int[capacity];
        if (useTypeArray) {
            this.type = new byte[capacity];
        }
    }
}
