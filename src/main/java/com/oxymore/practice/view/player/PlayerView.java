package com.oxymore.practice.view.player;

import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.view.AbstractView;
import com.oxymore.practice.view.View;
import com.oxymore.practice.view.ViewContext;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public abstract class PlayerView extends AbstractView implements View {
    public PlayerView(ViewConfiguration viewConfiguration) {
        super(viewConfiguration);
    }

    @Override
    public void show(Player player, ViewContext viewContext) {
        super.show(player, viewContext);
        setupInventory(player.getInventory(), viewContext);
        player.updateInventory();
    }

    @Override
    public void destroy(Player player) {
        player.getInventory().clear();
        player.updateInventory();
        super.destroy(player);
    }
}
