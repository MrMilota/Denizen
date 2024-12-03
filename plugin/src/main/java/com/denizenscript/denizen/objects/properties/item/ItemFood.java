package com.denizenscript.denizen.objects.properties.item;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.properties.Property;
import com.denizenscript.denizencore.objects.properties.PropertyParser;

import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;

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
    
        ItemMeta meta = item.getItemMeta();
        FoodComponent foodComponent = meta.getFood();
    
        list.add("nutrition=" + foodComponent.getNutrition());
        list.add("saturation=" + foodComponent.getSaturation());
        list.add("can_always_eat=" + foodComponent.canAlwaysEat());
    
        float eatSeconds = foodComponent.getEatSeconds();
        if (eatSeconds > 0) {
            list.add("eat_seconds=" + eatSeconds);
        }
    
        ItemStack convertsTo = foodComponent.getUsingConvertsTo();
        if (convertsTo != null) {
            list.add("using_converts_to=" + new ItemTag(convertsTo).identify());
        }
    
        List<FoodComponent.FoodEffect> effects = foodComponent.getEffects();
        for (FoodComponent.FoodEffect effect : effects) {
            StringBuilder effectStr = new StringBuilder();
            effectStr.append("effect=")
                    .append(effect.getEffect().getType().getKey().getKey())
                    .append(";duration=").append(effect.getEffect().getDuration() / 20)
                    .append(";amplifier=").append(effect.getEffect().getAmplifier())
                    .append(";probability=").append(effect.getProbability());
            list.add(effectStr.toString());
        }
    
        return list;
    }
    

    public static void register() {
        // <--[tag]
        // @attribute <ItemTag.food>
        // @returns ListTag
        // @mechanism ItemTag.food
        // @group properties
        // @description
        // Returns the food properties of the item, including nutrition, saturation, can_always_eat,
        // eat_seconds, using_converts_to, and effects.
        // Each effect is formatted as: effect=<type>;duration=<seconds>;amplifier=<level>;probability=<chance>
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
        // @input ListTag
        // @description
        // Sets the food's properties. Input should be a list of properties in key=value format.
        // Valid properties are:
        // - nutrition=<number> sets the amount of hunger restored
        // - saturation=<number> sets the saturation level
        // - can_always_eat=<true/false> whether the food can be eaten when not hungry
        // - eat_seconds=<number> sets how long it takes to eat the food
        // - using_converts_to=<item> what item it converts to after eating
        // - effect=<type>;duration=<seconds>;amplifier=<level>;probability=<chance> adds a potion effect
        // You can add multiple effects by adding multiple 'effect=' entries.
        // @example
        // # Creates a magic item that gives jump boost and strength
        // /ex inventory adjust slot:hand food:nutrition=5|saturation=1|can_always_eat=true|eat_seconds=0.3|using_converts_to=stone|effect=jump_boost;duration=60;amplifier=1;probability=0.1|effect=strength;duration=60;amplifier=1;probability=0.1
        // # Or for Denizen Script
        // food: 
        //  - nutrition=5
        //  - saturation=1
        //  - can_always_eat=true
        //  - eat_seconds=0.3
        //  - using_converts_to=stone
        //  - effect=jump_boost;duration=60;amplifier=1;probability=1
        //  - effect=strength;duration=60;amplifier=1;probability=1
        // @tags
        // <ItemTag.food>
        // -->
        if (mechanism.matches("food") && mechanism.hasValue()) {
            ItemMeta meta = item.getItemMeta();
            FoodComponent foodComponent = meta.getFood();

            ListTag list = mechanism.valueAsType(ListTag.class);
            MapTag foodMap = new MapTag();

            for (String entry : list) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    if (key.equals("effect")) {
                        String[] effectParts = value.split(";");
                        String effectType = effectParts[0].trim();
                        int duration = 30; 
                        int amplifier = 0;
                        float probability = 1.0f;
                        
                        for (int i = 1; i < effectParts.length; i++) {
                            String[] keyValue = effectParts[i].split("=");
                            if (keyValue.length == 2) {
                                String effectKey = keyValue[0].trim();
                                String effectValue = keyValue[1].trim();
                                
                                switch (effectKey) {
                                    case "duration":
                                        duration = Integer.parseInt(effectValue);
                                        break;
                                    case "amplifier":
                                        amplifier = Integer.parseInt(effectValue);
                                        break;
                                    case "probability":
                                        probability = Float.parseFloat(effectValue);
                                        break;
                                }
                            }
                        }
                        
                        PotionEffect effect = new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft(effectType)), duration * 20, amplifier);
                        foodComponent.addEffect(effect, probability);
                    } else {
                        foodMap.putObject(key, new ElementTag(value));
                    }
                }
            }

            foodComponent.setNutrition(foodMap.getElement("nutrition", "0").asInt());
            foodComponent.setSaturation(foodMap.getElement("saturation", "0").asFloat());
            foodComponent.setCanAlwaysEat(foodMap.getElement("can_always_eat", "false").asBoolean());

            if (foodMap.containsKey("eat_seconds")) {
                foodComponent.setEatSeconds(foodMap.getElement("eat_seconds").asFloat());
            }

            if (foodMap.containsKey("using_converts_to")) {
                ItemTag convertsToItem = foodMap.getObject("using_converts_to").asType(ItemTag.class, mechanism.context);
                foodComponent.setUsingConvertsTo(convertsToItem.getItemStack());
            }

            meta.setFood(foodComponent);
            item.setItemMeta(meta);
        }
    }
}