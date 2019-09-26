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
package com.linkedin.android.testbutler.utils;

import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Common reflection calls. All exceptions are wrapped as RemoteExceptions with descriptive
 * messages.
 */
public class ReflectionUtils {

    private static final String TAG = ReflectionUtils.class.getSimpleName();

    private ReflectionUtils() {
    }

    @NonNull
    public static Class<?> classForName(@NonNull String className) throws RemoteException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw ExceptionCreator.createRemoteException(TAG, "ClassNotFoundException for " + className, e);
        }
    }

    @NonNull
    public static Method getMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Class<?>... args) throws RemoteException {
        try {
            return clazz.getMethod(methodName, args);
        } catch (NoSuchMethodException e) {
            throw ExceptionCreator.createRemoteException(TAG, "NoSuchMethodException for " + methodDescriptor(clazz, methodName, args), e);
        }
    }

    @Nullable
    public static Object invoke(@NonNull Method method, @Nullable Object instance, @NonNull Object... args) throws RemoteException {
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException e) {
            throw ExceptionCreator.createRemoteException(TAG, "IllegalAccessException for " + methodDescriptor(method), e);
        } catch (IllegalArgumentException e) {
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(arg.getClass().getSimpleName());
            }
            throw ExceptionCreator.createRemoteException(TAG,
                    String.format("IllegalArgumentException for %s, given %s, %s",
                            methodDescriptor(method), instance.getClass().getSimpleName(), sb.toString()), e);
        } catch (InvocationTargetException e) {
            throw ExceptionCreator.createRemoteException(TAG, "InvocationTargetException for " + methodDescriptor(method), e);
        }
    }

    private static String methodDescriptor(Method method) {
        return methodDescriptor(method.getDeclaringClass(), method.getName(), method.getParameterTypes());
    }

    private static String methodDescriptor(Class<?> clazz, String methodName, Class<?>... args) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> arg : args) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(arg.getSimpleName());
        }
        return String.format("%s.%s(%s)", clazz.getName(), methodName, sb.toString());
    }
}
