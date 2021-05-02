package com.oxymore.practice.view.match;

import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.configuration.ui.ItemPlaceholder;
import com.oxymore.practice.controller.MatchingController;
import com.oxymore.practice.match.MatchType;
import com.oxymore.practice.match.queue.MatchQueue;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class MatchSelector extends ModeSelectorView {
    private final ModeSelectedConsumer selectCallback;

    public MatchSelector(Practice plugin, List<MatchMode> selectModes, MatchType matchType,
                         ModeSelectedConsumer selectCallback, String prefix) {
        super(plugin, selectModes, matchType, selectCallback, prefix);
        this.selectCallback = selectCallback;
    }

    public void update() {
        load();
    }

    @Override
    public ItemStack getIcon(MatchMode mode) {
        final MatchingController matchingController = plugin.getMatchingController();
        final MatchQueue<?> queue = matchingController.getQueues().get(matchType);
        final int inQueue = queue != null ? queue.count(it -> it.getMatchMode().equals(mode)) : 0;
        final int inMatch = (int) matchingController.getCurrentMatches().values().stream()
                .filter(match -> match.getMatchType() == matchType && match.getMode().equals(mode))
                .count();

        final Map<String, String> variables = new HashMap<>();
        variables.put("id", mode.id);
        variables.put("mode", mode.name);
        variables.put("mode_aux", matchType.getAux());
        variables.put("queue", String.valueOf(inQueue));
        variables.put("match", String.valueOf(inMatch));

        final ItemPlaceholder iconPlaceholder = matchType.isRanked() ?
                mode.rankedIcon : mode.unrankedIcon;
        return iconPlaceholder.buildFromVariables(plugin.getLocale(), variables);
    }
}
