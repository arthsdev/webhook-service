package com.artheus.webhookservice.shared.contract.subscription;

import java.util.UUID;

public record SubscriberInfo(UUID publicId, String targetUrl) {}
