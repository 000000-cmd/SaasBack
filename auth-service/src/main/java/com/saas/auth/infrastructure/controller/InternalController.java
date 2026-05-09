package com.saas.auth.infrastructure.controller;

import com.saas.auth.application.dto.event.UserEventPayload;
import com.saas.auth.domain.port.in.IUserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalController {

    private final IUserUseCase userUseCase;

    @GetMapping("/users/all")
    public List<UserEventPayload> listAllForReindex(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        log.info("Reindex fetch users: page={} size={}", page, size);
        return userUseCase.findAllPaged(page, size).stream()
                .map(UserEventPayload::from)
                .toList();
    }
    @GetMapping("/users/count")
    public Map<String, Long> countUsers() {
        return Map.of("total", userUseCase.count());
    }


}
