package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.ResourceCalendarResponse;
import ISIMS.olive_connect.dto.ResourceCalendarResponse.Entry;
import ISIMS.olive_connect.dto.ResourceCalendarResponse.EntryKind;
import ISIMS.olive_connect.dto.ResourceCalendarResponse.ResourceType;
import ISIMS.olive_connect.entity.CollectionTour;
import ISIMS.olive_connect.entity.Equipment;
import ISIMS.olive_connect.entity.EquipmentUnavailability;
import ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus;
import ISIMS.olive_connect.entity.Worker;
import ISIMS.olive_connect.entity.WorkerUnavailability;
import ISIMS.olive_connect.repository.CollectionTourRepository;
import ISIMS.olive_connect.repository.EquipmentRepository;
import ISIMS.olive_connect.repository.EquipmentUnavailabilityRepository;
import ISIMS.olive_connect.repository.WorkerRepository;
import ISIMS.olive_connect.repository.WorkerUnavailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * US-27: Read-only resource calendar, computed on each request from tours and unavailability periods (nothing stored).
 * It reads tours itself, so the worker/equipment services stay independent of tours.
 */
@Service
public class ResourceCalendarService {

    private final WorkerRepository workerRepository;
    private final EquipmentRepository equipmentRepository;
    private final WorkerUnavailabilityRepository workerUnavailabilityRepository;
    private final EquipmentUnavailabilityRepository equipmentUnavailabilityRepository;
    private final CollectionTourRepository tourRepository;

    public ResourceCalendarService(WorkerRepository workerRepository,
                                   EquipmentRepository equipmentRepository,
                                   WorkerUnavailabilityRepository workerUnavailabilityRepository,
                                   EquipmentUnavailabilityRepository equipmentUnavailabilityRepository,
                                   CollectionTourRepository tourRepository) {
        this.workerRepository = workerRepository;
        this.equipmentRepository = equipmentRepository;
        this.workerUnavailabilityRepository = workerUnavailabilityRepository;
        this.equipmentUnavailabilityRepository = equipmentUnavailabilityRepository;
        this.tourRepository = tourRepository;
    }

    // Every worker and equipment item (inactive ones included), with the entries intersecting [start, end)
    public List<ResourceCalendarResponse> getCalendar(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new RuntimeException("Start time and end time are required.");
        }
        if (!start.isBefore(end)) {
            throw new RuntimeException("Start time must be before end time.");
        }

        // Confirmed, in-progress and completed tours (history); drafts reserve nothing, cancelled tours disappear.
        // shortcut: loads every tour with these statuses and filters in memory, add a date-range query if tours grow
        List<CollectionTour> tours = tourRepository.findByStatusIn(CollectionTourService.CAPACITY_RESERVED_STATUSES);
        Map<String, List<Entry>> workerEntries = new HashMap<>();
        Map<String, List<Entry>> equipmentEntries = new HashMap<>();
        collect(tours, start, end, workerEntries, equipmentEntries);

        List<ResourceCalendarResponse> calendar = new ArrayList<>();
        for (Worker worker : workerRepository.findAll()) {
            ResourceCalendarResponse row = new ResourceCalendarResponse();
            row.setResourceType(ResourceType.WORKER);
            row.setId(worker.getId());
            row.setName(worker.getFullName());
            row.setActive(worker.isActive());
            row.setEntries(sorted(workerEntries.get(worker.getId())));
            calendar.add(row);
        }
        for (Equipment equipment : equipmentRepository.findAll()) {
            ResourceCalendarResponse row = new ResourceCalendarResponse();
            row.setResourceType(ResourceType.EQUIPMENT);
            row.setId(equipment.getId());
            row.setName(equipment.getName());
            row.setReference(equipment.getReference());
            row.setEquipmentType(equipment.getType());
            row.setActive(equipment.isActive());
            row.setEntries(sorted(equipmentEntries.get(equipment.getId())));
            calendar.add(row);
        }
        return calendar;
    }

    // Tours and unavailability periods intersecting [from, to), grouped by resource id
    private void collect(List<CollectionTour> tours, Instant from, Instant to,
                         Map<String, List<Entry>> workerEntries, Map<String, List<Entry>> equipmentEntries) {
        for (CollectionTour tour : tours) {
            Instant[] window = CollectionTourService.resolveTourWindow(tour);
            if (!overlaps(window[0], window[1], from, to)) {
                continue;
            }
            for (String workerId : orEmpty(tour.getAssignedWorkerIds())) {
                workerEntries.computeIfAbsent(workerId, k -> new ArrayList<>()).add(tourEntry(tour, window));
            }
            for (String equipmentId : orEmpty(tour.getAssignedEquipmentIds())) {
                equipmentEntries.computeIfAbsent(equipmentId, k -> new ArrayList<>()).add(tourEntry(tour, window));
            }
        }
        for (WorkerUnavailability period : workerUnavailabilityRepository.findByStartTimeLessThanAndEndTimeGreaterThan(to, from)) {
            workerEntries.computeIfAbsent(period.getWorkerId(), k -> new ArrayList<>())
                    .add(periodEntry(EntryKind.UNAVAILABLE, period.getStartTime(), period.getEndTime(), period.getReason()));
        }
        for (EquipmentUnavailability period : equipmentUnavailabilityRepository.findByStartTimeLessThanAndEndTimeGreaterThan(to, from)) {
            EntryKind kind = period.getStatus() == EquipmentUnavailabilityStatus.UNDER_MAINTENANCE
                    ? EntryKind.MAINTENANCE : EntryKind.UNAVAILABLE;
            equipmentEntries.computeIfAbsent(period.getEquipmentId(), k -> new ArrayList<>())
                    .add(periodEntry(kind, period.getStartTime(), period.getEndTime(), period.getReason()));
        }
    }

    private static List<Entry> sorted(List<Entry> entries) {
        if (entries == null) {
            return new ArrayList<>();
        }
        return entries.stream()
                .sorted(Comparator.comparing(Entry::getStart))
                .toList();
    }

    // Same rule as everywhere else: touching periods do not overlap
    private static boolean overlaps(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    // Tours saved without the field come back from MongoDB with null lists
    private static List<String> orEmpty(List<String> ids) {
        return ids != null ? ids : List.of();
    }

    private static Entry tourEntry(CollectionTour tour, Instant[] window) {
        Entry entry = new Entry();
        entry.setKind(EntryKind.TOUR);
        entry.setStart(window[0]);
        entry.setEnd(window[1]);
        entry.setTourId(tour.getId());
        entry.setTourTitle(tour.getTitle());
        entry.setTourStatus(tour.getStatus());
        return entry;
    }

    private static Entry periodEntry(EntryKind kind, Instant start, Instant end, String reason) {
        Entry entry = new Entry();
        entry.setKind(kind);
        entry.setStart(start);
        entry.setEnd(end);
        entry.setReason(reason);
        return entry;
    }
}
