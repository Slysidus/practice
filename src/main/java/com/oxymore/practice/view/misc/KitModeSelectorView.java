package com.oxymore.practice.view.misc;

import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.view.match.AbstractModeSelectorView;

import java.util.List;

public final class KitModeSelectorView extends AbstractModeSelectorView {
    public KitModeSelectorView(Practice plugin, List<MatchMode> selectModes) {
        super(plugin, selectModes, "kit.selector");
    }
}
