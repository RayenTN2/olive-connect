package ISIMS.olive_connect.dto;

import java.util.List;

public class AssignTourResourcesRequest {

    private List<String> workerIds;
    private List<String> equipmentIds;

    public List<String> getWorkerIds() { return workerIds; }
    public void setWorkerIds(List<String> workerIds) { this.workerIds = workerIds; }

    public List<String> getEquipmentIds() { return equipmentIds; }
    public void setEquipmentIds(List<String> equipmentIds) { this.equipmentIds = equipmentIds; }
}