package ISIMS.olive_connect.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class UpdateTourDropOffsRequest {

    @NotEmpty(message = "At least one oil mill drop-off allocation is required")
    @Valid
    private List<CreateCollectionTourRequest.OilMillDropOffInput> dropOffs;

    public List<CreateCollectionTourRequest.OilMillDropOffInput> getDropOffs() { return dropOffs; }
    public void setDropOffs(List<CreateCollectionTourRequest.OilMillDropOffInput> dropOffs) { this.dropOffs = dropOffs; }
}