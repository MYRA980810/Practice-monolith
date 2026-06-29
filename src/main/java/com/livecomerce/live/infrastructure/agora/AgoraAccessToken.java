package com.livecomerce.live.infrastructure.agora;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Vendored from https://github.com/AgoraIO/Tools/blob/master/DynamicKey/AgoraDynamicKey/java
 * Renamed to AgoraAccessToken. commons-codec dependency replaced with java.util.Base64.
 * Implements AccessToken2 / token007 format.
 */
class AgoraAccessToken {

    public enum PrivilegeRtc {
        PRIVILEGE_JOIN_CHANNEL(1),
        PRIVILEGE_PUBLISH_AUDIO_STREAM(2),
        PRIVILEGE_PUBLISH_VIDEO_STREAM(3),
        PRIVILEGE_PUBLISH_DATA_STREAM(4);

        public final short intValue;
        PrivilegeRtc(int value) { intValue = (short) value; }
    }

    public enum PrivilegeRtm {
        PRIVILEGE_LOGIN(1);

        public final short intValue;
        PrivilegeRtm(int value) { intValue = (short) value; }
    }

    private static final String VERSION = "007";
    public static final short SERVICE_TYPE_RTC = 1;
    public static final short SERVICE_TYPE_RTM = 2;

    String appCert = "";
    String appId = "";
    int expire;
    int issueTs;
    int salt;
    Map<Short, Service> services = new TreeMap<>();

    AgoraAccessToken() {}

    AgoraAccessToken(String appId, String appCert, int expire) {
        this.appCert  = appCert;
        this.appId    = appId;
        this.expire   = expire;
        this.issueTs  = (int) Instant.now().getEpochSecond();
        this.salt     = new SecureRandom().nextInt();
    }

    void addService(Service service) {
        this.services.put(service.getServiceType(), service);
    }

    String build() throws Exception {
        if (!isUUID(this.appId) || !isUUID(this.appCert)) {
            return "";
        }

        AgoraByteBuf buf = new AgoraByteBuf()
                .put(this.appId)
                .put(this.issueTs)
                .put(this.expire)
                .put(this.salt)
                .put((short) this.services.size());

        byte[] signing = getSign();
        this.services.forEach((k, v) -> v.pack(buf));

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signing, "HmacSHA256"));
        byte[] signature = mac.doFinal(buf.asBytes());

        AgoraByteBuf content = new AgoraByteBuf();
        content.put(signature);
        content.buffer.put(buf.asBytes());

        return VERSION + Base64.getEncoder().encodeToString(compress(content.asBytes()));
    }

    byte[] getSign() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(new AgoraByteBuf().put(this.issueTs).asBytes(), "HmacSHA256"));
        byte[] signing = mac.doFinal(this.appCert.getBytes());
        mac.init(new SecretKeySpec(new AgoraByteBuf().put(this.salt).asBytes(), "HmacSHA256"));
        return mac.doFinal(signing);
    }

    static String getUidStr(int uid) {
        if (uid == 0) return "";
        return String.valueOf(uid & 0xFFFFFFFFL);
    }

    private static boolean isUUID(String s) {
        return s != null && s.length() == 32 && s.matches("[0-9a-fA-F]+");
    }

    private static byte[] compress(byte[] data) {
        Deflater deflater = new Deflater();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try {
            deflater.setInput(data);
            deflater.finish();
            byte[] buf = new byte[data.length + 64];
            while (!deflater.finished()) {
                bos.write(buf, 0, deflater.deflate(buf));
            }
        } finally {
            deflater.end();
        }
        return bos.toByteArray();
    }

    static byte[] decompress(byte[] data) {
        Inflater inflater = new Inflater();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try {
            inflater.setInput(data);
            byte[] buf = new byte[8192];
            int len;
            while ((len = inflater.inflate(buf)) > 0) {
                bos.write(buf, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            inflater.end();
        }
        return bos.toByteArray();
    }

    // ── Service types ──────────────────────────────────────────────────────────

    static class Service {
        short type;
        TreeMap<Short, Integer> privileges = new TreeMap<>();

        Service() {}
        Service(short serviceType) { this.type = serviceType; }

        void addPrivilegeRtc(PrivilegeRtc privilege, int expire) {
            this.privileges.put(privilege.intValue, expire);
        }

        short getServiceType() { return this.type; }

        AgoraByteBuf pack(AgoraByteBuf buf) {
            return buf.put(this.type).putIntMap(this.privileges);
        }

        void unpack(AgoraByteBuf buf) {
            this.privileges = buf.readIntMap();
        }
    }

    static class ServiceRtc extends Service {
        String channelName;
        String uid;

        ServiceRtc() { this.type = SERVICE_TYPE_RTC; }

        ServiceRtc(String channelName, String uid) {
            this.type        = SERVICE_TYPE_RTC;
            this.channelName = channelName;
            this.uid         = uid;
        }

        @Override
        AgoraByteBuf pack(AgoraByteBuf buf) {
            return super.pack(buf).put(this.channelName).put(this.uid);
        }

        @Override
        void unpack(AgoraByteBuf buf) {
            super.unpack(buf);
            this.channelName = buf.readString();
            this.uid         = buf.readString();
        }
    }

    static class ServiceRtm extends Service {
        String userId;

        ServiceRtm() { this.type = SERVICE_TYPE_RTM; }

        ServiceRtm(String userId) {
            this.type   = SERVICE_TYPE_RTM;
            this.userId = userId;
        }

        void addPrivilege(PrivilegeRtm privilege, int expire) {
            this.privileges.put(privilege.intValue, expire);
        }

        @Override
        AgoraByteBuf pack(AgoraByteBuf buf) {
            return super.pack(buf).put(this.userId);
        }

        @Override
        void unpack(AgoraByteBuf buf) {
            super.unpack(buf);
            this.userId = buf.readString();
        }
    }
}
