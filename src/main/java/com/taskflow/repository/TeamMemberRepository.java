package com.taskflow.repository;

import com.taskflow.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    @Query("select m.team.id from TeamMember m where m.user.id = :userId")
    List<Long> findTeamIdsByUserId(@Param("userId") Long userId);

    @Query("select m from TeamMember m join fetch m.user where m.team.id = :teamId order by m.joinedAt")
    List<TeamMember> findAllByTeamIdWithUser(@Param("teamId") Long teamId);

    @Query("select m from TeamMember m join fetch m.team t join fetch t.owner "
            + "where m.user.id = :userId order by t.name")
    List<TeamMember> findAllByUserIdWithTeam(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from TeamMember m where m.team.id = :teamId")
    void deleteAllByTeamId(@Param("teamId") Long teamId);

    @Modifying(flushAutomatically = true)
    @Query("delete from TeamMember m where m.team.id = :teamId and m.user.id = :userId")
    int deleteMembership(@Param("teamId") Long teamId, @Param("userId") Long userId);
}
