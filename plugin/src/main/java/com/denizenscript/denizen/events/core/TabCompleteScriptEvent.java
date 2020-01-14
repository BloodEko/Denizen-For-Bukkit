package com.denizenscript.denizen.events.core;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

public class TabCompleteScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // tab complete
    //
    // @Regex ^on tab complete$
    //
    // @Cancellable true
    //
    // @Triggers when a player or the console is sent a list of available tab completions.
    //
    // @Context
    // <context.buffer> returns the full raw command buffer.
    // <context.command> returns the command name.
    // <context.current_arg> returns the current argument for completion.
    // <context.completions> returns a list of available tab completions.
    // <context.server> returns true if the tab completion was triggered from the console.
    //
    // @Determine
    // ListTag to set the list of available tab completions.
    //
    // @Player when the tab completion is done by a player.
    //
    // -->

    public TabCompleteScriptEvent() {
        instance = this;
    }

    public static TabCompleteScriptEvent instance;
    public TabCompleteEvent event;
    public String buffer;
    public ListTag completions;
    public CommandSender sender;

    public String getCommand() {
        String[] args = buffer.trim().split(" ");
        String cmd = args.length > 0 ? args[0] : "";
        if (sender instanceof Player) {
            cmd = cmd.replaceFirst("/", "");
        }
        return cmd;
    }

    public String getCurrentArg() {
        int i = buffer.lastIndexOf(' ');
        return i > 0 ? buffer.substring(i + 1) : getCommand();
    }

    @Override
    public boolean couldMatch(ScriptContainer scriptContainer, String s) {
        return CoreUtilities.toLowerCase(s).startsWith("tab complete");
    }

    @Override
    public String getName() {
        return "TabComplete";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String determination = determinationObj.toString();
        if (determination.length() > 0 && !isDefaultDetermination(determinationObj)) {
            completions = ListTag.valueOf(determination, getTagContext(path));
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(sender instanceof Player ? new PlayerTag((Player) sender) : null, null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("buffer")) {
            return new ElementTag(buffer);
        }
        else if (name.equals("command")) {
            return new ElementTag(getCommand());
        }
        else if (name.equals("current_arg")) {
            return new ElementTag(getCurrentArg());
        }
        else if (name.equals("completions")) {
            return completions;
        }
        else if (name.equals("server")) {
            return new ElementTag(sender instanceof ConsoleCommandSender);
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        buffer = event.getBuffer();
        completions = new ListTag(event.getCompletions());
        sender = event.getSender();
        this.event = event;
        fire(event);
        event.setCompletions(completions);
    }
}
