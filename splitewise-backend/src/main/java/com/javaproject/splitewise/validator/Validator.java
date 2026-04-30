package com.javaproject.splitewise.validator;

import com.javaproject.splitewise.dto.request.ApiRequest;

public interface Validator {

    void validateRequest(ApiRequest request);
}
