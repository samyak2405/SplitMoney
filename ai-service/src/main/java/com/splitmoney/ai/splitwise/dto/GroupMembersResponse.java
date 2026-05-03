package com.splitmoney.ai.splitwise.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupMembersResponse {

    private boolean success;
    private Data data;

    @lombok.Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private Long groupId;
        private String groupName;
        private String currency;
        private int memberCount;
        private List<Member> members;
    }

    @lombok.Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Member {
        private String email;
    }
}
