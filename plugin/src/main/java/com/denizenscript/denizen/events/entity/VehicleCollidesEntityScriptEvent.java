package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;

public class VehicleCollidesEntityScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // vehicle collides with entity
    // vehicle collides with <entity>
    // <vehicle> collides with entity
    // <vehicle> collides with <entity>
    //
    // @Regex ^on [^\s]+ collides with [^\s]+$
    //
    // @Switch in:<area> to only process the event if it occurred within a specified area.
    //
    // @Cancellable true
    //
    // @Triggers when a vehicle collides with an entity.
    //
    // @Context
    // <context.vehicle> returns the EntityTag of the vehicle.
    // <context.entity> returns the EntityTag of the entity the vehicle has collided with.
    // <context.pickup> returns whether the vehicle can pick up the entity.
    //
    // @Determine
    // "PICKUP:TRUE" to allow the vehicle to pick up the entity.
    // "PICKUP:FALSE" to stop the vehicle from picking up the entity.
    //
    // @Player when a vehicle collides with a player.
    //
    // @NPC when a vehicle collides with an NPC.
    //
    // -->

    public VehicleCollidesEntityScriptEvent() {
        instance = this;
    }

    public static VehicleCollidesEntityScriptEvent instance;

    public EntityTag vehicle;
    public EntityTag entity;
    public VehicleEntityCollisionEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.contains("collides with");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!tryEntity(vehicle, path.eventArgLowerAt(0))) {
            return false;
        }

        if (!tryEntity(entity, path.eventArgLowerAt(3))) {
            return false;
        }

        if (!runInCheck(path, vehicle.getLocation())) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "VehicleCollidesEntity";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag) {
            Argument arg = Argument.valueOf(determinationObj.toString());
            if (arg.matchesPrefix("pickup")) {
                event.setPickupCancelled(!arg.asElement().asBoolean());
                return true;
            }
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(entity);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("vehicle")) {
            return vehicle;
        }
        else if (name.equals("entity")) {
            return entity;
        }
        else if (name.equals("pickup")) {
            return new ElementTag(!event.isPickupCancelled());
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onVehicleCollidesEntity(VehicleEntityCollisionEvent event) {
        entity = new EntityTag(event.getEntity());
        vehicle = new EntityTag(event.getVehicle());
        this.event = event;
        fire(event);
    }
}
