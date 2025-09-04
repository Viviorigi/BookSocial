package com.duong.gateway.service;

import com.duong.gateway.dto.ApiResponse;
import com.duong.gateway.dto.request.IntrospectRequest;
import com.duong.gateway.dto.response.IntrospectResponse;
import com.duong.gateway.repository.IdentityClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdentityService {
    IdentityClient identityClient;

    public Mono<ApiResponse<IntrospectResponse>> introspect(String token) {
        return identityClient.introspect(IntrospectRequest.builder()
                    .token(token)
                .build());
    }
}
