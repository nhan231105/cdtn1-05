
package com.mekongmobile.smartcrm.reference.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
@Immutable
public class Ticket {

    @Id
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "ticket_code", nullable = false, length = 20)
    private String ticketCode;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    protected Ticket() {
        // Required by JPA
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getCenterId() {
        return centerId;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}
