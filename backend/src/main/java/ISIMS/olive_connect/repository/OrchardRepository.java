package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.MaturityLevel;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.OrchardHarvestStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrchardRepository extends MongoRepository<Orchard, String> {
    List<Orchard> findByFarmerId(String farmerId);

    // US-10: Interactive map filtering (all cooperative orchards)
    List<Orchard> findByCurrentMaturity(MaturityLevel currentMaturity);
    List<Orchard> findByHarvestStatus(OrchardHarvestStatus harvestStatus);
    List<Orchard> findByCurrentMaturityAndHarvestStatus(MaturityLevel currentMaturity, OrchardHarvestStatus harvestStatus);

    // US-10: Interactive map filtering (scoped to a specific farmer)
    List<Orchard> findByFarmerIdAndCurrentMaturity(String farmerId, MaturityLevel currentMaturity);
    List<Orchard> findByFarmerIdAndHarvestStatus(String farmerId, OrchardHarvestStatus harvestStatus);
    List<Orchard> findByFarmerIdAndCurrentMaturityAndHarvestStatus(String farmerId, MaturityLevel currentMaturity, OrchardHarvestStatus harvestStatus);
}