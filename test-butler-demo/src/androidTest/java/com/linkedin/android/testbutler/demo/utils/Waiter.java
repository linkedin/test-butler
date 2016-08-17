package com.linkedin.android.testbutler.demo.utils;

import android.support.annotation.NonNull;

/**
 * Created by art on 17.08.16.
 */
public class Waiter {
    public static boolean wait(int retryCount, @NonNull DelayDependOnCount delayDependOnCount, @NonNull Predicate predicate) {
        for(int i = 0; i < retryCount; i++) {
            if(predicate.compute(i)) {
                return true;
            }
            try {
                Thread.sleep(delayDependOnCount.getDelay(i));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public interface DelayDependOnCount {
        /**
         * Returns delay depends on try count.
         * @param tryCount - sequential number of calling this method
         * @return delay in MS
         */
        long getDelay(int tryCount);

        final class SimpleLinearDelay implements DelayDependOnCount {
            private final long pause;

            public SimpleLinearDelay(long pause) {
                this.pause = pause;
            }

            @Override
            public long getDelay(int tryCount) {
                return tryCount*pause;
            }
        }
    }

    public interface Predicate {
        boolean compute(int tryCount);
    }
}
