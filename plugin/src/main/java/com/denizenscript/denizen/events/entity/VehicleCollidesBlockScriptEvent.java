package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;

public class VehicleCollidesBlockScriptEvent extends BukkitScriptEvent implements Listener {

    // TODO: de-collide with 'collides with entity'
    // <--[event]
    // @Events
    // vehicle collides with block
    // vehicle collides with <material>
    // <vehicle> collides with block
    // <vehicle> collides with <material>
    //
    // @Regex ^on [^\s]+ collides with [^\s]+$
    //
    // @Switch in:<area> to only process the event if it occurred within a specified area.
    //
    // @Triggers when a vehicle collides with a block.
    //
    // @Context
    // <context.vehicle> returns the EntityTag of the vehicle.
    // <context.location> returns the LocationTag of the block.
    //
    // -->

    public VehicleCollidesBlockScriptEvent() {
        instance = this;
    }

    public static VehicleCollidesBlockScriptEvent instance;

    public EntityTag vehicle;
    public LocationTag location;
    private MaterialTag material;
    public VehicleBlockCollisionEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.contains("collides with");
    }

    @Override
    public boolean matches(ScriptPath path) {

        if (!tryEntity(vehicle, path.eventArgLowerAt(0))) {
            return false;
        }

        if (!tryMaterial(material, path.eventArgLowerAt(3))) {
            return false;
        }

        if (!runInCheck(path, location)) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "VehicleCollidesBlock";
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("vehicle")) {
            return vehicle;
        }
        else if (name.equals("location")) {
            return location;
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onVehicleCollidesBlock(VehicleBlockCollisionEvent event) {
        vehicle = new EntityTag(event.getVehicle());
        location = new LocationTag(event.getBlock().getLocation());
        material = new MaterialTag(event.getBlock());
        this.event = event;
        fire(event);
    }
}
