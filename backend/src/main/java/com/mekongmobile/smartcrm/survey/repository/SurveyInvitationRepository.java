
package com.mekongmobile.smartcrm.survey.repository;

import com.mekongmobile.smartcrm.survey.entity.SurveyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyInvitationRepository
        extends JpaRepository<SurveyInvitation, Long> {

    boolean existsByTicketId(Long ticketId);

    Optional<SurveyInvitation> findByTicketId(Long ticketId);

    Optional<SurveyInvitation> findByToken(String token);

    List<SurveyInvitation> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from SurveyInvitation i where i.invitationId = :id")
    Optional<SurveyInvitation> lockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from SurveyInvitation i where i.token = :token")
    Optional<SurveyInvitation> lockByToken(String token);
}
