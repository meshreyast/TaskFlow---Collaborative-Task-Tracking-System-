package com.taskflow.repository;

import com.taskflow.domain.Invitation;
import com.taskflow.domain.InvitationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    boolean existsByTeamIdAndEmailAndStatus(Long teamId, String email, InvitationStatus status);

    @Query("select i from Invitation i join fetch i.team join fetch i.invitedBy "
            + "where i.email = :email and i.status = :status order by i.createdAt desc")
    List<Invitation> findAllByEmailAndStatus(@Param("email") String email,
                                             @Param("status") InvitationStatus status);

    @EntityGraph(attributePaths = {"team", "invitedBy"})
    List<Invitation> findAllByTeamIdOrderByCreatedAtDesc(Long teamId);

    @EntityGraph(attributePaths = {"team", "invitedBy"})
    Optional<Invitation> findWithTeamById(Long id);

    @Modifying(flushAutomatically = true)
    @Query("delete from Invitation i where i.team.id = :teamId")
    void deleteAllByTeamId(@Param("teamId") Long teamId);
}
