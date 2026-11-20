package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.Equipment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EquipmentRepository extends MongoRepository<Equipment, String> {
    // US-24: Reference uniqueness checks (on create, and on update excluding the item itself)
    boolean existsByReference(String reference);
    boolean existsByReferenceAndIdNot(String reference, String id);

    // US-24: Optional list filters (type = built-in or custom type code)
    List<Equipment> findByType(String type);
    List<Equipment> findByActive(boolean active);
    List<Equipment> findByTypeAndActive(String type, boolean active);
}
