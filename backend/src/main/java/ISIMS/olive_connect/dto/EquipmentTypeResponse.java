package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.CustomEquipmentType;
import ISIMS.olive_connect.entity.EquipmentType;
import ISIMS.olive_connect.entity.EquipmentType.SpecField;

import java.util.List;

public class EquipmentTypeResponse {

    private String code;
    private String label;
    private boolean builtIn;
    private List<SpecField> fields;

    public static EquipmentTypeResponse fromBuiltIn(EquipmentType type) {
        EquipmentTypeResponse dto = new EquipmentTypeResponse();
        dto.setCode(type.name());
        dto.setLabel(type.getLabel());
        dto.setBuiltIn(true);
        dto.setFields(type.getFields());
        return dto;
    }

    public static EquipmentTypeResponse fromCustom(CustomEquipmentType type) {
        EquipmentTypeResponse dto = new EquipmentTypeResponse();
        dto.setCode(type.getCode());
        dto.setLabel(type.getLabel());
        dto.setBuiltIn(false);
        dto.setFields(List.of());
        return dto;
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isBuiltIn() { return builtIn; }
    public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }

    public List<SpecField> getFields() { return fields; }
    public void setFields(List<SpecField> fields) { this.fields = fields; }
}
