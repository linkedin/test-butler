/**
 * Copyright (C) 2019 LinkedIn Corp.
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
package com.linkedin.android.testbutler.shell;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

import com.linkedin.android.testbutler.utils.ExceptionCreator;

import java.lang.reflect.Method;

/**
 * A wrapper to expose hidden APIs in IActivityManager via reflection.
 */
class ActivityManagerWrapper {

    private static final String TAG = ActivityManagerWrapper.class.getSimpleName();

    private final Method broadcastIntent;
    private final Object iActivityManager;

    private ActivityManagerWrapper(Method broadcastIntent, Object iActivityManager) {
        this.broadcastIntent = broadcastIntent;
        this.iActivityManager = iActivityManager;
    }

    int broadcastIntent(Object iApplicationThreadCaller, Intent intent,
                        String resolvedType, Object iIntentReceiverResultTo, int resultCode,
                        String resultData, Bundle map, String[] requiredPermissions,
                        int appOp, Bundle options, boolean serialized, boolean sticky,
                        int userId) throws RemoteException {
        try {
            return (int) broadcastIntent.invoke(iActivityManager, iApplicationThreadCaller, intent,
                    resolvedType, iIntentReceiverResultTo, resultCode, resultData, map,
                    requiredPermissions, appOp, options, serialized, sticky, userId);
        } catch (Exception e) {
            throw ExceptionCreator.createRemoteException(TAG, "Failed to broadcast intent", e);
        }
    }

    static ActivityManagerWrapper newInstance() {
        try {
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManagerNative");
            Class<?> iApplicationThreadClass = Class.forName("android.app.IApplicationThread");
            Class<?> iIntentReceiverClass = Class.forName("android.content.IIntentReceiver");

            Object iActivityManager = activityManagerClass.getMethod("getDefault").invoke(null);

            Method broadcastIntent = iActivityManager.getClass().getMethod("broadcastIntent",
                    iApplicationThreadClass, Intent.class, String.class, iIntentReceiverClass,
                    int.class, String.class, Bundle.class, String[].class, int.class, Bundle.class,
                    boolean.class, boolean.class, int.class);
            return new ActivityManagerWrapper(broadcastIntent, iActivityManager);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ActivityManagerWrapper", e);
        }
    }
}
