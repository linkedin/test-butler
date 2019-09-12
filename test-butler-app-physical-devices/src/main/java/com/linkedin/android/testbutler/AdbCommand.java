package com.linkedin.android.testbutler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;


/**
 * A command that may be sent to an ADB server.
 *
 * See <a href="https://android.googlesource.com/platform/system/core/+/master/adb/SERVICES.TXT">
 * https://android.googlesource.com/platform/system/core/+/master/adb/SERVICES.TXT</a>
 * for details on the available ADB commands.
 */
abstract class AdbCommand {

    @Nullable
    protected abstract String execute(@NonNull AdbConnection connection) throws IOException;

    @NonNull
    protected abstract String getCommand();

    @NonNull
    @Override
    public String toString() {
        return getCommand();
    }

    @NonNull
    static AdbDevicesCommand getDevices() {
        return new AdbDevicesCommand();
    }

    @NonNull
    static AdbShellCommand shell(@Nullable String deviceSerial, @NonNull String command,
                                 @NonNull String... args) {
        return new AdbShellCommand(
                new AdbTransportCommand(deviceSerial),
                concatShellCommand(command, args));
    }

    private static String concatShellCommand(String command, String... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(command);
        for (String arg : args) {
            sb.append(' ');
            // escape arg if necessary
            if (arg.matches("\\S+")) {
                sb.append(arg);
            } else {
                sb.append("'");
                sb.append(arg.replace("\'", "'\"'\"'"));
                sb.append("'");
            }
        }
        return sb.toString();
    }

    static class AdbDevicesCommand extends AdbCommand {

        @NonNull
        @Override
        protected String execute(@NonNull AdbConnection connection) throws IOException {
            connection.sendCommand(getCommand());
            return connection.readMsg();
        }

        @NonNull
        @Override
        protected String getCommand() {
            return "host:devices";
        }
    }

    static class AdbTransportCommand extends AdbCommand {

        private final String serial;

        private AdbTransportCommand(String serial) {
            this.serial = serial;
        }

        @Nullable
        @Override
        protected String execute(@NonNull AdbConnection connection) throws IOException {
            connection.sendCommand(getCommand());
            // doesn't have any response -- just sets up for another command.
            return null;
        }

        @NonNull
        @Override
        protected String getCommand() {
            return serial == null ? "host:transport-any" : "host:transport:" + serial;
        }
    }

    static class AdbShellCommand extends AdbCommand {
        private final AdbTransportCommand transportCommand;
        private final String shellString;

        private AdbShellCommand(AdbTransportCommand transportCommand, String shellString) {
            this.transportCommand = transportCommand;
            this.shellString = shellString;
        }

        @NonNull
        @Override
        protected String execute(@NonNull AdbConnection connection) throws IOException {
            // really a combination of two commands -- select transport then execute shell command
            transportCommand.execute(connection);
            connection.sendCommand(getCommand());
            return connection.readAll();
        }

        @NonNull
        @Override
        protected String getCommand() {
            return "shell:" + shellString;
        }

        @NonNull
        @Override
        public String toString() {
            return transportCommand + ";" + getCommand();
        }
    }
}