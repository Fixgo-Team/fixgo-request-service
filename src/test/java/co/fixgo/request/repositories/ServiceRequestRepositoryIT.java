package co.fixgo.request.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.fixgo.request.models.RequestMode;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.models.ServiceRequest;
import co.fixgo.request.support.MySqlContainerConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba la tabla service_requests contra un MySQL 8 real: las migraciones corren desde una base vacía,
 * Hibernate valida el esquema contra la entidad y los CHECK rechazan datos que rompen las reglas (BD-05).
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
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);

        assertThat(failed).isZero();
        assertThat(applied).isGreaterThanOrEqualTo(2);
    }

    @Test
    void primaryKeyIsNumericAutoIncrement() {
        String type = jdbc.queryForObject("SELECT data_type FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'service_requests' AND column_name = 'id'",
                String.class);
        String extra = jdbc.queryForObject("SELECT extra FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'service_requests' AND column_name = 'id'",
                String.class);

        assertThat(type).isEqualTo("bigint");
        assertThat(extra).contains("auto_increment");
    }

    @Test
    void savesAndReadsAUrgentRequestWithGeneratedId() {
        ServiceRequest saved = repository.saveAndFlush(urgent());

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).get().satisfies(found -> {
            assertThat(found.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(found.getZoneId()).isEqualTo(3L);
            assertThat(found.getAgreedValue()).isEqualByComparingTo("50000.00");
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void listsByClientNewestFirst() {
        ServiceRequest first = urgent();
        first.setClientId(901L);
        ServiceRequest second = urgent();
        second.setClientId(901L);
        repository.saveAndFlush(first);
        repository.saveAndFlush(second);

        assertThat(repository.findByClientIdOrderByCreatedAtDesc(901L)).hasSize(2);
        assertThat(repository.findByClientIdAndStatusOrderByCreatedAtDesc(901L, RequestStatus.OPEN)).isEmpty();
    }

    @Test
    void urgentRequestWithoutLocationIsRejected() {
        assertThatThrownBy(() -> insert("URGENT", "PENDING", null, null, null, null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_sr_urgent_location");
    }

    @Test
    void scheduledRequestWithoutAddressIsRejected() {
        assertThatThrownBy(() -> insert("SCHEDULED", "OPEN", null, null, null, null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_sr_scheduled_address");
    }

    @Test
    void cancelledRequestWithoutReasonIsRejected() {
        assertThatThrownBy(() -> insert("SCHEDULED", "CANCELLED", "Calle 10 # 5-20", null, null, null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_sr_cancellation_reason");
    }

    @Test
    void negativeAgreedValueIsRejected() {
        assertThatThrownBy(() -> insert("SCHEDULED", "OPEN", "Calle 10 # 5-20", null, null, null, "-1.00"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_sr_agreed_value");
    }

    @Test
    void latitudeOutOfRangeIsRejected() {
        assertThatThrownBy(() -> insert("URGENT", "PENDING", null, "91.000000", "-75.281900", null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_sr_latitude");
    }

    @Test
    void unknownStatusIsRejectedByTheEnum() {
        assertThatThrownBy(() -> insert("SCHEDULED", "DELETED", "Calle 10 # 5-20", null, null, null, null))
                .isInstanceOf(DataAccessException.class);
    }

    private ServiceRequest urgent() {
        ServiceRequest request = new ServiceRequest();
        request.setClientId(55L);
        request.setServiceTypeId(7L);
        request.setZoneId(3L);
        request.setMode(RequestMode.URGENT);
        request.setStatus(RequestStatus.PENDING);
        request.setDescription("El carro no enciende");
        request.setLatitude(new BigDecimal("2.927300"));
        request.setLongitude(new BigDecimal("-75.281900"));
        request.setAgreedValue(new BigDecimal("50000.00"));
        return request;
    }

    private void insert(String mode, String status, String address, String latitude, String longitude,
                        String cancellationReason, String agreedValue) {
        jdbc.update("INSERT INTO service_requests (client_id, service_type_id, mode, status, description, "
                        + "address, latitude, longitude, cancellation_reason, agreed_value) "
                        + "VALUES (55, 7, ?, ?, 'Descripción', ?, ?, ?, ?, ?)",
                mode, status, address, latitude, longitude, cancellationReason, agreedValue);
    }
}
