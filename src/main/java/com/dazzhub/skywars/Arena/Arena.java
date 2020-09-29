package com.dazzhub.skywars.Arena;

import com.dazzhub.skywars.Arena.Menu.SpectatorMenu;
import com.dazzhub.skywars.Listeners.Custom.typeJoin.addPlayerEvent;
import com.dazzhub.skywars.Listeners.Custom.typeJoin.addSpectatorEvent;
import com.dazzhub.skywars.Listeners.Custom.typeJoin.removePlayerEvent;
import com.dazzhub.skywars.Listeners.Custom.typeJoin.removeSpectatorEvent;
import com.dazzhub.skywars.Main;
import com.dazzhub.skywars.MySQL.utils.GamePlayer;
import com.dazzhub.skywars.Runnables.RefillGame;
import com.dazzhub.skywars.Runnables.endGame;
import com.dazzhub.skywars.Runnables.inGame;
import com.dazzhub.skywars.Runnables.startingGame;
import com.dazzhub.skywars.Utils.Console;
import com.dazzhub.skywars.Utils.Cuboid;
import com.dazzhub.skywars.Utils.Enums;
import com.dazzhub.skywars.Utils.locUtils;
import com.dazzhub.skywars.Utils.signs.arena.ISign;
import com.dazzhub.skywars.Utils.vote.VotesSystem;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@Setter
public class Arena {

    private Main main;

    /* Arena */
    private String nameArena;
    private String nameWorld;

    private Enums.GameStatus gameStatus;
    private Enums.Mode mode;

    /* Checks */
    private boolean isUsable;

    private int StartingGame;
    private int FinishedGame;
    private int DurationGame;

    /* Player */
    private List<GamePlayer> players;
    private List<GamePlayer> spectators;

    private int minPlayers;
    private int maxPlayers;

    /* MENU SPECTATOR */
    private SpectatorMenu spectatorMenu;

    /* Locations */
    private List<ArenaTeam> spawns;
    private Location spawnSpectator;

    private List<Location> islandChest;
    private List<Location> centerChest;
    private List<Location> centerChestCheck;

    private List<Location> chestsAddInGame;

    /* RUNNABLE */
    private startingGame startingGameTask;
    private inGame inGameTask;
    private endGame endGameTask;

    /* TYPE VOTES */
    private String chestType;
    private Enums.TypeVotes healthType;
    private Enums.TypeVotes timeType;
    private Enums.TypeVotes eventsType;
    private Enums.TypeVotes scenariosType;
    private VotesSystem votesSystem;

    /* SING */
    private ISign iSign;

    /* REFILL */
    private RefillGame refillGame;
    private List<Integer> refillTime;

    /* TOP KILLERS */
    private HashMap<String, Integer> killers;

    private Configuration arenac;

    private boolean damageFallStarting;

    public Arena(String nameArena) {
        this.main = Main.getPlugin();

        /* Arena */
        this.nameArena = nameArena;

        /* Player */
        this.players = new ArrayList<>();
        this.spectators = new ArrayList<>();
        this.spawns = new ArrayList<>();

        /* Checks */
        this.gameStatus = Enums.GameStatus.DISABLED;
        this.isUsable = false;

        /* MENU SPECTATOR */
        this.spectatorMenu = new SpectatorMenu(this);

        /* LOCATIONS */
        this.islandChest = new ArrayList<>();
        this.centerChest = new ArrayList<>();
        this.centerChestCheck = new ArrayList<>();
        this.chestsAddInGame = new ArrayList<>();

        /* TYPE VOTES */
        this.chestType = Enums.TypeVotes.NORMAL.name();
        this.healthType = Enums.TypeVotes.HEART10;
        this.timeType = Enums.TypeVotes.DAY;
        this.votesSystem = new VotesSystem(this);

        /* REFILL*/
        this.refillGame = null;
        this.refillTime = new ArrayList<>();

        /* TOP KILLERS */
        this.killers = new HashMap<>();

        this.arenac = main.getConfigUtils().getConfig(main, "Arenas/" + nameArena + "/Settings");
        if (arenac != null) {
            this.nameWorld = arenac.getString("Arena.world");

            this.minPlayers = arenac.getInt("Arena.minPlayer");
            this.maxPlayers = arenac.getInt("Arena.maxPlayer");

            this.DurationGame = arenac.getInt("Arena.durationGame");
            this.StartingGame = arenac.getInt("Arena.startingGame");
            this.FinishedGame = arenac.getInt("Arena.finishedGame");

            this.iSign = main.getiSignManager().loadSign(this);

            this.refillTime.addAll(arenac.getIntegerList("Arena.refill"));

            this.mode = Enums.Mode.valueOf(arenac.getString("Arena.mode"));
        }

        /* RUNNABLE */
        this.startingGameTask = new startingGame(this);
        this.inGameTask = new inGame(this);
        this.endGameTask = new endGame(this);

        this.damageFallStarting = true;

        this.main.getResetWorld().importWorld(this, true);
    }

    public void resetArena(){
        /* Player */
        this.players = new ArrayList<>();
        this.spectators = new ArrayList<>();

        /* Checks */
        this.gameStatus = Enums.GameStatus.DISABLED;
        this.isUsable = true;

        /* MENU SPECTATOR */
        this.spectatorMenu = new SpectatorMenu(this);

        /* LOCATIONS */
        this.islandChest = new ArrayList<>();
        this.centerChest = new ArrayList<>();
        this.centerChestCheck = new ArrayList<>();
        this.chestsAddInGame = new ArrayList<>();

        /* RUNNABLE */
        this.startingGameTask = new startingGame(this);
        this.inGameTask = new inGame(this);
        this.endGameTask = new endGame(this);

        /* TYPE VOTES */
        this.chestType = Enums.TypeVotes.NORMAL.name();
        this.healthType = Enums.TypeVotes.HEART10;
        this.timeType = Enums.TypeVotes.DAY;
        this.votesSystem = new VotesSystem(this);

        /* REFILL */
        this.refillGame = null;
        this.refillTime = new ArrayList<>();

        if (arenac != null) {
            this.refillTime.addAll(arenac.getIntegerList("Arena.refill"));
        }

        /* TOP KILLERS */
        this.killers = new HashMap<>();

        this.damageFallStarting = true;

        this.main.getResetWorld().importWorld(this, false);
    }

    public void addPlayer(GamePlayer gamePlayer) {
        Bukkit.getScheduler().runTaskAsynchronously(main, () -> Bukkit.getPluginManager().callEvent(new addPlayerEvent(gamePlayer, this)));
        if (iSign != null) iSign.updateSign();
    }

    public void removePlayer(GamePlayer gamePlayer) {
        Bukkit.getScheduler().runTaskAsynchronously(main, () -> Bukkit.getPluginManager().callEvent(new removePlayerEvent(gamePlayer, this)));
        if (iSign != null) iSign.updateSign();
    }

    public void addSpectator(GamePlayer gamePlayer) {
        Bukkit.getScheduler().runTaskAsynchronously(main, () -> Bukkit.getPluginManager().callEvent(new addSpectatorEvent(gamePlayer, this)));
    }

    public void removeSpectator(GamePlayer gamePlayer, boolean goLobby) {
        Bukkit.getScheduler().runTaskAsynchronously(main, () -> Bukkit.getPluginManager().callEvent(new removeSpectatorEvent(gamePlayer, goLobby, this)));
    }

    public ArenaTeam getAvaibleTeam(int n) {
        return this.spawns.stream().filter(arenaTeam -> arenaTeam.getMembers().size() + n <= this.mode.getSize()).findFirst().orElse(null);
    }

    public List<ArenaTeam> getAliveTeams() {
        return this.spawns.stream().filter(gameTeam -> gameTeam.getAliveTeams().size() > 0).collect(Collectors.toList());
    }

    public ArenaTeam getRandomTeam() {
        List<ArenaTeam> teams = new ArrayList<>();
        for (ArenaTeam gameTeam : this.spawns) {
            if (!gameTeam.isFull()) {
                if (gameTeam.getMembers().size() >= 1) {
                    teams.add(gameTeam);
                    break;
                } else {
                    return getAvaibleTeam(getMode().getSize());
                }
            }
        }
        return teams.get(new Random().nextInt(teams.size()));
    }

    public int getHighest(Collection<Integer> collection, int n) {
        int n2 = 0;
        for (int intValue : collection) {
            if (intValue > n2 && intValue < n) {
                n2 = intValue;
            }
        }
        return n2;
    }

    public void checkVotes() {
        World world = Bukkit.getWorld(this.nameWorld);
        this.votesSystem.setTypes();

        switch (this.timeType) {
            case DAY: {
                world.setTime(1000L);
                break;
            }
            case SUNSET: {
                world.setTime(12000L);
                break;
            }
            case NIGHT: {
                world.setTime(14000L);
                break;
            }
        }

        switch (this.healthType) {
            case HEART10: {
                for (GamePlayer gamePlayer : this.players) {
                    gamePlayer.getPlayer().setHealthScale(20.0);
                }
                break;
            }
            case HEART20: {
                for (GamePlayer gamePlayer : this.players) {
                    gamePlayer.getPlayer().setHealthScale(40.0);
                }
                break;
            }
            case HEART30: {
                for (GamePlayer gamePlayer : this.players) {
                    gamePlayer.getPlayer().setHealthScale(60.0);
                }
                break;
            }
        }

    }

    public void fillChests() {
        switch (this.chestType) {
            case "BASIC": {
                getChests("BASIC");
                break;
            }
            case "NORMAL": {
                getChests("NORMAL");
                break;
            }
            case "OP": {
                getChests("OP");
                break;
            }
        }
    }

    private void getChests(String type) {
        for (Location location : this.islandChest) {
            if (location.getBlock().getType().equals(Material.CHEST)) {
                Chest chest = (Chest) location.getBlock().getState();
                main.getChestManager().getChestHashMap().get(type).refillChest(chest);
            }
        }
        for (Location location : this.centerChest) {
            if (location.getBlock().getType().equals(Material.CHEST)) {
                Chest chest = (Chest) location.getBlock().getState();
                main.getChestManager().getChestHashMap().get("CENTER").refillChest(chest);
            }
        }
    }

    public void getWinners(GamePlayer gamePlayer) {
        List<String> message1 = gamePlayer.getLangMessage().getStringList("Messages.WinnerGame");
        List<String> message = new ArrayList<>();
        List<String> winners = new ArrayList<>();

        if (this.killers.size() <= 2) {
            this.killers.put("NONE", 0);
        }

        for (GamePlayer winnersPlayers : this.players) {
            winners.add(winnersPlayers.getName());
        }

        String win = winners.toString().replace("[", "").replace("]", "");

        try {
            SortedMap<String, Integer> sortedMap = ImmutableSortedMap.copyOf(killers, Ordering.natural().reverse().onResultOf(Functions.forMap(killers)).compound(Ordering.natural().reverse()));

            String kills = String.valueOf(sortedMap.values()).replace("[", "").replace("]", "");
            String player = String.valueOf(sortedMap.keySet()).replace("[", "").replace("]", "");

            for (String s : message1) {
                message.add(s
                        .replace("%winner%", win)

                        .replace("%player1%", player.split(",")[0])
                        .replace("%player2%", player.split(", ")[1])
                        .replace("%player3%", player.split(", ")[2])

                        .replace("%kills1%", kills.split(",")[0])
                        .replace("%kills2%", kills.split(", ")[1])
                        .replace("%kills3%", kills.split(", ")[2]));
            }

            Bukkit.getScheduler().runTaskLater(main, () -> gamePlayer.sendMessage(message), 5);

        } catch (Exception e) {
            Console.warning("Win message no work");
        }
    }

    public void removeCage(GamePlayer gamePlayer, Enums.Mode mode, int yp) {
        Location loc = gamePlayer.getArenaTeam().getSpawn();

        Location point1 = null;
        Location point2 = null;

        if (mode.equals(Enums.Mode.SOLO)){
            int radius = 1;
            point1 = new Location(loc.getWorld(), loc.getX() + radius, loc.getY() + yp, loc.getZ() + radius);
            point2 = new Location(loc.getWorld(), loc.getX() - radius, loc.getY() - yp, loc.getZ() - radius);
        } else if (mode.equals(Enums.Mode.TEAM)) {
            int radius = 5;
            point1 = new Location(loc.getWorld(), loc.getX() + radius, loc.getY() + yp, loc.getZ() + radius);
            point2 = new Location(loc.getWorld(), loc.getX() - radius, loc.getY() - yp, loc.getZ() - radius);
        }

        if (point1 == null) return;

        Cuboid cuboid = new Cuboid(point1, point2);

        for (Block block : cuboid){
            block.setType(Material.AIR);
        }
    }

    public boolean checkUsable() {
        return getGameStatus().equals(Enums.GameStatus.WAITING) || getGameStatus().equals(Enums.GameStatus.STARTING) || !getGameStatus().equals(Enums.GameStatus.INGAME) && !getGameStatus().equals(Enums.GameStatus.DISABLED) && !isUsable() && getPlayers().size() < getMaxPlayers();
    }

    public boolean checkStart() {
        return this.players.size() >= this.minPlayers;
    }

    public String getStatusMsg() {
        Configuration config = main.getSigns();
        if (this.gameStatus.equals(Enums.GameStatus.WAITING)) {
            return this.c(config.getString("Status.Waiting.msg"));
        }
        if (this.gameStatus.equals(Enums.GameStatus.STARTING)) {
            return this.c(config.getString("Status.Starting.msg"));
        }
        if (this.gameStatus.equals(Enums.GameStatus.INGAME)) {
            return this.c(config.getString("Status.InGame.msg"));
        }
        if (this.gameStatus.equals(Enums.GameStatus.RESTARTING)) {
            return this.c(config.getString("Status.Restarting.msg"));
        }
        if (this.players.size() >= this.maxPlayers) {
            return this.c(config.getString("Status.Full.msg"));
        }

        return "Loading...";
    }

    public XMaterial getBlockStatus() {
        Configuration config = main.getSigns();
        if (gameStatus.equals(Enums.GameStatus.WAITING)) {
            ItemStack item;
            if (main.checkVersion()){
                item = new ItemStack(Material.getMaterial(config.getString("Status.Waiting.material")),1, (short) config.getInt("Status.Waiting.id"));
            } else {
                item = new ItemStack(Material.getMaterial(config.getString("Status.Waiting.material")));
            }
            return XMaterial.matchXMaterial(item);
        } else if (gameStatus.equals(Enums.GameStatus.STARTING)) {
            ItemStack item;
            if (main.checkVersion()){
                item = new ItemStack(Material.getMaterial(config.getString("Status.Starting.material")),1, (short) config.getInt("Status.Starting.id"));
            } else {
                item = new ItemStack(Material.getMaterial(config.getString("Status.Starting.material")));
            }
            return XMaterial.matchXMaterial(item);
        } else if (gameStatus.equals(Enums.GameStatus.INGAME)) {
            ItemStack item;
            if (main.checkVersion()){
                item = new ItemStack(Material.getMaterial(config.getString("Status.InGame.material")),1, (short) config.getInt("Status.InGame.id"));
            } else {
                item = new ItemStack(Material.getMaterial(config.getString("Status.InGame.material")));
            }
            return XMaterial.matchXMaterial(item);
        } else if (gameStatus.equals(Enums.GameStatus.RESTARTING)) {
            ItemStack item;
            if (main.checkVersion()){
                item = new ItemStack(Material.getMaterial(config.getString("Status.Restarting.material")),1, (short) config.getInt("Status.Restarting.id"));
            } else {
                item = new ItemStack(Material.getMaterial(config.getString("Status.Restarting.material")));
            }
            return XMaterial.matchXMaterial(item);
        } else if (this.players.size() >= this.maxPlayers) {
            ItemStack item;
            if (main.checkVersion()){
                item = new ItemStack(Material.getMaterial(config.getString("Status.Full.material")),1, (short) config.getInt("Status.Full.id"));
            } else {
                item = new ItemStack(Material.getMaterial(config.getString("Status.Full.material")));
            }
            return XMaterial.matchXMaterial(item);
        }

        ItemStack item;
        if (main.checkVersion()){
            item = new ItemStack(Material.getMaterial(config.getString("Status.Searching.material")),1, (short)config.getInt("Status.Searching.id"));
        } else {
            item = new ItemStack(Material.getMaterial(config.getString("Status.Searching.material")));
        }
        return XMaterial.matchXMaterial(item);
    }


    public void loadSpawns() {
        if (!arenac.getString("Arena.spawns", "").isEmpty()) {
            arenac.getConfigurationSection("Arena.spawns").getKeys(false).forEach(key ->
                    spawns.add(new ArenaTeam(this, locUtils.stringToLoc(arenac.getString("Arena.spawns." + key))))
            );
        }

        if (!arenac.getString("Arena.spawnSpectator", "").isEmpty()) {
            this.spawnSpectator = locUtils.stringToLoc(arenac.getString("Arena.spawnSpectator"));
        }

        if (!arenac.getString("Arena.centerChest", "").isEmpty()) {
            arenac.getStringList("Arena.centerChest").forEach(loc ->
                    this.centerChest.add(locUtils.stringToLoc(loc))
            );
        }
    }

    public String timeScore(GamePlayer gamePlayer){
        if (getStartingGameTask().getTimer() == getStartingGame()) {
            return gamePlayer.getLangMessage().getString("Messages.ScoreBoard.Waiting");
        } else {
            return getStartingGameTask().getTimer() + "";
        }
    }

    public String typeEvent(GamePlayer gamePlayer) {
        Configuration config = gamePlayer.getLangMessage();

        if (refillGame != null && refillGame.getTimer() >= 1) {
            return config.getString("Messages.ScoreBoard.Refill").replace("%time%", String.valueOf(calculateTime(refillGame.getTimer())));
        } else if (refillGame == null && !refillTime.isEmpty()) {
            return config.getString("Messages.ScoreBoard.Refill").replace("%time%", String.valueOf(calculateTime(0)));
        } else  if (gameStatus.equals(Enums.GameStatus.RESTARTING)) {
            return config.getString("Messages.ScoreBoard.EndGame");
        } else {
            return config.getString("Messages.ScoreBoard.None");
        }
    }

    public void setGameStatus(Enums.GameStatus gameStatus) {
        if (iSign != null) iSign.updateSign();
        this.gameStatus = gameStatus;
    }

    private String calculateTime(long seconds) {
        return String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds)), TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
    }

    private String c(String c) {
        return ChatColor.translateAlternateColorCodes('&', c);
    }
}
