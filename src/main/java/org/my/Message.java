/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2023 All Rights Reserved
 */
package org.my;

import java.io.Serializable;

/**
 * @author MY_c
 * @version $Id: Message.java, v 0.1 2023-09-28-9:18 pm
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 325627995035441028L;

    private UserInfo userInfo;

    private String content;

    private MessageType messageType;

    private boolean ack = false;

    public Message(){

    }

    public Message(UserInfo userInfo, String content){
        this.userInfo = userInfo;
        this.content = content;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public String getContent() {
        return content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public boolean isAck() {
        return ack;
    }

    public void setAck(boolean ack) {
        this.ack = ack;
    }
}
