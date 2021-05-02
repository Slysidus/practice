package com.oxymore.practice.configuration.condition;

import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Condition extends Predicate<Collection<String>> {
    class Deserialize implements Deserializer<Condition> {
        private static final Pattern CONDITION_PATTERN;

        static {
            CONDITION_PATTERN = Pattern.compile("(?:\\((.*?)\\))|(!?[\\w-]+)|(?:\\s+(\\|\\||\\&\\&)\\s+)");
        }

        @Override
        public Condition deserialize(Parser parser, ConfigurationSection configuration, String path) throws DeserializeException {
            if (!configuration.isString(path)) {
                return null;
            }

            final String str = configuration.getString(path);
            try {
                return parseCondition(str);
            } catch (DeserializeException e) {
                throw e.parent("for '", str, "'");
            }
        }

        private Condition parseCondition(String str) throws DeserializeException {
            Condition conditionSum = null;
            GroupConditionOperator nextOperator = null;

            final Matcher matcher = Deserialize.CONDITION_PATTERN.matcher(str);

            String group;
            int lastEnd = 0;
            while (matcher.find()) {
                if (lastEnd != matcher.start()) {
                    final String between = str.substring(lastEnd, matcher.start());
                    throw new DeserializeException("unrecognized part in condition: '", between, "'");
                }
                lastEnd = matcher.end();

                Condition condition;
                if ((group = matcher.group(1)) != null) {
                    try {
                        condition = parseCondition(group);
                    } catch (DeserializeException e) {
                        throw e.parent("for '", group, "'");
                    }
                } else if ((group = matcher.group(2)) != null) {
                    final boolean negate = group.startsWith("!");
                    final String value = negate ? group.substring(1) : group;
                    condition = new EqualsCondition(value, negate);
                } else if ((group = matcher.group(3)) != null) {
                    switch (group) {
                        case "&&":
                            nextOperator = GroupConditionOperator.AND;
                            break;
                        case "||":
                            nextOperator = GroupConditionOperator.OR;
                            break;
                    }
                    continue;
                } else {
                    throw new IllegalStateException();
                }

                conditionSum = nextOperator != null ? new GroupCondition(conditionSum, condition, nextOperator) : condition;
                if (nextOperator != null) {
                    nextOperator = null;
                }
            }

            return conditionSum;
        }
    }
}
