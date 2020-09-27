package com.denizenscript.denizen.events.world;

import com.denizenscript.denizen.objects.WorldTag;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;

public class WorldSavesScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // world saves
    // <world> saves
    //
    // @Regex ^on [^\s]+ saves$
    //
    // @Group World
    //
    // @Triggers when a world is saved.
    //
    // @Context
    // <context.world> returns the WorldTag that was saved.
    //
    // -->

    public WorldSavesScriptEvent() {
        instance = this;
    }

    public static WorldSavesScriptEvent instance;
    public WorldTag world;
    public WorldSaveEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(1).equals("saves")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runGenericCheck(path.eventArgAt(0), world.getName())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "WorldSaves";
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("world")) {
            return world;
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onWorldSaves(WorldSaveEvent event) {
        world = new WorldTag(event.getWorld());
        this.event = event;
        fire(event);
    }
}
