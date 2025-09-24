package com.duong.profile.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowSuggestionResponse {
    private SimpleUserDtoResponse user;
    private int mutualCount;
}
