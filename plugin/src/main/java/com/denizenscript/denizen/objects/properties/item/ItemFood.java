package com.denizenscript.denizen.objects.properties.item;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.properties.Property;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.denizencore.utilities.CoreUtilities;

import org.bukkit.inventory.meta.components.FoodComponent;

import java.util.Collection;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemFood implements Property {
    
    public static boolean describes(ObjectTag item) {
        return item instanceof ItemTag 
            && ((ItemTag) item).getBukkitMaterial() != Material.AIR;
    }
    
    public static ItemFood getFrom(ObjectTag _item) {
        if (!describes(_item)) {
            return null;
        }
        else {
            return new ItemFood((ItemTag) _item);
        }
    }

    public static final String[] handledTags = new String[] {
            "food"
    };

    public static final String[] handledMechs = new String[] {
            "food"
    };

    public ItemFood(ItemTag _item) {
        item = _item;
    }

    ItemTag item;

    public ListTag getFoodData() {
        ListTag list = new ListTag();
        MapTag foodMap = new MapTag();
    
        // Проверяем, является ли предмет съедобным
        if (item.getItemMeta() != null && item.getBukkitMaterial().isEdible()) {
            ItemMeta meta = item.getItemMeta();
    
            // Проверяем, если у предмета есть FoodComponent
            if (meta instanceof FoodComponent) {
                FoodComponent foodComponent = (FoodComponent) meta;
    
                // Записываем значения в MapTag
                foodMap.putObject("nutrition", new ElementTag(foodComponent.getNutrition()));
                foodMap.putObject("saturation", new ElementTag(foodComponent.getSaturation()));
                foodMap.putObject("canAlwaysEat", new ElementTag(foodComponent.canAlwaysEat()));
            }
        }
    
        list.addObject(foodMap);
        return list;
    }

    public static void register() {

        // <--[tag]
        // @attribute <ItemTag.food>
        // @returns ListTag
        // @group properties
        // @mechanism ItemTag.food
        // @description
        // Returns the food's property value as a list, matching the non-MapTag format of the mechanism.
        // Consider instead using <@link tag ItemTag.food_data>
        // -->
        PropertyParser.registerTag(ItemFood.class, ListTag.class, "food", (attribute, object) -> {
            return object.getFoodData();
        });
    };

    @Override 
    public String getPropertyString() {
        ListTag data = getFoodData();
        return data.size() > 0 ? data.identify() : null;
    }

    @Override
    public String getPropertyId() {
        return "food";
    }

    @Override
    public void adjust(Mechanism mechanism) {
        // <--[mechanism]
        // @object ItemTag
        // @name food
        // @input MapTag
        // @description
        // Sets the food's settings.
        // The input should be a MapTag with keys "nutrition", "saturation", and "can_always_eat".
        // For example: [nutrition=4;saturation=0.6;can_always_eat=true]
        // @tags
        // <ItemTag.food>
        // -->
        if (mechanism.matches("food")) {
            ItemMeta meta = item.getItemMeta();
        
            if (!mechanism.hasValue()) {
                return;
            }
            else {
                Collection<ObjectTag> list = CoreUtilities.objectToList(mechanism.getValue(), mechanism.context);
                for (ObjectTag object : list) {
                    if (object.canBeType(MapTag.class)) {
                        MapTag foodMap = object.asType(MapTag.class, mechanism.context);
                        FoodComponent foodComponent = meta.getFood();
                        foodComponent.setNutrition(foodMap.getElement("nutrition", "0").asInt());
                        foodComponent.setSaturation(foodMap.getElement("saturation", "0").asFloat());
                        foodComponent.setCanAlwaysEat(foodMap.getElement("can_always_eat", "false").asBoolean());
                        meta.setFood(foodComponent);
                    } else {
                        mechanism.echoError("Ошибка: данные о еде отсутствуют или неверны.");
                    }
                }
                item.setItemMeta(meta);
            }
        }        
    }        
}