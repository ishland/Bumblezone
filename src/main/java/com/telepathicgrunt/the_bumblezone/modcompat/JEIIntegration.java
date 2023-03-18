package com.telepathicgrunt.the_bumblezone.modcompat;
import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.entities.queentrades.QueensTradeManager;
import com.telepathicgrunt.the_bumblezone.entities.queentrades.TradeEntryObj;
import com.telepathicgrunt.the_bumblezone.entities.queentrades.TradeEntryReducedObj;
import com.telepathicgrunt.the_bumblezone.items.recipes.IncenseCandleRecipe;
import com.telepathicgrunt.the_bumblezone.modcompat.recipecategories.QueenRandomizeTradesJEICategory;
import com.telepathicgrunt.the_bumblezone.modcompat.recipecategories.QueenRandomizerTradesInfo;
import com.telepathicgrunt.the_bumblezone.modcompat.recipecategories.QueenTradesInfo;
import com.telepathicgrunt.the_bumblezone.modcompat.recipecategories.QueenTradesJEICategory;
import com.telepathicgrunt.the_bumblezone.modinit.BzFluids;
import com.telepathicgrunt.the_bumblezone.modinit.BzItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.stream.Collectors;

@JeiPlugin
public class JEIIntegration implements IModPlugin {

	public static final RecipeType<QueenTradesInfo> QUEEN_TRADES = RecipeType.create(Bumblezone.MODID, "queen_trades", QueenTradesInfo.class);
	public static final RecipeType<QueenRandomizerTradesInfo> QUEEN_RANDOMIZE_TRADES = RecipeType.create(Bumblezone.MODID, "queen_color_randomizer_trades", QueenRandomizerTradesInfo.class);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Bumblezone.MODID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        BzItems.CUSTOM_CREATIVE_TAB_ITEMS.forEach(item -> addInfo(registration, item));
        addInfo(registration, BzItems.PILE_OF_POLLEN);
        addInfo(registration, BzFluids.SUGAR_WATER_FLUID);
        addInfo(registration, BzFluids.ROYAL_JELLY_FLUID);
        addInfo(registration, BzFluids.HONEY_FLUID);

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;
        level.getRecipeManager().byKey(new ResourceLocation(Bumblezone.MODID, "incense_candle_from_super_candles"))
                .ifPresent(recipe -> registerExtraRecipes(recipe, registration, true));
		level.getRecipeManager().byKey(new ResourceLocation(Bumblezone.MODID, "incense_candle"))
				.ifPresent(recipe -> registerExtraRecipes(recipe, registration, false));

		List<QueenTradesInfo> trades = new LinkedList<>();
		if (!QueensTradeManager.QUEENS_TRADE_MANAGER.tradeReduced.isEmpty()) {
			for (Map.Entry<Item, WeightedRandomList<TradeEntryReducedObj>> trade : QueensTradeManager.QUEENS_TRADE_MANAGER.tradeReduced.entrySet()) {
				for (TradeEntryReducedObj tradeResult : trade.getValue().unwrap()) {
					if (!tradeResult.randomizerTrade()) {
						trades.add(new QueenTradesInfo(trade.getKey().getDefaultInstance(), new ItemStack(tradeResult.item(), tradeResult.count()), tradeResult.xpReward(), tradeResult.weight(), tradeResult.totalGroupWeight()));
					}
				}
			}
		}
		registration.addRecipes(QUEEN_TRADES, trades);

		List<QueenRandomizerTradesInfo> randomizerTrades = new LinkedList<>();
		if (!QueensTradeManager.QUEENS_TRADE_MANAGER.tradeReduced.isEmpty()) {
			for (List<TradeEntryReducedObj> tradeEntry : QueensTradeManager.QUEENS_TRADE_MANAGER.tradeRandomizer) {
				List<ItemStack> randomizeStack = tradeEntry.stream().map(e -> e.item().getDefaultInstance()).toList();
				for (ItemStack input : randomizeStack) {
					randomizerTrades.add(new QueenRandomizerTradesInfo(input, randomizeStack, 1, randomizeStack.size()));
				}
			}
		}
		registration.addRecipes(QUEEN_RANDOMIZE_TRADES, randomizerTrades);
	}

    private static void addInfo(IRecipeRegistration registration, Item item) {
        registration.addIngredientInfo(
                new ItemStack(item),
                VanillaTypes.ITEM_STACK,
                Component.translatable(Bumblezone.MODID + "." + Registry.ITEM.getKey(item).getPath() + ".jei_description"));
    }

    private static void addInfo(IRecipeRegistration registration, Fluid fluid) {
        registration.addIngredientInfo(
                new FluidStack(fluid, 1),
                FabricTypes.FLUID_STACK,
                Component.translatable(Bumblezone.MODID + "." + Registry.FLUID.getKey(fluid).getPath() + ".jei_description"));
    }

	private static void registerExtraRecipes(Recipe<?> baseRecipe, IRecipeRegistration registration, boolean oneRecipeOnly) {
		if (baseRecipe instanceof IncenseCandleRecipe incenseCandleRecipe) {
			List<CraftingRecipe> extraRecipes = FakeIncenseCandleRecipeCreator.constructFakeRecipes(incenseCandleRecipe, oneRecipeOnly);
			registration.addRecipes(RecipeTypes.CRAFTING, extraRecipes);
		}
	}

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new QueenTradesJEICategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new QueenRandomizeTradesJEICategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(BzItems.BEE_QUEEN_SPAWN_EGG.getDefaultInstance(), QUEEN_TRADES);
        registration.addRecipeCatalyst(BzItems.BEE_QUEEN_SPAWN_EGG.getDefaultInstance(), QUEEN_RANDOMIZE_TRADES);
    }

    private static class FluidStack implements IJeiFluidIngredient {

        private final Fluid fluid;
        private final int count;

        private FluidStack(Fluid fluid, int count) {
            this.fluid = fluid;
            this.count = count;
        }


        @Override
        public Fluid getFluid() {
            return fluid;
        }

        @Override
        public long getAmount() {
            return count;
        }

        @Override
        public Optional<CompoundTag> getTag() {
            return Optional.empty();
        }
    }
}
