package ISIMS.olive_connect.entity;

import java.util.Arrays;
import java.util.List;

// US-24: Built-in equipment types named in the spec (§I.4) and the issue.
// They cannot be renamed or deleted, so code can rely on them (e.g. bin capacity for the F5 KPI).
// The Logistics Manager can add more types at runtime: see CustomEquipmentType.
public enum EquipmentType {
    TRACTOR("Tractor", List.of(new SpecField("registrationNumber", false))),
    BIN("Bin", List.of(new SpecField("capacityKg", true))),
    NET("Net", List.of()),
    VIBRATOR("Vibrator", List.of());

    // A type-specific field, on top of the common ones (name, reference, brand, model, serialNumber)
    public record SpecField(String name, boolean required) {}

    private final String label;
    private final List<SpecField> fields;

    EquipmentType(String label, List<SpecField> fields) {
        this.label = label;
        this.fields = fields;
    }

    public String getLabel() { return label; }
    public List<SpecField> getFields() { return fields; }

    public static boolean isBuiltIn(String code) {
        return Arrays.stream(values()).anyMatch(type -> type.name().equals(code));
    }
}
