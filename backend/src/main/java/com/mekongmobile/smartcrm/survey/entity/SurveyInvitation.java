
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

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "reminder_attempts", nullable = false)
    private short reminderAttempts;

    @Column(name = "last_reminder_attempt_at")
    private LocalDateTime lastReminderAttemptAt;

    @Column(name = "survey_title", nullable = false, length = 120)
    private String surveyTitle;

    @Column(name = "survey_intro", nullable = false, length = 1000)
    private String surveyIntro;

    protected SurveyInvitation() {
        // Required by JPA
    }

    public SurveyInvitation(
            Long ticketId,
            String token,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        this(ticketId, token, createdAt, expiresAt, "Trải nghiệm của bạn thế nào?",
                "Hãy chia sẻ cảm nhận về dịch vụ bảo hành của Mekong Mobile.");
    }

    public SurveyInvitation(
            Long ticketId,
            String token,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            String surveyTitle,
            String surveyIntro
    ) {
        this.ticketId = Objects.requireNonNull(ticketId);
        this.token = Objects.requireNonNull(token);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.surveyTitle = Objects.requireNonNull(surveyTitle);
        this.surveyIntro = Objects.requireNonNull(surveyIntro);

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

    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }

    public void recordAttemptAt(LocalDateTime time) { lastAttemptAt = time; }

    public void markResponded() { status = "DA_PHAN_HOI"; }

    public void markReminded(LocalDateTime time) { remindedAt = time; }

    public short getReminderAttempts() { return reminderAttempts; }

    public LocalDateTime getLastReminderAttemptAt() { return lastReminderAttemptAt; }

    public String getSurveyTitle() { return surveyTitle; }

    public String getSurveyIntro() { return surveyIntro; }

    public void updateChannel(String channel) {
        if (!"CHO_GUI".equals(status) || sendAttempts != 0 || sentAt != null) {
            throw new IllegalStateException("Chỉ lời mời chưa gửi mới được đổi kênh");
        }
        if (!"SMS".equals(channel) && !"ZALO".equals(channel)) {
            throw new IllegalArgumentException("Kênh gửi chỉ được là SMS hoặc ZALO");
        }
        this.channel = channel;
    }

    public void recordReminderAttempt(LocalDateTime time) {
        reminderAttempts++;
        lastReminderAttemptAt = time;
    }

    public void markSent(String channel, LocalDateTime sentTime) {
        validateSendAttempt(channel);
        Objects.requireNonNull(sentTime, "sentTime");

        this.channel = channel;
        this.sendAttempts++;
        this.sentAt = sentTime;
        this.status = "DA_GUI";
    }

    public void markSendFailed(String channel) {
        validateSendAttempt(channel);

        this.channel = channel;
        this.sendAttempts++;
        this.status = "GUI_LOI";
    }

    private void validateSendAttempt(String channel) {
        if (!"SMS".equals(channel) && !"ZALO".equals(channel)) {
            throw new IllegalArgumentException(
                    "Kênh gửi chỉ được là SMS hoặc ZALO"
            );
        }

        if (!"CHO_GUI".equals(status) && !"GUI_LOI".equals(status)) {
            throw new IllegalStateException(
                    "Trạng thái hiện tại không cho phép gửi lời mời"
            );
        }

        if (sendAttempts >= 3) {
            throw new IllegalStateException(
                    "Đã vượt quá 3 lần thử gửi"
            );
        }
    }

}
