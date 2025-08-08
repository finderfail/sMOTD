import * as net from 'net';

const MOTD = '§aLegacy 1.12.2 Server\n§cNow wow double name';
const PORT = 25565;
const PROTOCOL_VERSION = 340;
const VERSION_NAME = '1.12.2';

const server = net.createServer((socket) => {
  const chunks: Buffer[] = [];

  socket.on('data', (data) => {
    chunks.push(data);

    try {
      const buffer = Buffer.concat(chunks);
      let offset = 0;

      // serialaize
      function readVarInt(): number {
        let num = 0;
        let shift = 0;
        let byte = 0;
        let bytes = 0;
        do {
          byte = buffer[offset++];
          num |= (byte & 0x7F) << shift;
          shift += 7;
          bytes++;
          if (bytes > 5) throw new Error('VarInt too big');
        } while (byte & 0x80);
        return num;
      }

      function writeVarInt(value: number): Buffer {
        const parts: number[] = [];
        while ((value & 0xFFFFFF80) !== 0) {
          parts.push((value & 0x7F) | 0x80);
          value >>>= 7;
        }
        parts.push(value);
        return Buffer.from(parts);
      }

      function readString(): string {
        const length = readVarInt();
        const strBuf = buffer.slice(offset, offset + length);
        offset += length;
        return strBuf.toString('utf-8');
      }

      function writeString(str: string): Buffer {
        const strBuf = Buffer.from(str, 'utf-8');
        return Buffer.concat([writeVarInt(strBuf.length), strBuf]);
      }

      // handshake
      const packetLength = readVarInt();
      const packetId = readVarInt();

      if (packetId !== 0) return; // not handshake

      const protocol = readVarInt();
      const address = readString();
      const port = buffer.readUInt16BE(offset);
      offset += 2;
      const nextState = readVarInt();

      if (nextState === 1) {
        // === Status request ===
        const json = JSON.stringify({
          version: {
            name: VERSION_NAME,
            protocol: PROTOCOL_VERSION,
          },
          players: {
            max: 1000,
            online: 999,
            sample: [],
          },
          description: MOTD,
        });

        const statusPayload = writeString(json);
        const statusPacket = Buffer.concat([
          writeVarInt(0), // packet ID
          statusPayload,
        ]);
        const fullPacket = Buffer.concat([
          writeVarInt(statusPacket.length),
          statusPacket,
        ]);
        socket.write(fullPacket);

        // ping
        const pingLength = readVarInt();
        const pingId = buffer[offset++]; // usually 1
        const payload = buffer.readBigInt64BE(offset);
        offset += 8;

        const pong = Buffer.alloc(9);
        pong.writeUInt8(0x01, 0); // pong packet ID
        pong.writeBigInt64BE(payload, 1);

        socket.write(Buffer.concat([
          writeVarInt(pong.length),
          pong,
        ]));

        socket.end();
      }
    } catch (err) {
      console.log('Error:', (err as Error).message);
    }
  });
});

server.listen(PORT, () => {
  console.log(`Ghost server started on port ${PORT}`);
  console.log(`With MOTD: ${MOTD}`);
  console.log(`With protocol: ${PROTOCOL_VERSION}`);
  console.log(`Version: ${VERSION_NAME}`);
});
