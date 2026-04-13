package net.silvertide.alchemical.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.silvertide.alchemical.registry.BlockRegistry;
import net.silvertide.alchemical.registry.ItemRegistry;

import java.util.concurrent.CompletableFuture;

public class AlchemicalRecipeProvider extends RecipeProvider {

    public AlchemicalRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Athanor — brewing stand alchemical station
        //   N A N
        //   N F N
        //   N B N
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, BlockRegistry.ATHANOR_ITEM.get())
                .pattern("NAN")
                .pattern("NFN")
                .pattern("NBN")
                .define('N', Blocks.BLACKSTONE)
                .define('A', Blocks.CAULDRON)
                .define('F', Blocks.FURNACE)
                .define('B', Items.BREWING_STAND)
                .unlockedBy("has_brewing_stand", has(Items.BREWING_STAND))
                .unlockedBy("has_blaze_rod", has(Items.BLAZE_ROD))
                .save(output);

        // Elixir Flask
        //   G A G
        //   G H G
        //     G
        ShapedRecipeBuilder.shaped(RecipeCategory.BREWING, ItemRegistry.ELIXIR.get())
                .pattern("GAG")
                .pattern("GHG")
                .pattern(" G ")
                .define('G', Items.GLASS_BOTTLE)
                .define('A', Items.AMETHYST_SHARD)
                .define('H', Items.HEART_OF_THE_SEA)
                .unlockedBy("has_heart_of_the_sea", has(Items.HEART_OF_THE_SEA))
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .save(output);
    }
}
