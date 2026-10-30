
package com.mekongmobile.smartcrm.reference.repository;

import com.mekongmobile.smartcrm.reference.entity.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TicketRepositoryIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void shouldFindClosedTicket() {
        Ticket ticket = ticketRepository.findById(401L)
                .orElseThrow();

        assertEquals(401L, ticket.getTicketId());
        assertEquals("DA_DONG", ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
    }

    @Test
    void shouldFindProcessingTicket() {
        Ticket ticket = ticketRepository.findById(404L)
                .orElseThrow();

        assertEquals(404L, ticket.getTicketId());
        assertEquals("DANG_XU_LY", ticket.getStatus());
        assertNull(ticket.getClosedAt());
    }
}
