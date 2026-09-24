package com.artheus.webhookservice.event;

public enum EventStatus {
    RECEIVED,
    DISPATCHED,
    PARTIALLY_DISPATCHED,
    DISPATCH_FAILED,
    NO_SUBSCRIBERS
}
