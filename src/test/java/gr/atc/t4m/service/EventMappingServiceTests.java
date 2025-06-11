package gr.atc.t4m.service;

import gr.atc.t4m.dto.operations.EventMappingCreationDto;
import gr.atc.t4m.dto.EventMappingDto;
import gr.atc.t4m.model.EventMapping;
import gr.atc.t4m.repository.EventMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.ErrorMessage;

import java.util.*;

import static gr.atc.t4m.exception.CustomExceptions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventMappingServiceTests {

    @Mock
    private EventMappingRepository eventMappingRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private EventMappingService eventMappingService;

    private EventMapping testEventMapping;
    private EventMappingDto testEventMappingDto;
    private EventMappingCreationDto testEventMappingCreationDto;
    private final Long TEST_MAPPING_ID = 1L;
    private final String TEST_DESCRIPTION = "Test Description";
    private final Set<String> TEST_USER_ROLES = Set.of("ADMIN", "USER");

    @BeforeEach
    void setUp() {
        String TEST_TOPIC = "test-topic";
        // Setup EventMapping entity
        testEventMapping = new EventMapping();
        testEventMapping.setId(TEST_MAPPING_ID);
        testEventMapping.setTopic(TEST_TOPIC);
        testEventMapping.setDescription(TEST_DESCRIPTION);
        testEventMapping.setUserRoles(TEST_USER_ROLES);

        // Setup EventMappingDto
        testEventMappingDto = new EventMappingDto();
        testEventMappingDto.setId(TEST_MAPPING_ID);
        testEventMappingDto.setTopic(TEST_TOPIC);
        testEventMappingDto.setDescription(TEST_DESCRIPTION);
        testEventMappingDto.setUserRoles(TEST_USER_ROLES);

        // Setup EventMappingCreationDto
        testEventMappingCreationDto = new EventMappingCreationDto(TEST_TOPIC, TEST_DESCRIPTION, TEST_USER_ROLES);
    }

    // =========================== Store Event Mapping Tests ===========================
    @DisplayName("Store Event Mapping : Success")
    @Test
    void storeEventMapping_Success() {
        // Given
        when(modelMapper.map(testEventMappingCreationDto, EventMapping.class))
                .thenReturn(testEventMapping);
        when(eventMappingRepository.save(testEventMapping))
                .thenReturn(testEventMapping);

        // When
        eventMappingService.storeEventMapping(testEventMappingCreationDto);

        // Then
        verify(modelMapper).map(testEventMappingCreationDto, EventMapping.class);
        verify(eventMappingRepository).save(testEventMapping);
    }

    @DisplayName("Store Event Mapping : Mapping Exception")
    @Test
    void storeEventMapping_MappingException_ThrowsModelMappingException() {
        // Given
        when(modelMapper.map(testEventMappingCreationDto, EventMapping.class))
                .thenThrow(new MappingException(List.of(new ErrorMessage("Mapping failed"))));

        // When & Then
        ModelMappingException exception = assertThrows(
                ModelMappingException.class,
                () -> eventMappingService.storeEventMapping(testEventMappingCreationDto)
        );

        assertEquals("Unable to convert Event Mapping model to DTO or vice versa", exception.getMessage());
        verify(modelMapper).map(testEventMappingCreationDto, EventMapping.class);
        verify(eventMappingRepository, never()).save(any());
    }

    @DisplayName("Store Event Mapping : Repository Exception")
    @Test
    void storeEventMapping_RepositoryException_PropagatesException() {
        // Given
        when(modelMapper.map(testEventMappingCreationDto, EventMapping.class))
                .thenReturn(testEventMapping);
        when(eventMappingRepository.save(testEventMapping))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventMappingService.storeEventMapping(testEventMappingCreationDto)
        );

        assertEquals("Database error", exception.getMessage());
        verify(modelMapper).map(testEventMappingCreationDto, EventMapping.class);
        verify(eventMappingRepository).save(testEventMapping);
    }

    // =========================== Retrieve all Mappings Tests ===========================
    @DisplayName("Retrieve All Event Mappings : Success")
    @Test
    void retrieveAllEventMappings_Success_SingleMapping() {
        // Given
        List<EventMapping> eventMappings = Collections.singletonList(testEventMapping);
        when(eventMappingRepository.findAll()).thenReturn(eventMappings);
        when(modelMapper.map(testEventMapping, EventMappingDto.class))
                .thenReturn(testEventMappingDto);

        // When
        List<EventMappingDto> result = eventMappingService.retrieveAllEventMappings();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testEventMappingDto, result.getFirst());
        verify(eventMappingRepository).findAll();
        verify(modelMapper).map(testEventMapping, EventMappingDto.class);
    }

    @DisplayName("Retrieve All Event Mappings : Success")
    @Test
    void retrieveAllEventMappings_Success_MultipleMappings() {
        // Given
        EventMapping secondEventMapping = new EventMapping();
        secondEventMapping.setId(2L);
        secondEventMapping.setTopic("second-topic");
        secondEventMapping.setDescription("Second Description");

        EventMappingDto secondEventMappingDto = new EventMappingDto();
        secondEventMappingDto.setId(2L);
        secondEventMappingDto.setTopic("second-topic");
        secondEventMappingDto.setDescription("Second Description");

        List<EventMapping> eventMappings = Arrays.asList(testEventMapping, secondEventMapping);
        when(eventMappingRepository.findAll()).thenReturn(eventMappings);
        when(modelMapper.map(testEventMapping, EventMappingDto.class))
                .thenReturn(testEventMappingDto);
        when(modelMapper.map(secondEventMapping, EventMappingDto.class))
                .thenReturn(secondEventMappingDto);

        // When
        List<EventMappingDto> result = eventMappingService.retrieveAllEventMappings();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testEventMappingDto, result.get(0));
        assertEquals(secondEventMappingDto, result.get(1));
        verify(eventMappingRepository).findAll();
        verify(modelMapper, times(2)).map(any(EventMapping.class), eq(EventMappingDto.class));
    }

    @DisplayName("Retrieve All Event Mappings : Mapping Exception")
    @Test
    void retrieveAllEventMappings_MappingException_ThrowsModelMappingException() {
        // Given
        List<EventMapping> eventMappings = Collections.singletonList(testEventMapping);
        when(eventMappingRepository.findAll()).thenReturn(eventMappings);
        when(modelMapper.map(testEventMapping, EventMappingDto.class))
                .thenThrow(new MappingException(List.of(new ErrorMessage("Mapping failed"))));

        // When & Then
        ModelMappingException exception = assertThrows(
                ModelMappingException.class,
                () -> eventMappingService.retrieveAllEventMappings()
        );

        assertEquals("Unable to convert Event Mapping model to DTO or vice versa", exception.getMessage());
        verify(eventMappingRepository).findAll();
        verify(modelMapper).map(testEventMapping, EventMappingDto.class);
    }

    // =========================== Delete Event Mapping Tests ===========================
    @DisplayName("Delete Event Mapping By Id : Success")
    @Test
    void deleteEventMappingById_Success() {
        // Given
        when(eventMappingRepository.existsById(TEST_MAPPING_ID)).thenReturn(true);

        // When
        eventMappingService.deleteEventMappingById(TEST_MAPPING_ID);

        // Then
        verify(eventMappingRepository).existsById(TEST_MAPPING_ID);
        verify(eventMappingRepository).deleteById(TEST_MAPPING_ID);
    }

    @DisplayName("Delete Event Mapping By Id : Not Found")
    @Test
    void deleteEventMappingById_NotFound_ThrowsResourceNotFoundException() {
        // Given
        when(eventMappingRepository.existsById(TEST_MAPPING_ID)).thenReturn(false);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventMappingService.deleteEventMappingById(TEST_MAPPING_ID)
        );

        assertEquals("Event Mapping with id " + TEST_MAPPING_ID + " not found", exception.getMessage());
        verify(eventMappingRepository).existsById(TEST_MAPPING_ID);
        verify(eventMappingRepository, never()).deleteById(any());
    }

    @DisplayName("Delete Event Mapping By Id : Repository Exception")
    @Test
    void deleteEventMappingById_RepositoryException_PropagatesException() {
        // Given
        when(eventMappingRepository.existsById(TEST_MAPPING_ID)).thenReturn(true);
        doThrow(new RuntimeException("Database error"))
                .when(eventMappingRepository).deleteById(TEST_MAPPING_ID);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventMappingService.deleteEventMappingById(TEST_MAPPING_ID)
        );

        assertEquals("Database error", exception.getMessage());
        verify(eventMappingRepository).existsById(TEST_MAPPING_ID);
        verify(eventMappingRepository).deleteById(TEST_MAPPING_ID);
    }

    // =========================== Update Event Mapping By ID Tests ===========================
    @DisplayName("Update Event Mapping By Id : Success")
    @Test
    void updateEventMappingById_Success_UpdateAllFields() {
        // Given
        String newDescription = "Updated Description";
        Set<String> newUserRoles = Set.of("SUPER_ADMIN", "MANAGER");

        testEventMappingDto.setDescription(newDescription);
        testEventMappingDto.setUserRoles(newUserRoles);

        when(eventMappingRepository.findById(TEST_MAPPING_ID))
                .thenReturn(Optional.of(testEventMapping));
        when(eventMappingRepository.save(any(EventMapping.class)))
                .thenReturn(testEventMapping);

        // When
        eventMappingService.updateEventMappingById(testEventMappingDto);

        // Then
        verify(eventMappingRepository).findById(TEST_MAPPING_ID);
        verify(eventMappingRepository).save(testEventMapping);
        assertEquals(newDescription, testEventMapping.getDescription());
        assertEquals(newUserRoles, testEventMapping.getUserRoles());
    }

    @DisplayName("Update Event Mapping By Id : Success / Update Description only")
    @Test
    void updateEventMappingById_Success_UpdateDescriptionOnly() {
        // Given
        String newDescription = "Updated Description";
        testEventMappingDto.setDescription(newDescription);
        testEventMappingDto.setUserRoles(null);

        when(eventMappingRepository.findById(TEST_MAPPING_ID))
                .thenReturn(Optional.of(testEventMapping));
        when(eventMappingRepository.save(any(EventMapping.class)))
                .thenReturn(testEventMapping);

        // When
        eventMappingService.updateEventMappingById(testEventMappingDto);

        // Then
        verify(eventMappingRepository).findById(TEST_MAPPING_ID);
        verify(eventMappingRepository).save(testEventMapping);
        assertEquals(newDescription, testEventMapping.getDescription());
        assertEquals(TEST_USER_ROLES, testEventMapping.getUserRoles());
    }

    @DisplayName("Update Event Mapping By Id : Success / Update User Roles only")
    @Test
    void updateEventMappingById_Success_UpdateUserRolesOnly() {
        // Given
        Set<String> newUserRoles = Set.of("SUPER_ADMIN", "MANAGER");
        testEventMappingDto.setDescription(null);
        testEventMappingDto.setUserRoles(newUserRoles);

        when(eventMappingRepository.findById(TEST_MAPPING_ID))
                .thenReturn(Optional.of(testEventMapping));
        when(eventMappingRepository.save(any(EventMapping.class)))
                .thenReturn(testEventMapping);

        // When
        eventMappingService.updateEventMappingById(testEventMappingDto);

        // Then
        verify(eventMappingRepository).findById(TEST_MAPPING_ID);
        verify(eventMappingRepository).save(testEventMapping);
        assertEquals(TEST_DESCRIPTION, testEventMapping.getDescription());
        assertEquals(newUserRoles, testEventMapping.getUserRoles());
    }
    
    @DisplayName("Update Event Mapping By Id : Success / No Fields to Update")
    @Test
    void updateEventMappingById_Success_NoFieldsToUpdate() {
        // Given
        testEventMappingDto.setDescription(null);
        testEventMappingDto.setUserRoles(null);

        when(eventMappingRepository.findById(TEST_MAPPING_ID))
                .thenReturn(Optional.of(testEventMapping));
        when(eventMappingRepository.save(any(EventMapping.class)))
                .thenReturn(testEventMapping);

        // When
        eventMappingService.updateEventMappingById(testEventMappingDto);

        // Then
        verify(eventMappingRepository).findById(TEST_MAPPING_ID);
        verify(eventMappingRepository).save(testEventMapping);
        assertEquals(TEST_DESCRIPTION, testEventMapping.getDescription());
        assertEquals(TEST_USER_ROLES, testEventMapping.getUserRoles());
    }

    @DisplayName("Update Event Mapping By Id : Not Found")
    @Test
    void updateEventMappingById_NotFound_ThrowsResourceNotFoundException() {
        // Given
        when(eventMappingRepository.findById(TEST_MAPPING_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventMappingService.updateEventMappingById(testEventMappingDto)
        );

        assertEquals("Event Mapping with id " + TEST_MAPPING_ID + " not found", exception.getMessage());
        verify(eventMappingRepository).findById(TEST_MAPPING_ID);
        verify(eventMappingRepository, never()).save(any());
    }

    @DisplayName("Update Event Mapping By Id : Repository Exception")
    @Test
    void updateEventMappingById_RepositoryException_PropagatesException() {
        // Given
        when(eventMappingRepository.findById(TEST_MAPPING_ID))
                .thenReturn(Optional.of(testEventMapping));
        when(eventMappingRepository.save(any(EventMapping.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventMappingService.updateEventMappingById(testEventMappingDto)
        );

        assertEquals("Database error", exception.getMessage());
        verify(eventMappingRepository).findById(TEST_MAPPING_ID);
        verify(eventMappingRepository).save(testEventMapping);
    }

    // =========================== Retrieve Event Mapping By Topic Tests ===========================
    @DisplayName("Retrieve Event Mapping By Topic : Success")
    @Test
    void givenTopicName_whenRetrieveEventMappingByTopic_thenSuccess() {
        // Given
        String topic = "test-topic";
        Optional<EventMapping> eventMappingOptional = Optional.of(testEventMapping);
        when(eventMappingRepository.findByTopic(topic))
                .thenReturn(eventMappingOptional);
        when(modelMapper.map(eventMappingOptional, EventMappingDto.class))
                .thenReturn(testEventMappingDto);

        // When
        EventMappingDto result = eventMappingService.retrieveEventMappingByTopic(topic);

        // Then
        assertNotNull(result);
        assertEquals(testEventMappingDto, result);
        verify(eventMappingRepository).findByTopic(topic);
        verify(modelMapper).map(eventMappingOptional, EventMappingDto.class);
    }

    @DisplayName("Retrieve Event Mapping By Topic : Not Found")
    @Test
    void givenTopicName_whenRetrieveEventMappingByTopic_thenThrowsResourceNotFoundException() {
        // Given
        String topic = "non-existent-topic";
        when(eventMappingRepository.findByTopic(topic))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventMappingService.retrieveEventMappingByTopic(topic)
        );

        assertEquals("Event Mapping with topic " + topic + " not found", exception.getMessage());
        verify(eventMappingRepository).findByTopic(topic);
        verify(modelMapper, never()).map(any(), eq(EventMappingDto.class));
    }

    @DisplayName("Retrieve Event Mapping By Topic : Mapping Exception")
    @Test
    void givenTopicName_whenRetrieveEventMappingByTopic_thenMappingException() {
        // Given
        String topic = "test-topic";
        Optional<EventMapping> eventMappingOptional = Optional.of(testEventMapping);
        when(eventMappingRepository.findByTopic(topic))
                .thenReturn(eventMappingOptional);
        when(modelMapper.map(eventMappingOptional, EventMappingDto.class))
                .thenThrow(new MappingException(List.of(new ErrorMessage("Mapping failed"))));

        // When & Then
        ModelMappingException exception = assertThrows(
                ModelMappingException.class,
                () -> eventMappingService.retrieveEventMappingByTopic(topic)
        );

        assertEquals("Unable to convert Event Mapping model to DTO or vice versa", exception.getMessage());
        verify(eventMappingRepository).findByTopic(topic);
        verify(modelMapper).map(eventMappingOptional, EventMappingDto.class);
    }

    // =========================== Create Default Notification Mapping Async Tests ===========================
    @DisplayName("Create Default Notification Mapping Async : Success")
    @Test
    void whenCreateDefaultNotificationMappingAsync_thenSuccess() {
        // Given
        String topic = "test-topic";
        EventMappingCreationDto expectedEventMapping = EventMappingCreationDto.builder()
                .description("Event mapping for topic '" + topic + "'")
                .topic(topic)
                .userRoles(Set.of("ALL"))
                .build();

        when(modelMapper.map(any(EventMappingCreationDto.class), eq(EventMapping.class)))
                .thenReturn(testEventMapping);
        when(eventMappingRepository.save(testEventMapping))
                .thenReturn(testEventMapping);

        // When
        eventMappingService.createDefaultNotificationMappingAsync(topic);

        // Then
        ArgumentCaptor<EventMappingCreationDto> captor = ArgumentCaptor.forClass(EventMappingCreationDto.class);
        verify(modelMapper).map(captor.capture(), eq(EventMapping.class));
        verify(eventMappingRepository).save(testEventMapping);

        EventMappingCreationDto capturedDto = captor.getValue();
        assertEquals(expectedEventMapping.description(), capturedDto.description());
        assertEquals(expectedEventMapping.topic(), capturedDto.topic());
        assertEquals(expectedEventMapping.userRoles(), capturedDto.userRoles());
    }

    @DisplayName("Create Default Notification Mapping Async : Exception Handled")
    @Test
    void whenCreateDefaultNotificationMappingAsync_thenExceptionHandled() {
        // Given
        String topic = "test-topic";
        when(modelMapper.map(any(EventMappingCreationDto.class), eq(EventMapping.class)))
                .thenThrow(new RuntimeException("Mapping error"));

        // When
        eventMappingService.createDefaultNotificationMappingAsync(topic);

        // Then
        verify(modelMapper).map(any(EventMappingCreationDto.class), eq(EventMapping.class));
        verify(eventMappingRepository, never()).save(any());
    }

    @DisplayName("Create Default Notification Mapping Async : Repository Exception Handled")
    @Test
    void whenCreateDefaultNotificationMappingAsync_thenRepositoryExceptionHandled() {
        // Given
        String topic = "test-topic";
        when(modelMapper.map(any(EventMappingCreationDto.class), eq(EventMapping.class)))
                .thenReturn(testEventMapping);
        when(eventMappingRepository.save(testEventMapping))
                .thenThrow(new RuntimeException("Database error"));

        // When
        eventMappingService.createDefaultNotificationMappingAsync(topic);

        // Then
        verify(modelMapper).map(any(EventMappingCreationDto.class), eq(EventMapping.class));
        verify(eventMappingRepository).save(testEventMapping);
    }

    // =========================== Edge Case Tests ===========================
    @DisplayName("Delete Event Mapping By Id : Null Id")
    @Test
    void deleteEventMappingById_NullId_CallsRepository() {
        // Given
        when(eventMappingRepository.existsById(null)).thenReturn(false);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventMappingService.deleteEventMappingById(null)
        );

        assertEquals("Event Mapping with id null not found", exception.getMessage());
        verify(eventMappingRepository).existsById(null);
    }

    @DisplayName("Update Event Mapping By Id : Null Id")
    @Test
    void updateEventMappingById_NullId_ThrowsResourceNotFoundException() {
        // Given
        testEventMappingDto.setId(null);
        when(eventMappingRepository.findById(null))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventMappingService.updateEventMappingById(testEventMappingDto)
        );

        assertEquals("Event Mapping with id null not found", exception.getMessage());
        verify(eventMappingRepository).findById(null);
        verify(eventMappingRepository, never()).save(any());
    }

    @DisplayName("Update Event Mapping By Id : Empty String Values")
    @Test
    void updateEventMappingById_EmptyStringValues_UpdatesWithEmptyValues() {
        // Given
        testEventMappingDto.setDescription("");
        testEventMappingDto.setUserRoles(Set.of());

        when(eventMappingRepository.findById(TEST_MAPPING_ID))
                .thenReturn(Optional.of(testEventMapping));
        when(eventMappingRepository.save(any(EventMapping.class)))
                .thenReturn(testEventMapping);

        // When
        eventMappingService.updateEventMappingById(testEventMappingDto);

        // Then
        verify(eventMappingRepository).findById(TEST_MAPPING_ID);
        verify(eventMappingRepository).save(testEventMapping);
        assertEquals("", testEventMapping.getDescription());
        assertEquals(Set.of(), testEventMapping.getUserRoles());
    }

    @DisplayName("Retrieve All Event Mappings : Repository Exception")
    @Test
    void retrieveAllEventMappings_RepositoryException_PropagatesException() {
        // Given
        when(eventMappingRepository.findAll())
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventMappingService.retrieveAllEventMappings()
        );

        assertEquals("Database connection failed", exception.getMessage());
        verify(eventMappingRepository).findAll();
        verify(modelMapper, never()).map(any(), any());
    }
}
