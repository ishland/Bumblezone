package com.telepathicgrunt.the_bumblezone.configs;

import com.telepathicgrunt.the_bumblezone.utils.ConfigHelper;
import com.telepathicgrunt.the_bumblezone.utils.ConfigHelper.ConfigValueListener;
import net.minecraftforge.common.ForgeConfigSpec;

public class BzModCompatibilityConfigs
{
	public static class BzModCompatibilityConfigValues
	{
		public ConfigValueListener<Boolean> allowBottledBeesCompat;
		public ConfigValueListener<Boolean> allowHoneyWandCompat;
		public ConfigValueListener<Boolean> crystallizedHoneyWorldgen;
		public ConfigValueListener<Boolean> allowRegularCandlesBeeDungeon;
		public ConfigValueListener<Boolean> allowScentedCandlesBeeDungeon;
		public ConfigValueListener<Boolean> allowScentedCandlesSpiderBeeDungeon;
		public ConfigValueListener<Integer> powerfulCandlesRarityBeeDungeon;
		public ConfigValueListener<Integer> powerfulCandlesRaritySpiderBeeDungeon;

	    public ConfigValueListener<Boolean> allowPotionOfBeesCompat;
	    public ConfigValueListener<Boolean> allowSplashPotionOfBeesCompat;

		public ConfigValueListener<Boolean> spawnResourcefulBeesBeesMob;
		public ConfigValueListener<Boolean> useSpawnInWorldConfigFromRB;
		public ConfigValueListener<Boolean> spawnResourcefulBeesHoneycombVariants;
		public ConfigValueListener<Integer> RBGreatHoneycombRarityBeeDungeon;
		public ConfigValueListener<Double> RBOreHoneycombSpawnRateBeeDungeon;
		public ConfigValueListener<Integer> RBGreatHoneycombRaritySpiderBeeDungeon;
		public ConfigValueListener<Double> RBOreHoneycombSpawnRateSpiderBeeDungeon;
		public ConfigValueListener<Boolean> RBBeesWaxWorldgen;
		public ConfigValueListener<String> RBBlacklistedBees;

	    public ConfigValueListener<Boolean> spawnProductiveBeesBeesMob;
		public ConfigValueListener<Boolean> allowHoneyTreatCompat;
	    public ConfigValueListener<Boolean> spawnProductiveBeesHoneycombVariants;
	    public ConfigValueListener<Integer> PBGreatHoneycombRarityBeeDungeon;
	    public ConfigValueListener<Double> PBOreHoneycombSpawnRateBeeDungeon;
	    public ConfigValueListener<Integer> PBGreatHoneycombRaritySpiderBeeDungeon;
	    public ConfigValueListener<Double> PBOreHoneycombSpawnRateSpiderBeeDungeon;
		public ConfigValueListener<String> PBBlacklistedBees;

		public ConfigValueListener<Boolean> spawnPokecubeBeePokemon;

		public ConfigValueListener<Boolean> allowCCandlesBeeDungeon;
		public ConfigValueListener<Boolean> allowCCandlesSpiderBeeDungeon;

		public ConfigValueListener<Boolean> allowCACCandlesBeeDungeon;
		public ConfigValueListener<Boolean> allowCACCandlesSpiderBeeDungeon;

		public ConfigValueListener<Boolean> allowCharmTradeCompat;
		public ConfigValueListener<Boolean> allowBuzzierBeesTradeCompat;
		public ConfigValueListener<Boolean> allowResorucefulBeesTradeCompat;

	    public BzModCompatibilityConfigValues(ForgeConfigSpec.Builder builder, ConfigHelper.Subscriber subscriber) {

	        builder.push("Mod Compatibility Options");

					builder.push("Caves and Cliffs Backport Options");

					allowCACCandlesBeeDungeon = subscriber.subscribe(builder
							.comment(" \n-----------------------------------------------------\n\n"
									+" Spawn Caves and Cliffs Backport Candles in Bee Dungeons.\n")
							.translation("the_bumblezone.config.allowcaccandlesbeedungeon")
							.define("allowCACCandlesBeeDungeon", true));

					allowCACCandlesSpiderBeeDungeon = subscriber.subscribe(builder
							.comment(" \n-----------------------------------------------------\n\n"
									+" Spawn Caves and Cliffs Backport Candles in Spider Infested Bee Dungeons.\n")
							.translation("the_bumblezone.config.allowcaccandlesspiderbeedungeon")
							.define("allowCACCandlesSpiderBeeDungeon", true));

					builder.pop();

					builder.push("Charm Options");

					allowCCandlesBeeDungeon = subscriber.subscribe(builder
							.comment(" \n-----------------------------------------------------\n\n"
									+" Spawn Charm Candles in Bee Dungeons.\n")
							.translation("the_bumblezone.config.allowccandlesbeedungeon")
							.define("allowCCandlesBeeDungeon", true));

					allowCCandlesSpiderBeeDungeon = subscriber.subscribe(builder
							.comment(" \n-----------------------------------------------------\n\n"
									+" Spawn Charm Candles in Spider Infested Bee Dungeons.\n")
							.translation("the_bumblezone.config.allowccandlesspiderbeedungeon")
							.define("allowCCandlesSpiderBeeDungeon", true));

					allowCharmTradeCompat = subscriber.subscribe(builder
							.comment(" \n-----------------------------------------------------\n\n"
									+" Add new trades to Charm's Beekeeper Villager.\n")
							.translation("the_bumblezone.config.allowcharmtradecompat")
							.define("allowCharmTradeCompat", true));

					builder.pop();

					builder.push("Pokecube Options");

					spawnPokecubeBeePokemon = subscriber.subscribe(builder
							.comment(" \n-----------------------------------------------------\n\n"
									+" Spawn Pokecube's bee-like pokemon in The Bumblezone and from Honey Brood Blocks.\n")
							.translation("the_bumblezone.config.spawnpokecubebeepokemon")
							.define("spawnPokecubeBeePokemon", true));

					builder.pop();

					builder.push("Resourceful Bees Options");

					spawnResourcefulBeesBeesMob = subscriber.subscribe(builder
							.comment(" \n-----------------------------------------------------\n\n"
									+" Spawn Resourceful Bees in The Bumblezone and from Honey Brood Blocks\n"
									+" alongside regular bees at a 1/15th chance when spawning regular bees.\n")
							.translation("the_bumblezone.config.spawnresourcefulbeesbeesmob")
							.define("spawnResourcefulBeesBeesMob", true));

					spawnResourcefulBeesHoneycombVariants = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Spawn Resourceful Bees's various honeycomb variants in The Bumblezone\n"
								+" at all kinds of heights and height bands. Start exploring to find \n"
								+" where they spawn!\n"
								+" \n"
								+" NOTE: Will require a restart of the world to take effect. \n")
						.translation("the_bumblezone.config.spawnproductivebeeshoneycombvariants")
						.define("spawnResourcefulBeesHoneycombVariants", true));

					RBBeesWaxWorldgen = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Spawn Resourceful Bees's Wax Block as part of The Bumblezone's worldgen.\n")
						.translation("the_bumblezone.config.rbbeeswaxworldgen")
						.define("RBBeesWaxWorldgen", true));

					useSpawnInWorldConfigFromRB = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Use Resourceful Bees's canSpawnInWorld config on their bee data\n"
								+" to know what bees to spawn in Bumblezone. This will stack with\n"
								+" RBBlacklistedBees config entry that Bumblezone uses.\n"
								+" Bees blacklisted from either will not spawn and their combs will not spawn either.\n"
								+" \n"
								+" NOTE: Will require a restart of the world to take effect. \n")
						.translation("the_bumblezone.config.usespawninworldconfigfromrb")
						.define("useSpawnInWorldConfigFromRB", false));

					RBBlacklistedBees = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Blacklist what Resourceful Bees bees should not spawn in Bumblezone. \n"
								+" Separate each entry with a comma. Example: \"resourcefulbees:iron,resourcefulbees:coal\"\n"
								+" \n"
								+" Note: Blacklisted bees will automatically blacklist their respective combs from worldgen too.")
						.translation("the_bumblezone.config.rbblacklistedbees")
						.define("RBBlacklistedBees", ""));

					RBOreHoneycombSpawnRateBeeDungeon = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" How much of Bee Dungeons is made of ore-based honeycombs.\n"
								+" 0 is no or honeycombs, 1 is max ore honeycombs, and default is 0.3D\n")
						.translation("the_bumblezone.config.rborehoneycombspawnratebeedungeon")
						.defineInRange("RBOreHoneycombSpawnRateBeeDungeon", 0.3D, 0D, 1D));

					RBGreatHoneycombRarityBeeDungeon = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" How rare good ore-based Honeycombs (diamonds, ender, emerald, etc) are \n"
								+" in Bee Dungeons. \n"
								+" Higher numbers means more rare. Default rate is 3.\n")
						.translation("the_bumblezone.config.rbgreathoneycombraritybeedungeon")
						.defineInRange("RBGreatHoneycombRarityBeeDungeon", 2, 1, 1001));

					RBOreHoneycombSpawnRateSpiderBeeDungeon = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" How much of Spider Infested Bee Dungeons is made of ore-based honeycombs.\n"
								+" 0 is no or honeycombs, 1 is max ore honeycombs, and default is 0.1D\n")
						.translation("the_bumblezone.config.rborehoneycombspawnratespiderbeedungeon")
						.defineInRange("RBOreHoneycombSpawnRateSpiderBeeDungeon", 0.1D, 0D, 1D));

					RBGreatHoneycombRaritySpiderBeeDungeon = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" How rare good ore-based Honeycombs (diamonds, ender, emerald, etc) are \n"
								+" in Spider Infested Bee Dungeons. \n"
								+" Higher numbers means more rare. Default rate is 2.\n")
						.translation("the_bumblezone.config.rbgreathoneycombrarityspiderbeedungeon")
						.defineInRange("RBGreatHoneycombRaritySpiderBeeDungeon", 2, 1, 1001));

					allowResorucefulBeesTradeCompat = subscriber.subscribe(builder
							.comment(" \n-----------------------------------------------------\n\n"
									+" Add new trades to Resourceful Bees's Beekeeper Villager.\n")
							.translation("the_bumblezone.config.allowresorucefulbeestradecompat")
							.define("allowResorucefulBeesTradeCompat", true));

				builder.pop();

				builder.push("Productive Bees Options");

					spawnProductiveBeesBeesMob = subscriber.subscribe(builder
		                    .comment(" \n-----------------------------------------------------\n\n"
		                    		+" Spawn Productive Bees in The Bumblezone and from Honey Brood Blocks\n"
		                    		+" alongside regular bees at a 1/15th chance when spawning regular bees.\n")
		                    .translation("the_bumblezone.config.spawnproductivebeesbeesmob")
		                    .define("spawnProductiveBeesBeesMob", true));

					spawnProductiveBeesHoneycombVariants = subscriber.subscribe(builder
		                    .comment(" \n-----------------------------------------------------\n\n"
		                    		+" Spawn Productive Bees's various honeycomb variants in The Bumblezone\n"
		                    		+" at all kinds of heights and height bands. Start exploring to find \n"
		                    		+" where they spawn!"
		                    		+" \n"
		                    		+" NOTE: Will require a restart of the world to take effect. \n")
		                    .translation("the_bumblezone.config.spawnproductivebeeshoneycombvariants")
		                    .define("spawnProductiveBeesHoneycombVariants", true));

					allowHoneyTreatCompat = subscriber.subscribe(builder
							.comment(" \n-----------------------------------------------------\n\n"
									+" Allow Honey Treat to be able to feed bees and Honeycomb Brood Blocks.\n")
							.translation("the_bumblezone.config.allowhoneytreatcompat")
							.define("allowHoneyTreatCompat", true));


	        		PBOreHoneycombSpawnRateBeeDungeon = subscriber.subscribe(builder
	    		            .comment(" \n-----------------------------------------------------\n\n"
	    		            		+" How much of Bee Dungeons is made of ore-based honeycombs.\n"
	    		            		+" 0 is no or honeycombs, 1 is max ore honeycombs, and default is 0.3D\n")
	    		            .translation("the_bumblezone.config.pborehoneycombspawnratebeedungeon")
	    		            .defineInRange("PBOreHoneycombSpawnRateBeeDungeon", 0.3D, 0D, 1D));

	        		PBGreatHoneycombRarityBeeDungeon = subscriber.subscribe(builder
	    		            .comment(" \n-----------------------------------------------------\n\n"
	    		            		+" How rare good ore-based Honeycombs (diamonds, ender, emerald, etc) are \n"
	    		            		+" in Bee Dungeons. \n"
	    		            		+" Higher numbers means more rare. Default rate is 3.\n")
	    		            .translation("the_bumblezone.config.pbgreathoneycombraritybeedungeon")
	    		            .defineInRange("PBGreatHoneycombRarityBeeDungeon", 2, 1, 1001));

	        		PBOreHoneycombSpawnRateSpiderBeeDungeon = subscriber.subscribe(builder
	    		            .comment(" \n-----------------------------------------------------\n\n"
	    		            		+" How much of Spider Infested Bee Dungeons is made of ore-based honeycombs.\n"
	    		            		+" 0 is no or honeycombs, 1 is max ore honeycombs, and default is 0.1D\n")
	    		            .translation("the_bumblezone.config.pborehoneycombspawnratespiderbeedungeon")
	    		            .defineInRange("PBOreHoneycombSpawnRateSpiderBeeDungeon", 0.1D, 0D, 1D));

	        		PBGreatHoneycombRaritySpiderBeeDungeon = subscriber.subscribe(builder
	    		            .comment(" \n-----------------------------------------------------\n\n"
	    		            		+" How rare good ore-based Honeycombs (diamonds, ender, emerald, etc) are \n"
	    		            		+" in Spider Infested Bee Dungeons. \n"
	    		            		+" Higher numbers means more rare. Default rate is 2.\n")
	    		            .translation("the_bumblezone.config.pbgreathoneycombrarityspiderbeedungeon")
	    		            .defineInRange("PBGreatHoneycombRaritySpiderBeeDungeon", 2, 1, 1001));


					PBBlacklistedBees = subscriber.subscribe(builder
							.comment(" \n-----------------------------------------------------\n\n"
									+" Blacklist what Productive Bees bees should not spawn in Bumblezone. \n"
									+" Separate each entry with a comma. Example: \"productivebees:iron,productivebees:coal\"\n"
									+" \n"
									+" Note: this is only for the entities. To blacklist blocks as well,\n"
									+" use a datapack to add blacklisted blocks to this tag:\n"
									+" data/the_bumblezone/tags/blocks/blacklisted_productive_bees_combs.json\n")
							.translation("the_bumblezone.config.pbblacklistedbees")
							.define("PBBlacklistedBees", ""));

	            builder.pop();


	            builder.push("Potion of Bees Options");

	            		allowPotionOfBeesCompat = subscriber.subscribe(builder
		                    .comment(" \n-----------------------------------------------------\n\n"
		                    		+" Allow Potion of Bees item to turn Empty Honeycomb Brood blocks \n"
			                    	+" back into Honeycomb Brood Blocks with a larva in it. (affects Dispenser too)\n")
		                    .translation("the_bumblezone.config.allowpotionofbeescompat")
		                    .define("allowPotionOfBeesCompat", true));

	            		allowSplashPotionOfBeesCompat = subscriber.subscribe(builder
			            .comment(" \n-----------------------------------------------------\n\n"
			                    	+" Allow Splash Potion of Bees item to turn Empty Honeycomb Brood ",
				               " blocks back into Honeycomb Brood Blocks with a larva in it when ",
				               " the potion is thrown and splashed near the block. (affects Dispenser too)\n")
			            .translation("the_bumblezone.config.allowsplashpotionofbeescompat")
			            .define("allowSplashPotionOfBeesCompat", true));

	            builder.pop();


			builder.push("Buzzier Bees Options");

				allowBottledBeesCompat = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Allow Bottles Bees item to turn Empty Honeycomb Brood blocks \n"
								+" back into Honeycomb Brood Blocks with a larva in it. (affects Dispenser too)\n")
						.translation("the_bumblezone.config.allowbottledbeescompat")
						.define("allowBottledBeesCompat", true));

				allowHoneyWandCompat = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Allow Honey Wand to take honey from Filled Porous Honeycomb Block \n"
								+" and put honey into Porous Honeycomb Block without angering bees.\n")
						.translation("the_bumblezone.config.allowhoneywandcompat")
						.define("allowHoneyWandCompat", true));

				crystallizedHoneyWorldgen = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Place Buzzier Bees's Crystallized Honey Blocks on the /r/n"
								+" surface of land around sea level and above.\n")
						.translation("the_bumblezone.config.crystallizedhoneyworldgen")
						.define("crystallizedHoneyWorldgen", true));

				allowRegularCandlesBeeDungeon = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Allow Bee Dungeons to have normal unscented candles./r/n")
						.translation("the_bumblezone.config.allowregularcandlesbeedungeon")
						.define("allowRegularCandlesBeeDungeon", true));

				allowScentedCandlesBeeDungeon = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Allow Bee Dungeons to have scented candles that gives status effects./r/n")
						.translation("the_bumblezone.config.allowscentedcandlesbeedungeon")
						.define("allowScentedCandlesBeeDungeon", true));

				allowScentedCandlesSpiderBeeDungeon = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Allow Spider Infested Bee Dungeons to have scented candles that gives status effects./r/n")
						.translation("the_bumblezone.config.allowscentedcandlesspiderbeedungeon")
						.define("allowScentedCandlesSpiderBeeDungeon", true));

				powerfulCandlesRarityBeeDungeon = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" How rare are powerful candles in Bee Dungeons. \n"
								+" Higher numbers means more rare.\n"
								+" Default rate is 2.\n")
						.translation("the_bumblezone.config.powerfulcandlesraritybeedungeon")
						.defineInRange("powerfulCandlesRarityBeeDungeon", 2, 0, 10));

				powerfulCandlesRaritySpiderBeeDungeon = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" How rare are powerful candles in Spider Infested Bee Dungeons. \n"
								+" Higher numbers means more rare.\n"
								+" Default rate is 2.\n")
						.translation("the_bumblezone.config.powerfulcandlesrarityspiderbeedungeon")
						.defineInRange("powerfulCandlesRaritySpiderBeeDungeon", 0, 0, 10));

				allowBuzzierBeesTradeCompat = subscriber.subscribe(builder
						.comment(" \n-----------------------------------------------------\n\n"
								+" Add new trades to Buzzier Bees's Beekeeper Villager.\n")
						.translation("the_bumblezone.config.allowbuzzierbeestradecompat")
						.define("allowBuzzierBeesTradeCompat", true));

			builder.pop();


			builder.pop();
	    }
	}
}
