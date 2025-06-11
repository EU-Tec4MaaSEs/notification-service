package gr.atc.t4m.controller;


import gr.atc.t4m.controller.responses.BaseAppResponse;
import gr.atc.t4m.dto.EventMappingDto;
import gr.atc.t4m.dto.operations.EventMappingCreationDto;
import gr.atc.t4m.enums.MessageBusTopic;
import gr.atc.t4m.service.interfaces.IEventMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/event-mappings")
@Tag(name = "Event Mappings Controller", description = "API for managing event mappings")
public class EventMappingController {

    private final IEventMappingService eventMappingService;

    public EventMappingController(IEventMappingService eventMappingService) {
        this.eventMappingService = eventMappingService;
    }

    /**
     * Create a new event Mapping
     *
     * @param eventMapping: Event Mapping Dto
     * @return Event Mapping ID
     */
    @Operation(summary = "Create a new event Mapping", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event mapping created successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid JWT token attributes")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<BaseAppResponse<String>> storeNewEventMapping(@Valid @RequestBody EventMappingCreationDto eventMapping) {
        eventMappingService.storeEventMapping(eventMapping);
        return new ResponseEntity<>(BaseAppResponse.success(null, "Event mapping created successfully"), HttpStatus.CREATED);
    }

    /**
     * Get all Event Mappings
     *
     * @return List<EventMappingsDto>
     */
    @Operation(summary = "Get all Event Mappings", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event mappings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<BaseAppResponse<List<EventMappingDto>>> getAllEventMappings() {
        List<EventMappingDto> eventMappings = eventMappingService.retrieveAllEventMappings();
        return new ResponseEntity<>(BaseAppResponse.success(eventMappings, "Event mappings retrieved successfully"), HttpStatus.OK);
    }

    /**
     * Get Event Mapping by Name
     *
     * @param topicName: Name of event mapping
     * @return EventMappingsDto
     */
    @Operation(summary = "Get Event Mapping by Topic Name", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event mapping retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/topics/{topicName}")
    public ResponseEntity<BaseAppResponse<EventMappingDto>> getEventMappingByName(@PathVariable String topicName) {
        EventMappingDto eventMapping = eventMappingService.retrieveEventMappingByTopic(topicName);
        return new ResponseEntity<>(BaseAppResponse.success(eventMapping, "Event mapping retrieved successfully"), HttpStatus.OK);
    }

    /**
     * Delete an event mapping by ID
     *
     * @param mappingId: Id of event mapping
     * @return Message of success or error
     */
    @Operation(summary = "Delete an event mapping by ID", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event mapping deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<BaseAppResponse<String>> deleteEventMappingById(@PathVariable Long mappingId) {
        eventMappingService.deleteEventMappingById(mappingId);
        return new ResponseEntity<>(BaseAppResponse.success(null, "Event mapping deleted successfully"), HttpStatus.OK);
    }

    /**
     * Update an Event Mapping
     *
     * @param mappingId: Id of event mapping
     * @return Message of success or error
     */
    @Operation(summary = "Update an Event Mapping", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event mapping updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation Error"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{mappingId}")
    public ResponseEntity<BaseAppResponse<String>> updateEventMappingById(@PathVariable Long mappingId, @RequestBody @Valid EventMappingDto eventMapping) {
        eventMapping.setId(mappingId);
        eventMappingService.updateEventMappingById(eventMapping);
        return new ResponseEntity<>(BaseAppResponse.success(null, "Event mapping updated successfully"), HttpStatus.OK);
    }

    /**
     * Get all Event Topics
     *
     * @return List<String>
     */
    @Operation(summary = "Get all Event Topics", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event topics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/topics")
    public ResponseEntity<BaseAppResponse<List<String>>> getAllEventTopics() {
        List<String> eventTopics = Arrays.stream(MessageBusTopic.values()).map(MessageBusTopic::toString).toList();
        return new ResponseEntity<>(BaseAppResponse.success(eventTopics, "Event topics retrieved successfully"), HttpStatus.OK);
    }
}
