package com.oxymore.practice;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocaleController {
    private static final Pattern PLACEHOLDER_PATTERN, PLACEHOLDER_LIST_PATTERN, PLACEHOLDER_RAW_PATTERN;

    static {
        PLACEHOLDER_PATTERN = Pattern.compile("(\\$)?\\{([\\w-\\.]+)\\}");
        PLACEHOLDER_LIST_PATTERN = Pattern.compile("\\$\\.([\\w-]+)=(.*)\\n?");
        PLACEHOLDER_RAW_PATTERN = Pattern.compile("\\|#\\|([^;]*?)(?:;([^;]*?))?(?:;([^;]*?))?\\|#\\|");
    }

    private final Configuration messagesConfiguration;

    public LocaleController(Configuration messagesConfiguration) {
        this.messagesConfiguration = messagesConfiguration;
    }

    public MessageContext get(String key) {
        if (!messagesConfiguration.contains(key)) {
            return new MessageContext("ERROR : message key {key} doesn't exist!")
                    .var("key", key);
        }
        return makeContext(messagesConfiguration.getString(key));
    }

    public MessageContext makeContext(String str) {
        return new MessageContext(str);
    }

    public String applyCtx(String str, LocaleVariables variables) {
        if (variables == null) {
            return new MessageContext(str).toString();
        }
        final Map<String, String> strVars = variables.variables != null ? variables.variables : new HashMap<>();
        final Map<String, Collection<ExpansionElement>> expansions = variables.expansions != null ?
                variables.expansions : new HashMap<>();
        return new MessageContext(str, strVars, expansions).toString();
    }

    public BaseComponent[] maybeParseRaw(String message) {
        final Matcher rawMatcher = LocaleController.PLACEHOLDER_RAW_PATTERN.matcher(message);
        if (!rawMatcher.find()) {
            return null;
        }

        rawMatcher.reset();
        final List<BaseComponent> components = new ArrayList<>();
        boolean finalIt = false;
        int lastEnd = 0;
        while (rawMatcher.find() || (finalIt = lastEnd + 1 < message.length())) {
            final String between = message.substring(lastEnd, !finalIt ? rawMatcher.start() : message.length());
            if (!between.isEmpty()) {
                components.addAll(Arrays.asList(TextComponent.fromLegacyText(between)));
            }
            if (finalIt) {
                break;
            }

            final TextComponent component = new TextComponent(TextComponent.fromLegacyText(rawMatcher.group(1)));

            final String command = rawMatcher.group(2);
            final String hoverMessage = rawMatcher.group(3);
            if (command != null) {
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
            }
            if (hoverMessage != null) {
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hoverMessage)));
            }

            components.add(component);
            lastEnd = rawMatcher.end();
        }

        return components.toArray(new BaseComponent[0]);
    }

    @AllArgsConstructor
    public class MessageContext {
        private final String message;
        private final Map<String, String> variables;
        private final Map<String, Collection<ExpansionElement>> expansions;

        public MessageContext(String message, Map<String, String> variables) {
            this.message = message;
            this.variables = variables;
            this.expansions = new HashMap<>();
        }

        public MessageContext(String message) {
            this.message = message;
            this.variables = new HashMap<>();
            this.expansions = new HashMap<>();
        }

        public MessageContext var(String var, String value) {
            if (value != null) {
                variables.put(var, value);
            }
            return this;
        }

        public MessageContext expansion(String var, Collection<ExpansionElement> expansion) {
            if (expansion != null) {
                expansions.put(var, expansion);
            }
            return this;
        }

        public void send(CommandSender... senders) {
            send(Arrays.asList(senders));
        }

        public void send(Iterable<? extends CommandSender> senders) {
            final String result = toString();
            final BaseComponent[] baseComponents = LocaleController.this.maybeParseRaw(result);
            if (baseComponents != null) {
                for (CommandSender sender : senders) {
                    if (sender instanceof Player) {
                        ((Player) sender).spigot().sendMessage(baseComponents);
                    } else {
                        sender.sendMessage(new TextComponent(baseComponents).toLegacyText());
                    }
                }
            } else {
                for (CommandSender sender : senders) {
                    sender.sendMessage(result);
                }
            }
        }

        @Override
        public String toString() {
            if (message == null) {
                return null;
            }

            // delete expansion templates lines
            final StringBuffer contentBuffer = new StringBuffer();
            Map<String, String> listExpansions = null;
            final Matcher listExpansionMatcher = LocaleController.PLACEHOLDER_LIST_PATTERN.matcher(message);
            while (listExpansionMatcher.find()) {
                if (listExpansions == null) {
                    listExpansions = new HashMap<>();
                }
                listExpansions.put(listExpansionMatcher.group(1), listExpansionMatcher.group(2));
                listExpansionMatcher.appendReplacement(contentBuffer, "");
            }
            listExpansionMatcher.appendTail(contentBuffer);

            // build message
            final Matcher placeholdersMatcher = LocaleController.PLACEHOLDER_PATTERN.matcher(contentBuffer.toString());
            final StringBuffer resultBuffer = new StringBuffer();
            while (placeholdersMatcher.find()) {
                String replacement;
                final String placeholder = placeholdersMatcher.group(2);
                if (placeholdersMatcher.group(1) != null) { // is template
                    final String message = LocaleController.this.messagesConfiguration.getString(placeholder);
                    replacement = new MessageContext(message, variables, expansions).toString();
                } else if (expansions.containsKey(placeholder)) {
                    if (listExpansions == null) {
                        continue;
                    }

                    final String expansionTemplate = listExpansions.get(placeholder);
                    if (expansionTemplate != null) {
                        final StringBuilder expansionResult = new StringBuilder();
                        final Iterator<ExpansionElement> expansion = expansions.get(placeholder).iterator();
                        while (expansion.hasNext()) {
                            final ExpansionElement expansionElement = expansion.next();
                            final MessageContext expansionContext = new MessageContext(expansionTemplate);
                            final String[] expandedSplit = expansionElement.expand(expansionContext).toString()
                                    .split("<,>", 2);
                            if (expandedSplit.length == 0) {
                                continue;
                            }
                            expansionResult.append(expandedSplit[0]);

                            if (expandedSplit.length > 1 && expansion.hasNext()) {
                                expansionResult.append(expandedSplit[1]);
                            }
                        }
                        replacement = expansionResult.toString();
                    } else {
                        replacement = null;
                    }
                } else {
                    replacement = variables.get(placeholder);
                }

                if (replacement == null) {
                    replacement = "";
                }
                placeholdersMatcher.appendReplacement(resultBuffer, replacement);
            }
            placeholdersMatcher.appendTail(resultBuffer);

            return ChatColor.translateAlternateColorCodes('&', resultBuffer.toString())
                    .replace("\\n", "\n");
        }
    }

    public interface ExpansionElement {
        MessageContext expand(MessageContext ctx);
    }

    @Data
    public static class LocaleVariables {
        private final Map<String, String> variables;
        private final Map<String, Collection<ExpansionElement>> expansions;
    }
}
