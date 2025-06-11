package gr.atc.t4m.controller;

import gr.atc.t4m.controller.responses.BaseAppResponse;
import gr.atc.t4m.controller.responses.PaginatedResults;
import gr.atc.t4m.dto.NotificationDto;
import gr.atc.t4m.service.interfaces.INotificationService;
import gr.atc.t4m.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications Controller", description = "API for managing notifications")
public class NotificationController {

    private final INotificationService notificationService;

    private static final String NOTIFICATION_SUCCESS = "Notifications retrieved successfully";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    public NotificationController(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Retrieve all Notifications
     *
     * @param jwt : JWT Token
     * @param page: Number of Page
     * @param size: Size of Page Elements
     * @param sortAttribute: Sort Based on Variable field
     * @param isAscending: ASC or DESC
     * @return PaginatedResultsDto<NotificationDto> : Notifications with pagination
     */
    @Operation(summary = "Retrieve all Notifications", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = NOTIFICATION_SUCCESS),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "404", description = "Invalid sort attributes")
    })
    @GetMapping
    public ResponseEntity<BaseAppResponse<PaginatedResults<NotificationDto>>> getAllNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "timestamp") String sortAttribute,
            @RequestParam(required = false, defaultValue = "false") boolean isAscending) {

        // Fix the pagination parameters
        Pageable pageable = createPaginationParameters(page, size, sortAttribute, isAscending);
        if (pageable == null)
            return new ResponseEntity<>(BaseAppResponse.error("Invalid sort attributes"), HttpStatus.BAD_REQUEST);

        // Retrieve role and id of user requested the resource
        String pilotRole = JwtUtils.extractPilotRole(jwt);

        Page<NotificationDto> resultsPage;
        if (pilotRole.equals(SUPER_ADMIN_ROLE))
            resultsPage = notificationService.retrieveAllNotificationsPerUserId(SUPER_ADMIN_ROLE,pageable);
        else{
            String userId = JwtUtils.extractUserId(jwt);
            resultsPage = notificationService.retrieveAllNotificationsPerUserId(userId,pageable);
        }

        // Fix the pagination class object
        PaginatedResults<NotificationDto> results = new PaginatedResults<>(
                resultsPage.getContent(),
                resultsPage.getTotalPages(),
                (int) resultsPage.getTotalElements(),
                resultsPage.isLast());

        return new ResponseEntity<>(BaseAppResponse.success(results, NOTIFICATION_SUCCESS), HttpStatus.OK);
    }

    /**
     * Retrieve all unread notifications for a specific user
     *
     * @param jwt : JWT Token
     * @param page: Number of Page
     * @param size: Size of Page Elements
     * @param sortAttribute: Sort Based on Variable field
     * @param isAscending: ASC or DESC
     * @return List<NotificationDto> : Unread notifications
     */
    @Operation(summary = "Retrieve all unread notifications", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unread notifications retrieved successfully!"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token")
    })
    @GetMapping("/unread")
    public ResponseEntity<BaseAppResponse<PaginatedResults<NotificationDto>>> getAllUnreadNotificationPerUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "timestamp") String sortAttribute,
            @RequestParam(required = false, defaultValue = "false") boolean isAscending) {

            // Fix the pagination parameters
            Pageable pageable = createPaginationParameters(page, size, sortAttribute, isAscending);
            if (pageable == null)
                return new ResponseEntity<>(BaseAppResponse.error("Invalid sort attributes"), HttpStatus.BAD_REQUEST);

            // Retrieve role and id of user requested the resource
            String pilotRole = JwtUtils.extractPilotRole(jwt);

            Page<NotificationDto> resultsPage;
            if (pilotRole.equals(SUPER_ADMIN_ROLE))
                resultsPage = notificationService.retrieveUnreadNotificationsPerUserId(SUPER_ADMIN_ROLE,pageable);
            else {
                String userId = JwtUtils.extractUserId(jwt);
                resultsPage = notificationService.retrieveUnreadNotificationsPerUserId(userId,pageable);
            }

            // Fix the pagination class object
            PaginatedResults<NotificationDto> results = new PaginatedResults<>(
                    resultsPage.getContent(),
                    resultsPage.getTotalPages(),
                    (int) resultsPage.getTotalElements(),
                    resultsPage.isLast());

            return new ResponseEntity<>(BaseAppResponse.success(results, NOTIFICATION_SUCCESS), HttpStatus.OK);
    }

    /**
     * Retrieve Notification By Id
     *
     * @param jwt : JWT Token
     * @param notificationId : Notification Id
     * @return NotificationDto : Notification if exists
     */
    @Operation(summary = "Retrieve Notification By ID", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = NOTIFICATION_SUCCESS),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Notification with id [ID] not found in DB")
    })
    @GetMapping("/{notificationId}")
    public ResponseEntity<BaseAppResponse<NotificationDto>> getNotificationById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long notificationId) {
        String pilotRole = JwtUtils.extractPilotRole(jwt);
        String userId;

        if (pilotRole.equals(SUPER_ADMIN_ROLE))
            userId = SUPER_ADMIN_ROLE;
        else
            userId = JwtUtils.extractUserId(jwt);

        return new ResponseEntity<>(BaseAppResponse.success(notificationService.retrieveNotificationById(notificationId, userId), NOTIFICATION_SUCCESS), HttpStatus.OK);
    }

    /**
     * Update Notification Status by ID (From Unread/To Read)
     *
     * @param jwt : JWT Token
     * @param notificationId : Notification Id
     * @return Success message
     */
    @Operation(summary = "Update notification status command of a specific Notification (From Unread to Read)", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification status updated successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Notification with id [ID] not found in DB")
    })
    @PutMapping("/{notificationId}/notificationStatus")
    public ResponseEntity<BaseAppResponse<String>> updateNotificationStatusToRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long notificationId) {
        String pilotRole = JwtUtils.extractPilotRole(jwt);
        String userId;

        if (pilotRole.equals(SUPER_ADMIN_ROLE))
            userId = SUPER_ADMIN_ROLE;
        else
            userId = JwtUtils.extractUserId(jwt);
        notificationService.updateNotificationStatusToRead(notificationId, userId);
        return new ResponseEntity<>(BaseAppResponse.success(null, "Notification status updated successfully"), HttpStatus.OK);
    }

    /**
     * Delete Notification by ID
     *
     * @param jwt : JWT Token
     * @param notificationId : Notification Id
     * @return Success message
     */
    @Operation(summary = "Delete notification by ID", security = @SecurityRequirement(name = "bearerToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Invalid authorization parameters. Check JWT or CSRF Token"),
            @ApiResponse(responseCode = "404", description = "Notification with id [ID] not found in DB")
    })
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<BaseAppResponse<String>> deleteNotificationByIdEndpoint(@AuthenticationPrincipal Jwt jwt, @PathVariable Long notificationId) {
        String pilotRole = JwtUtils.extractPilotRole(jwt);
        String userId;

        if (pilotRole.equals(SUPER_ADMIN_ROLE))
            userId = SUPER_ADMIN_ROLE;
        else
            userId = JwtUtils.extractUserId(jwt);
        notificationService.deleteNotificationById(notificationId, userId);
        return new ResponseEntity<>(BaseAppResponse.success(null, "Notification deleted successfully"), HttpStatus.OK);
    }

    /**
     * Create pagination parameters
     *
     * @param page : Page of results
     * @param size : Results per page
     * @param sortAttribute : Sort attribute
     * @param isAscending : Sort order
     * @return pageable : Pagination Object
     */
    private Pageable createPaginationParameters(int page, int size, String sortAttribute, boolean isAscending){
        // Check if sort attribute is valid
        boolean isValidField = Arrays.stream(NotificationDto.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals(sortAttribute));

        // If not valid, return null
        if (!isValidField) {
            return null;
        }

        // Create pagination parameters
        return isAscending
                ? PageRequest.of(page, size, Sort.by(sortAttribute).ascending())
                : PageRequest.of(page, size, Sort.by(sortAttribute).descending());
    }
}
