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
package com.linkedin.android.testbutler.utils;

import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public class ExceptionCreator {
    /**
     * Creates Remote exception with maximum possible verbosity for current SDK running on.
     * If it is not possible to add desired information to exception it will be logged at "error" level
     *
     * @param tag - tag for Log
     * @param message - message which will be included in exception
     * @param exception - Optional {@link Exception} instance which may be included in result
     * @return {@link RemoteException} instance
     */
    public static RemoteException createRemoteException(@NonNull String tag,
                                                        @NonNull String message,
                                                        @Nullable Exception exception) {
        boolean printed = false;
        RemoteException remoteException;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            remoteException = new RemoteException(message);
        } else {
            Log.e(tag, message, exception);
            remoteException = new RemoteException();
            printed = true;
        }

        if(exception != null) {
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                remoteException.addSuppressed(exception);
            } else {
                if(!printed) {
                    Log.e(tag, message, exception);
                }
            }
        }
        return remoteException;
    }
}
