package com.denizenscript.denizen.utilities;

import com.denizenscript.denizen.objects.*;
import com.denizenscript.denizen.tags.core.*;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.depends.Depends;
import com.denizenscript.denizencore.objects.ObjectFetcher;

public class CommonRegistries {

    // <--[language]
    // @name ObjectTags
    // @group Object System
    // @description
    // ObjectTags are a system put into place by Denizen that make working with things, or 'objects',
    // in Minecraft and Denizen easier. Many parts of scripts will require some kind of object as an
    // argument, identifier/type, or such as in world events, part of an event name. The ObjectTags notation
    // system helps both you and Denizen know what type of objects are being referenced and worked with.
    //
    // So when should you use ObjectTags? In arguments, event names, replaceable tags, configs, flags, and
    // more! If you're just a beginner, you've probably been using them without even realizing it!
    //
    // ObjectTag is a broader term for a 'type' of object that more specifically represents something,
    // such as a LocationTag or ScriptTag, often times just referred to as a 'location' or 'script'. Denizen
    // employs many object types that you should be familiar with. You'll notice that many times objects
    // are referenced with their 'ObjectTag notation' which is in the format of 'x@', the x being the specific
    // notation of an object type. Example: player objects use the p@ notation, and locations use l@.
    // This notation is automatically generated when directly displaying objects, or saving them into data files.
    // It should never be manually typed into a script.
    //
    // Let's take the tag system, for example. It uses the ObjectTags system pretty heavily. For instance,
    // every time you use <player.name> or <npc.id>, you're using a ObjectTag, which brings us to a simple
    // clarification: Why <player.name> and not <PlayerTag.name>? That's because Denizen allows Players,
    // NPCs and other 'in-context objects' to be linked to certain scripts. In short, <player> already
    // contains a reference to a specific player, such as the player that died in a world event 'on player dies'.
    // <PlayerTag.name> is instead the format for documentation, with "PlayerTag" simply indicating 'any player object here'.
    //
    // ObjectTags can be used to CREATE new instances of objects, too! Though not all types allow 'new'
    // objects to be created, many do, such as ItemTags. With the use of tags, it's easy to reference a specific
    // item, say -- an item in the Player's hand -- items are also able to use a constructor to make a new item,
    // and say, drop it in the world. Take the case of the command/usage '- drop diamond_ore'. The item object
    // used is a brand new diamond_ore, which is then dropped by the command to a location of your choice -- just
    // specify an additional location argument.
    //
    // There's a great deal more to learn about ObjectTags, so be sure to check out each object type for more
    // specific information. While all ObjectTags share some features, many contain goodies on top of that!
    // -->

    // <--[language]
    // @name Tick
    // @group Common Terminology
    // @description
    // A 'tick' is usually referred to as 1/20th of a second, the speed at which Minecraft servers update
    // and process everything on them.
    // -->

    public static void registerMainTagHandlers() {
        // Objects
        new BiomeTagBase();
        new ChunkTagBase();
        new ColorTagBase();
        new CuboidTagBase();
        new EllipsoidTagBase();
        new EntityTagBase();
        new InventoryTagBase();
        new ItemTagBase();
        new LocationTagBase();
        new MaterialTagBase();
        if (Depends.citizens != null) {
            new NPCTagBase();
        }
        new PlayerTagBase();
        new PluginTagBase();
        new TradeTagBase();
        new WorldTagBase();
        // Other bases
        new ServerTagBase();
        new TextTagBase();
        new ParseTagBase();
    }

    public static void registerMainObjects() {
        ObjectFetcher.registerWithObjectFetcher(BiomeTag.class, BiomeTag.tagProcessor); // b@
        ObjectFetcher.registerWithObjectFetcher(ChunkTag.class, ChunkTag.tagProcessor); // ch@
        ObjectFetcher.registerWithObjectFetcher(ColorTag.class, ColorTag.tagProcessor); // co@
        ObjectFetcher.registerWithObjectFetcher(CuboidTag.class, CuboidTag.tagProcessor); // cu@
        ObjectFetcher.registerWithObjectFetcher(EllipsoidTag.class, EllipsoidTag.tagProcessor); // ellipsoid@
        ObjectFetcher.registerWithObjectFetcher(EntityTag.class, EntityTag.tagProcessor); // e@
        ObjectFetcher.registerWithObjectFetcher(InventoryTag.class, InventoryTag.tagProcessor); // in@
        ObjectFetcher.registerWithObjectFetcher(ItemTag.class, ItemTag.tagProcessor); // i@
        ObjectFetcher.registerWithObjectFetcher(LocationTag.class, LocationTag.tagProcessor); // l@
        ObjectFetcher.registerWithObjectFetcher(MaterialTag.class, MaterialTag.tagProcessor); // m@
        if (Depends.citizens != null) {
            ObjectFetcher.registerWithObjectFetcher(NPCTag.class, NPCTag.tagProcessor); // n@
        }
        ObjectFetcher.registerWithObjectFetcher(PlayerTag.class, PlayerTag.tagProcessor); // p@
        ObjectFetcher.registerWithObjectFetcher(PluginTag.class, PluginTag.tagProcessor); // pl@
        ObjectFetcher.registerWithObjectFetcher(TradeTag.class, TradeTag.tagProcessor); // trade@
        ObjectFetcher.registerWithObjectFetcher(WorldTag.class, WorldTag.tagProcessor); // w@
        StringBuilder debug = new StringBuilder(256);
        for (ObjectFetcher.ObjectType<?> objectType : ObjectFetcher.objectsByPrefix.values()) {
            debug.append(objectType.clazz.getSimpleName()).append(" as ").append(objectType.prefix).append(", ");
        }
        Debug.echoApproval("Loaded core object types: [" + debug.substring(0, debug.length() - 2) + "]");
    }
}
