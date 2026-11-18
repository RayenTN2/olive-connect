package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.Orchard;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrchardRepository extends MongoRepository<Orchard, String> {
    List<Orchard> findByFarmerId(String farmerId);
}