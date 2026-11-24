package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.CollectionTourStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// US-27: One worker or equipment item with its tour assignments and unavailability periods in the requested range
public class ResourceCalendarResponse {

    public enum ResourceType { WORKER, EQUIPMENT }

    public enum EntryKind { TOUR, UNAVAILABLE, MAINTENANCE }

    private ResourceType resourceType;
    private String id;
    private String name;
    private String reference;      // equipment only
    private String equipmentType;  // equipment only
    private boolean active;
    private List<Entry> entries = new ArrayList<>();

    // Actual start/end of the tour or period, even when it extends beyond the requested range
    public static class Entry {
        private EntryKind kind;
        private Instant start;
        private Instant end;
        private String tourId;
        private String tourTitle;
        private CollectionTourStatus tourStatus;
        private String reason;
        private boolean conflict;

        public EntryKind getKind() { return kind; }
        public void setKind(EntryKind kind) { this.kind = kind; }

        public Instant getStart() { return start; }
        public void setStart(Instant start) { this.start = start; }

        public Instant getEnd() { return end; }
        public void setEnd(Instant end) { this.end = end; }

        public String getTourId() { return tourId; }
        public void setTourId(String tourId) { this.tourId = tourId; }

        public String getTourTitle() { return tourTitle; }
        public void setTourTitle(String tourTitle) { this.tourTitle = tourTitle; }

        public CollectionTourStatus getTourStatus() { return tourStatus; }
        public void setTourStatus(CollectionTourStatus tourStatus) { this.tourStatus = tourStatus; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public boolean isConflict() { return conflict; }
        public void setConflict(boolean conflict) { this.conflict = conflict; }
    }

    // Getters and Setters
    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<Entry> getEntries() { return entries; }
    public void setEntries(List<Entry> entries) { this.entries = entries; }
}
