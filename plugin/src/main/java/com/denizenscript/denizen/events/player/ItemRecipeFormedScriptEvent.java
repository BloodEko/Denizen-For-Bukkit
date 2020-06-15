package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.InventoryTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class ItemRecipeFormedScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // item recipe formed
    // <item> recipe formed
    // <material> recipe formed
    //
    // @Regex ^on [^\s]+ recipe formed$
    //
    // @Cancellable true
    //
    // @Triggers when an item's recipe is correctly formed.
    // @Context
    // <context.inventory> returns the InventoryTag of the crafting inventory.
    // <context.item> returns the ItemTag to be formed in the result slot.
    // <context.recipe> returns a ListTag of ItemTags in the recipe.
    //
    // @Determine
    // ItemTag to change the item that is formed in the result slot.
    //
    // @Player Always.
    //
    // -->

    public ItemRecipeFormedScriptEvent() {
        instance = this;
    }

    public static ItemRecipeFormedScriptEvent instance;

    public PrepareItemCraftEvent event;
    public ItemTag result;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (path.eventArgLowerAt(1).equals("crafted")) {
            return true;
        }
        if (!path.eventArgLowerAt(1).equals("recipe") || !path.eventArgLowerAt(2).equals("formed")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String eItem = path.eventArgLowerAt(0);

        if (!tryItem(result, eItem)) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "ItemRecipeFormed";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String determination = determinationObj.toString();
        if (ItemTag.matches(determination)) {
            ItemTag result = ItemTag.valueOf(determination, path.container);
            event.getInventory().setResult(result.getItemStack());
            return true;
        }
        else {
            return super.applyDetermination(path, determinationObj);
        }
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(EntityTag.getPlayerFrom(event.getView().getPlayer()), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("item")) {
            Recipe eRecipe = event.getRecipe();
            return new ItemTag(eRecipe.getResult());
        }
        else if (name.equals("inventory")) {
            return InventoryTag.mirrorBukkitInventory(event.getInventory());
        }
        else if (name.equals("recipe")) {
            ListTag recipe = new ListTag();
            for (ItemStack itemStack : event.getInventory().getMatrix()) {
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    recipe.addObject(new ItemTag(itemStack));
                }
                else {
                    recipe.addObject(new ItemTag(Material.AIR));
                }
            }
            return recipe;
        }
        return super.getContext(name);
    }

    @Override
    public void cancellationChanged() {
        if (cancelled) { // Hacked-in cancellation helper
            event.getInventory().setResult(null);
        }
        super.cancellationChanged();
    }

    @EventHandler
    public void onRecipeFormed(PrepareItemCraftEvent event) {
        this.event = event;
        Recipe eRecipe = event.getRecipe();
        if (eRecipe == null || eRecipe.getResult() == null) {
            return;
        }
        result = new ItemTag(eRecipe.getResult());
        cancelled = false;
        fire(event);
    }
}
