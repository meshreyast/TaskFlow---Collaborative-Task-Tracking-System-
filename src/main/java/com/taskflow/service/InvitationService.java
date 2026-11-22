package com.taskflow.service;

import com.taskflow.domain.Invitation;
import com.taskflow.domain.InvitationStatus;
import com.taskflow.domain.Team;
import com.taskflow.domain.TeamMember;
import com.taskflow.domain.TeamRole;
import com.taskflow.domain.User;
import com.taskflow.dto.TeamDtos.InvitationResponse;
import com.taskflow.dto.TeamDtos.TeamResponse;
import com.taskflow.exception.ApiException;
import com.taskflow.repository.InvitationRepository;
import com.taskflow.repository.TeamMemberRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.util.Text;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Team owners invite people by email; the invitee sees pending invitations and accepts or declines.
 * The invitee does not need an account yet: the invitation is matched by email once they register.
 */
@Service
public class InvitationService {

    private final InvitationRepository invitations;
    private final TeamMemberRepository members;
    private final UserRepository users;
    private final TeamAccessService access;

    public InvitationService(InvitationRepository invitations, TeamMemberRepository members,
                             UserRepository users, TeamAccessService access) {
        this.invitations = invitations;
        this.members = members;
        this.users = users;
        this.access = access;
    }

    @Transactional
    public InvitationResponse invite(Long teamId, Long inviterId, String rawEmail) {
        Team team = access.requireOwner(teamId, inviterId);
        String email = Text.normalizeEmail(rawEmail);

        boolean alreadyMember = users.findByEmail(email)
                .map(u -> members.existsByTeamIdAndUserId(teamId, u.getId()))
                .orElse(false);
        if (alreadyMember) {
            throw ApiException.conflict("This user is already a member of the team");
        }
        if (invitations.existsByTeamIdAndEmailAndStatus(teamId, email, InvitationStatus.PENDING)) {
            throw ApiException.conflict("An invitation is already pending for this email");
        }
        User inviter = users.getReferenceById(inviterId);
        return InvitationResponse.from(invitations.save(new Invitation(team, email, inviter)));
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listForTeam(Long teamId, Long userId) {
        access.requireOwner(teamId, userId);
        return invitations.findAllByTeamIdOrderByCreatedAtDesc(teamId).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listPendingForUser(Long userId) {
        User user = loadUser(userId);
        return invitations.findAllByEmailAndStatus(user.getEmail(), InvitationStatus.PENDING).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @Transactional
    public TeamResponse accept(Long invitationId, Long userId) {
        User user = loadUser(userId);
        Invitation invitation = findPendingFor(invitationId, user);
        Team team = invitation.getTeam();
        if (!members.existsByTeamIdAndUserId(team.getId(), userId)) {
            members.save(new TeamMember(team, user, TeamRole.MEMBER));
        }
        invitation.respond(InvitationStatus.ACCEPTED);
        return TeamResponse.from(team, TeamRole.MEMBER);
    }

    @Transactional
    public void decline(Long invitationId, Long userId) {
        User user = loadUser(userId);
        findPendingFor(invitationId, user).respond(InvitationStatus.DECLINED);
    }

    /** Invitations addressed to someone else are reported as not found. */
    private Invitation findPendingFor(Long invitationId, User user) {
        Invitation invitation = invitations.findWithTeamById(invitationId)
                .filter(i -> i.getEmail().equals(user.getEmail()))
                .orElseThrow(() -> ApiException.notFound("Invitation not found"));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw ApiException.conflict("Invitation has already been answered");
        }
        return invitation;
    }

    private User loadUser(Long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
    }
}
