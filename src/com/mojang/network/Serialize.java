package com.mojang.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Serialize {

    public static int readVarInt(DataInputStream in) throws IOException {
        int value = 0, bytes = 0;
        byte b;
        do {
            b = in.readByte();
            value |= (b & 0x7F) << (bytes++ * 7);
            if (bytes > 5) throw new IOException("VarInt too big");
        } while ((b & 0x80) != 0);
        return value;
    }

    public static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            out.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    public static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] data = new byte[length];
        in.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, data.length);
        out.write(data);
    }
}
