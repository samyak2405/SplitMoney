package com.javaproject.splitewise.validator;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.ExpenseSettlementRequest;
import com.javaproject.splitewise.exception.custom.ApiValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@AllArgsConstructor
public class ExpenseSettlementValidator implements Validator {

    @Override
    public void validateRequest(ApiRequest apiRequest) {
        ExpenseSettlementRequest request = (ExpenseSettlementRequest) apiRequest;

        if (request.getGroupId() == null) {
            throw new ApiValidationException("group_id is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getPaidByEmail() == null || request.getPaidByEmail().isBlank()) {
            throw new ApiValidationException("paid_by_email is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getPaidTo() == null || request.getPaidTo().isBlank()) {
            throw new ApiValidationException("paid_to is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getAmount() == null || request.getAmount().isBlank()) {
            throw new ApiValidationException("amount is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new ApiValidationException("currency is required", HttpStatus.BAD_REQUEST);
        }

        try {
            BigDecimal amount = new BigDecimal(request.getAmount().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiValidationException("amount must be positive", HttpStatus.BAD_REQUEST);
            }
        } catch (NumberFormatException e) {
            throw new ApiValidationException("amount must be a valid number", HttpStatus.BAD_REQUEST);
        }

        if (request.getCurrency().trim().length() != 3) {
            throw new ApiValidationException("currency must be a 3-letter ISO code", HttpStatus.BAD_REQUEST);
        }

        if (request.getPaidByEmail().trim().equalsIgnoreCase(request.getPaidTo().trim())) {
            throw new ApiValidationException("paid_by_email and paid_to must be different users", HttpStatus.BAD_REQUEST);
        }

        String method = request.getPaymentMethod();
        if (method == null || method.isBlank()) {
            throw new ApiValidationException("payment_method is required", HttpStatus.BAD_REQUEST);
        }
        if (!method.trim().equalsIgnoreCase("CARD") && !method.trim().equalsIgnoreCase("UPI")) {
            throw new ApiValidationException("payment_method must be CARD or UPI", HttpStatus.BAD_REQUEST);
        }
    }
}
