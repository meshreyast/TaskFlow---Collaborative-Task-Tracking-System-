package com.taskflow.dto;

import com.taskflow.domain.Invitation;
import com.taskflow.domain.InvitationStatus;
import com.taskflow.domain.Team;
import com.taskflow.domain.TeamMember;
import com.taskflow.domain.TeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class TeamDtos {

    private TeamDtos() {
    }

    public record TeamRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 1000) String description) {
    }

    /** myRole is the role of the user making the request. */
    public record TeamResponse(Long id, String name, String description, UserDtos.UserSummary owner,
                               TeamRole myRole, Instant createdAt) {
        public static TeamResponse from(Team team, TeamRole myRole) {
            return new TeamResponse(team.getId(), team.getName(), team.getDescription(),
                    UserDtos.UserSummary.of(team.getOwner()), myRole, team.getCreatedAt());
        }
    }

    public record MemberResponse(UserDtos.UserSummary user, TeamRole role, Instant joinedAt) {
        public static MemberResponse from(TeamMember member) {
            return new MemberResponse(UserDtos.UserSummary.of(member.getUser()), member.getRole(),
                    member.getJoinedAt());
        }
    }

    public record InviteRequest(@NotBlank @Email @Size(max = 255) String email) {
    }

    public record InvitationResponse(Long id, Long teamId, String teamName, String email,
                                     UserDtos.UserSummary invitedBy, InvitationStatus status, Instant createdAt) {
        public static InvitationResponse from(Invitation invitation) {
            return new InvitationResponse(invitation.getId(), invitation.getTeam().getId(),
                    invitation.getTeam().getName(), invitation.getEmail(),
                    UserDtos.UserSummary.of(invitation.getInvitedBy()), invitation.getStatus(),
                    invitation.getCreatedAt());
        }
    }
}
