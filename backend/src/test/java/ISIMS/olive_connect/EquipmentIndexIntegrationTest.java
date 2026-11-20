package ISIMS.olive_connect;

import ISIMS.olive_connect.entity.CustomEquipmentType;
import ISIMS.olive_connect.entity.Equipment;
import ISIMS.olive_connect.repository.CustomEquipmentTypeRepository;
import ISIMS.olive_connect.repository.EquipmentIndexInitializer;
import ISIMS.olive_connect.repository.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * US-24: Proves the unique indexes really exist in MongoDB, so two simultaneous requests
 * cannot both save the same reference / type code. Real throw-away MongoDB (Docker); skipped without Docker.
 */
@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
@Import(EquipmentIndexInitializer.class)
class EquipmentIndexIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private EquipmentIndexInitializer indexInitializer;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private CustomEquipmentTypeRepository customTypeRepository;

    @BeforeEach
    void setUp() {
        equipmentRepository.deleteAll();
        customTypeRepository.deleteAll();
        indexInitializer.createIndexes(); // runs on ApplicationReadyEvent in the real app
    }

    private static Equipment equipment(String reference) {
        Equipment equipment = new Equipment();
        equipment.setName("Bin");
        equipment.setReference(reference);
        equipment.setType("BIN");
        equipment.setCapacityKg(500);
        return equipment;
    }

    private static CustomEquipmentType customType(String code) {
        CustomEquipmentType type = new CustomEquipmentType();
        type.setCode(code);
        type.setLabel(code);
        return type;
    }

    @Test
    void duplicateReference_isRejectedByTheDatabase() {
        equipmentRepository.save(equipment("BN-01"));
        assertThrows(DuplicateKeyException.class, () -> equipmentRepository.save(equipment("BN-01")));
    }

    @Test
    void duplicateCustomTypeCode_isRejectedByTheDatabase() {
        customTypeRepository.save(customType("TRAILER_BIN"));
        assertThrows(DuplicateKeyException.class, () -> customTypeRepository.save(customType("TRAILER_BIN")));
    }

    @Test
    void creatingIndexesTwice_isHarmless() {
        indexInitializer.createIndexes(); // every app restart does this
        equipmentRepository.save(equipment("BN-02"));
    }
}
