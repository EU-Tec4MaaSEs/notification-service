package gr.atc.t4m.service.interfaces;

import gr.atc.t4m.dto.EventMappingDto;
import gr.atc.t4m.dto.operations.EventMappingCreationDto;
import jakarta.validation.Valid;

import java.util.List;

public interface IEventMappingService {
    void storeEventMapping(EventMappingCreationDto eventMapping);

    List<EventMappingDto> retrieveAllEventMappings();

    EventMappingDto retrieveEventMappingByTopic(String topic);

    void deleteEventMappingById(Long mappingId);

    void updateEventMappingById(@Valid EventMappingDto eventMapping);

    void createDefaultNotificationMappingAsync(String topic);
}
