package com.oxymore.practice.view.player;

import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.controller.ViewController;
import com.oxymore.practice.view.ViewContext;
import org.bukkit.entity.Player;

public class PartyView extends DefaultView {
    public PartyView(ViewConfiguration viewConfiguration) {
        super(viewConfiguration);
    }

    @Override
    public void performAction(Practice plugin, Player player, String action) {
        switch (action) {
            case "party-event": {
                player.closeInventory();
                plugin.getViewPanel(player).setGUIView(ViewController.GUIViews.SELECT_EVENT, ViewContext.builder()
                        .locale(plugin.getLocale())
                        .build());
                break;
            }
            case "party-info": {
                player.performCommand("party info");
                break;
            }
            case "party-disband": {
                player.performCommand("party disband");
                break;
            }
            case "party-leave": {
                player.performCommand("party leave");
                break;
            }
            default: {
                super.performAction(plugin, player, action);
            }
        }
    }
}
