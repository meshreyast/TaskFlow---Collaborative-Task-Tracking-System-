package com.taskflow.service;

import com.taskflow.domain.Team;
import com.taskflow.exception.ApiException;
import com.taskflow.repository.TeamMemberRepository;
import com.taskflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Team authorization checks. Non-members get 404 rather than 403 so the existence of
 * teams they do not belong to is not revealed.
 */
@Service
@Transactional(readOnly = true)
public class TeamAccessService {

    private final TeamRepository teams;
    private final TeamMemberRepository members;

    public TeamAccessService(TeamRepository teams, TeamMemberRepository members) {
        this.teams = teams;
        this.members = members;
    }

    public Team requireMember(Long teamId, Long userId) {
        Team team = teams.findById(teamId).orElseThrow(() -> ApiException.notFound("Team not found"));
        if (!members.existsByTeamIdAndUserId(teamId, userId)) {
            throw ApiException.notFound("Team not found");
        }
        return team;
    }

    public Team requireOwner(Long teamId, Long userId) {
        Team team = requireMember(teamId, userId);
        if (!team.getOwner().getId().equals(userId)) {
            throw ApiException.forbidden("Only the team owner can do this");
        }
        return team;
    }
}
