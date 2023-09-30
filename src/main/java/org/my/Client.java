/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2023 All Rights Reserved
 */
package org.my;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * The client server
 * @author MY_c
 * @version $Id: Client.java, v 0.1 2023-09-25-8:19 pm
 */
public class Client {

    /*** UserInfo **/
    private final UserInfo      userInfo;

    /*** Client channel **/
    private SocketChannel       serverChannel;

    /*** Selector **/
    private Selector            selector;

    /*** The flag to check if the user has been registered in chatroom server **/
    private volatile boolean isRegistered = false;

    /*** Buffer size **/
    private static final int    BUFFER_SIZE = 1024;

    /*** Chatroom server port **/
    private static final int    SERVER_PORT = 8088;

    /*** Chatroom server ip **/
    private static final String SERVER_HOST = "localhost";


    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("To run client server, you must provide two parameters [userName] and [userId]");
            System.exit(1);
        }
        String userName = args[0];
        String userId = args[1];
        new Client(userName, userId).join();
    }

    public Client(String userName, String userId) {
        this.userInfo = new UserInfo(userName, userId);
        serverConnecting();
    }

    /***
     * Run client server
     */
    public void join() {
        try {
            // Send a registration message to chatroom server if client has not done that yet
            if (this.serverChannel.isConnected()) {
                this.serverChannel.register(this.selector, SelectionKey.OP_READ);
                sendRegisterMessage();
            }
            this.serverChannel.register(this.selector, SelectionKey.OP_READ);
            getMessageReceivingTask().start();
            runMessageSendingTask();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /***
     * Connect chatroom server
     */
    private void serverConnecting() {
        try {
            this.serverChannel = SocketChannel
                .open(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            this.serverChannel.configureBlocking(false);
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /***
     * Get the task to receive and process incoming message from other users in chat room
     * @return The thread to perform the task
     */
    private Thread getMessageReceivingTask() {
        return new Thread(new MessageReceivingRunner());
    }

    private void runMessageSendingTask() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String input;
            while ((input = reader.readLine()) != null) {
                sendChatMessage(input);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /***
     * Send the message to our clients in chatroom
     * @param msg message to send
     * @throws IOException Errors from sending message
     */
    private void sendChatMessage(String msg) throws IOException {
        UserInfo userInfo = UserInfo.copy(this.userInfo);
        Message message = new Message();
        message.setUserInfo(userInfo);
        message.setMessageType(MessageType.CHAT);
        message.setContent(msg);

        byte[] messageBytes = MessageUtils.serialize(message);
        doMessageSend(messageBytes);
    }

    /***
     * After connecting to the chatroom server, client must send a registration message with user information
     * to chatroom server to register his identity.
     * The chatroom server takes advantage of client's identity to filter out some users when send message
     * @throws IOException Errors from sending registration message
     */
    private void sendRegisterMessage() throws IOException {
        UserInfo userInfo = UserInfo.copy(this.userInfo);
        Message message = new Message();
        message.setUserInfo(userInfo);
        message.setContent(null);
        message.setMessageType(MessageType.REGISTRATION);

        byte[] authInfoBytes = MessageUtils.serialize(message);
        doMessageSend(authInfoBytes);
    }

    /***
     * Send message to chatroom server
     * @param messageBytes byte arrays of message to be sent
     * @throws IOException Errors from sending message to chatroom server
     */
    private void doMessageSend(byte[] messageBytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
        this.serverChannel.write(buffer);
    }

    private String wrapMessageToPresent(Message message) {
        UserInfo userInfo = message.getUserInfo();
        String content = message.getContent();
        return userInfo.getUserName() + ": " + content;
    }

    /***
     * The runner to receive incoming message from chat room server and then process it
     */
    private class MessageReceivingRunner implements Runnable {

        @Override
        public void run() {
            while(true) {
                try {
                    int con = selector.select();
                    if(con == 0){
                        continue;
                    }
                    Iterator<SelectionKey> itr = selector.selectedKeys().iterator();
                    while(itr.hasNext()){
                        SelectionKey key = itr.next();
                        itr.remove();
                        // Receive and process new message from other users in chatroom
                        if (key.isReadable()) {
                            SocketChannel serverChannel = (SocketChannel) key.channel();
                            ByteArrayOutputStream outputStream = MessageUtils
                                    .readMessageFromChannel(serverChannel, BUFFER_SIZE);
                            Message message = MessageUtils.deserializeMessage(outputStream);
                            // If the user was not registered before, check if it is the authentication message ack
                            if(!isRegistered){
                                if(message.isAck() && message.getMessageType() == MessageType.REGISTRATION){
                                    isRegistered = true;
                                }
                                System.out.println("User[" + userInfo.getUserName() + "] has joined our chatroom");
                                continue;
                            }
                            // If chat message received, print the message in console directly
                            if(message.getMessageType() == MessageType.CHAT){
                                String messageToPresent = wrapMessageToPresent(message);
                                System.out.println(messageToPresent);
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
