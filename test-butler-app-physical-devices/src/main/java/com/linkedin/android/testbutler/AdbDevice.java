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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;


/**
 * Represents a device connected to an ADB server. Entry point for running ADB commands.
 */
class AdbDevice {

    private static final String TAG = AdbDevice.class.getSimpleName();

    private static final SimpleDateFormat LOGCAT_DATE_FORMAT = new SimpleDateFormat("MM-dd kk:mm:ss.SSS", Locale.US);

    private final String host;
    private final int port;
    private final String deviceId;

    /**
     * @param host The ADB server hostname
     * @param port The ADB server port
     * @param deviceId The serial ID of the device (equivalent to adb -s SERIAL). May be null to
     *                 run commands against the only connected device, assuming only one device is
     *                 or will be connected.
     */
    private AdbDevice(String host, int port, @Nullable String deviceId) {
        this.host = host;
        this.port = port;
        this.deviceId = deviceId;
    }

    /**
     * @param host The ADB server hostname
     * @param port The ADB server port
     * @return The AdbDevice for the device on which this code is running
     */
    @NonNull
    static AdbDevice getCurrentDevice(@NonNull String host, int port) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return getDevicePreO(host, port);
        } else {
            return findCurrentDevice(host, port);
        }
    }

    // Suppressing the serial identifier warning, as we need a guaranteed unique ID for all devices
    @SuppressLint("HardwareIds")
    @TargetApi(Build.VERSION_CODES.O)
    private static AdbDevice getDevicePreO(String host, int port) {
        try {
            String deviceId = Build.SERIAL;
            AdbDevice adbDevice = new AdbDevice(host, port, deviceId);
            // test the connection...
            adbDevice.shellCommand("echo").get();
            return adbDevice;
        } catch (Exception e) {
            Log.w(TAG, "Could not execute command using device serial number, assuming this device is the only device attached...");
            return new AdbDevice(host, port, null);
        }
    }

    private static AdbDevice findCurrentDevice(String host, int port) {
        // to find this device without using the device serial, we log a random string and then
        // check every device's logcat for that string.
        try {
            String key = String.format("deviceKey=%s", Integer.toHexString(new Random().nextInt()));
            List<AdbDevice> devices = AdbDevice.getDevices(host, port);
            for (AdbDevice device : devices) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.SECOND, -5);  // so that -t <timestamp> has some buffer
                String timestamp = LOGCAT_DATE_FORMAT.format(cal.getTime());

                Log.v(TAG, key);

                // find the above ButlerService log entry
                String log = device.shellCommand("logcat",
                        "-t", timestamp,    // only entries from last 5 seconds
                        "-s",               // silence all tags (besides TAG)
                        "-e", "deviceKey=", // only print lines containing "deviceKey="
                        TAG).get().trim();

                if (log.contains(key)) {
                    Log.d(TAG, "Found current device with id: " + device.deviceId);
                    return device;
                }
            }
            Log.w(TAG, "Could not find current device in 'adb devices', assuming it is the only device attached...");
        } catch (Exception e) {
            Log.w(TAG, "Could not find current device in 'adb devices', assuming it is the only device attached...", e);
        }
        return new AdbDevice(host, port, null);
    }

    /**
     * Lists the currently connected devices (equivalent to 'adb devices').
     * @param host The ADB server hostname
     * @param port The ADB server port
     * @return A list of AdbDevice instances. Each AdbDevice has an explicit DeviceId.
     */
    private static List<AdbDevice> getDevices(String host, int port) throws InterruptedException, ExecutionException {
        String result = send(host, port, AdbCommand.getDevices()).get();
        ArrayList<AdbDevice> devices = new ArrayList<>();
        for (String line : result.trim().split("\n")) {
            String[] parts = line.split("\t");
            if (parts.length > 1) {
                devices.add(new AdbDevice(host, port, parts[0].trim()));
            }
        }
        return devices;
    }

    @NonNull
    private static AdbCommandTask send(@NonNull String host, int port, AdbCommand command) {
        AdbCommandTask task = new AdbCommandTask(host, port, command);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return task;
    }

    /**
     * Runs a shell command on this device (i.e. {@code adb shell <command> <args>}).
     * @param command Command to run
     * @param args Arguments to the command
     * @return An AsyncTask representing the shell command. Use {@link AsyncTask#get()} to wait for
     * the command to finish. The result is the output of the shell command.
     */
    @NonNull
    AdbCommandTask shellCommand(@NonNull String command, @NonNull String... args) {
        return send(host, port, AdbCommand.shell(deviceId, command, args));
    }

    static class AdbCommandTask extends AsyncTask<Void, Void, String> {
        private final String host;
        private final int port;
        private final AdbCommand command;
        private final Socket socket = new Socket();

        AdbCommandTask(@NonNull String host, int port, AdbCommand command) {
            this.host = host;
            this.port = port;
            this.command = command;
        }

        @Override
        protected String doInBackground(Void... ignored) {
            Log.d(TAG, "Executing command: " + command);
            try (Socket socket = this.socket) {
                socket.connect(new InetSocketAddress(host, port));
                AdbConnection connection = new AdbConnection(socket);
                String response = command.execute(connection);
                Log.d(TAG, "Command '" + command + "' returned " + response);
                return response;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void closeSocket() throws IOException {
            // note: we cannot use a standard task cancel w/ thread interrupt, as socket IO is
            // non-interruptible. Closing the socket works -- it unblocks the task thread (will
            // return exception), and the adb server will kill any corresponding device process.
            socket.close();
        }
    }
}
