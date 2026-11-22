package com.taskflow.web;

import com.taskflow.dto.TeamDtos.InvitationResponse;
import com.taskflow.dto.TeamDtos.TeamResponse;
import com.taskflow.security.CurrentUser;
import com.taskflow.service.InvitationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The invitee's side of team invitations. */
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /** Pending invitations addressed to the current user's email. */
    @GetMapping
    public List<InvitationResponse> pending(@AuthenticationPrincipal CurrentUser user) {
        return invitationService.listPendingForUser(user.id());
    }

    @PostMapping("/{invitationId}/accept")
    public TeamResponse accept(@AuthenticationPrincipal CurrentUser user, @PathVariable Long invitationId) {
        return invitationService.accept(invitationId, user.id());
    }

    @PostMapping("/{invitationId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@AuthenticationPrincipal CurrentUser user, @PathVariable Long invitationId) {
        invitationService.decline(invitationId, user.id());
    }
}
