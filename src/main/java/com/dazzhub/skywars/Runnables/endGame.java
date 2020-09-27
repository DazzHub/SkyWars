package com.dazzhub.skywars.Runnables;

import com.dazzhub.skywars.Arena.Arena;
import com.dazzhub.skywars.Main;
import com.dazzhub.skywars.MySQL.utils.GamePlayer;
import com.dazzhub.skywars.Utils.effects.getTypeWins;
import com.google.common.collect.Lists;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

@Getter
public class endGame extends BukkitRunnable {

    private Arena arena;
    private int timer;

    public endGame(Arena arena) {
        this.arena = arena;
        this.timer = arena.getFinishedGame();
    }

    @Override
    public void run() {

        if (timer == arena.getFinishedGame()) {
            Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                for (GamePlayer gamePlayer: arena.getPlayers()) {
                    switch (gamePlayer.getArena().getMode()){
                        case SOLO:{
                            getTypeWins win = gamePlayer.getTypeWin(gamePlayer.getWinEffectSolo());
                            if (win != null) win.playWinEffect();
                            break;
                        }
                        case TEAM:{
                            getTypeWins win = gamePlayer.getTypeWin(gamePlayer.getWinEffectTeam());
                            if (win != null) win.playWinEffect();
                            break;
                        }
                    }
                }
            });
        }

        if (timer <= 1) {
            Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                List<GamePlayer> playerArena = Lists.newArrayList(arena.getPlayers());
                List<GamePlayer> playerSpectator = Lists.newArrayList(arena.getSpectators());

                playerArena.forEach(gamePlayer -> arena.removePlayer(gamePlayer));
                playerSpectator.forEach(gamePlayer -> arena.removeSpectator(gamePlayer, true));
            });
        }

        if (timer <= 0) {
            cancel();
            this.arena.resetArena();
        }

        timer--;
    }
}
