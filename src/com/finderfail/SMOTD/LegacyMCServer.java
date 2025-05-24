package com.finderfail.SMOTD;

import java.io.*;
import java.net.*;

import static com.mojang.network.Serialize.*;

public class LegacyMCServer {
    private static final String MOTD = "§aLegacy 1.12.2 Server\n§cNow wow double name";
    private static final int PORT = 25565;
    private static final int PROTOCOL_VERSION = 340;
    private static final String VERSION_NAME = "1.12.2";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Ghost server started " + PORT);
        System.out.println("With MOTD: " + MOTD + "\nWith protocol: " + PROTOCOL_VERSION + "\nVersion: " + VERSION_NAME + "\nEngine version: " + System.getProperty("java.runtime.version"));

        while (true) {
            try (Socket client = serverSocket.accept();
                 DataInputStream in = new DataInputStream(client.getInputStream());
                 DataOutputStream out = new DataOutputStream(client.getOutputStream())) {

                // Read handshake
                int packetLength = readVarInt(in);
                byte[] packetData = new byte[packetLength];
                in.readFully(packetData);
                DataInputStream packetIn = new DataInputStream(new ByteArrayInputStream(packetData));
                if (readVarInt(packetIn) != 0) return; // Check packet ID

                // Parse handshake
                int protocol = readVarInt(packetIn);
                String address = readString(packetIn);
                packetIn.readShort();
                int nextState = readVarInt(packetIn);

                if (nextState == 1) { // Status request
                    // Send status response
                    String json = String.format(
                            "{\"version\":{\"name\":\"%s\",\"protocol\":%d},\"players\":{\"max\":1000,\"online\":999,\"sample\":[]},\"description\":\"%s\"}",
                            VERSION_NAME, PROTOCOL_VERSION, MOTD
                    );

                    ByteArrayOutputStream response = new ByteArrayOutputStream();
                    DataOutputStream resOut = new DataOutputStream(response);
                    writeString(resOut, json);

                    ByteArrayOutputStream packet = new ByteArrayOutputStream();
                    DataOutputStream packetOut = new DataOutputStream(packet);
                    writeVarInt(packetOut, 0);
                    packetOut.write(response.toByteArray());

                    writeVarInt(out, packet.size());
                    out.write(packet.toByteArray());

                    // Handle ping
                    readVarInt(in);
                    in.readByte();

                    ByteArrayOutputStream pong = new ByteArrayOutputStream();
                    DataOutputStream pongOut = new DataOutputStream(pong);
                    pongOut.writeByte(0x01); // Pong
                    pongOut.writeLong(in.readLong());

                    writeVarInt(out, pong.size());
                    out.write(pong.toByteArray());
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

}