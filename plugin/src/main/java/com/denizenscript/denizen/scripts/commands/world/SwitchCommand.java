package com.denizenscript.denizen.scripts.commands.world;

import com.denizenscript.denizen.utilities.DenizenAPI;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.blocks.ModernBlockData;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class SwitchCommand extends AbstractCommand {

    public SwitchCommand() {
        setName("switch");
        setSyntax("switch [<location>|...] (state:[{toggle}/on/off]) (duration:<value>)");
        setRequiredArguments(1, 3);
        isProcedural = false;
    }

    // <--[command]
    // @Name Switch
    // @Syntax switch [<location>|...] (state:[{toggle}/on/off]) (duration:<value>)
    // @Required 1
    // @Maximum 3
    // @Short Switches state of the block.
    // @Group world
    //
    // @Description
    // Changes the state of a block at the given location, or list of blocks at the given locations.
    //
    // Optionally specify "state:on" to turn a block on (or open it, or whatever as applicable) or "state:off" to turn it off (or close it, etc).
    // By default, will toggle the state (on to off, or off to on).
    //
    // Optionally specify the "duration" argument to set a length of time after which the block will return to the original state.
    //
    // Works on any interactable blocks, including:
    // - the standard toggling levers, doors, gates...
    // - Single-use interactables like buttons, note blocks, dispensers, droppers, ...
    // - Redstone interactables like repeaters, ...
    // - Special interactables like tripwires, ...
    // - Almost any other block with an interaction handler.
    //
    // This will generally (but not always) function equivalently to a user right-clicking the block
    // (so it will open and close doors, flip levers on and off, press and depress buttons, ...).
    //
    // @Tags
    // <LocationTag.switched>
    // <MaterialTag.is_switchable>
    // <MaterialTag.switched>
    //
    // @Usage
    // At the player's location, switch the state of the block to on, no matter what state it was in before.
    // - switch <player.location> state:on
    //
    // @Usage
    // Opens a door that the player is looking at.
    // - switch <player.cursor_on> state:on
    //
    // @Usage
    // Toggle a block at the player's foot location.
    // - switch <player.location>
    //
    // -->

    private enum SwitchState {ON, OFF, TOGGLE}

    private Map<Location, Integer> taskMap = new HashMap<>(32);

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("locations") &&
                    arg.matchesArgumentList(LocationTag.class)) {
                scriptEntry.addObject("locations", arg.asType(ListTag.class));
            }
            else if (!scriptEntry.hasObject("duration") &&
                    arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("duration", arg.asType(DurationTag.class));
            }
            else if (!scriptEntry.hasObject("state") &&
                    arg.matchesEnum(SwitchState.values())) {
                scriptEntry.addObject("switchstate", new ElementTag(arg.getValue().toUpperCase()));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("locations")) {
            throw new InvalidArgumentsException("Must specify a location!");
        }
        scriptEntry.defaultObject("duration", new DurationTag(0));
        scriptEntry.defaultObject("switchstate", new ElementTag("TOGGLE"));
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        final ListTag interactLocations = scriptEntry.getObjectTag("locations");
        long duration = ((DurationTag) scriptEntry.getObject("duration")).getTicks();
        final SwitchState switchState = SwitchState.valueOf(scriptEntry.getElement("switchstate").asString());
        final Player player = Utilities.entryHasPlayer(scriptEntry) ? Utilities.getEntryPlayer(scriptEntry).getPlayerEntity() : null;
        // Switch the Block
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), interactLocations.debug()
                    + ArgumentHelper.debugObj("duration", duration + "t")
                    + ArgumentHelper.debugObj("switchstate", switchState.name()));
        }
        for (final LocationTag interactLocation : interactLocations.filter(LocationTag.class, scriptEntry)) {
            switchBlock(scriptEntry, interactLocation, switchState, player);
            // If duration set, schedule a delayed task.
            if (duration > 0) {
                // If this block already had a delayed task, cancel it.
                if (taskMap.containsKey(interactLocation)) {
                    try {
                        Bukkit.getScheduler().cancelTask(taskMap.get(interactLocation));
                    }
                    catch (Exception e) {
                    }
                }
                Debug.echoDebug(scriptEntry, "Setting delayed task 'SWITCH' for " + interactLocation.identify());
                // Store new delayed task ID, for checking against, then schedule new delayed task.
                taskMap.put(interactLocation, Bukkit.getScheduler().scheduleSyncDelayedTask(DenizenAPI.getCurrentInstance(),
                        new Runnable() {
                            public void run() {
                                switchBlock(scriptEntry, interactLocation, SwitchState.TOGGLE, player);
                            }
                        }, duration));
            }
        }
    }

    public static boolean switchState(Block b) {
        ModernBlockData mbd = new ModernBlockData(b);
        Boolean switchState = mbd.getSwitchState();
        if (switchState != null) {
            return switchState;
        }
        return false;
    }

    // Break off this portion of the code from execute() so it can be used in both execute and the delayed runnable
    public void switchBlock(ScriptEntry scriptEntry, Location interactLocation, SwitchState switchState, Player player) {
        boolean currentState = switchState(interactLocation.getBlock());
        if ((switchState.equals(SwitchState.ON) && !currentState) || (switchState.equals(SwitchState.OFF) && currentState) || switchState.equals(SwitchState.TOGGLE)) {
            ModernBlockData mbd = new ModernBlockData(interactLocation.getBlock());
            mbd.setSwitchState(interactLocation.getBlock(), !currentState);
            Debug.echoDebug(scriptEntry, "Switched " + interactLocation.getBlock().getType().toString() + "! Current state now: " + (switchState(interactLocation.getBlock()) ? "ON" : "OFF"));
        }
    }
}
