package com.mr208.aftertaste;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectUtils;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientEventHandler {
	static Map<ResourceLocation,FoodEffect> tagFoodEffectMap = new HashMap<>();
	
	public static void createCache() {
		for(IRecipe recipe : Minecraft.getInstance().world.getRecipeManager().getRecipes()) {
			if(recipe instanceof FoodEffect) {
				FoodEffect effectRecipe = (FoodEffect) recipe;
				tagFoodEffectMap.put(effectRecipe.itemTag.getId(), effectRecipe);
			}
		}
	}
	
	@SubscribeEvent
	public static void onItemToolTip(ItemTooltipEvent event) {
		if(!event.getItemStack().isEmpty())
		{
			if(tagFoodEffectMap.isEmpty())
				createCache();
			
			ItemStack heldstack=event.getItemStack();
			for(ResourceLocation tag : heldstack.getItem().getTags())
				if(tagFoodEffectMap.containsKey(tag)) {
					addTooltipInfo(event.getToolTip(), tagFoodEffectMap.get(tag));
					return;
				}
			
		}
	}
	
	@SubscribeEvent
	public static void tagsUpdate(TagsUpdatedEvent event)
	{
		tagFoodEffectMap.clear();
	}
	
	public static void addTooltipInfo(List<ITextComponent> tooltip, FoodEffect foodEffectIn) {
		tooltip.add(new TranslationTextComponent("tooltip.aftertaste.effect").applyTextStyle(TextFormatting.GRAY));
		for(EffectInstance instance:  foodEffectIn.effects) {
			ITextComponent component = new StringTextComponent(" ");
			component.appendSibling(new TranslationTextComponent(instance.getEffectName()));
			
			if (instance.getAmplifier() > 0) {
				component.appendText(" ").appendSibling(new TranslationTextComponent("potion.potency." + instance.getAmplifier()));
			}
			
			if (instance.getDuration() > 20) {
				component.appendText(" (").appendText(EffectUtils.getPotionDurationString(instance, 1.0F)).appendText(")");
			}
			
			tooltip.add(component.applyTextStyle(instance.getPotion().getEffectType().getColor()));
		}
	}
}
