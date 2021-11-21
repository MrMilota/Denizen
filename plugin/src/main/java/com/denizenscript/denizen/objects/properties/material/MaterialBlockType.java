package com.denizenscript.denizen.objects.properties.material;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.NMSVersion;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizen.utilities.MultiVersionHelper1_17;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.properties.Property;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.TechnicalPiston;

public class MaterialBlockType implements Property {

    public static boolean describes(ObjectTag material) {
        if (!(material instanceof MaterialTag)) {
            return false;
        }
        MaterialTag mat = (MaterialTag) material;
        if (!mat.hasModernData()) {
            return false;
        }
        BlockData data = mat.getModernData();
        return data instanceof Slab
                || data instanceof TechnicalPiston
                || data instanceof Campfire
                || (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_17) && data instanceof PointedDripstone);
    }

    public static MaterialBlockType getFrom(ObjectTag _material) {
        if (!describes(_material)) {
            return null;
        }
        else {
            return new MaterialBlockType((MaterialTag) _material);
        }
    }

    public static final String[] handledMechs = new String[] {
            "type", "slab_type"
    };

    private MaterialBlockType(MaterialTag _material) {
        material = _material;
    }

    public MaterialTag material;

    public static void registerTags() {

        // <--[tag]
        // @attribute <MaterialTag.type>
        // @returns ElementTag
        // @mechanism MaterialTag.type
        // @group properties
        // @description
        // Returns the current type of the block.
        // For slabs, output is TOP, BOTTOM, or DOUBLE.
        // For piston_heads, output is NORMAL or STICKY.
        // For campfires, output is NORMAL or SIGNAL.
        // For pointed dripstone, output is BASE, FRUSTUM, MIDDLE, TIP, or TIP_MERGE.
        // -->
        PropertyParser.<MaterialBlockType, ElementTag>registerStaticTag(ElementTag.class, "type", (attribute, material) -> {
            return new ElementTag(material.getPropertyString());
        }, "slab_type");
    }

    public boolean isSlab() {
        return material.getModernData() instanceof Slab;
    }

    public boolean isPistonHead() {
        return material.getModernData() instanceof TechnicalPiston;
    }

    public boolean isCampfire() {
        return material.getModernData() instanceof Campfire;
    }

    public boolean isDripstone() {
        return NMSHandler.getVersion().isAtLeast(NMSVersion.v1_17) && material.getModernData() instanceof PointedDripstone;
    }

    public Slab getSlab() {
        return (Slab) material.getModernData();
    }

    public TechnicalPiston getPistonHead() {
        return (TechnicalPiston) material.getModernData();
    }

    public Campfire getCampfire() {
        return (Campfire) material.getModernData();
    }

    /*public PointedDripstone getDripstone() { // TODO: 1.17
        return (PointedDripstone) material.getModernData();
    }*/

    @Override
    public String getPropertyString() {
        if (isSlab()) {
            return getSlab().getType().name();
        }
        else if (isCampfire()) {
            return getCampfire().isSignalFire() ? "SIGNAL" : "NORMAL";
        }
        else if (isPistonHead()) {
            return getPistonHead().getType().name();
        }
        else if (isDripstone()) {
            return ((PointedDripstone) material.getModernData()).getThickness().name(); // TODO: 1.17
        }
        return null; // Unreachable.
    }

    @Override
    public String getPropertyId() {
        return "type";
    }

    @Override
    public void adjust(Mechanism mechanism) {

        // <--[mechanism]
        // @object MaterialTag
        // @name type
        // @input ElementTag
        // @description
        // Sets the current type of the block.
        // For slabs, input is TOP, BOTTOM, or DOUBLE.
        // For piston_heads, input is NORMAL or STICKY.
        // For campfires, input is NORMAL or SIGNAL.
        // For pointed dripstone, input is BASE, FRUSTUM, MIDDLE, TIP, or TIP_MERGE.
        // @tags
        // <MaterialTag.type>
        // -->
        if (mechanism.matches("type") || (mechanism.matches("slab_type"))) {
            if (isSlab() && mechanism.requireEnum(false, Slab.Type.values())) {
                getSlab().setType(Slab.Type.valueOf(mechanism.getValue().asString().toUpperCase()));
            }
            else if (isCampfire()) {
                getCampfire().setSignalFire(CoreUtilities.equalsIgnoreCase(mechanism.getValue().asString(), "signal"));
            }
            else if (isPistonHead() && mechanism.requireEnum(false, TechnicalPiston.Type.values())) {
                getPistonHead().setType(TechnicalPiston.Type.valueOf(mechanism.getValue().asString().toUpperCase()));
            }
            else {
                MultiVersionHelper1_17.materialBlockTypeRunMech(mechanism, this);
            }
        }
    }
}
