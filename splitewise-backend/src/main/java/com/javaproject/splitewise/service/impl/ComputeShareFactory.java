package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.model.SplitType;
import com.javaproject.splitewise.service.ComputeShareStrategy;
import com.javaproject.splitewise.service.strategy.EqualShareStrategy;
import com.javaproject.splitewise.service.strategy.ExactShareStrategy;
import com.javaproject.splitewise.service.strategy.PercentageShareStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ComputeShareFactory {

    private final PercentageShareStrategy percentageShareStrategy;
    private final ExactShareStrategy exactShareStrategy;
    private final EqualShareStrategy equalShareStrategy;

    public ComputeShareStrategy getComputeShareStrategy(SplitType splitType) {
        return switch (splitType) {
            case PERCENTAGE -> percentageShareStrategy;
            case EXACT -> exactShareStrategy;
            case EQUAL -> equalShareStrategy;
            default -> throw new IllegalArgumentException("Invalid share type");
        };
    }
}
