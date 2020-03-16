package com.denizenscript.denizen.scripts.commands.world;

import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizen.objects.properties.material.MaterialDirectional;
import com.denizenscript.denizen.utilities.blocks.MaterialCompat;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.NMSVersion;
import com.denizenscript.denizen.nms.interfaces.BlockData;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

public class SignCommand extends AbstractCommand {

    // <--[command]
    // @Name Sign
    // @Syntax sign (type:{automatic}/sign_post/wall_sign) (material:<material>) [<line>|...] [<location>] (direction:north/east/south/west)
    // @Required 1
    // @Short Modifies a sign.
    // @Group world
    //
    // @Description
    // Modifies a sign that replaces the text shown on it. If no sign is at the location, it replaces the location with the modified sign.
    //
    // The direction argument tells which direction the text shown. If a direction is not specified, it defaults to south.
    // Specify 'automatic' as a type to use whatever sign type and direction is already placed there.
    // If there is not already a sign there, defaults to a sign_post.
    //
    // Optionally specify a material to use. If not specified, will use an oak sign.
    //
    // @Tags
    // <LocationTag.sign_contents>
    //
    // @Usage
    // Use to edit some text on a sign
    // - sign type:automatic "Hello|this is|some|text" <player.location>
    //
    // @Usage
    // Use to show the time on a sign that points north
    // - sign type:automatic "I point|North.|System Time<&co>|<util.date.time>" <context.location> direction:north
    //
    // @Usage
    // Use to force a sign to be a wall_sign if no sign is found.
    // - sign type:wall_sign "Player<&co>|<player.name>|Online Players<&co>|<server.list_online_players.size>" <context.location>
    //
    // -->

    private enum Type {AUTOMATIC, SIGN_POST, WALL_SIGN}

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("type")
                    && arg.matchesEnum(Type.values())) {
                scriptEntry.addObject("type", arg.asElement());
            }
            else if (!scriptEntry.hasObject("location")
                    && arg.matchesArgumentType(LocationTag.class)) {
                scriptEntry.addObject("location", arg.asType(LocationTag.class).setPrefix("location"));
            }
            else if (!scriptEntry.hasObject("direction")
                    && arg.matchesPrefix("direction", "dir")) {
                scriptEntry.addObject("direction", arg.asElement());
            }
            else if (!scriptEntry.hasObject("material")
                    && arg.matchesPrefix("material")
                    && arg.matchesArgumentType(MaterialTag.class)) {
                scriptEntry.addObject("material", arg.asType(MaterialTag.class));
            }
            else if (!scriptEntry.hasObject("text")) {
                scriptEntry.addObject("text", arg.asType(ListTag.class));
            }
            else {
                arg.reportUnhandled();
            }
        }

        if (!scriptEntry.hasObject("location")) {
            throw new InvalidArgumentsException("Must specify a Sign location!");
        }
        if (!scriptEntry.hasObject("text")) {
            throw new InvalidArgumentsException("Must specify sign text!");
        }

        scriptEntry.defaultObject("type", new ElementTag(Type.AUTOMATIC.name()));
    }

    public void setWallSign(Block sign, BlockFace bf, MaterialTag material) {
        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_13)) {
            sign.setType(material == null ? MaterialCompat.WALL_SIGN : material.getMaterial(), false);
            MaterialTag signMaterial = new MaterialTag(sign);
            MaterialDirectional.getFrom(signMaterial).setFacing(bf);
            signMaterial.getModernData().setToBlock(sign);
        }
        else {
            org.bukkit.material.Sign sgntmp = new org.bukkit.material.Sign(MaterialCompat.WALL_SIGN);
            sgntmp.setFacingDirection(bf);
            BlockData blockData = NMSHandler.getBlockHelper().getBlockData(MaterialCompat.WALL_SIGN, sgntmp.getData());
            blockData.setBlock(sign, false);
        }
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {

        // Get objects
        String direction = scriptEntry.hasObject("direction") ? ((ElementTag) scriptEntry.getObject("direction")).asString() : null;
        ElementTag typeElement = scriptEntry.getElement("type");
        ListTag text = scriptEntry.getObjectTag("text");
        LocationTag location = scriptEntry.getObjectTag("location");
        MaterialTag material = scriptEntry.getObjectTag("material");

        // Report to dB
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), typeElement.debug()
                    + location.debug()
                    + (direction == null ? "" : ArgumentHelper.debugObj("direction", direction))
                    + (material == null ? "" : material.debug())
                    + text.debug());
        }

        Type type = Type.valueOf(typeElement.asString().toUpperCase());
        Block sign = location.getBlock();
        if (type != Type.AUTOMATIC
                || !MaterialCompat.isAnySign(sign.getType())) {
            if (type == Type.WALL_SIGN) {
                BlockFace bf;
                if (direction != null) {
                    bf = Utilities.chooseSignRotation(direction);
                }
                else {
                    bf = Utilities.chooseSignRotation(sign);
                }
                setWallSign(sign, bf, material);
            }
            else {
                sign.setType(material == null ? MaterialCompat.SIGN : material.getMaterial(), false);
                if (direction != null) {
                    Utilities.setSignRotation(sign.getState(), direction);
                }
            }
        }
        else if (!MaterialCompat.isAnySign(sign.getType())) {
            if (sign.getRelative(BlockFace.DOWN).getType().isSolid()) {
                sign.setType(material == null ? MaterialCompat.SIGN : material.getMaterial(), false);
            }
            else {
                BlockFace bf = Utilities.chooseSignRotation(sign);
                setWallSign(sign, bf, material);
            }
        }
        BlockState signState = sign.getState();

        Utilities.setSignLines((Sign) signState, text.toArray(new String[4]));
    }
}
