
package com.mekongmobile.smartcrm.survey.repository;

import com.mekongmobile.smartcrm.survey.entity.SurveyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurveyInvitationRepository
        extends JpaRepository<SurveyInvitation, Long> {

    boolean existsByTicketId(Long ticketId);

    Optional<SurveyInvitation> findByTicketId(Long ticketId);

    Optional<SurveyInvitation> findByToken(String token);
}
