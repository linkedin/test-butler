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
package com.linkedin.android.testbutler;


import android.os.Build;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class PermissionChanger {

    public @NonNull String grantPermission(@NonNull String packageName, @NonNull String permission) {
        return executeOnShell("pm grant " + packageName + " " + permission);
    }

    public @NonNull String revokePermission(@NonNull String packageName, @NonNull String permission) {
        return executeOnShell("pm revoke " + packageName + " " + permission);
    }

    private @NonNull String executeOnShell(@NonNull String command) {
        StringBuilder builder = new StringBuilder("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Process child = Runtime.getRuntime().exec(command);
                InputStream in = null;
                try {
                    in = child.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

                    String buffer;
                    while ((buffer = bufferedReader.readLine()) != null) {
                        builder.append(buffer);
                    }
                } finally {
                    if(in != null) {
                        in.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return builder.toString();
    }
}
