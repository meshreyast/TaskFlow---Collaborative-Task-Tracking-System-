package com.taskflow.web;

import com.taskflow.dto.TeamDtos.InvitationResponse;
import com.taskflow.dto.TeamDtos.InviteRequest;
import com.taskflow.dto.TeamDtos.MemberResponse;
import com.taskflow.dto.TeamDtos.TeamRequest;
import com.taskflow.dto.TeamDtos.TeamResponse;
import com.taskflow.security.CurrentUser;
import com.taskflow.service.InvitationService;
import com.taskflow.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final InvitationService invitationService;

    public TeamController(TeamService teamService, InvitationService invitationService) {
        this.teamService = teamService;
        this.invitationService = invitationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(@AuthenticationPrincipal CurrentUser user,
                               @Valid @RequestBody TeamRequest request) {
        return teamService.create(user.id(), request);
    }

    /** Teams the current user belongs to. */
    @GetMapping
    public List<TeamResponse> mine(@AuthenticationPrincipal CurrentUser user) {
        return teamService.listMine(user.id());
    }

    @GetMapping("/{teamId}")
    public TeamResponse get(@AuthenticationPrincipal CurrentUser user, @PathVariable Long teamId) {
        return teamService.get(teamId, user.id());
    }

    @PutMapping("/{teamId}")
    public TeamResponse update(@AuthenticationPrincipal CurrentUser user, @PathVariable Long teamId,
                               @Valid @RequestBody TeamRequest request) {
        return teamService.update(teamId, user.id(), request);
    }

    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long teamId) {
        teamService.delete(teamId, user.id());
    }

    @GetMapping("/{teamId}/members")
    public List<MemberResponse> members(@AuthenticationPrincipal CurrentUser user, @PathVariable Long teamId) {
        return teamService.listMembers(teamId, user.id());
    }

    /** Owner removes a member, or a member removes themselves (leaves the team). */
    @DeleteMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal CurrentUser user, @PathVariable Long teamId,
                             @PathVariable Long userId) {
        teamService.removeMember(teamId, user.id(), userId);
    }

    @PostMapping("/{teamId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse invite(@AuthenticationPrincipal CurrentUser user, @PathVariable Long teamId,
                                     @Valid @RequestBody InviteRequest request) {
        return invitationService.invite(teamId, user.id(), request.email());
    }

    @GetMapping("/{teamId}/invitations")
    public List<InvitationResponse> invitations(@AuthenticationPrincipal CurrentUser user,
                                                @PathVariable Long teamId) {
        return invitationService.listForTeam(teamId, user.id());
    }
}
