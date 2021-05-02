package com.oxymore.practice.configuration.condition;

import lombok.Data;

import java.util.Collection;

@Data
public class EqualsCondition implements Condition {
    private final String expectValue;
    private final boolean negate;

    @Override
    public boolean test(Collection<String> values) {
        return values.contains(expectValue) != negate;
    }
}
