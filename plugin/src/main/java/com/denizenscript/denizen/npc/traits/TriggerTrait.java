package com.denizenscript.denizen.npc.traits;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.objects.NPCTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.scripts.commands.npc.EngageCommand;
import com.denizenscript.denizen.scripts.triggers.AbstractTrigger;
import com.denizenscript.denizen.scripts.triggers.TriggerRegistry;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.Settings;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import net.citizensnpcs.api.command.exception.CommandException;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.Paginator;
import net.citizensnpcs.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class TriggerTrait extends Trait implements Listener {

    @Persist(value = "enabled", collectionType = HashMap.class)
    private Map<String, Boolean> enabled = new HashMap<>();
    @Persist(value = "properly_set", collectionType = HashMap.class)
    public Map<String, Boolean> properly_set = new HashMap<>();
    @Persist(value = "duration", collectionType = HashMap.class)
    private Map<String, Double> duration = new HashMap<>();
    @Persist(value = "cooldowntype", collectionType = HashMap.class)
    private Map<String, TriggerRegistry.CooldownType> type = new HashMap<>();
    @Persist(value = "radius", collectionType = HashMap.class)
    private Map<String, Integer> radius = new HashMap<>();

    public void report() {
        Debug.log("enabled: " + enabled.entrySet().toString());
        Debug.log("duration: " + duration.entrySet().toString());
        Debug.log("type: " + type.entrySet().toString());
        Debug.log("radius: " + radius.entrySet().toString());
    }

    public TriggerTrait() {
        super("triggers");
        for (Map.Entry<String, Boolean> entry : enabled.entrySet()) {
            if (!properly_set.containsKey(entry.getKey())) {
                properly_set.put(entry.getKey(), entry.getValue());
            }
        }
        for (String triggerName : Denizen.getInstance().getTriggerRegistry().list().keySet()) {
            if (!enabled.containsKey(triggerName)) {
                enabled.put(triggerName, Settings.triggerEnabled(triggerName));
                properly_set.put(triggerName, false);
            }
        }
    }

    @Override
    public void onSpawn() {
        for (String triggerName : Denizen.getInstance().getTriggerRegistry().list().keySet()) {
            if (!enabled.containsKey(triggerName)) {
                enabled.put(triggerName, Settings.triggerEnabled(triggerName));
            }
        }
    }

    @Override
    public void load(DataKey key) {
        if (!key.keyExists("properly_set") && key.keyExists("enabled")) {
            for (final String triggerName : Denizen.getInstance().getTriggerRegistry().list().keySet()) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(Denizen.getInstance(), () -> properly_set.put(triggerName, key.getBoolean("enabled." + triggerName)));
            }
        }
    }

    /**
     * Toggles a trigger on or off for this NPC.
     *
     * @param triggerName name of the Trigger, as specified by the Trigger. Case in-sensitive.
     * @param toggle      new state of the trigger
     * @return output debug information.
     */
    public String toggleTrigger(String triggerName, boolean toggle) {
        if (enabled.containsKey(triggerName.toUpperCase())) {
            enabled.put(triggerName.toUpperCase(), toggle);
            properly_set.put(triggerName.toUpperCase(), true);
            return triggerName + " trigger is now " + (toggle ? "enabled." : "disabled.");
        }
        else {
            return triggerName + " trigger not found!";
        }
    }

    public String toggleTrigger(String triggerName) {
        if (enabled.containsKey(triggerName.toUpperCase())) {
            if (enabled.get(triggerName.toUpperCase())) {
                enabled.put(triggerName.toUpperCase(), false);
                return triggerName + " trigger is now disabled.";
            }
            else {
                enabled.put(triggerName.toUpperCase(), true);
                properly_set.put(triggerName.toUpperCase(), true);
                return triggerName + " trigger is now enabled.";
            }
        }
        else {
            return triggerName + " trigger not found!";
        }
    }

    public boolean hasTrigger(String triggerName) {
        return enabled.containsKey(triggerName.toUpperCase()) && enabled.get(triggerName.toUpperCase());
    }

    public boolean isEnabled(String triggerName) {
        if (!new NPCTag(npc).getAssignmentTrait().hasAssignment()) {
            return false;
        }
        if (enabled.containsKey(triggerName.toUpperCase())) {
            return enabled.get(triggerName.toUpperCase());
        }
        else {
            return false;
        }
    }

    public void setLocalCooldown(String triggerName, double value) {
        if (value < 0) {
            value = 0;
        }
        duration.put(triggerName.toUpperCase(), value);
    }

    public double getCooldownDuration(String triggerName) {
        if (duration.containsKey(triggerName.toUpperCase())) {
            return duration.get(triggerName.toUpperCase());
        }
        else {
            return Settings.triggerDefaultCooldown(triggerName);
        }
    }

    public TriggerRegistry.CooldownType getCooldownType(String triggerName) {
        try {
            if (type.containsKey(triggerName.toUpperCase())) {
                return type.get(triggerName.toUpperCase());
            }
            else {
                return TriggerRegistry.CooldownType.valueOf(Settings.triggerDefaultCooldownType(triggerName).toUpperCase());
            }
        }
        catch (Exception e) {
            return TriggerRegistry.CooldownType.PLAYER;
        }
    }

    public void setLocalRadius(String triggerName, int value) {
        radius.put(triggerName.toUpperCase(), value);
    }

    public double getRadius(String triggerName) {
        if (radius.containsKey(triggerName.toUpperCase())) {
            return radius.get(triggerName.toUpperCase());
        }
        else {
            return Settings.triggerDefaultRange(triggerName);
        }
    }

    public void describe(CommandSender sender, int page) throws CommandException {
        Paginator paginator = new Paginator().header("Triggers");
        paginator.addLine("<e>Key: <a>Name  <b>Status  <c>Cooldown  <d>Cooldown Type  <e>(Radius)");
        for (Entry<String, Boolean> entry : enabled.entrySet()) {
            String line = "<a> " + entry.getKey()
                    + "<b> " + (entry.getValue() ? "Enabled" : "Disabled")
                    + "<c> " + getCooldownDuration(entry.getKey())
                    + "<d> " + getCooldownType(entry.getKey()).name()
                    + "<e> " + (getRadius(entry.getKey()) == -1 ? "" : getRadius(entry.getKey()));
            paginator.addLine(line);
        }
        if (!paginator.sendPage(sender, page)) {
            throw new CommandException(Messages.COMMAND_PAGE_MISSING, page);
        }
    }

    public boolean triggerCooldownOnly(AbstractTrigger triggerClass, PlayerTag player) {
        // Check cool down, return false if not yet met
        if (!Denizen.getInstance().getTriggerRegistry().checkCooldown(npc, player, triggerClass, getCooldownType(triggerClass.getName()))) {
            return false;
        }
        // Check engaged
        if (EngageCommand.getEngaged(npc)) {
            return false;
        }
        // Set cool down
        Denizen.getInstance().getTriggerRegistry().setCooldown(npc, player, triggerClass, getCooldownDuration(triggerClass.getName()), getCooldownType(triggerClass.getName()));
        return true;
    }

    // <--[action]
    // @Actions
    // unavailable
    //
    // @Triggers when a trigger fires but the NPC is engaged.
    //
    // @Context
    // <context.trigger_type> return the type of trigger fired
    //
    // -->
    public TriggerContext trigger(AbstractTrigger triggerClass, PlayerTag player) {
        return trigger(triggerClass, player, null);
    }

    public TriggerContext trigger(AbstractTrigger triggerClass, PlayerTag player, Map<String, ObjectTag> context) {

        String trigger_type = triggerClass.getName();

        // Check cool down, return false if not yet met
        if (!Denizen.getInstance().getTriggerRegistry().checkCooldown(npc, player, triggerClass, getCooldownType(trigger_type))) {
            return new TriggerContext(false);
        }

        if (context == null) {
            context = new HashMap<>();
        }

        // Check engaged
        if (EngageCommand.getEngaged(npc)) {

            // Put the trigger_type into context
            context.put("trigger_type", new ElementTag(trigger_type));

            //
            // On Unavailable Action

            // TODO: Should this be refactored?
            if (new NPCTag(npc).action("unavailable", player, context).equalsIgnoreCase("available")) {
                // If determined available, continue on...
                // else, return a 'non-triggered' state.
            }
            else {
                return new TriggerContext(false);
            }
        }

        // Set cool down
        Denizen.getInstance().getTriggerRegistry()
                .setCooldown(npc, player, triggerClass, getCooldownDuration(trigger_type), getCooldownType(trigger_type));

        // Grab the determination of the action
        String determination = new NPCTag(npc).action(trigger_type, player, context);

        return new TriggerContext(determination, true);
    }

    /**
     * Contains whether the trigger successfully 'triggered' and any context that was
     * available while triggering or attempting to trigger.
     */
    public class TriggerContext {

        public TriggerContext(boolean triggered) {
            this.triggered = triggered;
        }

        public TriggerContext(String determination, boolean triggered) {
            this.determination = determination;
            this.triggered = triggered;
        }

        String determination;
        boolean triggered;

        public boolean hasDetermination() {
            return determination != null && !determination.equalsIgnoreCase("none");
        }

        public String getDetermination() {
            return determination;
        }

        public boolean wasTriggered() {
            return triggered;
        }

    }
}
