package com.artheus.webhookservice.event;

import com.artheus.webhookservice.shared.contract.subscription.SubscriberInfo;
import com.artheus.webhookservice.shared.contract.subscription.SubscriptionLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final SubscriptionLookup subscriptionLookup;
    private final EventRepository eventRepository;
    private final Clock clock;

    @Transactional
    public EventResponse createEvent(EventRequest eventRequest) {
        Event event = Event.create(eventRequest.eventType(), eventRequest.payload(), clock);

        List<SubscriberInfo> subscribers = subscriptionLookup.findActiveByEventType(eventRequest.eventType());

        if (subscribers.isEmpty()) {
            event.markNoSubscribers();
        }
        // TODO: call delivery-service once it exists.
        // On success -> event.dispatch(); on failure -> event.failDispatch();
        // For now, events with subscribers stay RECEIVED.

        eventRepository.save(event);

        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public EventResponse findByPublicId(UUID publicId) {
        Event event = eventRepository.findByPublicId(publicId)
                .orElseThrow(()-> new EventNotFoundException(publicId));

        return EventResponse.from(event);
    }
}
