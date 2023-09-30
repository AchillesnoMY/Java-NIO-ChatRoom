/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2023 All Rights Reserved
 */
package org.my;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author MY_c
 * @version $Id: MessageUtils.java, v 0.1 2023-09-28-9:04 pm
 */
public class MessageUtils {

    /***
     * Serialize message to send
     * @param message message to be serialized
     * @return the byte array of message
     * @throws IOException Errors from serializing message
     */
    public static byte[] serialize(Message message) throws IOException {
        return doSerialization(message);
    }

    /***
     * The actual serialization job
     * @param obj The object to be serialized
     * @return byte array
     * @throws IOException
     */
    private static byte[] doSerialization(Object obj) throws IOException{
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream objStream = new ObjectOutputStream(byteStream)) {
            objStream.writeObject(obj);
            return byteStream.toByteArray();
        }
    }

    /***
     *
     * @param out
     * @param type
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Message deserializeMessage(ByteArrayOutputStream out) throws IOException, ClassNotFoundException {
        return (Message) doDeserialization(out.toByteArray());
    }

    /***
     *
     * @param bytes
     * @param type
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object doDeserialization(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
            ObjectInputStream objStream = new ObjectInputStream(byteStream);){
            return objStream.readObject();
        }
    }

    /***
     *
     * @param channel
     * @param bufferSize
     * @return
     * @throws IOException
     */
    public static ByteArrayOutputStream readMessageFromChannel(SocketChannel channel, int bufferSize) throws IOException {
        ByteBuffer readBuffer = ByteBuffer.allocate(bufferSize);
        int bytesRead = channel.read(readBuffer);
        // ByteArrayOutputStream.write(byte[]) will grow size if needed
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while(bytesRead > 0){
            readBuffer.flip();
            outputStream.write(readBuffer.array());
            readBuffer.clear();
            bytesRead = channel.read(readBuffer);
        }
        return outputStream;
    }
}
