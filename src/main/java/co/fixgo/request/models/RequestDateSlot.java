package co.fixgo.request.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Franja horaria que el cliente ofrece en una solicitud programada (RF3.2). Las fechas van en UTC.
 */
@Entity
@Table(name = "request_date_slots")
public class RequestDateSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "request_id", nullable = false, updatable = false)
    private Long requestId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RequestDateSlot() {
    }

    public RequestDateSlot(Long requestId, Instant startsAt, Instant endsAt) {
        this.requestId = requestId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /** true si el instante cae dentro de la franja: desde el inicio (incluido) hasta el fin (excluido). */
    public boolean contains(Instant instant) {
        return !instant.isBefore(startsAt) && instant.isBefore(endsAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
