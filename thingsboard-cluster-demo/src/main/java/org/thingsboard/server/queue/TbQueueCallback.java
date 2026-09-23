package org.thingsboard.server.queue;

public interface TbQueueCallback {
    TbQueueCallback EMPTY = new TbQueueCallback() {
        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
        }

        @Override
        public void onFailure(Throwable error) {
        }
    };

    void onSuccess(TbQueueMsgMetadata metadata);

    void onFailure(Throwable error);
}
