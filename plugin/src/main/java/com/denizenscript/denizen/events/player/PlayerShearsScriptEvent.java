package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;

public class PlayerShearsScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // player shears entity
    // player shears <entity>
    // player shears <color> sheep
    //
    // @Regex ^on player shears [^\s]+( sheep)?$
    //
    // @Switch in:<area> to only process the event if it occurred within a specified area.
    //
    // @Cancellable true
    //
    // @Triggers when a player shears an entity.
    //
    // @Context
    // <context.entity> returns the EntityTag of the sheep.
    //
    // @Player Always.
    //
    // -->

    public PlayerShearsScriptEvent() {
        instance = this;
    }

    public static PlayerShearsScriptEvent instance;
    public EntityTag entity;
    public PlayerShearEntityEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("player shears");
    }

    @Override
    public boolean matches(ScriptPath path) {
        String ent = path.eventArgLowerAt(3).equals("sheep") ? "sheep" : path.eventArgLowerAt(2);

        if (!ent.equals("sheep") && !tryEntity(entity, ent)) {
            return false;
        }

        String color = path.eventArgLowerAt(3).equals("sheep") ? path.eventArgLowerAt(2) : "";
        if (color.length() > 0 && !color.equals(CoreUtilities.toLowerCase(((Sheep) entity.getBukkitEntity()).getColor().name()))) {
            return false;
        }

        if (!runInCheck(path, entity.getLocation())) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "PlayerShears";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(new PlayerTag(event.getPlayer()), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("entity")) {
            return entity.getDenizenObject();
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onPlayerShears(PlayerShearEntityEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        entity = new EntityTag(event.getEntity());
        this.event = event;
        fire(event);
    }
}
