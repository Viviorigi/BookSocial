package com.duong.identity.repository.httpclient;

import com.duong.identity.configuration.AuthenticationRequestInterceptor;
import com.duong.identity.dto.request.ApiResponse;
import com.duong.identity.dto.request.ProfileCreationRequest;
import com.duong.identity.dto.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name="profile-service", url="${app.services.profile}",
        configuration = {AuthenticationRequestInterceptor.class})
public interface ProfileClient {
    @PostMapping(value = "/internal/users",produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<UserProfileResponse> createProfile(@RequestBody ProfileCreationRequest profile);

}
