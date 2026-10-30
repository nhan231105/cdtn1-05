
package com.mekongmobile.smartcrm.survey.service;

import com.mekongmobile.smartcrm.reference.entity.Ticket;
import com.mekongmobile.smartcrm.reference.repository.TicketRepository;
import com.mekongmobile.smartcrm.survey.dto.CreateSurveyInvitationResponse;
import com.mekongmobile.smartcrm.survey.entity.SurveyInvitation;
import com.mekongmobile.smartcrm.survey.exception.SurveyBusinessException;
import com.mekongmobile.smartcrm.survey.repository.SurveyInvitationRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SurveyInvitationServiceTest {

    private final TicketRepository ticketRepository =
            mock(TicketRepository.class);

    private final SurveyInvitationRepository invitationRepository =
            mock(SurveyInvitationRepository.class);

    private final SurveyInvitationService service =
            new SurveyInvitationService(
                    ticketRepository,
                    invitationRepository,
                    "http://localhost:5173/khao-sat"
            );

    @Test
    void shouldRejectMissingTicket() {
        when(ticketRepository.findById(999L))
                .thenReturn(Optional.empty());

        SurveyBusinessException ex = assertThrows(
                SurveyBusinessException.class,
                () -> service.createInvitation(999L)
        );

        assertEquals(
                SurveyBusinessException.Code.TICKET_NOT_FOUND,
                ex.getCode()
        );

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void shouldRejectProcessingTicket() {
        Ticket ticket = mock(Ticket.class);

        when(ticket.getStatus())
                .thenReturn("DANG_XU_LY");

        when(ticketRepository.findById(404L))
                .thenReturn(Optional.of(ticket));

        SurveyBusinessException ex = assertThrows(
                SurveyBusinessException.class,
                () -> service.createInvitation(404L)
        );

        assertEquals(
                SurveyBusinessException.Code.TICKET_NOT_CLOSED,
                ex.getCode()
        );

        assertEquals(
                "DANG_XU_LY",
                ex.getFields().get("status")
        );

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void shouldRejectDuplicateInvitation() {
        Ticket ticket = mock(Ticket.class);

        when(ticket.getStatus()).thenReturn("DA_DONG");
        when(ticketRepository.findById(401L))
                .thenReturn(Optional.of(ticket));

        SurveyInvitation existing =
                mock(SurveyInvitation.class);

        when(existing.getInvitationId()).thenReturn(501L);

        when(invitationRepository.findByTicketId(401L))
                .thenReturn(Optional.of(existing));

        SurveyBusinessException ex = assertThrows(
                SurveyBusinessException.class,
                () -> service.createInvitation(401L)
        );

        assertEquals(
                SurveyBusinessException.Code.INVITATION_ALREADY_EXISTS,
                ex.getCode()
        );

        assertEquals(
                501L,
                ex.getFields().get("existing_invitation_id")
        );

        verify(invitationRepository, never())
                .saveAndFlush(any(SurveyInvitation.class));
    }

    @Test
    void shouldCreateInvitationForClosedTicket() {
        Ticket ticket = mock(Ticket.class);

        when(ticket.getStatus()).thenReturn("DA_DONG");
        when(ticket.getTicketCode())
                .thenReturn("DEMO-L8-0001");

        when(ticketRepository.findById(401L))
                .thenReturn(Optional.of(ticket));

        when(invitationRepository.findByTicketId(401L))
                .thenReturn(Optional.empty());

        when(invitationRepository.saveAndFlush(
                any(SurveyInvitation.class)
        )).thenAnswer(invocation -> {
            SurveyInvitation original =
                    invocation.getArgument(0);

            SurveyInvitation saved =
                    mock(SurveyInvitation.class);

            when(saved.getInvitationId()).thenReturn(501L);
            when(saved.getTicketId())
                    .thenReturn(original.getTicketId());
            when(saved.getStatus())
                    .thenReturn(original.getStatus());
            when(saved.getCreatedAt())
                    .thenReturn(original.getCreatedAt());
            when(saved.getExpiresAt())
                    .thenReturn(original.getExpiresAt());

            return saved;
        });

        CreateSurveyInvitationResponse response =
                service.createInvitation(401L);

        ArgumentCaptor<SurveyInvitation> captor =
                ArgumentCaptor.forClass(
                        SurveyInvitation.class
                );

        verify(invitationRepository)
                .saveAndFlush(captor.capture());

        SurveyInvitation invitation = captor.getValue();

        assertEquals(401L, invitation.getTicketId());
        assertEquals("CHO_GUI", invitation.getStatus());
        assertEquals("SMS", invitation.getChannel());
        assertEquals(0, invitation.getSendAttempts());

        assertEquals(43, invitation.getToken().length());
        assertTrue(
                invitation.getToken()
                        .matches("[A-Za-z0-9_-]{43}")
        );

        assertEquals(
                invitation.getCreatedAt().plusDays(7),
                invitation.getExpiresAt()
        );

        assertEquals(501L, response.invitationId());
        assertEquals("CHO_GUI", response.status());

        assertEquals(
                "http://localhost:5173/khao-sat/"
                        + invitation.getToken(),
                response.surveyUrl()
        );
    }

    @Test
    void shouldRejectZeroTicketId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createInvitation(0L)
        );

        verifyNoInteractions(ticketRepository);
        verifyNoInteractions(invitationRepository);
    }
}
