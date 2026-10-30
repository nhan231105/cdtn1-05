
package com.mekongmobile.smartcrm.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "survey_invitation")
public class SurveyInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long invitationId;

    @Column(name = "ticket_id", nullable = false, unique = true)
    private Long ticketId;

    @Column(name = "token", nullable = false, unique = true, length = 43)
    private String token;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "channel", nullable = false, length = 10)
    private String channel;

    @Column(name = "send_attempts", nullable = false)
    private short sendAttempts;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "reminded_at")
    private LocalDateTime remindedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected SurveyInvitation() {
        // Required by JPA
    }

    public SurveyInvitation(
            Long ticketId,
            String token,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        this.ticketId = Objects.requireNonNull(ticketId);
        this.token = Objects.requireNonNull(token);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);

        this.status = "CHO_GUI";
        this.channel = "SMS";
        this.sendAttempts = 0;
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getToken() {
        return token;
    }

    public String getStatus() {
        return status;
    }

    public String getChannel() {
        return channel;
    }

    public short getSendAttempts() {
        return sendAttempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getRemindedAt() {
        return remindedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
