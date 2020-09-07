package com.mr208.aftertaste;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class FoodEffect implements IRecipe<IInventory>
{
	public static final Serializer SERIALIZER = new Serializer();
	protected final ItemStack outputdummy = ItemStack.EMPTY;
	
	protected final ResourceLocation id;
	protected final Tag<Item> itemTag;
	protected final EffectInstance[] effects;
	
	protected FoodEffect(ResourceLocation id, Tag<Item> tagIn, EffectInstance... effectInstances) {
		this.id = id;
		this.itemTag = tagIn;
		this.effects = effectInstances;
	}
	
	public boolean doesItemMatch(ItemStack stackIn) {
		return doesItemMatch(stackIn.getItem());
	}
	
	public boolean doesItemMatch(Item itemIn) {
		return this.itemTag.contains(itemIn);
	}
	
	public EffectInstance[] getEffectInstances() {
		return this.effects;
	}
	
	@Override
	public boolean matches(IInventory inv, World worldIn)
	{
		return false;
	}
	
	@Override
	public ItemStack getCraftingResult(IInventory inv)
	{
		return this.outputdummy;
	}
	
	@Override
	public boolean canFit(int width, int height)
	{
		return false;
	}
	
	@Override
	public ItemStack getRecipeOutput()
	{
		return this.outputdummy;
	}
	
	@Override
	public ResourceLocation getId()
	{
		return this.id;
	}
	
	@Override
	public IRecipeSerializer<?> getSerializer()
	{
		return this.SERIALIZER;
	}
	
	@Override
	public IRecipeType<?> getType()
	{
		return AfterTaste.FOOD_EFFECT_TYPE;
	}
	
	@Override
	public boolean isDynamic()
	{
		return true;
	}
	
	private static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<FoodEffect> {
		Serializer() {
			this.setRegistryName(new ResourceLocation(AfterTaste.MOD_ID,"food_effect"));
		}
		
		@Override
		public FoodEffect read(ResourceLocation recipeId, JsonObject json)
		{
			final Tag<Item> itemTag = ItemTags.getCollection().getOrCreate(new ResourceLocation(JSONUtils.getString(json,"itemTag")));
			ArrayList<EffectInstance> tempList = new ArrayList<>();
			final JsonElement effectElement = JSONUtils.isJsonArray(json,"effects") ? JSONUtils.getJsonArray(json,"effects") : JSONUtils.getJsonObject(json,"effects");
			
			if(effectElement.isJsonNull())
				throw new IllegalStateException("Effects must be defined for " + recipeId.toString());
			
			if(effectElement.isJsonArray()){
				effectElement.getAsJsonArray().forEach(entry -> {
					if(!entry.isJsonObject())
						throw new IllegalStateException("Effect Entry is not JSON Object for " + recipeId.toString());
					JsonObject entryObj = entry.getAsJsonObject();
					String effect = entryObj.has("name") ? JSONUtils.getString(entryObj, "name") : "minecraft:milk";
					int duration = entryObj.has("duration") ? JSONUtils.getInt(entryObj, "duration") : 20;
					int amp = entryObj.has("amp") ? JSONUtils.getInt(entryObj, "amp") : 0;
					boolean ambient = entryObj.has("ambient") ? JSONUtils.getBoolean(entryObj, "ambient") : false;
					boolean particles = entryObj.has("particles") ? JSONUtils.getBoolean(entryObj, "particles") : true;
					
					Effect effectIn = ForgeRegistries.POTIONS.getValue(new ResourceLocation(effect));
					if(effectIn == null)
						throw new IllegalStateException("Unable to find Effect for " + recipeId.toString());
					
					tempList.add(new EffectInstance(effectIn, duration, amp, ambient, particles));
				});
			} else {
				JsonObject entryObj = effectElement.getAsJsonObject();
				String effect = entryObj.has("name") ? JSONUtils.getString(entryObj, "name") : "minecraft:milk";
				int duration = entryObj.has("duration") ? JSONUtils.getInt(entryObj, "duration") : 20;
				int amp = entryObj.has("amp") ? JSONUtils.getInt(entryObj, "amp") : 0;
				boolean ambient = entryObj.has("ambient") ? JSONUtils.getBoolean(entryObj, "ambient") : false;
				boolean particles = entryObj.has("particles") ? JSONUtils.getBoolean(entryObj, "particles") : true;
				
				Effect effectIn = ForgeRegistries.POTIONS.getValue(new ResourceLocation(effect));
				if(effectIn == null)
					throw new IllegalStateException("Unable to find Effect for " + recipeId.toString());
				
				tempList.add(new EffectInstance(effectIn, duration, amp, ambient, particles));
			}
			
			return new FoodEffect(recipeId, itemTag, tempList.toArray(new EffectInstance[0]));
		}
		
		@Nullable
		@Override
		public FoodEffect read(ResourceLocation recipeId, PacketBuffer buffer)
		{
			Tag<Item> item = ItemTags.getCollection().getOrCreate(buffer.readResourceLocation());
			EffectInstance[] effects;
			ArrayList<EffectInstance> tempList = new ArrayList<>();
			
			CompoundNBT effectTag = buffer.readCompoundTag();
			
			ListNBT list = effectTag.getList("Effects", 10);
			
			list.forEach(inbt -> {
				assert inbt instanceof CompoundNBT;
				tempList.add(EffectInstance.read((CompoundNBT)inbt));
			});
			
			if(!tempList.isEmpty())
				effects = tempList.toArray(new EffectInstance[0]);
			else
				effects = null;
			
			if(effects == null)
				throw new IllegalStateException("No Effects Registered for " + recipeId.toString());
			
			return new FoodEffect(recipeId, item, effects);
		}
		
		@Override
		public void write(PacketBuffer buffer, FoodEffect recipe)
		{
			buffer.writeResourceLocation(recipe.itemTag.getId());
			
			ListNBT list = new ListNBT();
			
			for(EffectInstance inst:recipe.effects)
				list.add(inst.write(new CompoundNBT()));
			CompoundNBT effects = new CompoundNBT();
			effects.put("Effects", list);
			buffer.writeCompoundTag(effects);
		}
	}
	
	public static class RecipeTypeFoodEffect implements IRecipeType<FoodEffect> {
		@Override
		public String toString()
		{
			return AfterTaste.MOD_ID + ":food_effect";
		}
	}
}
