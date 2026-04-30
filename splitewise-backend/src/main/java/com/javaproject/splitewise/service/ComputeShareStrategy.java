package com.javaproject.splitewise.service;

import com.javaproject.splitewise.dto.request.ExpenseParticipantRequest;

import java.util.List;
import java.util.Map;

public interface ComputeShareStrategy {

    Map<String, String> computeShare(List<ExpenseParticipantRequest> participants, String totalAmount);
}
