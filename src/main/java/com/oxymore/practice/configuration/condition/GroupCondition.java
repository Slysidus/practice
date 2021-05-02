package com.oxymore.practice.configuration.condition;

import lombok.Data;

import java.util.Collection;

@Data
public class GroupCondition implements Condition {
    private final Condition lhs;
    private final Condition rhs;
    private final GroupConditionOperator operator;

    @Override
    public boolean test(Collection<String> values) {
        switch (operator) {
            case AND:
                return lhs.and(rhs).test(values);
            case OR:
                return lhs.or(rhs).test(values);
        }
        throw new IllegalStateException();
    }
}
