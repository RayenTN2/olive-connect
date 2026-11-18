package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.Worker;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorkerRepository extends MongoRepository<Worker, String> {
    // US-22: Filter workers by active status
    List<Worker> findByActive(boolean active);
}
