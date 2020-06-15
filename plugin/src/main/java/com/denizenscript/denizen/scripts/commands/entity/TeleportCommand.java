package com.denizenscript.denizen.scripts.commands.entity;

import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Arrays;
import java.util.List;

public class TeleportCommand extends AbstractCommand {

    public TeleportCommand() {
        setName("teleport");
        setSyntax("teleport (<entity>|...) [<location>]");
        setRequiredArguments(1, 2);
    }

    // <--[command]
    // @Name Teleport
    // @Syntax teleport (<entity>|...) [<location>]
    // @Required 1
    // @Maximum 2
    // @Short Teleports the entity(s) to a new location.
    // @Group entity
    //
    // @Description
    // Teleports the entity or entities to the new location.
    // Entities can be teleported between worlds using this command.
    //
    // @Tags
    // <EntityTag.location>
    //
    // @Usage
    // Use to teleport a player to the location its cursor is pointing on
    // - teleport <player> <player.cursor_on>
    //
    // @Usage
    // Use to teleport a player high above
    // - teleport <player> <player.location.add[0,200,0]>
    //
    // @Usage
    // Use to teleport to a random online player
    // - teleport <player> <server.list_online_players.random.location>
    //
    // @Usage
    // Use to teleport all players to your location
    // - teleport <server.list_online_players> <player.location>
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (!scriptEntry.hasObject("location")
                    && arg.matchesArgumentType(LocationTag.class)) {
                scriptEntry.addObject("location", arg.asType(LocationTag.class));
            }
            else if (!scriptEntry.hasObject("entities")
                    && arg.matchesArgumentList(EntityTag.class)) {
                scriptEntry.addObject("entities", arg.asType(ListTag.class).filter(EntityTag.class, scriptEntry));
            }

            // NPC arg for compatibility with old scripts
            else if (arg.matches("npc") && Utilities.entryHasNPC(scriptEntry)) {
                scriptEntry.addObject("entities", Arrays.asList(Utilities.getEntryNPC(scriptEntry).getDenizenEntity()));
            }
            else {
                arg.reportUnhandled();
            }
        }

        if (!scriptEntry.hasObject("location")) {
            throw new InvalidArgumentsException("Must specify a location!");
        }

        // Use player or NPC as default entity
        if (!scriptEntry.hasObject("entities")) {
            scriptEntry.defaultObject("entities", (Utilities.entryHasPlayer(scriptEntry) ? Arrays.asList(Utilities.getEntryPlayer(scriptEntry).getDenizenEntity()) : null),
                    (Utilities.entryHasNPC(scriptEntry) ? Arrays.asList(Utilities.getEntryNPC(scriptEntry).getDenizenEntity()) : null));
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(final ScriptEntry scriptEntry) {

        LocationTag location = scriptEntry.getObjectTag("location");
        List<EntityTag> entities = (List<EntityTag>) scriptEntry.getObject("entities");

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), ArgumentHelper.debugObj("location", location) +
                    ArgumentHelper.debugObj("entities", entities.toString()));
        }

        for (EntityTag entity : entities) {
            // Call a Bukkit event for compatibility with "on entity teleports"
            // world event and other plugins
            if (entity.isSpawned()) {
                if (entity.getBukkitEntityType() != EntityType.PLAYER) {
                    Bukkit.getPluginManager().callEvent(new EntityTeleportEvent(entity.getBukkitEntity(), entity.getLocation(), location));
                }
                else {
                    Bukkit.getPluginManager().callEvent(new PlayerTeleportEvent((Player) entity.getBukkitEntity(), entity.getLocation(), location));
                }
            }
            entity.spawnAt(location);
        }
    }
}
