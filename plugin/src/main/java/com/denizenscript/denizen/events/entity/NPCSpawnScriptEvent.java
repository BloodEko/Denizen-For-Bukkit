package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.NPCTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NPCSpawnScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // npc spawns
    //
    // @Regex ^on npc spawns$
    //
    // @Switch in:<area> to only process the event if it occurred within a specified area.
    //
    // @Cancellable true
    //
    // @Triggers when an NPC spawns.
    //
    // @Context
    // <context.location> returns the location the entity will spawn at.
    //
    // -->

    public NPCSpawnScriptEvent() {
        instance = this;
    }

    public static NPCSpawnScriptEvent instance;
    public NPCTag npc;
    public LocationTag location;
    public NPCSpawnEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("npc spawns");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runInCheck(path, location)) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "NPCSpawn";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(null, npc);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("location")) {
            return location;
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onNPCSpawn(NPCSpawnEvent event) {
        this.npc = new NPCTag(event.getNPC());
        location = new LocationTag(event.getLocation());
        this.event = event;
        fire(event);
    }

}
