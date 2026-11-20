package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.CustomEquipmentType;
import ISIMS.olive_connect.entity.Equipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * US-24: Creates the unique indexes that @Indexed(unique = true) would only create with
 * spring.data.mongodb.auto-index-creation=true (not set). The service "already in use" checks give
 * friendly messages; these indexes stop duplicates when two requests arrive at once (e.g. a double click).
 */
@Component
public class EquipmentIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(EquipmentIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public EquipmentIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        createUniqueIndex(Equipment.class, "reference");
        createUniqueIndex(CustomEquipmentType.class, "code");
    }

    private void createUniqueIndex(Class<?> entity, String field) {
        try {
            mongoTemplate.indexOps(entity).createIndex(new Index().on(field, Sort.Direction.ASC).unique());
        } catch (RuntimeException ex) {
            // e.g. duplicates already stored: keep the app running, the service checks still apply
            log.error("Could not create unique index on {}.{}: {}", entity.getSimpleName(), field, ex.getMessage());
        }
    }
}
