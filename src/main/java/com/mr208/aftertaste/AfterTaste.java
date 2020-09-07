package com.mr208.aftertaste;

import com.mr208.aftertaste.FoodEffect.RecipeTypeFoodEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.event.entity.item.ItemEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AfterTaste.MOD_ID)
public class AfterTaste {
	public static final String MOD_ID = "aftertaste";
	public static final Logger LOGGER =LogManager.getLogger("After Taste");
	
	public static final IRecipeType<FoodEffect> FOOD_EFFECT_TYPE = new RecipeTypeFoodEffect();
	
	public AfterTaste() {
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		
		bus.addGenericListener(IRecipeSerializer.class, this::registerRecipeSerializers);
		bus.addListener(this::onClientSetup);
		
		MinecraftForge.EVENT_BUS.addListener(this::onItemUsed);
	}
	
	private void onClientSetup(final FMLClientSetupEvent event) {
		MinecraftForge.EVENT_BUS.addListener(ClientEventHandler::onItemToolTip);
		MinecraftForge.EVENT_BUS.addListener(ClientEventHandler::tagsUpdate);
	}
	
	private void registerRecipeSerializers(Register<IRecipeSerializer<?>> event) {
		Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(FOOD_EFFECT_TYPE.toString()), FOOD_EFFECT_TYPE);
		
		event.getRegistry().register(FoodEffect.SERIALIZER);
	}
	
	private void onItemUsed(LivingEntityUseItemEvent.Finish event) {
		if(event.isCanceled())
			return;
		
		if(event.getEntityLiving()!=null && event.getEntityLiving().world != null && !event.getEntityLiving().world.isRemote) {
			
			for(final IRecipe<?> recipe : event.getEntityLiving().world.getRecipeManager().getRecipes()) {
				if(recipe instanceof FoodEffect) {
					final FoodEffect foodEffect = (FoodEffect) recipe;
					
					if(foodEffect.doesItemMatch(event.getItem())) {
						LivingEntity entity = event.getEntityLiving();
						for(EffectInstance inst : foodEffect.effects) {
							entity.addPotionEffect(new EffectInstance(inst));
						}
					}
				}
			}
		}
	}
}
