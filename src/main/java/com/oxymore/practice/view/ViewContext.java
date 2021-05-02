package com.oxymore.practice.view;

import com.google.common.base.Preconditions;
import com.oxymore.practice.LocaleController;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.function.Consumer;

@Data
@Builder
public class ViewContext {
    private final LocaleController locale;
    private final LocaleController.LocaleVariables variables;
    private final Collection<String> conditionValues;

    private final Consumer<Object> callback;
    private final Object data;

    public ViewContext updateCondition(String condition, boolean value) {
        Preconditions.checkNotNull(conditionValues, "trying to update condition on ctx without cond values");
        if (value && !conditionValues.contains(condition)) {
            conditionValues.add(condition);
        } else if (!value) {
            conditionValues.remove(condition);
        }
        return this;
    }
}
