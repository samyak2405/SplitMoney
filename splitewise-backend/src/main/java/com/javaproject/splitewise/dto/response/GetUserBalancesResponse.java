package com.javaproject.splitewise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class GetUserBalancesResponse {
    private String groupName;
    private String userEmail;
    private String currency;
    private String totalToReceive;
    private String totalToPay;
    private Integer membersWhoNeedToPayUserCount;
    private Integer membersUserNeedsToPayCount;
    private List<UserBalanceDetailResponse> membersWhoNeedToPayUser;
    private List<UserBalanceDetailResponse> membersUserNeedsToPay;
}
