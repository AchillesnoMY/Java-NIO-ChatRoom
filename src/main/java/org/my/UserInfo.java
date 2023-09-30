/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2023 All Rights Reserved
 */
package org.my;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author MY_c
 * @version $Id: UserInfo.java, v 0.1 2023-09-25-10:56 pm
 */
public class UserInfo implements Serializable {

    private static final long serialVersionUID = -3256093623438961998L;


    private final String userName;

    private final String userId;

    public UserInfo(String userName, String userId) {
        this.userName = userName;
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return userName.equals(userInfo.userName) && userId.equals(userInfo.userId);
    }

    public static UserInfo copy(UserInfo userInfo){
        return new UserInfo(userInfo.userName, userInfo.userId);
    }
}


