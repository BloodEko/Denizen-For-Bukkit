package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SlimeSplitEvent;

public class SlimeSplitsScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // slime splits (into <#>)
    //
    // @Regex ^on slime splits( into [^\s]+)?$
    //
    // @Switch in:<area> to only process the event if it occurred within a specified area.
    //
    // @Cancellable true
    //
    // @Triggers when a slime splits into smaller slimes.
    //
    // @Context
    // <context.entity> returns the EntityTag of the slime.
    // <context.count> returns an ElementTag(Number) of the number of smaller slimes it will split into.
    //
    // @Determine
    // ElementTag(Number) to set the number of smaller slimes it will split into.
    //
    // -->

    public SlimeSplitsScriptEvent() {
        instance = this;
    }

    public static SlimeSplitsScriptEvent instance;
    public EntityTag entity;
    public int count;
    public SlimeSplitEvent event;

    @Override
    public boolean couldMatch(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        return lower.startsWith("slime splits");
    }

    @Override
    public boolean matches(ScriptPath path) {
        String counts = path.eventArgLowerAt(3);

        if (path.eventArgLowerAt(2).equals("into") && !counts.isEmpty()) {
            try {
                if (Integer.parseInt(counts) != count) {
                    return false;
                }
            }
            catch (NumberFormatException e) {
                return false;
            }
        }

        if (!runInCheck(path, entity.getLocation())) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "SlimeSplits";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag && ((ElementTag) determinationObj).isInt()) {
            count = ((ElementTag) determinationObj).asInt();
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("entity")) {
            return entity;
        }
        else if (name.equals("count")) {
            return new ElementTag(count);
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onSlimeSplits(SlimeSplitEvent event) {
        entity = new EntityTag(event.getEntity());
        count = event.getCount();
        this.event = event;
        fire(event);
        event.setCount(count);
    }

}
