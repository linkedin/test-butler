/**
 * Copyright (C) 2016 LinkedIn Corp.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.testbutler.demo.utils;

import androidx.annotation.NonNull;

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
