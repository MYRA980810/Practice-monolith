package com.livecomerce.live.infrastructure.agora;

/**
 * Builds Agora RTM tokens using the official AccessToken2 / token007 format.
 * Source: https://github.com/AgoraIO/Tools/tree/master/DynamicKey/AgoraDynamicKey/java
 * Vendored because io.agora:agora-token is not available on Maven Central.
 *
 * appId and appCertificate must be 32-char hex strings (Agora Dashboard format, no dashes).
 */
public class AgoraRtmTokenBuilder {

    private AgoraRtmTokenBuilder() {}

    public static String buildTokenWithUserAccount(String appId, String appCertificate,
                                                   String userId, int expireSeconds) {
        AgoraAccessToken accessToken = new AgoraAccessToken(appId, appCertificate, expireSeconds);
        AgoraAccessToken.ServiceRtm service = new AgoraAccessToken.ServiceRtm(userId);
        service.addPrivilege(AgoraAccessToken.PrivilegeRtm.PRIVILEGE_LOGIN, expireSeconds);
        accessToken.addService(service);
        try {
            String token = accessToken.build();
            if (token.isEmpty()) {
                throw new IllegalArgumentException(
                    "Agora RTM token generation failed — appId and appCertificate must be 32-char hex strings");
            }
            return token;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Agora RTM token", e);
        }
    }
}
