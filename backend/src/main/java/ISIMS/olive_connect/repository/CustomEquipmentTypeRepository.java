package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.CustomEquipmentType;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomEquipmentTypeRepository extends MongoRepository<CustomEquipmentType, String> {
    boolean existsByCode(String code);
}
