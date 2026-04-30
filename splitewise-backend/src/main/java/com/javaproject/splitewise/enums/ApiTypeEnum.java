package com.javaproject.splitewise.enums;


public enum ApiTypeEnum {
    CREATE_GROUP("CREATE_GROUP"),
    ADD_MEMBER_TO_GROUP("ADD_MEMBER_TO_GROUP"),
    REMOVE_MEMBER_FROM_GROUP("REMOVE_MEMBER_FROM_GROUP"),
    GET_GROUP_DETAILS("GET_GROUP_DETAILS"),
    GET_USER_GROUPS("GET_USER_GROUPS"),
    GET_USER_BALANCES("GET_USER_BALANCES"),
    ADD_EXPENSE("ADD_EXPENSE"),
    SETTLE_EXPENSE("SETTLE_EXPENSE");
    private final String apiType;

    ApiTypeEnum(String apiType) {
        this.apiType = apiType;
    }
}
