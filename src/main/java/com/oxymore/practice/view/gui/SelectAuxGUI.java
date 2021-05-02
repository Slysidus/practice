package com.oxymore.practice.view.gui;

import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.match.MatchType;
import com.oxymore.practice.view.ViewContext;
import org.bukkit.entity.Player;

public class SelectAuxGUI extends GUIView {
    public SelectAuxGUI(ViewConfiguration viewConfiguration) {
        super(viewConfiguration);
    }

    @Override
    public void show(Player player, ViewContext viewContext) {
        super.show(player, viewContext);
        player.openInventory(setupInventory(viewContext));
    }

    @Override
    public void performAction(Practice plugin, Player player, String action) {
        final boolean ranked = (boolean) playerContexts.get(player.getUniqueId()).getData();

        MatchType matchType = null;
        switch (action) {
            case "1v1": {
                matchType = ranked ? MatchType.RANKED_1v1 : MatchType.UNRANKED_1v1;
                break;
            }
            case "2v2": {
                matchType = ranked ? MatchType.RANKED_2v2 : MatchType.UNRANKED_2v2;
                break;
            }
        }

        plugin.getViewPanel(player).setGUIView(null, null);
        if (matchType != null) {
            plugin.getMatchingController().getSelectors().get(matchType).open(player);
        }
    }
}
