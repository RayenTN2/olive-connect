package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.ResourceCalendarResponse;
import ISIMS.olive_connect.service.ResourceCalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/logistics/calendar")
@PreAuthorize("hasAnyRole('LOGISTICS_MANAGER', 'ADMIN')")
public class ResourceCalendarController {

    private final ResourceCalendarService calendarService;

    public ResourceCalendarController(ResourceCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    // US-27: e.g. ?start=2026-11-23T00:00:00Z&end=2026-11-30T00:00:00Z
    @GetMapping
    public ResponseEntity<List<ResourceCalendarResponse>> getCalendar(
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(calendarService.getCalendar(start, end));
    }
}
