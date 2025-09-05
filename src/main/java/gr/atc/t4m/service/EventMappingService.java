package gr.atc.t4m.service;

import gr.atc.t4m.dto.EventMappingDto;
import gr.atc.t4m.dto.operations.EventMappingCreationDto;
import gr.atc.t4m.model.EventMapping;
import gr.atc.t4m.repository.EventMappingRepository;
import gr.atc.t4m.service.interfaces.IEventMappingService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import static gr.atc.t4m.exception.CustomExceptions.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class EventMappingService implements IEventMappingService {

    private final EventMappingRepository eventMappingRepository;

    private final ModelMapper modelMapper;

    private static final String MAPPING_EXCEPTION = "Unable to convert Event Mapping model to DTO or vice versa";

    private static final String GLOBAL_ROLES = "ALL";

    public EventMappingService(EventMappingRepository eventMappingRepository, ModelMapper modelMapper) {
        this.eventMappingRepository = eventMappingRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Create a new Event Mapping
     *
     * @param eventMapping : EventMappingDto
     * @throws MappingException : if mapping fails
     */
    @Override
    public void storeEventMapping(EventMappingCreationDto eventMapping) {
        try {
            Optional<EventMapping> existingEventMapping = eventMappingRepository.findByTopic(eventMapping.topic());
            if (existingEventMapping.isPresent())
                throw new ResourceAlreadyExists("Event Mapping with topic " + eventMapping.topic() + " already exists");

            eventMappingRepository.save(modelMapper.map(eventMapping, EventMapping.class));
        } catch (MappingException e){
            throw new ModelMappingException(MAPPING_EXCEPTION);
        }
    }

    /**
     * Return a list of all Event Mappings
     *
     * @throws MappingException : if mapping fails
     * @return List<EventMappingDto>
     */
    @Override
    public List<EventMappingDto> retrieveAllEventMappings() {
        try{
            return eventMappingRepository.findAll().stream().map(mapping -> modelMapper.map(mapping, EventMappingDto.class)).toList();
        } catch (MappingException e){
            throw new ModelMappingException(MAPPING_EXCEPTION);
        }
    }

    /**
     * Return an Event Mapping by its topic name
     *
     * @param topic : Topic Name
     * @return EventMappingDto
     */
    @Override
    public EventMappingDto retrieveEventMappingByTopic(String topic) {
        try {
            Optional<EventMapping> eventMapping = eventMappingRepository.findByTopic(topic);
            if (eventMapping.isEmpty())
                throw new ResourceNotFoundException("Event Mapping with topic " + topic + " not found");

            return modelMapper.map(eventMapping,EventMappingDto.class);
        } catch (MappingException e){
            throw new ModelMappingException(MAPPING_EXCEPTION);
        }
    }

    /**
     * Delete an Event Mapping by its id
     *
     * @param mappingId : Mapping ID
     * @throws ResourceNotFoundException : if mapping not found
     */
    @Override
    public void deleteEventMappingById(Long mappingId) {
        if(!eventMappingRepository.existsById(mappingId)){
            throw new ResourceNotFoundException("Event Mapping with id " + mappingId + " not found");
        }
        eventMappingRepository.deleteById(mappingId);
    }

    /**
     * Update an Event Mapping by its id
     *
     * @param eventMapping : Event Mapping DTO updated data with ID
     * @throws ResourceNotFoundException : if mapping not found
     */
    @Override
    public void updateEventMappingById(EventMappingDto eventMapping) {
        Optional<EventMapping> existingEventMapping = eventMappingRepository.findById(eventMapping.getId());
        if(existingEventMapping.isEmpty()){
            throw new ResourceNotFoundException("Event Mapping with id " + eventMapping.getId() + " not found");
        }

        EventMapping eventMappingToUpdate = existingEventMapping.get();

        // Update Fields (Topic can not be updated)
        if (eventMapping.getDescription() != null)
            eventMappingToUpdate.setDescription(eventMapping.getDescription());

        if (eventMapping.getUserRoles() != null)
            eventMappingToUpdate.setUserRoles(eventMapping.getUserRoles());

        eventMappingRepository.save(eventMappingToUpdate);
    }

    @Override
    @Async
    public void createDefaultNotificationMappingAsync(String topic) {
        try {
            // Create the Event Mapping
            EventMappingCreationDto newEventMapping = EventMappingCreationDto.builder()
                    .description("Event mapping for topic '" + topic + "'")
                    .topic(topic)
                    .userRoles(Set.of(GLOBAL_ROLES))
                    .build();

            // Store the Event Mapping
            storeEventMapping(newEventMapping);
            log.info("Default mapping created for topic '{}'", topic);
        } catch (Exception e) {
            log.error("Error creating default mapping for topic {}: {}", topic, e.getMessage(), e);
        }
    }
}
