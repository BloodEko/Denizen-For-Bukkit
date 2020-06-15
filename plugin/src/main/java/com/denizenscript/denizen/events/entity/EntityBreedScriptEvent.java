package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

public class EntityBreedScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // entity breeds
    // <entity> breeds
    //
    // @Regex ^on [^\s]+ breeds$
    //
    // @Switch in:<area> to only process the event if it occurred within a specified area.
    //
    // @Cancellable true
    //
    // @Triggers when two entities breed.
    //
    // @Context
    // <context.breeder> returns the EntityTag responsible for breeding, if it exists.
    // <context.child> returns the child EntityTag.
    // <context.mother> returns the parent EntityTag creating the child. The child will spawn at the mother's location.
    // <context.father> returns the other parent EntityTag.
    // <context.item> returns the ItemTag used to initiate breeding, if it exists.
    // <context.experience> returns the amount of experience granted by breeding.
    //
    // @Determine
    // ElementTag(Number) to set the amount of experience granted by breeding.
    //
    // -->

    public EntityBreedScriptEvent() {
        instance = this;
    }

    public static EntityBreedScriptEvent instance;
    private EntityTag entity;
    private EntityTag breeder;
    private EntityTag father;
    private EntityTag mother;
    private ItemTag item;
    private int experience;
    public EntityBreedEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventArgLowerAt(1).equals("breeds");
    }

    @Override
    public boolean matches(ScriptPath path) {

        if (!tryEntity(entity, path.eventArgLowerAt(0))) {
            return false;
        }

        if (!runInCheck(path, entity.getLocation())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "EntityBreeds";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag && ((ElementTag) determinationObj).isInt()) {
            experience = ((ElementTag) determinationObj).asInt();
            event.setExperience(experience);
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(null, null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("child")) {
            return entity.getDenizenObject();
        }
        else if (name.equals("breeder")) {
            return breeder.getDenizenObject();
        }
        else if (name.equals("father")) {
            return father.getDenizenObject();
        }
        else if (name.equals("mother")) {
            return mother.getDenizenObject();
        }
        else if (name.equals("item")) {
            return item;
        }
        else if (name.equals("experience")) {
            return new ElementTag(experience);
        }
        return super.getContext(name);
    }

    @Override
    public void cancellationChanged() {
        // Prevent entities from continuing to breed with each other
        if (cancelled) {
            NMSHandler.getEntityHelper().setBreeding((Animals) father.getLivingEntity(), false);
            NMSHandler.getEntityHelper().setBreeding((Animals) mother.getLivingEntity(), false);
        }
        super.cancellationChanged();
    }

    @EventHandler
    public void onEntityBreeds(EntityBreedEvent event) {
        Entity entity = event.getEntity();
        this.entity = new EntityTag(entity);
        breeder = new EntityTag(event.getBreeder());
        father = new EntityTag(event.getFather());
        mother = new EntityTag(event.getMother());
        item = new ItemTag(event.getBredWith());
        experience = event.getExperience();
        this.event = event;
        EntityTag.rememberEntity(entity);
        fire(event);
        EntityTag.forgetEntity(entity);
    }
}
