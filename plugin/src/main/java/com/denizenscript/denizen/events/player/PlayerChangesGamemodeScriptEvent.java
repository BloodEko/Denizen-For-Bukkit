package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class PlayerChangesGamemodeScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // player changes gamemode (to <gamemode>)
    //
    // @Regex ^on player changes gamemode( to [^\s]+)?$
    //
    // @Group Player
    //
    // @Cancellable true
    //
    // @Triggers when a player's gamemode is changed.
    //
    // @Context
    // <context.gamemode> returns an ElementTag of the gamemode. Game Modes: <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/GameMode.html>
    //
    // @Player Always.
    //
    // -->

    public PlayerChangesGamemodeScriptEvent() {
        instance = this;
    }

    public static PlayerChangesGamemodeScriptEvent instance;
    public ElementTag gamemode;
    public PlayerGameModeChangeEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("player changes gamemode")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String mode = path.eventArgLowerAt(4);
        if (mode.length() > 0) {
            if (!runGenericCheck(mode, gamemode.asString())) {
                return false;
            }
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "PlayerChangesGamemode";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(new PlayerTag(event.getPlayer()), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("gamemode")) {
            return gamemode;
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onPlayerChangesGamemode(PlayerGameModeChangeEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        gamemode = new ElementTag(event.getNewGameMode().name());
        this.event = event;
        fire(event);
    }
}
