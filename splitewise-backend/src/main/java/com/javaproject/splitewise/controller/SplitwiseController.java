package com.javaproject.splitewise.controller;

import com.javaproject.splitewise.dto.request.*;
import com.javaproject.splitewise.dto.response.ApiResponse;
import com.javaproject.splitewise.dto.response.GetGroupDetailsResponse;
import com.javaproject.splitewise.enums.ApiTypeEnum;
import com.javaproject.splitewise.exception.custom.ApiValidationException;
import com.javaproject.splitewise.security.JwtAuthenticationFilter;
import com.javaproject.splitewise.service.Processor;
import com.javaproject.splitewise.service.ProcessorFactory;
import com.javaproject.splitewise.service.impl.GetGroupMembersService;
import com.javaproject.splitewise.validator.Validator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.javaproject.splitewise.validator.ValidatoryFactory;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/splitwise")
public class SplitwiseController {

    private final ValidatoryFactory validatorFactory;
    private final ProcessorFactory processorFactory;
    private final GetGroupMembersService groupMembersService;

    @PostMapping("/v1/create-group")
    public ResponseEntity<ApiResponse<?>> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            HttpServletRequest httpRequest
    ) {
        String authenticatedEmail = getAuthenticatedUserEmail(httpRequest);
        request.setCreatedBy(UserDto.builder().email(authenticatedEmail).build());
        log.info("Create group request: {}", request);
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.CREATE_GROUP.name());
        validator.validateRequest(request);
        log.info("Create group request validation successful: {}", request);
        Processor processor = processorFactory.getProcessor(ApiTypeEnum.CREATE_GROUP.name());
        ApiResponse<?> apiResponse = processor.processApiRequest(request);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @PutMapping("/v1/add-member")
    public ResponseEntity<ApiResponse<?>> addMemberToGroup(
            @Valid @RequestBody AddMemberToGroupRequest request,
            HttpServletRequest httpRequest
    ) {
        String authenticatedEmail = getAuthenticatedUserEmail(httpRequest);
        request.setAddedBy(UserDto.builder().email(authenticatedEmail).build());
        log.info("Add member to group request: {}", request);
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.ADD_MEMBER_TO_GROUP.name());
        validator.validateRequest(request);
        log.info("Add member to group request validation successful: {}", request);
        Processor processor = processorFactory.getProcessor(ApiTypeEnum.ADD_MEMBER_TO_GROUP.name());
        ApiResponse<?> apiResponse = processor.processApiRequest(request);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @DeleteMapping("/v1/remove-member")
    public ResponseEntity<ApiResponse<?>> removeMemberFromGroup(
            @Valid @RequestBody RemoveMemberRequest request,
            HttpServletRequest httpRequest
    ) {
        String authenticatedEmail = getAuthenticatedUserEmail(httpRequest);
        request.setRemovedBy(UserDto.builder().email(authenticatedEmail).build());
        log.info("Remove member from group request: {}", request);
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.REMOVE_MEMBER_FROM_GROUP.name());
        validator.validateRequest(request);
        log.info("Remove member fro group request validation successful: {}", request);
        Processor processor = processorFactory.getProcessor(ApiTypeEnum.REMOVE_MEMBER_FROM_GROUP.name());
        ApiResponse<?> apiResponse = processor.processApiRequest(request);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/group-details")
    public ResponseEntity<ApiResponse<?>> getGroupDetails(
            @Valid @RequestBody GetGroupDetailsRequest request,
            HttpServletRequest httpRequest
    ) {
        request.setUserId(getAuthenticatedUserId(httpRequest));
        log.info("Get group details request: {}", request);
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.GET_GROUP_DETAILS.name());
        validator.validateRequest(request);
        log.info("Get group details request validation successful: {}", request);
        Processor processor = processorFactory.getProcessor(ApiTypeEnum.GET_GROUP_DETAILS.name());
        ApiResponse<?> apiResponse = processor.processApiRequest(request);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/user-groups")
    public ResponseEntity<ApiResponse<?>> getUserGroups(
            @Valid @RequestBody GetUserGroupsRequest request,
            HttpServletRequest httpRequest
    ) {
        request.setUserId(getAuthenticatedUserId(httpRequest));
        log.info("Get user groups request: {}", request);
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.GET_USER_GROUPS.name());
        validator.validateRequest(request);
        log.info("Get user groups request validation successful: {}", request);
        Processor processor = processorFactory.getProcessor(ApiTypeEnum.GET_USER_GROUPS.name());
        ApiResponse<?> apiResponse = processor.processApiRequest(request);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/user-balances")
    public ResponseEntity<ApiResponse<?>> getUserBalances(
            @Valid @RequestBody GetUserBalancesRequest request,
            HttpServletRequest httpRequest
    ) {
        request.setUserEmail(getAuthenticatedUserEmail(httpRequest));
        log.info("Get user balances request: {}", request);
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.GET_USER_BALANCES.name());
        validator.validateRequest(request);
        log.info("Get user balances request validation successful: {}", request);
        Processor processor = processorFactory.getProcessor(ApiTypeEnum.GET_USER_BALANCES.name());
        ApiResponse<?> apiResponse = processor.processApiRequest(request);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/v1/groups/{groupId}/members")
    public ResponseEntity<ApiResponse<GetGroupDetailsResponse>> getGroupMembers(
            @PathVariable Long groupId,
            HttpServletRequest httpRequest
    ) {
        UUID userId = getAuthenticatedUserId(httpRequest);
        GetGroupDetailsResponse data = groupMembersService.getMembers(groupId, userId);
        return ResponseEntity.ok(ApiResponse.<GetGroupDetailsResponse>builder()
                .success(true)
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .responseMessage("Group members fetched successfully")
                .data(data)
                .build());
    }

    private UUID getAuthenticatedUserId(HttpServletRequest request) {
        Object value = request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        throw new ApiValidationException("Authenticated user id is missing");
    }

    private String getAuthenticatedUserEmail(HttpServletRequest request) {
        Object value = request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_EMAIL_ATTR);
        if (value instanceof String email && !email.isBlank()) {
            return email.trim().toLowerCase(Locale.ROOT);
        }
        throw new ApiValidationException("Authenticated user email is missing");
    }
}
