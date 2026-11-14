package com.home.backend.controllers;

import com.home.backend.controllers.dtos.HomeRequest;
import com.home.backend.controllers.dtos.HomeResponse;
import com.home.backend.controllers.dtos.MemberResponse;
import com.home.backend.services.HomeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/homes")
public class HomeController {
    @Autowired
    private HomeService homeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HomeResponse createHome(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody HomeRequest request) {
        return homeService.createHome(request.name(), jwt.getSubject());
    }

    // casas em que o usuário logado é membro
    @GetMapping
    public List<HomeResponse> getHomes(@AuthenticationPrincipal Jwt jwt) {
        return homeService.getHomes(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public HomeResponse getHome(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return homeService.getHome(id, jwt.getSubject());
    }

    @PutMapping("/{id}")
    public HomeResponse updateHome(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable Long id,
                                   @Valid @RequestBody HomeRequest request) {
        return homeService.updateHome(id, request.name(), jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHome(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        homeService.deleteHome(id, jwt.getSubject());
    }

    @GetMapping("/{id}/members")
    public List<MemberResponse> getMembers(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return homeService.getMembers(id, jwt.getSubject());
    }

    // remover membro (dono) ou sair da casa (o próprio usuário)
    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal Jwt jwt,
                             @PathVariable Long id,
                             @PathVariable String userId) {
        homeService.removeMember(id, userId, jwt.getSubject());
    }
}