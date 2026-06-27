package com.livecomerce.live.infrastructure.agora;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.TreeMap;

/**
 * Vendored from https://github.com/AgoraIO/Tools/blob/master/DynamicKey/AgoraDynamicKey/java
 * Renamed to AgoraByteBuf to avoid package conflicts. Package changed to livecomerce.
 */
class AgoraByteBuf {

    ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

    AgoraByteBuf() {}

    AgoraByteBuf(byte[] bytes) {
        this.buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    byte[] asBytes() {
        byte[] out = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(out, 0, out.length);
        return out;
    }

    AgoraByteBuf put(short v) {
        buffer.putShort(v);
        return this;
    }

    AgoraByteBuf put(byte[] v) {
        put((short) v.length);
        buffer.put(v);
        return this;
    }

    AgoraByteBuf put(int v) {
        buffer.putInt(v);
        return this;
    }

    AgoraByteBuf put(String v) {
        return put(v.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    AgoraByteBuf putIntMap(TreeMap<Short, Integer> extra) {
        put((short) extra.size());
        for (Map.Entry<Short, Integer> pair : extra.entrySet()) {
            put(pair.getKey());
            put(pair.getValue());
        }
        return this;
    }

    short readShort() {
        return buffer.getShort();
    }

    int readInt() {
        return buffer.getInt();
    }

    byte[] readBytes() {
        short length = readShort();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    String readString() {
        return new String(readBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    TreeMap<Short, Integer> readIntMap() {
        TreeMap<Short, Integer> map = new TreeMap<>();
        short length = readShort();
        for (short i = 0; i < length; ++i) {
            short k = readShort();
            int v = readInt();
            map.put(k, v);
        }
        return map;
    }
}
