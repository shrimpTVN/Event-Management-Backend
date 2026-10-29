package com.ddd.application.service.event;

public interface StoredEventCommandService {
    void storeEvent(Long eventId, String email);
    void unstoreEvent(Long eventId, String email);
}
