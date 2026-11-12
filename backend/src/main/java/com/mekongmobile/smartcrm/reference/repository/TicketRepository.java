
package com.mekongmobile.smartcrm.reference.repository;

import com.mekongmobile.smartcrm.reference.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository
        extends JpaRepository<Ticket, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Ticket t where t.ticketId = :id")
    java.util.Optional<Ticket> lockById(Long id);
}
