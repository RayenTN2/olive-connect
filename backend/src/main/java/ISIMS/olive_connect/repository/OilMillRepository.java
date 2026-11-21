package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.OilMill;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OilMillRepository extends MongoRepository<OilMill, String> {
    List<OilMill> findByActive(boolean active);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);
}