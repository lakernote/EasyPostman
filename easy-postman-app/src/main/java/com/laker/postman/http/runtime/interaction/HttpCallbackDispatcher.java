package com.laker.postman.http.runtime.interaction;

public interface HttpCallbackDispatcher {
    HttpCallbackDispatcher DIRECT = new HttpCallbackDispatcher() {
        @Override
        public boolean isDispatchThread() {
            return true;
        }

        @Override
        public void dispatch(Runnable action) {
            if (action != null) {
                action.run();
            }
        }
    };

    boolean isDispatchThread();

    void dispatch(Runnable action);

    static HttpCallbackDispatcher direct() {
        return DIRECT;
    }
}
