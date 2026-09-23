package com.artheus.webhookservice.event;

import com.artheus.webhookservice.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class EventNotFoundException extends ApplicationException {
    public EventNotFoundException(UUID publicId) {
        super("Event not found: " + publicId, HttpStatus.NOT_FOUND);
    }
}
