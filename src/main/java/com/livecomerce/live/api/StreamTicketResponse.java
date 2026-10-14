package com.livecomerce.live.api;

/** Single-use ticket for {@code X-Stream-Ticket}, valid for {@code expiresInMs} from issuance. */
public record StreamTicketResponse(String ticket, long expiresInMs) {}
