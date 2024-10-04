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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
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
    
        list.add(foodComponent.getNutrition() + "," +
                foodComponent.getSaturation() + "," +
                foodComponent.canAlwaysEat());
    
        float eatSeconds = foodComponent.getEatSeconds();
        if (eatSeconds > 0) {
            list.add("eat_seconds:" + eatSeconds);
        }
    
        ItemStack convertsTo = foodComponent.getUsingConvertsTo();
        if (convertsTo != null) {
            list.add("using_converts_to:" + new ItemTag(convertsTo).identify());
        }
    
        List<FoodComponent.FoodEffect> effects = foodComponent.getEffects();
        for (FoodComponent.FoodEffect effect : effects) {
            list.add("effect:" + effect.getEffect().getType().getKey() +
                    ",probability:" + effect.getProbability());
        }
    
        return list;
    }
    

    public ListTag getFoodDataMap() {
        ListTag list = new ListTag();
        MapTag foodMap = new MapTag();
    
        ItemMeta meta = item.getItemMeta();
        FoodComponent foodComponent = meta.getFood();
    
        foodMap.putObject("nutrition", new ElementTag(foodComponent.getNutrition()));
        foodMap.putObject("saturation", new ElementTag(foodComponent.getSaturation()));
        foodMap.putObject("canAlwaysEat", new ElementTag(foodComponent.canAlwaysEat()));
    
        float eatSeconds = foodComponent.getEatSeconds();
        if (eatSeconds > 0) {
            foodMap.putObject("eat_seconds", new ElementTag(eatSeconds));
        }
    
        ItemStack convertsTo = foodComponent.getUsingConvertsTo();
        if (convertsTo != null) {
            foodMap.putObject("using_converts_to", new ItemTag(convertsTo));
        }
    
        List<FoodComponent.FoodEffect> effects = foodComponent.getEffects();
        ListTag effectsList = new ListTag();
        for (FoodComponent.FoodEffect effect : effects) {
            MapTag effectMap = new MapTag();
            effectMap.putObject("effect", new ElementTag(effect.getEffect().getType().getKey().toString()));
            effectMap.putObject("duration", new ElementTag(effect.getEffect().getDuration()));
            effectMap.putObject("amplifier", new ElementTag(effect.getEffect().getAmplifier()));
            effectMap.putObject("probability", new ElementTag(effect.getProbability()));
            effectsList.addObject(effectMap);
        }
        if (!effectsList.isEmpty()) {
            foodMap.putObject("effects", effectsList);
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

        // <--[tag]
        // @attribute <ItemTag.food>
        // @returns ListTag
        // @group properties
        // @mechanism ItemTag.food
        // @description
        // Returns the food's property value as a ListTag of MapTags, matching the MapTag format of the mechanism.
        // -->
        PropertyParser.registerTag(ItemFood.class, ListTag.class, "food_data", (attribute, object) -> {
            return object.getFoodDataMap();
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
        // Sets the food's settings, including nutrition, saturation, canAlwaysEat, eatSeconds, usingConvertsTo, and effects.
        // The input should be a MapTag with keys "nutrition", "saturation", "can_always_eat", "eat_seconds", "using_converts_to", and "effects".
        // Effects are specified as a list of [effect=<effect_name>;duration=<duration_value(in sec)>;amplifer=<amplifer_value>;probability=<probability_value>]
        // Usage in DenizenScript (use map@, bcs in denizen don't work canBeType or im stupid):
        // 1. Comma-separated effect data in the format: nutrition,saturation, example: 1,1
        // 2. A MapTag:
        // Example (2 effects): map@[nutrition=0;saturation=0;can_always_eat=false;eat_seconds=1;using_converts_to=stone;effects=[effect=jump_boost;duration=60;amplifier=1;probability=1]|[effect=strength;duration=60;amplifier=1;probability=1]]
        // Usage in minecraft, like in DenizenScript just without map@
        // Example: /ex inventory adjust slot:hand food:[nutrition=5;saturation=1;can_always_eat=true;eat_seconds=1;using_converts_to=stone]
        // Note that this is an add operation, provide new values to update every settings.
        // @tags
        // <ItemTag.food>
        // <ItemTag.food_data>
        // -->
        if (mechanism.matches("food") && mechanism.hasValue()) {
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
                        
                        if (foodMap.containsKey("eat_seconds")) {
                            foodComponent.setEatSeconds(foodMap.getElement("eat_seconds").asFloat());
                        }

                        if (foodMap.containsKey("using_converts_to")) {
                            ItemTag convertsToItem = foodMap.getObject("using_converts_to").asType(ItemTag.class, mechanism.context);
                            foodComponent.setUsingConvertsTo(convertsToItem.getItemStack());
                        }

                        if (foodMap.containsKey("effects")) {
                            ListTag effectsList = foodMap.getObject("effects").asType(ListTag.class, mechanism.context);
                            for (String effectEntry : effectsList) {
                                MapTag effectMap = new ElementTag(effectEntry).asType(MapTag.class, mechanism.context);
                                String effectType = effectMap.getElement("effect").asString();
                                int duration = effectMap.getElement("duration", "600").asInt();
                                int amplifier = effectMap.getElement("amplifier", "0").asInt();
                                float probability = effectMap.getElement("probability", "1").asFloat();
                                
                                PotionEffect effect = new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft(effectType)), duration*20, amplifier);
                                foodComponent.addEffect(effect, probability);
                            }
                        }
                        meta.setFood(foodComponent);
                    } else {
                        String getFoodData = object.toString();
                        String[] data = getFoodData.split(",");
                        if (data.length == 2) {
                            FoodComponent foodComponent = meta.getFood();
                            foodComponent.setNutrition(new ElementTag(data[0]).asInt());
                            foodComponent.setSaturation(new ElementTag(data[1]).asFloat());
                            meta.setFood(foodComponent);
                        } else {
                            mechanism.echoError("Error: food data are missing or incorrect.");
                        }
                    }
                }
                item.setItemMeta(meta);
            }
        }
    }
}