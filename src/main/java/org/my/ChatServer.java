/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2023 All Rights Reserved
 */
package org.my;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The chatroom server
 * @author MY_c
 * @version $Id: ChatServer.java, v 0.1 2023-09-25-7:18 pm
 */
public class ChatServer {

    /*** Server port **/
    private int port;

    /*** Selector **/
    private Selector selector;

    /*** Server channel **/
    private ServerSocketChannel serverChannel;

    /*** The map maintains user and channel relationship **/
    private final Map<UserInfo, SocketChannel> usersMap = new ConcurrentHashMap<>();

    /*** Buffer size **/
    private static final int BUFFER_SIZE = 1024;

    /*** Server port **/
    private static final int SERVER_PORT = 8088;


    public static void main(String[] args) {
        new ChatServer(SERVER_PORT).start();
    }

    public ChatServer(int port){
        try{
            this.port = port;
            this.selector = Selector.open();
            this.serverChannel = ServerSocketChannel.open();
            this.serverChannel.configureBlocking(false);
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /***
     * Run chatroom server
     */
    public void start(){
        try {
            this.serverChannel.socket().bind(new InetSocketAddress(this.port));
            this.serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
            System.out.println("The chatroom server has started");
            processIncomingMessage();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /***
     * Receive and process incoming message
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void processIncomingMessage() throws IOException, ClassNotFoundException {
        while(true){
            try{
                if(this.selector.select() > 0){
                    Iterator<SelectionKey> itr = this.selector.selectedKeys().iterator();
                    while(itr.hasNext()){
                        SelectionKey key = itr.next();
                        itr.remove();
                        if(key.isAcceptable()){
                            processUserConnection(key);
                        }
                        else if(key.isReadable()){
                            processMessage(key);
                        }
                        else if(key.isWritable()){
                            processResendMessage(key);
                        }
                        else{
                            throw new IllegalStateException("Illegal interest set: " + key.interestOps());
                        }
                    }
                }
            }
            catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }
    }

    /***
     * Process new user connection
     * @param key The selection key
     * @throws IOException
     */
    private void processUserConnection(SelectionKey key) throws IOException {
        SocketChannel clientChannel = this.serverChannel.accept();
        if(Objects.nonNull(clientChannel)){
            System.out.println("A new user is trying to join: " + clientChannel.getRemoteAddress());
            clientChannel.configureBlocking(false);
            clientChannel.register(this.selector, SelectionKey.OP_READ);
        }
    }

    /***
     * Process chat message from users
     * @param key
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void processMessage(SelectionKey key) throws IOException, ClassNotFoundException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        ByteArrayOutputStream outputStream = MessageUtils.readMessageFromChannel(clientChannel, BUFFER_SIZE);
        Message message = MessageUtils.deserializeMessage(outputStream);
        if(Objects.isNull(message)){
            return;
        }
        UserInfo userInfo = message.getUserInfo();
        // The user was not registered before, register the user and send an ACK back to user
        if(!isUserRegistered(userInfo)){
            processUserRegistrationMessage(userInfo, clientChannel);
            return;
        }
        if(message.getMessageType() == MessageType.CHAT){
            sendMessageToUsersInOneRoom(message);
        }
    }

    private void processResendMessage(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        Message message = (Message) key.attachment();
        if(Objects.isNull(message)){
            return;
        }
        byte[] messageBytes = MessageUtils.serialize(message);
        ByteBuffer msgBuffer = ByteBuffer.wrap(messageBytes);
        clientChannel.write(msgBuffer);
        // remove the OP_WRITE
        int op = key.interestOps() - SelectionKey.OP_WRITE;
        key.interestOps(op);
    }

    /***
     *
     * @param user
     * @param clientChannel
     * @throws IOException
     */
    private void processUserRegistrationMessage(UserInfo user, SocketChannel clientChannel) throws IOException {
        this.usersMap.put(user, clientChannel);
        Message registerAckMessage = new Message();
        registerAckMessage.setAck(true);
        registerAckMessage.setMessageType(MessageType.REGISTRATION);

        byte[] messageBytes = MessageUtils.serialize(registerAckMessage);
        doMessageSend(clientChannel, messageBytes);
    }

    /***
     * Resend message
     * @param message message to send
     * @throws IOException
     */
    private void sendMessageToUsersInOneRoom(Message message) throws IOException{
        List<SocketChannel> usersToSend = new ArrayList<>();
        // Filter the message sender, the sender should not receive his message
        for(Map.Entry<UserInfo, SocketChannel> entry: this.usersMap.entrySet()){
            UserInfo user = entry.getKey();
            if(user.equals(message.getUserInfo())){
                continue;
            }
            usersToSend.add(entry.getValue());
        }
        byte[] messageBytes = MessageUtils.serialize(message);
        ByteBuffer msgBuffer = ByteBuffer.wrap(messageBytes);
        for(SocketChannel channel: usersToSend){
            int size = channel.write(msgBuffer);
            if(size == 0){
                // The socket buffer is full, register OP_WRITE and resend later
                channel.register(this.selector, SelectionKey.OP_WRITE + SelectionKey.OP_READ, message);
            }
        }
    }

    /***
     *
     * @param channel
     * @param messageBytes
     * @throws IOException
     */
    private void doMessageSend(SocketChannel channel, byte[] messageBytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
        channel.write(buffer);
    }

    /***
     *
     * @param user
     * @return
     */
    private boolean isUserRegistered(UserInfo user){
        return this.usersMap.containsKey(user);
    }
}
