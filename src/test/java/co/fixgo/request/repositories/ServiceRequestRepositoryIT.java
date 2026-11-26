package co.fixgo.request.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.fixgo.request.models.RequestMode;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.models.ServiceRequest;
import co.fixgo.request.support.MySqlContainerConfig;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba la tabla service_requests contra un MySQL 8 real: las migraciones corren desde una base vacía
 * y los CHECK del esquema rechazan datos que rompen las reglas (BD-05).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MySqlContainerConfig.class)
class ServiceRequestRepositoryIT {

    @Autowired
    private ServiceRequestRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migrationsRunFromEmptyDatabase() {
        Integer failed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);
        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'service_requests'", Integer.class);

        assertThat(failed).isZero();
        assertThat(tables).isEqualTo(1);
    }

    @Test
    void savesAndReadsAUrgentRequest() {
        ServiceRequest request = new ServiceRequest();
        request.setClientId(UUID.randomUUID());
        request.setServiceTypeId(UUID.randomUUID());
        request.setMode(RequestMode.URGENT);
        request.setStatus(RequestStatus.PENDING);
        request.setDescription("El carro no enciende");
        request.setLatitude(new BigDecimal("2.927300"));
        request.setLongitude(new BigDecimal("-75.281900"));

        ServiceRequest saved = repository.saveAndFlush(request);

        assertThat(repository.findById(saved.getId())).get()
                .satisfies(found -> {
                    assertThat(found.getStatus()).isEqualTo(RequestStatus.PENDING);
                    assertThat(found.getCreatedAt()).isNotNull();
                });
    }

    @Test
    void urgentRequestWithoutLocationIsRejected() {
        assertThatThrownBy(() -> insert("URGENT", "PENDING", null, null, null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_service_requests_urgent_location");
    }

    @Test
    void scheduledRequestWithoutAddressIsRejected() {
        assertThatThrownBy(() -> insert("SCHEDULED", "OPEN", null, null, null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_service_requests_scheduled_address");
    }

    @Test
    void cancelledRequestWithoutReasonIsRejected() {
        assertThatThrownBy(() -> insert("SCHEDULED", "CANCELLED", "Calle 10 # 5-20", null, null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_service_requests_cancellation");
    }

    @Test
    void unknownStatusIsRejected() {
        assertThatThrownBy(() -> insert("SCHEDULED", "DELETED", "Calle 10 # 5-20", null, null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_service_requests_status");
    }

    @Test
    void latitudeOutOfRangeIsRejected() {
        assertThatThrownBy(() -> insert("URGENT", "PENDING", null, "91.000000", "-75.281900", null))
                .isInstanceOf(DataAccessException.class);
    }

    private void insert(String mode, String status, String address, String latitude, String longitude,
                        String cancellationReason) {
        jdbc.update("INSERT INTO service_requests (id, client_id, service_type_id, mode, status, description, "
                        + "address, latitude, longitude, cancellation_reason, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, 'Descripción', ?, ?, ?, ?, NOW(6), NOW(6))",
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                mode, status, address, latitude, longitude, cancellationReason);
    }
}
