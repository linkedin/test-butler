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
package com.linkedin.android.testbutler;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;


/**
 * A connection to an ADB server via Socket. Contains the ADB protocol implementation responsible
 * for sending ADB commands and reading responses.
 */
class AdbConnection {

    // since strings are prefixed with 4 hex chars, max message length is 16^4
    private static final int BUF_SIZE = 65536;

    private final DataInputStream input;
    private final DataOutputStream output;
    private final byte[] buf = new byte[BUF_SIZE];

    /**
     * @param socket A connected socket
     */
    AdbConnection(@NonNull Socket socket) throws IOException {
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    private void writeMsg(String message) throws IOException {
        String lengthHex = String.format("%04x", message.length());
        output.writeBytes(lengthHex);
        output.writeBytes(message);
    }

    private String readExactly(int length) throws IOException {
        input.readFully(buf, 0, length);
        return new String(buf, 0, length, "ascii");
    }

    private String readMsg() throws IOException {
        String lengthHex = readExactly(4);
        int length = Integer.parseInt(lengthHex, 16);
        return readExactly(length);
    }

    private void checkResponse() throws IOException {
        String response = readExactly(4);
        if (!"OKAY".equals(response)) {
            String error = readMsg();
            throw new IOException("command failed: " + error);
        }
    }

    void sendCommand(@NonNull String command) throws IOException {
        writeMsg(command);
        output.flush();
        checkResponse();
    }

    @NonNull
    String getResponse() {
        Scanner scanner = new Scanner(new BufferedInputStream(input)).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
