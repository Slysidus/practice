package com.oxymore.practice.view.match;

import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.match.MatchType;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.List;

@Getter
public class ModeSelectorView extends AbstractModeSelectorView {
    protected final MatchType matchType;
    private final ModeSelectedConsumer callback;

    public ModeSelectorView(Practice plugin, List<MatchMode> selectModes, MatchType matchType, ModeSelectedConsumer callback, String prefix) {
        super(plugin, selectModes, prefix);
        this.matchType = matchType;
        this.callback = callback;
    }

    public ModeSelectorView(Practice plugin, List<MatchMode> selectModes, MatchType matchType, ModeSelectedConsumer callback) {
        this(plugin, selectModes, matchType, callback, "mode.selector");
    }

    public interface ModeSelectedConsumer {
        void onModeSelect(Player player, MatchType matchType, MatchMode matchMode, String arena);
    }
}
