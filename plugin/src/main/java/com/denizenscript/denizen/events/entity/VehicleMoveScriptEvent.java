package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;

public class VehicleMoveScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // vehicle moves
    // <vehicle> moves
    //
    // @Regex ^on [^\s]+ moves$
    //
    // @Switch in:<area> to only process the event if it occurred within a specified area.
    //
    // @Warning This event fires very very rapidly!
    //
    // @Triggers when a vehicle moves in the slightest.
    //
    // @Context
    // <context.vehicle> returns the EntityTag of the vehicle.
    // <context.from> returns the location of where the vehicle was.
    // <context.to> returns the location of where the vehicle is.
    //
    // -->

    public VehicleMoveScriptEvent() {
        instance = this;
    }

    public static VehicleMoveScriptEvent instance;
    public EntityTag vehicle;
    public LocationTag from;
    public LocationTag to;
    public VehicleMoveEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventArgLowerAt(1).equals("moves");
    }

    @Override
    public boolean matches(ScriptPath path) {

        if (!tryEntity(vehicle, path.eventArgLowerAt(0))) {
            return false;
        }

        if (!runInCheck(path, vehicle.getLocation())) {
            return false;
        }

        return super.matches(path);
    }

    // TODO: Can the vehicle be an NPC?

    @Override
    public String getName() {
        return "VehicleMoves";
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("from")) {
            return from;
        }
        else if (name.equals("to")) {
            return to;
        }
        else if (name.equals("vehicle")) {
            return vehicle;
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        to = new LocationTag(event.getTo());
        from = new LocationTag(event.getFrom());
        vehicle = new EntityTag(event.getVehicle());
        this.event = event;
        fire(event);
    }
}
