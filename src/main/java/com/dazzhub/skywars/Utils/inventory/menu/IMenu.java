package com.dazzhub.skywars.Utils.inventory.menu;

import com.dazzhub.skywars.Arena.Arena;
import com.dazzhub.skywars.Listeners.Custom.LeftEvent;
import com.dazzhub.skywars.Main;
import com.dazzhub.skywars.MySQL.utils.GamePlayer;
import com.dazzhub.skywars.Utils.Enums;
import com.dazzhub.skywars.Utils.inventory.actions.OptionClickEvent;
import com.dazzhub.skywars.Utils.inventory.actions.OptionClickEventHandler;
import com.dazzhub.skywars.Utils.inventory.ordItems;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Collection;
import java.util.HashMap;

@Getter
@Setter
public class IMenu {

    private Main main;
    private Inventory inv;

    private String name;
    private Integer rows;
    private String command;

    private HashMap<Integer, ordItems> itemsList;
    private OptionClickEventHandler handler;
    private BukkitScheduler scheduler;

    public IMenu(String name, Integer rows, String command, HashMap<Integer, ordItems> items) {
        this.main = Main.getPlugin();
        this.name = name;
        this.rows = rows;
        this.command = command;
        this.itemsList = items;

        this.scheduler = main.getServer().getScheduler();

        this.handler = (event -> {
            Player player = event.getPlayer();
            String target = event.getTarget();
            String cmd = event.getCmd();
            if (cmd == null || cmd.equals("")) {
                return;
            }
            scheduler.runTaskLater(main, () -> {
                if (cmd.contains(";")) {
                    String[] array = cmd.split(";");
                    String[] array2;
                    for (int length = (array2 = array).length, i = 0; i < length; ++i) {
                        String sub = array2[i];
                        if (sub.startsWith(" ")) {
                            sub = sub.substring(1);
                        }
                        parseCommand(player, target, sub);
                    }
                }
                else {
                    parseCommand(player, target, cmd);
                }
            }, 2L);
        });

    }

    public void open(Player p, String target){
        if (target == null){
            this.inv = Bukkit.createInventory(p, rows*9, c(name));
        } else {
            this.inv = Bukkit.createInventory(p, rows*9, c(name + "/" + target));
        }

        main.getPlayerManager().getPlayer(p.getUniqueId()).setTaskId(new BukkitRunnable() {
            @Override
            public void run() {
                itemsList.values().forEach(item -> inv.setItem(item.getSlot(), hideAttributes(item.getIcon().build(p))));
            }
        }.runTaskTimerAsynchronously(main, 0,10).getTaskId());

        p.openInventory(inv);
    }


    public void onInventoryClick(InventoryClickEvent event) {

        int slot = event.getRawSlot();
        Player p = (Player) event.getWhoClicked();

        if (p == null) {
            return;
        }

        GamePlayer gamePlayer = main.getPlayerManager().getPlayer(p.getUniqueId());

        if (event.getView().getTitle().equalsIgnoreCase(inv.getTitle())) {
            ordItems ordItems = itemsList.get(slot);
            if (ordItems == null) return;

            if (slot == ordItems.getSlot()) {
                OptionClickEvent e = new OptionClickEvent(p, inv.getTitle().contains("/") ? inv.getTitle().split("/")[1] : null, ordItems.getIcon(), slot, ordItems.getCommand(), ordItems.getPermission(), ordItems.getInteract());
                if (hasPerm(p, e)) {
                    this.handler.onOptionClick(e);
                } else {
                    gamePlayer.sendMessage(c(gamePlayer.getLangMessage().getString("Messages.menu-deny")));
                }
                Bukkit.getScheduler().runTaskLater(main, p::closeInventory, 1L);
            }
        }
    }

    public void closeInv(Player p){
        Bukkit.getScheduler().cancelTask(main.getPlayerManager().getPlayer(p.getUniqueId()).getTaskId());
    }

    public boolean hasPerm(Player player, OptionClickEvent e) {
        return e.getPermission() == null || e.getPermission().length() == 0 || player.hasPermission(e.getPermission());
    }

    public ItemStack hideAttributes(ItemStack item) {
        if (item == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (isNullOrEmpty(meta.getItemFlags())) {
            if (!meta.hasEnchants()) {
                meta.addItemFlags(ItemFlag.values());
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private boolean isNullOrEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    private void parseCommand(Player p, String target, String cmd) {
        if (cmd != null && !cmd.equals("")) {

            if (cmd.contains("%player%")) {
                cmd = cmd.replace("%player%", p.getName());
            }

            if (cmd.contains("%target%") && target != null) {
                cmd = cmd.replace("%target%", target);
            }

            GamePlayer gamePlayer = main.getPlayerManager().getPlayer(p.getUniqueId());

            if (cmd.startsWith("console:")) {
                String consoleCommand = cmd.substring(8);
                if (consoleCommand.startsWith(" ")) {
                    consoleCommand = consoleCommand.substring(1);
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCommand);
            } else if (cmd.startsWith("player:")) {
                String playerCmd = cmd.substring(7);
                if (playerCmd.startsWith(" ")) {
                    playerCmd = playerCmd.substring(1);
                }
                p.chat(playerCmd);
            } else if (cmd.startsWith("open:")) {
                String openCommand = cmd.substring(5);
                if (openCommand.startsWith(" ")) {
                    openCommand = openCommand.substring(1);
                }

                IMenu menu = main.getMenuManager().getMenuFileName().get(openCommand);
                if (menu == null) return;

                menu.open(p, target);
            } else if (cmd.startsWith("buycage:")) {
                String action = cmd.substring(8);
                if (action.startsWith(" ")) {
                    action = action.substring(1);
                }

                String[] actioncage = action.split("/");

                String cage = actioncage[0];
                int price = Integer.parseInt(actioncage[1]);
                String mode = actioncage[2];

                if (mode.equalsIgnoreCase("SOLO")) {
                    System.out.println(main.getCageManager().getCagesSolo().containsKey(cage));
                    if (main.getCageManager().getCagesSolo().containsKey(cage)) {
                        if (!gamePlayer.getCagesSoloList().contains(cage)) {
                            if (gamePlayer.getCoins() >= price) {
                                gamePlayer.getCagesSoloList().add(cage);
                                gamePlayer.setCageSolo(cage);
                                gamePlayer.removeCoins(price);
                                gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Cage.Buy").replace("%cage%", cage));

                                if (gamePlayer.isInArena()) {
                                    main.getCageManager().getCagesSolo().get(gamePlayer.getCageSolo()).loadCage(gamePlayer.getArenaTeam().getSpawn());
                                }

                            } else {
                                gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Cage.InsufficientCoins").replace("%coins%", String.valueOf(price)));
                            }
                        } else {
                            if (!gamePlayer.getCageSolo().equals(cage)) {
                                gamePlayer.setCageSolo(cage);
                                gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Cage.Selected").replace("%cage%", cage));

                                if (gamePlayer.isInArena()) {
                                    main.getCageManager().getCagesSolo().get(gamePlayer.getCageSolo()).loadCage(gamePlayer.getArenaTeam().getSpawn());
                                }
                            }
                        }
                    }
                } else if (mode.equalsIgnoreCase("TEAM")) {
                    if (main.getCageManager().getCagesTeam().containsKey(cage)) {
                        if (!gamePlayer.getCagesTeamList().contains(cage)) {
                            if (gamePlayer.getCoins() >= price) {
                                gamePlayer.getCagesTeamList().add(cage);
                                gamePlayer.setCageTeam(cage);
                                gamePlayer.removeCoins(price);
                                gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Cage.Buy").replace("%cage%", cage));

                                if (gamePlayer.isInArena()) {
                                    main.getCageManager().getCagesTeam().get(gamePlayer.getCageTeam()).loadCage(gamePlayer.getArenaTeam().getSpawn());
                                }
                            } else {
                                gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Cage.InsufficientCoins").replace("%coins%", String.valueOf(price)));
                            }
                        } else {
                            if (!gamePlayer.getCageTeam().equals(cage)) {
                                gamePlayer.setCageTeam(cage);
                                gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Cage.Selected").replace("%cage%", cage));

                                if (gamePlayer.isInArena()) {
                                    main.getCageManager().getCagesTeam().get(gamePlayer.getCageTeam()).loadCage(gamePlayer.getArenaTeam().getSpawn());
                                }
                            }
                        }
                    }
                }
            } else if (cmd.startsWith("kit:")) {
                String action = cmd.substring(4);
                if (action.startsWith(" ")) {
                    action = action.substring(1);
                }

                String[] kit = action.split("/");

                if (kit[1].equalsIgnoreCase("SOLO")) {
                    if (main.getiKitManager().getKitSoloHashMap().containsKey(kit[0].toLowerCase())) {

                        if (!gamePlayer.getKitSoloList().contains(kit[0])) {

                            int coins = Integer.parseInt(cmd.split("/")[2]);
                            if (gamePlayer.getCoins() >= coins) {
                                gamePlayer.getKitSoloList().add(kit[0]);
                                gamePlayer.setKitSolo(kit[0]);
                                gamePlayer.removeCoins(coins);
                                gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Kit.Buy").replace("%kit%", kit[0]));
                            } else {
                                gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Kit.InsufficientCoins").replace("%coins%", String.valueOf(coins)));
                            }
                        } else {
                            gamePlayer.setKitSolo(kit[0]);
                            gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Kit.Selected").replace("%kit%", kit[0]));
                        }

                    }
                } else if (kit[1].equalsIgnoreCase("TEAM")) {
                    if (main.getiKitManager().getKitTeamHashMap().containsKey(kit[0].toLowerCase())) {

                        if (!gamePlayer.getKitTeamList().contains(kit[0])) {
                            int coins = Integer.parseInt(cmd.split("/")[2]);
                            if (gamePlayer.getCoins() >= coins) {
                                gamePlayer.getKitTeamList().add(kit[0]);
                                gamePlayer.setKitTeam(kit[0]);
                                gamePlayer.removeCoins(coins);
                                gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Kit.Buy").replace("%kit%", kit[0]));
                            } else {
                                gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Kit.InsufficientCoins").replace("%coins%", String.valueOf(coins)));
                            }
                        } else {
                            gamePlayer.setKitTeam(kit[0]);
                            gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.Kit.Selected").replace("%kit%", kit[0]));
                        }

                    }
                }

            } else if (cmd.startsWith("vote:")) {
                String vote = cmd.substring(5);
                if (vote.startsWith(" ")) {
                    vote = vote.substring(1);
                }

                vote = vote.toUpperCase();

                if (gamePlayer.isInArena()) {
                    Arena arena = gamePlayer.getArena();

                    if (!arena.getVotesSystem().containsVote(p, vote.replace("%", ""))) {
                        arena.getVotesSystem().addVote(p, Enums.TypeVotes.valueOf(vote.replace("%", "")));

                        for (GamePlayer game : arena.getPlayers()) {
                            Configuration lang = game.getLangMessage();

                            game.sendMessage(lang.getString("Messages.TypeVote.VoteFor")

                                    .replace("%player%", p.getName())
                                    .replace("%vote%", vote)

                                    .replace("%BASIC%", lang.getString("Messages.TypeVote.Chest.basic", "basic"))
                                    .replace("%NORMAL%", lang.getString("Messages.TypeVote.Chest.normal", "normal"))
                                    .replace("%OP%", lang.getString("Messages.TypeVote.Chest.op", "op"))

                                    .replace("%DAY%", lang.getString("Messages.TypeVote.Time.day", "day"))
                                    .replace("%SUNSET%", lang.getString("Messages.TypeVote.Time.sunset", "sunset"))
                                    .replace("%NIGHT%", lang.getString("Messages.TypeVote.Time.night", "night"))

                                    .replace("%HEART10%", lang.getString("Messages.TypeVote.Heart.10h", "10 hearts"))
                                    .replace("%HEART20%", lang.getString("Messages.TypeVote.Heart.20h", "20 hearts"))
                                    .replace("%HEART30%", lang.getString("Messages.TypeVote.Heart.30h", "30 hearts"))

                                    .replace("%votes%", String.valueOf(arena.getVotesSystem().getVotes(vote.replace("%", ""))))
                                    .replace("%NONE%", lang.getString("Messages.TypeVote.none"))
                            );

                        }
                    } else {
                        gamePlayer.sendMessage(gamePlayer.getLangMessage().getString("Messages.TypeVote.AlreadyVote"));
                    }
                }
            } else if (cmd.startsWith("leave")) {
                if (gamePlayer.isInArena()) {
                    Arena arena = gamePlayer.getArena();
                    LeftEvent leftEvent = new LeftEvent(p, arena, Enums.LeftCause.INTERACT);
                    Bukkit.getPluginManager().callEvent(leftEvent);
                }
            }
        }
    }

    private String c(String c) {
        return ChatColor.translateAlternateColorCodes('&', c);
    }

}
