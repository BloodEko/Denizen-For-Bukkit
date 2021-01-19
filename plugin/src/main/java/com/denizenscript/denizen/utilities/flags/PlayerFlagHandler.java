package com.denizenscript.denizen.utilities.flags;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.SavableMapFlagTracker;
import com.denizenscript.denizencore.scripts.ScriptHelper;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PlayerFlagHandler implements Listener {

    public static long cacheTimeoutSeconds = 300;

    public static boolean asyncPreload = false;

    public static class CachedPlayerFlag {

        public long lastAccessed;

        public SavableMapFlagTracker tracker;

        public boolean savingNow = false;

        public boolean loadingNow = false;

        public boolean shouldExpire() {
            if (cacheTimeoutSeconds == -1) {
                return false;
            }
            if (cacheTimeoutSeconds == 0) {
                return true;
            }
            return lastAccessed + (cacheTimeoutSeconds * 1000) < System.currentTimeMillis();
        }
    }

    public static File dataFolder;

    public static HashMap<UUID, CachedPlayerFlag> playerFlagTrackerCache = new HashMap<>();

    public static HashMap<UUID, SoftReference<CachedPlayerFlag>> secondaryPlayerFlagTrackerCache = new HashMap<>();

    private static ArrayList<UUID> toClearCache = new ArrayList<>();

    public static void cleanCache() {
        if (cacheTimeoutSeconds == -1) {
            return;
        }
        toClearCache.clear();
        for (Map.Entry<UUID, SoftReference<CachedPlayerFlag>> entry : secondaryPlayerFlagTrackerCache.entrySet()) {
            if (entry.getValue().get() == null) {
                toClearCache.add(entry.getKey());
            }
        }
        for (UUID id : toClearCache) {
            toClearCache.remove(id);
        }
        long timeNow = System.currentTimeMillis();
        for (Map.Entry<UUID, CachedPlayerFlag> entry : playerFlagTrackerCache.entrySet()) {
            if (cacheTimeoutSeconds > 0 && entry.getValue().lastAccessed + (cacheTimeoutSeconds * 1000) < timeNow) {
                continue;
            }
            if (Bukkit.getPlayer(entry.getKey()) != null) {
                entry.getValue().lastAccessed = timeNow;
                continue;
            }
            saveThenExpire(entry.getKey(), entry.getValue());
        }
    }

    public static void saveThenExpire(UUID id, CachedPlayerFlag cache) {
        BukkitRunnable expireTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (cache.shouldExpire()) {
                    playerFlagTrackerCache.remove(id);
                    secondaryPlayerFlagTrackerCache.put(id, new SoftReference<>(cache));
                }
            }
        };
        if (cache.savingNow || cache.loadingNow) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    CachedPlayerFlag newCache = playerFlagTrackerCache.get(id);
                    if (newCache != null) {
                        saveThenExpire(id, newCache);
                    }
                }
            }.runTaskLater(Denizen.getInstance(), 10);
            return;
        }
        if (!cache.tracker.modified) {
            expireTask.runTaskLater(Denizen.getInstance(), 1);
            return;
        }
        cache.tracker.modified = false;
        String text = cache.tracker.toString();
        cache.savingNow = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    saveFlags(id, text);
                }
                catch (Throwable ex) {
                    Debug.echoError(ex);
                }
                cache.savingNow = false;
                expireTask.runTaskLater(Denizen.getInstance(), 1);
            }
        }.runTaskAsynchronously(Denizen.getInstance());
    }

    public static void loadFlags(UUID id, CachedPlayerFlag cache) {
        try {
            File realPath;
            File flagFile = new File(dataFolder, id.toString() + ".dat");
            if (flagFile.exists()) {
                realPath = flagFile;
            }
            else {
                File bakFile = new File(dataFolder, id.toString() + ".dat~2");
                if (bakFile.exists()) {
                    realPath = bakFile;
                }
                // Note: ~1 are likely corrupted, so ignore them.
                else {
                    cache.tracker = new SavableMapFlagTracker();
                    cache.loadingNow = false;
                    return;
                }
            }
            FileInputStream fis = new FileInputStream(realPath);
            String str = ScriptHelper.convertStreamToString(fis);
            fis.close();
            cache.tracker = new SavableMapFlagTracker(str);
        }
        catch (Throwable ex) {
            Debug.echoError("Failed to load player data for player ID '" + id + "'");
            Debug.echoError(ex);
            cache.tracker = new SavableMapFlagTracker();
        }
        cache.loadingNow = false;
    }

    public static AbstractFlagTracker getTrackerFor(UUID id) {
        CachedPlayerFlag cache = playerFlagTrackerCache.get(id);
        if (cache == null) {
            SoftReference<CachedPlayerFlag> softRef = secondaryPlayerFlagTrackerCache.get(id);
            if (softRef != null) {
                cache = softRef.get();
                if (cache != null) {
                    cache.lastAccessed = System.currentTimeMillis();
                    playerFlagTrackerCache.put(id, cache);
                    secondaryPlayerFlagTrackerCache.remove(id);
                    return null;
                }
            }
            cache = new CachedPlayerFlag();
            cache.lastAccessed = System.currentTimeMillis();
            cache.loadingNow = true;
            playerFlagTrackerCache.put(id, cache);
            loadFlags(id, cache);
        }
        else {
            while (cache.loadingNow) {
                try {
                    Thread.sleep(1);
                }
                catch (InterruptedException ex) {
                    Debug.echoError(ex);
                }
            }
        }
        return cache.tracker;
    }

    public static Future loadAsync(UUID id) {
        try {
            CachedPlayerFlag cache = playerFlagTrackerCache.get(id);
            if (cache != null) {
                return null;
            }
            SoftReference<CachedPlayerFlag> softRef = secondaryPlayerFlagTrackerCache.get(id);
            if (softRef != null) {
                cache = softRef.get();
                if (cache != null) {
                    cache.lastAccessed = System.currentTimeMillis();
                    playerFlagTrackerCache.put(id, cache);
                    secondaryPlayerFlagTrackerCache.remove(id);
                    return null;
                }
            }
            CachedPlayerFlag newCache = new CachedPlayerFlag();
            newCache.lastAccessed = System.currentTimeMillis();
            newCache.loadingNow = true;
            playerFlagTrackerCache.put(id, newCache);
            CompletableFuture future = new CompletableFuture();
            new BukkitRunnable() {
                @Override
                public void run() {
                    loadFlags(id, newCache);
                    future.complete(null);
                }
            }.runTaskAsynchronously(Denizen.getInstance());
            return future;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    public static void saveAllNow(boolean canSleep) {
        for (Map.Entry<UUID, CachedPlayerFlag> entry : playerFlagTrackerCache.entrySet()) {
            if (entry.getValue().tracker.modified) {
                if (!canSleep && entry.getValue().savingNow || entry.getValue().loadingNow) {
                    continue;
                }
                while (entry.getValue().savingNow || entry.getValue().loadingNow) {
                    try {
                        Thread.sleep(10);
                    }
                    catch (InterruptedException ex) {
                        Debug.echoError(ex);
                    }
                }
                entry.getValue().tracker.modified = false;
                saveFlags(entry.getKey(), entry.getValue().tracker.toString());
            }
        }
    }

    public static void saveFlags(UUID id, String flagData) {
        File saveToFile = new File(dataFolder, id.toString() + ".dat~1");
        try {
            Charset charset = ScriptHelper.encoding == null ? null : ScriptHelper.encoding.charset();
            FileOutputStream fiout = new FileOutputStream(saveToFile);
            OutputStreamWriter writer;
            if (charset == null) {
                writer = new OutputStreamWriter(fiout);
            }
            else {
                writer = new OutputStreamWriter(fiout, charset);
            }
            writer.write(flagData);
            writer.close();
            File bakFile = new File(dataFolder, id.toString() + ".dat~2");
            File realFile = new File(dataFolder, id.toString() + ".dat");
            if (realFile.exists()) {
                realFile.renameTo(bakFile);
            }
            saveToFile.renameTo(realFile);
            if (bakFile.exists()) {
                bakFile.delete();
            }
        }
        catch (Throwable ex) {
            Debug.echoError("Failed to save player data for player ID '" + id + "'");
            Debug.echoError(ex);
        }
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (!asyncPreload) {
            return;
        }
        UUID id = event.getUniqueId();
        if (!Bukkit.isPrimaryThread()) {
            Future<Future> future = Bukkit.getScheduler().callSyncMethod(Denizen.getInstance(), () -> {
                return loadAsync(id);
            });
            try {
                Future newFuture = future.get(15, TimeUnit.SECONDS);
                if (newFuture != null) {
                    newFuture.get(15, TimeUnit.SECONDS);
                }
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
        }
    }
}
