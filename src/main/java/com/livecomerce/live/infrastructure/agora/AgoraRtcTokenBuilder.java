package com.livecomerce.live.infrastructure.agora;

/**
 * Builds Agora RTC tokens using the official AccessToken2 / token007 format.
 * Source: https://github.com/AgoraIO/Tools/tree/master/DynamicKey/AgoraDynamicKey/java
 * Vendored because io.agora:agora-token is not available on Maven Central.
 *
 * appId and appCertificate must be 32-char hex strings (Agora Dashboard format, no dashes).
 */
public class AgoraRtcTokenBuilder {

    private AgoraRtcTokenBuilder() {}

    public static String buildToken(String appId, String appCertificate,
                                    String channelName, String uid, int expireSeconds) {
        AgoraAccessToken accessToken = new AgoraAccessToken(appId, appCertificate, expireSeconds);
        AgoraAccessToken.Service service = new AgoraAccessToken.ServiceRtc(channelName, uid);
        service.addPrivilegeRtc(AgoraAccessToken.PrivilegeRtc.PRIVILEGE_JOIN_CHANNEL, expireSeconds);
        service.addPrivilegeRtc(AgoraAccessToken.PrivilegeRtc.PRIVILEGE_PUBLISH_AUDIO_STREAM, expireSeconds);
        service.addPrivilegeRtc(AgoraAccessToken.PrivilegeRtc.PRIVILEGE_PUBLISH_VIDEO_STREAM, expireSeconds);
        service.addPrivilegeRtc(AgoraAccessToken.PrivilegeRtc.PRIVILEGE_PUBLISH_DATA_STREAM, expireSeconds);
        accessToken.addService(service);
        try {
            String token = accessToken.build();
            if (token.isEmpty()) {
                throw new IllegalArgumentException(
                    "Agora token generation failed — appId and appCertificate must be 32-char hex strings");
            }
            return token;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Agora RTC token", e);
        }
    }
}
