package com.denizenscript.denizen.scripts.commands.world;

import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

public class ExplodeCommand extends AbstractCommand {

    // <--[command]
    // @Name Explode
    // @Syntax explode (power:<#.#>) (<location>) (fire) (breakblocks)
    // @Required 0
    // @Short Causes an explosion at the location.
    // @Group world
    //
    // @Description
    // This command causes an explosion at the location specified (or the npc / player location) which does not
    // destroy blocks or set fire to blocks within the explosion. It accepts a 'fire' option which will set blocks
    // on fire within the explosion radius. It also accepts a 'breakblocks' option which will cause the explosion to
    // break blocks within the power radius as well as creating an animation and sounds.
    // Default power: 1
    // Default location: npc.location, or if no NPC link, player.location.
    // It is highly recommended you specify a location to be safe.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to create an explosion at a player's location.
    // - explode <player.location>
    //
    // @Usage
    // Use to create an explosion at a player, which breaks blocks and causes fire with a power of 5.
    // - explode power:5 <player.location> fire breakblocks
    //
    // @Usage
    // Use to create an explosion with a power radius of 3 at an NPC's location.
    // - explode power:3 <npc.location>
    //
    // @Usage
    // Use to create an explosion with a power radius of 3 at a related location which breaks blocks.
    // - explode power:3 <context.location> breakblocks
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        // Iterate through arguments
        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (!scriptEntry.hasObject("location")
                    && arg.matchesArgumentType(LocationTag.class)) {

                scriptEntry.addObject("location", arg.asType(LocationTag.class));
            }
            else if (!scriptEntry.hasObject("power")
                    && arg.matchesPrimitive(ArgumentHelper.PrimitiveType.Float)
                    && arg.matchesPrefix("power", "p")) {

                scriptEntry.addObject("power", arg.asElement());
            }
            else if (!scriptEntry.hasObject("breakblocks")
                    && arg.matches("breakblocks")) {

                scriptEntry.addObject("breakblocks", "");
            }
            else if (!scriptEntry.hasObject("fire")
                    && arg.matches("fire")) {

                scriptEntry.addObject("fire", "");
            }
            else {
                arg.reportUnhandled();
            }
        }

        // Use default values if necessary
        scriptEntry.defaultObject("power", new ElementTag(1.0));
        scriptEntry.defaultObject("location",
                Utilities.entryHasNPC(scriptEntry) ? Utilities.getEntryNPC(scriptEntry).getLocation() : null,
                Utilities.entryHasPlayer(scriptEntry) ? Utilities.getEntryPlayer(scriptEntry).getLocation() : null);

        if (!scriptEntry.hasObject("location")) {
            throw new InvalidArgumentsException("Missing location argument!");
        }
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        // Get objects

        final LocationTag location = (LocationTag) scriptEntry.getObject("location");
        ElementTag power = (ElementTag) scriptEntry.getObject("power");
        boolean breakblocks = scriptEntry.hasObject("breakblocks");
        boolean fire = scriptEntry.hasObject("fire");

        // Report to dB
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(),
                    (ArgumentHelper.debugObj("location", location.toString()) +
                            ArgumentHelper.debugObj("power", power) +
                            ArgumentHelper.debugObj("breakblocks", breakblocks) +
                            ArgumentHelper.debugObj("fire", fire)));
        }

        location.getWorld().createExplosion
                (location.getX(), location.getY(), location.getZ(),
                        power.asFloat(), fire, breakblocks);
    }
}
