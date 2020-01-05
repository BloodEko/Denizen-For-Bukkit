package com.denizenscript.denizen.nms.interfaces;

import com.denizenscript.denizen.nms.abstracts.ImprovedOfflinePlayer;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public abstract class PlayerHelper {

    public abstract int getFlyKickCooldown(Player player);

    public abstract void setFlyKickCooldown(Player player, int ticks);

    public abstract int ticksPassedDuringCooldown(Player player);

    public abstract float getMaxAttackCooldownTicks(Player player);

    public abstract float getAttackCooldownPercent(Player player);

    public abstract void setAttackCooldown(Player player, int ticks);

    public abstract boolean hasChunkLoaded(Player player, Chunk chunk);

    public abstract int getPing(Player player);

    public abstract void setTemporaryOp(Player player, boolean op);

    public abstract void showEndCredits(Player player);

    public abstract ImprovedOfflinePlayer getOfflineData(UUID uuid);

    public abstract ImprovedOfflinePlayer getOfflineData(OfflinePlayer offlinePlayer);

    public abstract void resendDiscoveredRecipes(Player player);

    public abstract void quietlyAddRecipe(Player player, NamespacedKey key);

    public abstract void resendRecipeDetails(Player player);
}
