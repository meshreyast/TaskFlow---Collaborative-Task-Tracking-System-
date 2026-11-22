package com.taskflow.service;

import com.taskflow.domain.Team;
import com.taskflow.domain.TeamMember;
import com.taskflow.domain.TeamRole;
import com.taskflow.domain.User;
import com.taskflow.dto.TeamDtos.MemberResponse;
import com.taskflow.dto.TeamDtos.TeamRequest;
import com.taskflow.dto.TeamDtos.TeamResponse;
import com.taskflow.exception.ApiException;
import com.taskflow.repository.InvitationRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.TeamMemberRepository;
import com.taskflow.repository.TeamRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.util.Text;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teams;
    private final TeamMemberRepository members;
    private final InvitationRepository invitations;
    private final TaskRepository tasks;
    private final UserRepository users;
    private final TeamAccessService access;
    private final TaskService taskService;

    public TeamService(TeamRepository teams, TeamMemberRepository members, InvitationRepository invitations,
                       TaskRepository tasks, UserRepository users, TeamAccessService access,
                       TaskService taskService) {
        this.teams = teams;
        this.members = members;
        this.invitations = invitations;
        this.tasks = tasks;
        this.users = users;
        this.access = access;
        this.taskService = taskService;
    }

    @Transactional
    public TeamResponse create(Long userId, TeamRequest request) {
        User owner = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
        Team team = teams.save(new Team(request.name().trim(), Text.trimToNull(request.description()), owner));
        members.save(new TeamMember(team, owner, TeamRole.OWNER));
        return TeamResponse.from(team, TeamRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listMine(Long userId) {
        return members.findAllByUserIdWithTeam(userId).stream()
                .map(m -> TeamResponse.from(m.getTeam(), m.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse get(Long teamId, Long userId) {
        Team team = access.requireMember(teamId, userId);
        return TeamResponse.from(team, roleOf(team, userId));
    }

    @Transactional
    public TeamResponse update(Long teamId, Long userId, TeamRequest request) {
        Team team = access.requireOwner(teamId, userId);
        team.setName(request.name().trim());
        team.setDescription(Text.trimToNull(request.description()));
        return TeamResponse.from(team, TeamRole.OWNER);
    }

    /** Deletes the team together with its tasks (and their comments and attachments), members and invitations. */
    @Transactional
    public void delete(Long teamId, Long userId) {
        Team team = access.requireOwner(teamId, userId);
        taskService.deleteAllForTeam(teamId);
        invitations.deleteAllByTeamId(teamId);
        members.deleteAllByTeamId(teamId);
        teams.delete(team);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long teamId, Long userId) {
        access.requireMember(teamId, userId);
        return members.findAllByTeamIdWithUser(teamId).stream().map(MemberResponse::from).toList();
    }

    /**
     * The owner can remove any member; a member can remove only themselves (leave the team).
     * Tasks assigned to the removed member become unassigned.
     */
    @Transactional
    public void removeMember(Long teamId, Long requesterId, Long targetUserId) {
        Team team = access.requireMember(teamId, requesterId);
        Long ownerId = team.getOwner().getId();
        if (!ownerId.equals(requesterId) && !requesterId.equals(targetUserId)) {
            throw ApiException.forbidden("Only the team owner can remove other members");
        }
        if (ownerId.equals(targetUserId)) {
            throw ApiException.badRequest("The owner cannot leave or be removed; delete the team instead");
        }
        if (!members.existsByTeamIdAndUserId(teamId, targetUserId)) {
            throw ApiException.notFound("Member not found");
        }
        tasks.unassignInTeam(teamId, targetUserId);
        members.deleteMembership(teamId, targetUserId);
    }

    private static TeamRole roleOf(Team team, Long userId) {
        return team.getOwner().getId().equals(userId) ? TeamRole.OWNER : TeamRole.MEMBER;
    }
}
