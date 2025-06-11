package gr.atc.t4m.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.t4m.dto.EventMappingDto;
import gr.atc.t4m.dto.operations.EventMappingCreationDto;
import gr.atc.t4m.exception.CustomExceptions;
import gr.atc.t4m.service.interfaces.IEventMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventMappingController.class)
@DisplayName("Event Mappings Controller Tests")
@EnableMethodSecurity
class EventMappingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEventMappingService eventMappingService;

    @Autowired
    private ObjectMapper objectMapper;

    private EventMappingDto eventMappingDto;
    private EventMappingCreationDto eventMappingCreationDto;
    private List<EventMappingDto> sampleEventMappings;

    @BeforeEach
    void setUp() {
        eventMappingDto = EventMappingDto.builder()
                .id(1L)
                .description("Test Event Mapping")
                .topic("service-decomposition-finished")
                .userRoles(Set.of("User-Role"))
                .build();

        eventMappingCreationDto = EventMappingCreationDto.builder()
                .description("Test Event Mapping")
                .topic("service-decomposition-finished")
                .userRoles(Set.of("User-Role-1"))
                .build();

        EventMappingDto secondEventMappingDto = EventMappingDto.builder()
                .id(2L)
                .description("Test Event Mapping")
                .topic("service-decomposition-finished")
                .userRoles(Set.of("ALL"))
                .build();

        // Create sample notifications list
        sampleEventMappings = Arrays.asList(
                eventMappingDto,
                secondEventMappingDto
        );
    }

    //========================= Create Event Mapping Tests ================================
    @DisplayName("Create Event Mapping : Success")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void givenEventMappingDataAndSuperAdmin_whenCreateEventMapping_thenReturnSuccess() throws Exception {
        // Given
        doNothing().when(eventMappingService).storeEventMapping(any(EventMappingCreationDto.class));

        // When & Then
        mockMvc.perform(post("/api/event-mappings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventMappingCreationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event mapping created successfully"));

        verify(eventMappingService).storeEventMapping(any(EventMappingCreationDto.class));
    }

    @DisplayName("Create Event Mapping : Failure")
    @Test
    @WithMockUser(roles = "ADMIN")
    void givenEventMappingDataAndAdminUser_whenCreateEventMapping_thenReturnFailure() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/event-mappings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventMappingCreationDto)))
                .andExpect(status().isForbidden());

        verify(eventMappingService, never()).storeEventMapping(any(EventMappingCreationDto.class));
    }

    @DisplayName("Create Event Mapping : Invalid Data")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void givenInvalidEventMappingData_whenCreateEventMapping_thenReturnFailure() throws Exception {
        // Given - Invalid data (empty topic)
        EventMappingCreationDto invalidDto = EventMappingCreationDto.builder()
                .description("Test Description")
                .userRoles(Set.of("TEST_ROL"))
                .build();

        // When & Then
        mockMvc.perform(post("/api/event-mappings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(eventMappingService, never()).storeEventMapping(any(EventMappingCreationDto.class));
    }

    @DisplayName("Create Event Mapping : Unauthorized")
    @Test
    void givenNoAuthentication_whenCreateEventMapping_thenReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/event-mappings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventMappingCreationDto)))
                .andExpect(status().isForbidden());
    }

    @DisplayName("Create Event Mapping : Mapping already exists")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void givenExistingTopicMapping_whenCreateEventMapping_thenReturnConflict() throws Exception {
        // Given
        doThrow(new CustomExceptions.ResourceAlreadyExists("Event mapping already exists")).when(eventMappingService).storeEventMapping(any(EventMappingCreationDto.class));

        // When & Then
        mockMvc.perform(post("/api/event-mappings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventMappingCreationDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Resource already exists"));

        verify(eventMappingService).storeEventMapping(any(EventMappingCreationDto.class));
    }

    //========================= Retrieve Event Mapping by Topic Tests ================================
    @DisplayName("Retrieve Event Mapping by Topic : Success")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void whenRetrieveEventMappingByTopic_thenReturnSuccess() throws Exception {
        // Given
        when(eventMappingService.retrieveEventMappingByTopic(anyString())).thenReturn(eventMappingDto);

        // When & Then
        mockMvc.perform(get("/api/event-mappings/topics/service-decomposition-finished"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventMappingId").value(1))
                .andExpect(jsonPath("$.message").value("Event mapping retrieved successfully"));

        verify(eventMappingService).retrieveEventMappingByTopic(anyString());
    }

    @DisplayName("Retrieve Event Mapping : Success for ADMIN Users")
    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminUser_whenRetrieveEventMapping_thenReturnSuccess() throws Exception {
        // Given
        when(eventMappingService.retrieveEventMappingByTopic(anyString())).thenReturn(eventMappingDto);

        // When & Then
        mockMvc.perform(get("/api/event-mappings/topics/service-decomposition-finished"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventMappingId").value(1))
                .andExpect(jsonPath("$.message").value("Event mapping retrieved successfully"));

        verify(eventMappingService).retrieveEventMappingByTopic(anyString());
    }


    @DisplayName("Retrieve Event Mapping : Unauthorized / No Auth Provided")
    @Test
    void givenNoAuthentication_whenRetrieveEventMappingByTopic_thenReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/event-mappings/topics/service-decomposition-finished"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Retrieve Event Mapping : Not Found")
    @Test
    @WithMockUser(roles = "ADMIN")
    void givenTopic_whenRetrieveEventMappingByTopic_thenReturnNotFound() throws Exception {
        // Given
        when(eventMappingService.retrieveEventMappingByTopic(anyString())).thenThrow(new CustomExceptions.ResourceNotFoundException("Event mapping not found"));

        // When & Then
        mockMvc.perform(get("/api/event-mappings/topics/non-existent-topic"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Requested resource not found in DB"));

        verify(eventMappingService).retrieveEventMappingByTopic(anyString());
    }

    //========================= Retrieve Event Mappings Tests ================================
    @DisplayName("Retrieve Event Mappings : Success")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void whenRetrieveAllEventMappings_thenReturnSuccess() throws Exception {
        // Given
        when(eventMappingService.retrieveAllEventMappings()).thenReturn(sampleEventMappings);

        // When & Then
        mockMvc.perform(get("/api/event-mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].eventMappingId").value(1))
                .andExpect(jsonPath("$.data[1].eventMappingId").value(2))
                .andExpect(jsonPath("$.message").value("Event mappings retrieved successfully"));

        verify(eventMappingService).retrieveAllEventMappings();
    }

    @DisplayName("Retrieve Event Mappings : Success for ADMIN Users")
    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminUser_whenRetrieveAllEventMappings_thenReturnSuccess() throws Exception {
        // Given
        when(eventMappingService.retrieveAllEventMappings()).thenReturn(sampleEventMappings);

        // When & Then
        mockMvc.perform(get("/api/event-mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));

        verify(eventMappingService).retrieveAllEventMappings();
    }


    @DisplayName("Retrieve Event Mappings : Unauthorized / No Auth Provided")
    @Test
    void givenNoAuthentication_whenRetrieveAllEventMappings_thenReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/event-mappings"))
                .andExpect(status().isUnauthorized());
    }

    //========================= Delete Event Mapping Tests ================================
    @DisplayName("Delete Event Mapping : Success")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void givenEventMappingId_whenDeleteEventMapping_thenReturnSuccess() throws Exception {
        // Given
        Long mappingId = 1L;
        doNothing().when(eventMappingService).deleteEventMappingById(mappingId);

        // When & Then
        mockMvc.perform(delete("/api/event-mappings/{mappingId}", mappingId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("Event mapping deleted successfully"));

        verify(eventMappingService).deleteEventMappingById(mappingId);
    }

    @DisplayName("Delete Event Mapping : Failure / Forbidden")
    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminUser_whenDeleteEventMapping_thenReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/event-mappings/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(eventMappingService, never()).deleteEventMappingById(anyLong());
    }

    @DisplayName("Delete Event Mapping : Not Found")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void givenNonExistentMappingId_whenDeleteEventMapping_thenReturnNotFound() throws Exception {
        // Given
        Long nonExistentId = 999L;
        doThrow(new CustomExceptions.ResourceNotFoundException("Event mapping not found"))
                .when(eventMappingService).deleteEventMappingById(nonExistentId);

        // When & Then
        mockMvc.perform(delete("/api/event-mappings/{mappingId}", nonExistentId)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(eventMappingService).deleteEventMappingById(nonExistentId);
    }


    //========================= Update Event Mapping Tests ================================
    @DisplayName("Update Event Mapping : Success")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void givenUpdatedEventMappingData_whenUpdateEventMapping_thenReturnSuccess() throws Exception {
        // Given
        Long mappingId = 1L;
        EventMappingDto updateDto = EventMappingDto.builder()
                .description("Updated Description")
                .topic("service-decomposition-finished")
                .userRoles(Set.of("UPDATED_ROLE"))
                .build();

        doNothing().when(eventMappingService).updateEventMappingById(any(EventMappingDto.class));

        // When & Then
        mockMvc.perform(put("/api/event-mappings/{mappingId}", mappingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("Event mapping updated successfully"));

        verify(eventMappingService).updateEventMappingById(any(EventMappingDto.class));
    }

    @DisplayName("Update Event Mapping : Failure / Forbidden")
    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminUser_whenUpdateEventMapping_thenReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/event-mappings/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventMappingDto)))
                .andExpect(status().isForbidden());

        verify(eventMappingService, never()).updateEventMappingById(any(EventMappingDto.class));
    }

    @DisplayName("Update Event Mapping : Invalid Data")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void givenInvalidEventMappingData_whenUpdateEventMapping_thenReturnFailure() throws Exception {
        // Given - Invalid data
        EventMappingDto invalidDto = EventMappingDto.builder()
                .description("")  // Invalid empty description
                .topic("invalid-topic")
                .userRoles(Set.of("ADMIN"))
                .build();

        // When & Then
        mockMvc.perform(put("/api/event-mappings/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(eventMappingService, never()).updateEventMappingById(any(EventMappingDto.class));
    }

    //========================= Retrieve Event Topics Tests ================================
    @DisplayName("Retrieve Event Topics : Success")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void whenRetrieveEventTopics_thenReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/event-mappings/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.message").value("Event topics retrieved successfully"));

        verifyNoInteractions(eventMappingService);
    }

    @DisplayName("Retrieve Event Topics : Success for ADMINs")
    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminUser_whenRetrieveEventTopics_thenReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/event-mappings/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verifyNoInteractions(eventMappingService);
    }

    //=================== Edge Cases Tests ==========================
    @DisplayName("Handle Service Layer Exceptions")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void whenStoreEventMapping_thenThrowInternalException() throws Exception {
        // Given
        doThrow(new RuntimeException("Database connection failed"))
                .when(eventMappingService).storeEventMapping(any(EventMappingCreationDto.class));

        // When & Then
        mockMvc.perform(post("/api/event-mappings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventMappingCreationDto)))
                .andExpect(status().isInternalServerError());
    }

    @DisplayName("Handle Malformed JSON")
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void givenMalformedJsonInput_whenCreateEventMapping_thenReturnFailure() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/event-mappings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }
}
