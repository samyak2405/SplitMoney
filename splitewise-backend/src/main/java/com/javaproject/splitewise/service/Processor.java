package com.javaproject.splitewise.service;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.CreateGroupRequest;
import com.javaproject.splitewise.dto.response.ApiResponse;

public interface Processor {

    public ApiResponse processApiRequest(ApiRequest request);
}
