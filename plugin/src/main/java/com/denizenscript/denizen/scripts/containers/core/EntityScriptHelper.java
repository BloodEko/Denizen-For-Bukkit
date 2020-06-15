package com.denizenscript.denizen.scripts.containers.core;

import com.denizenscript.denizen.scripts.commands.item.DisplayItemCommand;
import com.denizenscript.denizen.utilities.DataPersistenceHelper;
import com.denizenscript.denizen.utilities.DenizenAPI;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.world.DenizenWorldAccess;
import com.denizenscript.denizen.events.entity.EntityDespawnScriptEvent;
import com.denizenscript.denizen.flags.FlagManager;
import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.NMSVersion;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.properties.entity.EntityBoundingBox;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.*;

public class EntityScriptHelper implements Listener {

    static HashMap<UUID, String> entities = new HashMap<>();

    public EntityScriptHelper() {
        DenizenAPI.getCurrentInstance().getServer().getPluginManager()
                .registerEvents(this, DenizenAPI.getCurrentInstance());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        EntityTag.rememberEntity(entity);
        EntityDespawnScriptEvent.instance.entity = new EntityTag(entity);
        EntityDespawnScriptEvent.instance.cause = new ElementTag("DEATH");
        EntityDespawnScriptEvent.instance.cancelled = false;
        EntityDespawnScriptEvent.instance.fire();
        EntityTag.forgetEntity(entity);
        unlinkEntity(event.getEntity());
    }

    public static void linkWorld(World world) {
        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14)) {
            return;
        }
        NMSHandler.getWorldHelper().setWorldAccess(world, new DenizenWorldAccess());
    }

    public static void unlinkWorld(World world) {
        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14)) {
            return;
        }
        NMSHandler.getWorldHelper().removeWorldAccess(world);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        linkWorld(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        if (event.isCancelled()) {
            return;
        }
        unlinkWorld(event.getWorld());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (event instanceof Cancellable &&((Cancellable) event).isCancelled()) {
            return;
        }
        // TODO: This doesn't work. Awaiting Entity Despawn Event PR's for Bukkit:
        // Bukkit: https://github.com/Bukkit/Bukkit/pull/1070
        // CraftBukkit: https://github.com/Bukkit/CraftBukkit/pull/1386
        for (Entity ent : event.getChunk().getEntities()) {
            if (!(ent instanceof LivingEntity) || ((LivingEntity) ent).getRemoveWhenFarAway()) {
                EntityTag.rememberEntity(ent);
                EntityDespawnScriptEvent.instance.entity = new EntityTag(ent);
                EntityDespawnScriptEvent.instance.cause = new ElementTag("CHUNK_UNLOAD");
                EntityDespawnScriptEvent.instance.cancelled = false;
                EntityDespawnScriptEvent.instance.fire();
                EntityTag.forgetEntity(ent);
                unlinkEntity(ent);
            }
        }
    }

    public static void reloadEntities() {

        // Unizen start

        List<String> permanentDisplayItems = DenizenAPI.getCurrentInstance().getEntities().getStringList("entities.permanent display items");
        for (String uuid : permanentDisplayItems) {
            ((DisplayItemCommand) DenizenAPI.getCurrentInstance().getCommandRegistry().get("DISPLAYITEM")).getProtectedPermanentEntities().add(UUID.fromString(uuid));
        }

        List<String> removableDisplayItems = DenizenAPI.getCurrentInstance().getEntities().getStringList("entities.disposable display items");
        for (String uuid : removableDisplayItems) {
            Entity entity = Bukkit.getServer().getEntity(UUID.fromString(uuid));
            if (entity != null && entity.getType() == EntityType.DROPPED_ITEM) {
                entity.remove();
            }
        }

        // Unizen end

        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14)) {
            return;
        }
        entities.clear();
        ConfigurationSection entity_scripts = DenizenAPI.getCurrentInstance()
                .getEntities().getConfigurationSection("entities.scripts");
        if (entity_scripts == null) {
            return;
        }
        for (String Path : entity_scripts.getKeys(false)) {
            UUID id = UUID.fromString(Path);
            String scriptname = entity_scripts.getString(Path + ".scriptname");
            entities.put(id, scriptname);
        }
    }

    public static void saveEntities() {

        // Unizen start

        FileConfiguration entitySaves = DenizenAPI.getCurrentInstance().getEntities();

        // Save protected permanent items created by displayitem command, and slate any old displayitem for immediate removal (since they won't disappear on their own)
        List<String> permDisplayItems = new ArrayList<>();
        for (UUID uuid : ((DisplayItemCommand) DenizenAPI.getCurrentInstance().getCommandRegistry().instances.get("displayitem")).getProtectedPermanentEntities()) {
            permDisplayItems.add(uuid.toString());
        }
        entitySaves.set("entities.permanent display items", permDisplayItems);

        List<String> tempDisplayItems = new ArrayList<>();
        for (UUID uuid : ((DisplayItemCommand) DenizenAPI.getCurrentInstance().getCommandRegistry().instances.get("displayitem")).getProtectedEntities()) {
            tempDisplayItems.add(uuid.toString());
        }
        entitySaves.set("entities.disposable display items", tempDisplayItems);

        // Unizen end

        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14)) {
            return;
        }

        entitySaves.set("entities.scripts", null);
        for (Map.Entry<UUID, String> entry : entities.entrySet()) {
            entitySaves.set("entities.scripts." + entry.getKey() + ".scriptname", entry.getValue());
        }
    }

    /**
     * Indicates whether a specified entity has a custom entity script.
     */
    public static boolean entityHasScript(Entity ent) {
        return getEntityScript(ent) != null;
    }

    /**
     * Returns the name of the entity script that defined this entity, or null if none.
     */
    public static String getEntityScript(Entity ent) {
        if (ent == null) {
            return null;
        }
        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14)) {
            if (!DataPersistenceHelper.hasDenizenKey(ent, "entity_script")) {
                return null;
            }
            ObjectTag scriptObject = DataPersistenceHelper.getDenizenKey(ent, "entity_script");
            if (!(scriptObject instanceof ScriptTag)) {
                return null;
            }
            return ((ScriptTag) scriptObject).getName();
        }
        return getEntityScript(ent.getUniqueId());
    }

    /**
     * Returns the name of the entity script that defined the entity by this UUID, or null if none.
     */
    public static String getEntityScript(UUID entID) {
        if (entID == null) {
            return null;
        }
        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14)) {
            return getEntityScript(Bukkit.getEntity(entID));
        }
        return entities.get(entID);
    }

    /**
     * Marks the entity as having been created by a specified script.
     */
    public static void setEntityScript(Entity ent, String script) {
        if (ent == null || ent.getUniqueId() == null || script == null) {
            return;
        }
        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14)) {
            ScriptTag scriptObj = ScriptTag.valueOf(script);
            if (scriptObj == null) {
                Debug.echoError("Can't set entity script to '" + script + "': not a valid script!");
            }
            DataPersistenceHelper.setDenizenKey(ent, "entity_script", scriptObj);
            return;
        }
        entities.put(ent.getUniqueId(), script);
    }

    /**
     * Removes the entity from the list of scripted entities.
     */
    public static void unlinkEntity(final Entity ent) {
        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14)) {
            return;
        }
        if (ent == null || ent.getUniqueId() == null) {
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(DenizenAPI.getCurrentInstance(),
                new Runnable() {
                    @Override
                    public void run() {
                        entities.remove(ent.getUniqueId());
                        FlagManager.clearEntityFlags(new EntityTag(ent));
                        EntityBoundingBox.remove(ent.getUniqueId());
                    }
                }, 5);
    }
}
