package com.javaproject.splitewise.controller;

import com.javaproject.splitewise.dto.request.AddExpenseRequest;
import com.javaproject.splitewise.dto.request.ExpenseSettlementRequest;
import com.javaproject.splitewise.dto.response.ApiResponse;
import com.javaproject.splitewise.enums.ApiTypeEnum;
import com.javaproject.splitewise.service.Processor;
import com.javaproject.splitewise.service.ProcessorFactory;
import com.javaproject.splitewise.validator.Validator;
import com.javaproject.splitewise.validator.ValidatoryFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class ExpenseController {

    private final ValidatoryFactory validatorFactory;
    private final ProcessorFactory processorFactory;

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<?>> addExpense(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AddExpenseRequest request
    ) {
        request.setIdempotencyKey(idempotencyKey);
        log.info("Add expense request: {}", request);

        Validator validator = validatorFactory.getValidator(ApiTypeEnum.ADD_EXPENSE.name());
        validator.validateRequest(request);
        log.info("Add expense request validation successful: {}", request);

        Processor processor = processorFactory.getProcessor(ApiTypeEnum.ADD_EXPENSE.name());
        ApiResponse<?> apiResponse = processor.processApiRequest(request);
        log.info("Add expense response validation successful: {}", apiResponse.toString());
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @PostMapping("/expenses/settle/initiate")
    public ResponseEntity<ApiResponse<?>> settleExpenses(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ExpenseSettlementRequest request
    ) {
        request.setIdempotencyKey(idempotencyKey);
        log.info("Settle expense request: groupId={} paidBy={} paidTo={} amount={}",
                request.getGroupId(), request.getPaidByEmail(), request.getPaidTo(), request.getAmount());

        Validator validator = validatorFactory.getValidator("EXPENSE_SETTLEMENT");
        validator.validateRequest(request);

        Processor processor = processorFactory.getProcessor(ApiTypeEnum.SETTLE_EXPENSE.name());
        ApiResponse<?> apiResponse = processor.processApiRequest(request);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
