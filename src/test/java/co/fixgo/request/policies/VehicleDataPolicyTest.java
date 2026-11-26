package co.fixgo.request.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.fixgo.request.clients.ServiceTypeInfo;
import co.fixgo.request.exceptions.InvalidRequestDataException;
import co.fixgo.request.models.ServiceRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VehicleDataPolicyTest {

    private final VehicleDataPolicy policy = new VehicleDataPolicy();

    @Test
    void serviceTypesWithoutVehicleDataAcceptAnyRequest() {
        assertThatCode(() -> policy.validate(new ServiceRequest(), serviceType(false))).doesNotThrowAnyException();
    }

    @Test
    void completeVehicleDataIsAccepted() {
        assertThatCode(() -> policy.validate(vehicle("ABC123", "Chevrolet", "Automóvil"), serviceType(true)))
                .doesNotThrowAnyException();
    }

    @Test
    void everyMissingFieldIsReportedWithTheSpecificCode() {
        assertThatThrownBy(() -> policy.validate(vehicle(null, "  ", ""), serviceType(true)))
                .isInstanceOfSatisfying(InvalidRequestDataException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("VEHICLE_DATA_REQUIRED");
                    assertThat(ex.hasCustomMessage()).isTrue();
                    assertThat(ex.getDetails()).containsOnlyKeys("vehiclePlate", "vehicleBrand", "vehicleType");
                });
    }

    @Test
    void onlyTheMissingFieldIsReported() {
        assertThatThrownBy(() -> policy.validate(vehicle("ABC123", "Chevrolet", null), serviceType(true)))
                .isInstanceOfSatisfying(InvalidRequestDataException.class,
                        ex -> assertThat(ex.getDetails()).containsOnlyKeys("vehicleType"));
    }

    private ServiceRequest vehicle(String plate, String brand, String type) {
        ServiceRequest request = new ServiceRequest();
        request.setVehiclePlate(plate);
        request.setVehicleBrand(brand);
        request.setVehicleType(type);
        return request;
    }

    private ServiceTypeInfo serviceType(boolean requiresVehicleData) {
        return new ServiceTypeInfo(7L, true, true, new BigDecimal("50000.00"), false, requiresVehicleData);
    }
}
