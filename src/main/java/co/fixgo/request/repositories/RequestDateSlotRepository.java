package co.fixgo.request.repositories;

import co.fixgo.request.models.RequestDateSlot;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestDateSlotRepository extends JpaRepository<RequestDateSlot, Long> {

    List<RequestDateSlot> findByRequestIdOrderByStartsAtAsc(Long requestId);

    List<RequestDateSlot> findByRequestIdInOrderByStartsAtAsc(Collection<Long> requestIds);

    void deleteByRequestId(Long requestId);
}
