package ISIMS.olive_connect.entity;

// US-25: Status shown to users, computed (never stored).
// Priority: UNDER_MAINTENANCE > UNAVAILABLE (period or inactive equipment) > IN_USE > AVAILABLE.
public enum EquipmentStatus {
    AVAILABLE,
    // shortcut: never returned yet — no tour assignments exist; compute it from confirmed assignments in #35/#36
    IN_USE,
    UNAVAILABLE,
    UNDER_MAINTENANCE
}
