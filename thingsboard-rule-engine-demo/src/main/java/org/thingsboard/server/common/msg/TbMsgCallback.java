package org.thingsboard.server.common.msg;

public interface TbMsgCallback {
    void onSuccess();

    void onFailure(String reason);
}
