package com.artheus.webhookservice.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByStatus(EventStatus status);

    Optional<Event> findByPublicId(UUID publicId);

}
