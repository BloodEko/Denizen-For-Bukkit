package com.denizenscript.denizen.scripts.containers.core;

import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.objects.InventoryTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.NPCTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class InventoryScriptContainer extends ScriptContainer {

    // <--[language]
    // @name Inventory Script Containers
    // @group Script Container System
    // @description
    // Inventory script containers are an easy way to pre-define custom inventories for use within scripts.
    // Inventory scripts work with the InventoryTag object, and can be fetched with the Object Fetcher by using the
    // InventoryTag constructor InventoryTag_script_name.
    //
    // Example: - inventory open d:MyInventoryScript
    //
    // The following is the format for the container.
    //
    // The 'inventory:' key is required, other keys vary based on the type.
    // Some types will require you define either 'size:' or 'slots:' (or both).
    // 'Procedural items:' and 'definitions:' are optional, and should only be defined if needed.
    //
    // <code>
    // # The name of the script is the same name that you can use to construct a new
    // # InventoryTag based on this inventory script. For example, an inventory script named 'Super_Cool_Inventory'
    // # can be referred to as 'Super_Cool_Inventory'.
    // Inventory_Script_Name:
    //
    //   type: inventory
    //
    //   # Must be a valid inventory type.
    //   # Valid inventory types: BREWING, CHEST, DISPENSER, ENCHANTING, ENDER_CHEST, HOPPER, PLAYER, WORKBENCH
    //   # | All inventory scripts MUST have this key!
    //   inventory: inventory type
    //
    //   # The title can be anything you wish. Use color tags to make colored titles.
    //   # Note that titles only work for some inventory types, including CHEST, DISPENSER, FURNACE, ENCHANTING, and HOPPER.
    //   # | MOST inventory scripts should have this key!
    //   title: custom title
    //
    //   # The size must be a multiple of 9. It is recommended to not go above 54, as it will not show correctly when a player looks into it.
    //   # | Some inventory scripts should have this key! Most can exclude it if 'slots' is used.
    //   size: 27
    //
    //   # You can use definitions to define items to use in the slots. These are not like normal script definitions, and do not need to be in a definition tag.
    //   # | Some inventory scripts MAY have this key, but it is optional. Most scripts will just specify items directly.
    //   definitions:
    //     my item: ItemTag
    //     other item: ItemTag
    //
    //   # Procedural items can be used to specify a list of ItemTags for the empty slots to be filled with.
    //   # Each item in the list represents the next available empty slot.
    //   # When the inventory has no more empty slots, it will discard any remaining items in the list.
    //   # A slot is considered empty when it has no value specified in the slots section.
    //   # If the slot is filled with air, it will no longer count as being empty.
    //   # | Most inventory scripts should exclude this key, but it may be useful in some cases.
    //   procedural items:
    //     - define list <list[]>
    //     - foreach <server.list_online_players>:
    //       - define item human_skull[skull_skin=<[value].name>]
    //       - define list <[list].include[<[item]>]>
    //     - determine <[list]>
    //
    //   # You can specify the items in the slots of the inventory. For empty spaces, simply put an empty "slot" value, like "[]".
    //   # Note the quotes around the entire lines.
    //   # | Most inventory scripts SHOULD have this key!
    //   slots:
    //     - "[] [] [] [my item] [ItemTag] [] [other item] [] []"
    //     - "[my item] [] [] [] [] [ItemTag] [ItemTag] [] []"
    //     - "[] [] [] [] [] [] [] [] [other item]"
    // </code>
    //
    // -->

    public InventoryScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);
        InventoryScriptHelper.inventory_scripts.put(getName(), this);
    }

    public InventoryType getInventoryType() {
        String typeStr = getString("inventory", "CHEST");

        try {
            return InventoryType.valueOf(typeStr.toUpperCase());
        }
        catch (Exception e) {
            return InventoryType.CHEST;
        }
    }

    public InventoryTag getInventoryFrom(PlayerTag player, NPCTag npc) {

        // TODO: Clean all this code!

        InventoryTag inventory = null;
        BukkitTagContext context = new BukkitTagContext(player, npc, new ScriptTag(this));

        try {
            if (contains("inventory")) {
                try {
                    inventory = new InventoryTag(InventoryType.valueOf(getString("inventory").toUpperCase()));
                    if (contains("title")) {
                        inventory.setTitle(TagManager.tag(getString("title"), context));
                    }
                    inventory.setIdentifiers("script", getName());
                }
                catch (IllegalArgumentException ex) {
                    Debug.echoError("Invalid inventory type specified. Assuming \"CHEST\" (" + ex.getMessage() + ")");
                }
            }
            int size = 0;
            if (contains("size")) {
                if (inventory != null && !getInventoryType().name().equalsIgnoreCase("chest")) {
                    Debug.echoError("You can only set the size of chest inventories!");
                }
                else {
                    String sizeText = TagManager.tag(getString("size"), context);
                    if (!ArgumentHelper.matchesInteger(sizeText)) {
                        Debug.echoError("Inventory script '" + getName() + "' has invalid (not-a-number) size value.");
                    }
                    else {
                        size = Integer.parseInt(sizeText);
                    }

                    if (size == 0) {
                        Debug.echoError("Inventory size can't be 0. Assuming default of inventory type...");
                    }
                    if (size % 9 != 0) {
                        size = (int) Math.ceil(size / 9.0) * 9;
                        Debug.echoError("Inventory size must be a multiple of 9! Rounding up to " + size + "...");
                    }
                    if (size < 0) {
                        size = size * -1;
                        Debug.echoError("Inventory size must be a positive number! Inverting to " + size + "...");
                    }

                    inventory = new InventoryTag(size, contains("title") ? TagManager.tag(getString("title"), context) : "Chest");
                    inventory.setIdentifiers("script", getName());
                }
            }
            if (size == 0) {
                if (contains("slots") && getInventoryType().name().equalsIgnoreCase("chest")) {
                    size = getStringList("slots").size() * 9;
                }
                else {
                    size = getInventoryType().getDefaultSize();
                }
            }
            boolean[] filledSlots = new boolean[size];
            if (contains("slots")) {
                ItemStack[] finalItems = new ItemStack[size];
                int itemsAdded = 0;
                for (String items : getStringList("slots")) {
                    items = TagManager.tag(items, context).trim();
                    if (items.isEmpty()) {
                        continue;
                    }
                    if (!items.startsWith("[") || !items.endsWith("]")) {
                        Debug.echoError("Inventory script \"" + getName() + "\" has an invalid slots line: ["
                                + items + "]... Ignoring it");
                        continue;
                    }
                    String[] itemsInLine = items.substring(1, items.length() - 1).split("\\[?\\]?\\s+\\[", -1);
                    for (String item : itemsInLine) {
                        if (contains("definitions." + item)) {
                            ItemTag def = ItemTag.valueOf(TagManager.tag(getString("definitions." + item), context), context);
                            if (def == null) {
                                Debug.echoError("Invalid definition '" + item + "' in inventory script '" + getName() + "'" + "... Ignoring it and assuming \"AIR\"");
                                finalItems[itemsAdded] = new ItemStack(Material.AIR);
                            }
                            else {
                                finalItems[itemsAdded] = def.getItemStack();
                            }
                        }
                        else if (ItemTag.matches(item)) {
                            try {
                                finalItems[itemsAdded] = ItemTag.valueOf(item, context).getItemStack();
                            }
                            catch (Exception ex) {
                                Debug.echoError("Inventory script \"" + getName() + "\" has an invalid slot item: ["
                                        + item + "]...");
                                Debug.echoError(ex);
                            }
                        }
                        else {
                            finalItems[itemsAdded] = new ItemStack(Material.AIR);
                            if (!item.isEmpty()) {
                                Debug.echoError("Inventory script \"" + getName() + "\" has an invalid slot item: ["
                                        + item + "]... Ignoring it and assuming \"AIR\"");
                            }
                        }
                        filledSlots[itemsAdded] = !item.isEmpty();
                        itemsAdded++;
                    }
                }
                if (inventory == null) {
                    size = finalItems.length % 9 == 0 ? finalItems.length : (int) (Math.ceil(finalItems.length / 9.0) * 9);
                    inventory = new InventoryTag(size == 0 ? 9 : size,
                            contains("title") ? TagManager.tag(getString("title"), context) : "Chest");
                }
                inventory.setContents(finalItems);
            }
            if (contains("procedural items")) {
                if (inventory == null) {
                    size = InventoryType.CHEST.getDefaultSize();
                    inventory = new InventoryTag(size, contains("title") ? TagManager.tag(getString("title"), context) : "Chest");
                }
                List<ScriptEntry> entries = getEntries(new BukkitScriptEntryData(player, npc), "procedural items");
                if (!entries.isEmpty()) {
                    InstantQueue queue = new InstantQueue("INV_SCRIPT_ITEM_PROC");
                    queue.addEntries(entries);
                    if (contains("definitions")) {
                        YamlConfiguration section = getConfigurationSection("definitions");
                        for (StringHolder string : section.getKeys(false)) {
                            String definition = string.str;
                            queue.addDefinition(definition, section.getString(definition));
                        }
                    }
                    queue.start();
                    if (queue.determinations != null) {
                        ListTag list = ListTag.getListFor(queue.determinations.getObject(0), context);
                        if (list != null) {
                            int x = 0;
                            for (ItemTag item : list.filter(ItemTag.class, context, true)) {
                                while (x < filledSlots.length && filledSlots[x]) {
                                    x++;
                                }
                                if (x >= filledSlots.length || filledSlots[x]) {
                                    break;
                                }
                                inventory.setSlots(x, item.getItemStack());
                                filledSlots[x] = true;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Debug.echoError("Woah! An exception has been called with this inventory script!");
            Debug.echoError(e);
            inventory = null;
        }

        if (inventory != null) {
            InventoryTag.trackTemporaryInventory(inventory);
            inventory.scriptName = getName();
        }

        return inventory;

    }
}
