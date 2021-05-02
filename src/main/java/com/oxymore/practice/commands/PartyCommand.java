package com.oxymore.practice.commands;

import com.google.common.base.Preconditions;
import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.controller.PartyController;
import com.oxymore.practice.match.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyCommand implements CommandExecutor {
    private final Practice plugin;

    public PartyCommand(Practice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        Preconditions.checkArgument(commandSender instanceof Player, "you must be a player");

        final LocaleController locale = plugin.getLocale();
        final PartyController partyController = plugin.getPartyController();
        final Player player = (Player) commandSender;

        switch (args.length > 0 ? args[0].toLowerCase() : "") {
            case "create": {
                if (partyController.getParty(player) != null) {
                    locale.get("party.command.errors.already")
                            .send(player);
                    return true;
                }

                final Party party = new Party(player);
                partyController.getParties().put(player, party);
                locale.get("party.command.create.success")
                        .send(player);
                partyController.partyHook(player);
                break;
            }
            case "disband": {
                final Party party = partyController.getParty(player);
                if (party == null) {
                    locale.get("party.command.errors.none")
                            .send(player);
                    return true;
                }

                if (!party.getLeader().equals(player)) {
                    locale.get("party.command.errors.not-leader")
                            .send(player);
                    return true;
                }

                party.broadcast(locale.get("party.command.disband.success-alert"));
                partyController.disbandParty(party);
                break;
            }
            case "leave": {
                final Party party = partyController.getParty(player);
                if (party == null) {
                    locale.get("party.command.errors.none")
                            .send(player);
                    return true;
                }

                if (party.getLeader().equals(player)) {
                    locale.get("party.command.leave.leader")
                            .send(player);
                    return true;
                }

                partyController.getParties().remove(player)
                        .getMembers().remove(player);
                locale.get("party.command.leave.success")
                        .send(player);
                party.broadcast(locale.get("party.command.leave.success-alert")
                        .var("player", player.getName()));
                partyController.partyHook(player);
                break;
            }
            case "open": {
                final Party party = partyController.getParty(player);
                if (party == null) {
                    locale.get("party.command.errors.none")
                            .send(player);
                    return true;
                }

                if (!party.getLeader().equals(player)) {
                    locale.get("party.command.errors.not-leader")
                            .send(player);
                    return true;
                }

                if (!party.isOpen()) {
                    party.setOpen(true);
                    locale.get("party.command.open.broadcast")
                            .var("player", player.getName())
                            .send(Bukkit.getOnlinePlayers());
                } else {
                    locale.get("party.command.open.already")
                            .send(player);
                }
                break;
            }
            case "invite": {
                final Party party = partyController.getParty(player);
                if (party == null) {
                    locale.get("party.command.errors.none")
                            .send(player);
                    return true;
                }

                if (!party.getLeader().equals(player)) {
                    locale.get("party.command.errors.not-leader")
                            .send(player);
                    return true;
                }
                if (args.length < 2) {
                    locale.get("party.command.errors.missing-target");
                    return true;
                }

                final Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    locale.get("party.command.errors.no-target")
                            .send(player);
                    return true;
                }

                if (party.equals(partyController.getParty(target))) {
                    locale.get("party.command.errors.target-in-this-party")
                            .var("target", target.getName())
                            .send(player);
                    return true;
                }

                partyController.getInvites().put(target, party);
                locale.get("party.command.invite.success")
                        .var("target", target.getName())
                        .send(player);
                locale.get("party.command.invite.success-target")
                        .var("sender", player.getName())
                        .send(target);
                break;
            }
            case "info": {
                final Party party;
                if (args.length < 2) {
                    party = partyController.getParty(player);
                    if (party == null) {
                        locale.get("party.command.errors.none")
                                .send(player);
                        return true;
                    }
                } else {
                    final Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        locale.get("party.command.errors.no-target")
                                .send(player);
                        return true;
                    }

                    party = partyController.getParty(target);
                    if (party == null) {
                        locale.get("party.command.errors.target-no-party")
                                .send(player);
                        return true;
                    }
                }
                locale.get("party.command.info")
                        .var("leader", party.getLeader().getName())
                        .var("players", String.valueOf(party.getMembers().size() + 1))
                        .var("members", String.valueOf(party.getMembers().size()))
                        .send(player);
                break;
            }
            case "lead": {
                final Party party = partyController.getParty(player);
                if (party == null) {
                    locale.get("party.command.errors.none")
                            .send(player);
                    return true;
                }

                if (!party.getLeader().equals(player)) {
                    locale.get("party.command.errors.not-leader")
                            .send(player);
                    return true;
                }

                if (args.length < 2) {
                    locale.get("party.command.errors.missing-target");
                    return true;
                }

                final Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    locale.get("party.command.errors.no-target")
                            .send(player);
                    return true;
                }

                if (target.equals(player)) {
                    locale.get("party.command.errors.target-yourself")
                            .send(player);
                    return true;
                }

                if (!party.getMembers().contains(target)) {
                    locale.get("party.command.errors.target-not-in-party")
                            .var("target", target.getName())
                            .send(player);
                    return true;
                }

                party.getMembers().remove(target);
                party.setLeader(target);
                party.getMembers().add(player);
                party.broadcast(locale.get("party.command.lead.success-alert")
                        .var("new", target.getName())
                        .var("previous", player.getName()));
                plugin.getViewController().updateSpawnView(player, false);
                plugin.getViewController().updateSpawnView(target, false);
                break;
            }
            case "kick": {
                final Party party = partyController.getParty(player);
                if (party == null) {
                    locale.get("party.command.errors.none")
                            .send(player);
                    return true;
                }

                if (!party.getLeader().equals(player)) {
                    locale.get("party.command.errors.not-leader")
                            .send(player);
                    return true;
                }

                if (args.length < 2) {
                    locale.get("party.command.errors.missing-target");
                    return true;
                }

                final Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    locale.get("party.command.errors.no-target")
                            .send(player);
                    return true;
                }

                if (target.equals(player)) {
                    locale.get("party.command.errors.target-yourself")
                            .send(player);
                    return true;
                }

                if (!party.getMembers().contains(target)) {
                    locale.get("party.command.errors.target-not-in-party")
                            .var("target", target.getName())
                            .send(player);
                    return true;
                }

                party.getMembers().remove(target);
                party.broadcast(locale.get("party.command.kick.success-alert")
                        .var("target", target.getName())
                        .var("sender", player.getName()));
                locale.get("party.command.kick.success-target")
                        .var("sender", player.getName())
                        .send(target);
                partyController.partyHook(target);
                break;
            }
            case "join": {
                if (args.length < 2) {
                    locale.get("party.command.errors.missing-target");
                    return true;
                }

                if (partyController.getParty(player) != null) {
                    locale.get("party.command.errors.already")
                            .send(player);
                    return true;
                }

                final Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    locale.get("party.command.errors.no-target")
                            .send(player);
                    return true;
                }

                final Party party = partyController.getParty(target);
                if (party == null) {
                    locale.get("party.command.errors.target-no-party")
                            .send(player);
                    return true;
                }

                if (partyController.getParty(player) != null) {
                    locale.get("party.command.join.already")
                            .send(player);
                    return true;
                }

                if ((partyController.getInvites().containsKey(player) && partyController.getInvites().get(player).remove(party))
                        || party.isOpen()) {
                    party.getMembers().add(player);
                    partyController.getParties().put(player, party);
                    locale.get("party.command.join.success")
                            .send(player);
                    party.broadcast(locale.get("party.command.join.success-alert")
                            .var("player", player.getName()));
                    partyController.partyHook(player);
                } else {
                    locale.get("party.command.join.not-invited")
                            .send(player);
                }
                break;
            }
            case "help": {
                locale.get("party.command.help")
                        .var("label", label)
                        .send(player);
                break;
            }
            default: {
                locale.get("party.command.invalid-usage")
                        .var("label", label)
                        .send(player);
            }
        }

        return true;
    }
}
