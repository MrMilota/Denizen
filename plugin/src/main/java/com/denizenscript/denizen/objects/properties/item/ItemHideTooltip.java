package com.denizenscript.denizen.objects.properties.item;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.properties.Property;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemHideTooltip implements Property {

    public static boolean describes(ObjectTag item) {
        return item instanceof ItemTag 
            && ((ItemTag) item).getBukkitMaterial() != Material.AIR;
    }

    public static ItemHideTooltip getFrom(ObjectTag _item) {
        if (!describes(_item)) {
            return null;
        }
        else {
            return new ItemHideTooltip((ItemTag) _item);
        }
    }

    public static final String[] handledTags = new String[] {
            "hide_tooltip"
    };

    public static final String[] handledMechs = new String[] {
            "hide_tooltip"
    };

    public ItemHideTooltip(ItemTag _item) {
        item = _item;
    }

    ItemTag item;

    @Override
    public String getPropertyString() {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.isHideTooltip()) {
            return "true";
        }
        return null;
    }

    @Override
    public String getPropertyId() {
        return "hide_tooltip";
    }

    public static void register() {
        PropertyParser.registerTag(ItemHideTooltip.class, ElementTag.class, "hide_tooltip", (attribute, object) -> {
            return new ElementTag(object.item.getItemMeta().isHideTooltip());
        });
    }

    @Override
    public void adjust(Mechanism mechanism) {
        if (mechanism.matches("hide_tooltip")) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                mechanism.echoError("ItemMeta is null.");
                return;
            }

            if (mechanism.requireBoolean()) {
                boolean hideTooltip = Boolean.parseBoolean(mechanism.getValue().toString());
                meta.setHideTooltip(hideTooltip);
                item.setItemMeta(meta);
            }
        }
    }
}