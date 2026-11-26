package co.fixgo.request.policies;

import co.fixgo.request.clients.ServiceTypeInfo;
import co.fixgo.request.exceptions.InvalidRequestDataException;
import co.fixgo.request.models.ServiceRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * RF3.1: en los subservicios que lo exigen (desvare y cerrajería automotriz) la solicitud trae
 * placa, marca y tipo de vehículo.
 */
@Component
public class VehicleDataPolicy implements RequestPolicy {

    public static final String CODE = "VEHICLE_DATA_REQUIRED";

    @Override
    public void validate(ServiceRequest request, ServiceTypeInfo serviceType) {
        if (!serviceType.requiresVehicleData()) {
            return;
        }
        Map<String, String> missing = new LinkedHashMap<>();
        if (isBlank(request.getVehiclePlate())) {
            missing.put("vehiclePlate", "La placa es obligatoria para este servicio.");
        }
        if (isBlank(request.getVehicleBrand())) {
            missing.put("vehicleBrand", "La marca es obligatoria para este servicio.");
        }
        if (isBlank(request.getVehicleType())) {
            missing.put("vehicleType", "El tipo de vehículo es obligatorio para este servicio.");
        }
        if (!missing.isEmpty()) {
            throw new InvalidRequestDataException(CODE,
                    "Este servicio requiere la placa, la marca y el tipo del vehículo.", missing);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
