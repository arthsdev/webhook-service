package com.artheus.webhookservice.subscription;

import com.artheus.webhookservice.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class SubscriptionNotFoundException extends ApplicationException {
    public SubscriptionNotFoundException(UUID publicId) {
        super("Subscription not found: " + publicId, HttpStatus.NOT_FOUND);
    }
}
