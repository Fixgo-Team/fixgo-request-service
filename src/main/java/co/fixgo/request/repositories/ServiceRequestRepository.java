package co.fixgo.request.repositories;

import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.models.ServiceRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findAllByOrderByCreatedAtDesc();

    List<ServiceRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);

    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<ServiceRequest> findByClientIdAndStatusOrderByCreatedAtDesc(Long clientId, RequestStatus status);
}
