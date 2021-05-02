package com.oxymore.practice.view;

import com.oxymore.practice.Practice;
import org.bukkit.entity.Player;

public interface View {
    void show(Player player, ViewContext viewContext);

    void destroy(Player player);

    void performAction(Practice plugin, Player player, String action);
}
