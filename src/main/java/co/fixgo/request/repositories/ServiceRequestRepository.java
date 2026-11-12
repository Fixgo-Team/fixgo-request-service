package co.fixgo.request.repositories;

import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.models.ServiceRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findAllByOrderByCreatedAtDesc();

    List<ServiceRequest> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<ServiceRequest> findByClientIdAndStatusOrderByCreatedAtDesc(UUID clientId, RequestStatus status);
}
