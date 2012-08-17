/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.configs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import silentium.commons.utils.StringUtil;

public final class PlayersConfig extends ConfigEngine
{
	public static int STARTING_ADENA;
	public static boolean EFFECT_CANCELING;
	public static double HP_REGEN_MULTIPLIER;
	public static double MP_REGEN_MULTIPLIER;
	public static double CP_REGEN_MULTIPLIER;
	public static int PLAYER_SPAWN_PROTECTION;
	public static int PLAYER_FAKEDEATH_UP_PROTECTION;
	public static double RESPAWN_RESTORE_HP;
	public static boolean RESPAWN_RANDOM_ENABLED;
	public static int RESPAWN_RANDOM_MAX_OFFSET;
	public static int MAX_PVTSTORE_SLOTS_DWARF;
	public static int MAX_PVTSTORE_SLOTS_OTHER;
	public static boolean DEEPBLUE_DROP_RULES;
	public static boolean ALT_GAME_DELEVEL;
	public static int DEATH_PENALTY_CHANCE;

	public static int INVENTORY_MAXIMUM_NO_DWARF;
	public static int INVENTORY_MAXIMUM_DWARF;
	public static int INVENTORY_MAXIMUM_QUEST_ITEMS;
	public static int INVENTORY_MAXIMUM_PET;
	public static int MAX_ITEM_IN_PACKET;
	public static double ALT_WEIGHT_LIMIT;
	public static int WAREHOUSE_SLOTS_NO_DWARF;
	public static int WAREHOUSE_SLOTS_DWARF;
	public static int WAREHOUSE_SLOTS_CLAN;
	public static int FREIGHT_SLOTS;
	public static boolean ALT_GAME_FREIGHTS;
	public static int ALT_GAME_FREIGHT_PRICE;

	public static double ENCHANT_CHANCE_WEAPON_MAGIC;
	public static double ENCHANT_CHANCE_WEAPON_MAGIC_15PLUS;
	public static double ENCHANT_CHANCE_WEAPON_NONMAGIC;
	public static double ENCHANT_CHANCE_WEAPON_NONMAGIC_15PLUS;
	public static double ENCHANT_CHANCE_ARMOR;
	public static int ENCHANT_MAX_WEAPON;
	public static int ENCHANT_MAX_ARMOR;
	public static int ENCHANT_SAFE_MAX;
	public static int ENCHANT_SAFE_MAX_FULL;

	public static int AUGMENTATION_NG_SKILL_CHANCE;
	public static int AUGMENTATION_NG_GLOW_CHANCE;
	public static int AUGMENTATION_MID_SKILL_CHANCE;
	public static int AUGMENTATION_MID_GLOW_CHANCE;
	public static int AUGMENTATION_HIGH_SKILL_CHANCE;
	public static int AUGMENTATION_HIGH_GLOW_CHANCE;
	public static int AUGMENTATION_TOP_SKILL_CHANCE;
	public static int AUGMENTATION_TOP_GLOW_CHANCE;
	public static int AUGMENTATION_BASESTAT_CHANCE;

	public static boolean KARMA_PLAYER_CAN_BE_KILLED_IN_PZ;
	public static boolean KARMA_PLAYER_CAN_SHOP;
	public static boolean KARMA_PLAYER_CAN_USE_GK;
	public static boolean KARMA_PLAYER_CAN_TELEPORT;
	public static boolean KARMA_PLAYER_CAN_TRADE;
	public static boolean KARMA_PLAYER_CAN_USE_WH;
	public static int KARMA_MIN_KARMA;
	public static int KARMA_MAX_KARMA;
	public static int KARMA_XP_DIVIDER;
	public static int KARMA_LOST_BASE;
	public static boolean KARMA_DROP_GM;
	public static boolean KARMA_AWARD_PK_KILL;
	public static int KARMA_PK_LIMIT;
	public static String KARMA_NONDROPPABLE_PET_ITEMS;
	public static String KARMA_NONDROPPABLE_ITEMS;
	public static int[] KARMA_LIST_NONDROPPABLE_PET_ITEMS;
	public static int[] KARMA_LIST_NONDROPPABLE_ITEMS;

	public static int PVP_NORMAL_TIME;
	public static int PVP_PVP_TIME;

	public static String PARTY_XP_CUTOFF_METHOD;
	public static int PARTY_XP_CUTOFF_LEVEL;
	public static double PARTY_XP_CUTOFF_PERCENT;
	public static int ALT_PARTY_RANGE;
	public static int ALT_PARTY_RANGE2;

	public static boolean EVERYBODY_HAS_ADMIN_RIGHTS;
	public static int MASTERACCESS_LEVEL;
	public static int MASTERACCESS_NAME_COLOR;
	public static int MASTERACCESS_TITLE_COLOR;
	public static boolean GM_HERO_AURA;
	public static boolean GM_STARTUP_INVULNERABLE;
	public static boolean GM_STARTUP_INVISIBLE;
	public static boolean GM_STARTUP_SILENCE;
	public static boolean GM_STARTUP_AUTO_LIST;

	public static boolean PETITIONING_ALLOWED;
	public static int MAX_PETITIONS_PER_PLAYER;
	public static int MAX_PETITIONS_PENDING;

	public static boolean IS_CRAFTING_ENABLED;
	public static int DWARF_RECIPE_LIMIT;
	public static int COMMON_RECIPE_LIMIT;
	public static boolean ALT_BLACKSMITH_USE_RECIPES;

	public static boolean AUTO_LEARN_SKILLS;
	public static boolean ALT_GAME_MAGICFAILURES;
	public static boolean ALT_GAME_SHIELD_BLOCKS;
	public static int ALT_PERFECT_SHLD_BLOCK;
	public static boolean LIFE_CRYSTAL_NEEDED;
	public static boolean SP_BOOK_NEEDED;
	public static boolean ES_SP_BOOK_NEEDED;
	public static boolean DIVINE_SP_BOOK_NEEDED;
	public static boolean ALT_GAME_SUBCLASS_WITHOUT_QUESTS;

	public static boolean STORE_SKILL_COOLTIME;
	public static byte BUFFS_MAX_AMOUNT;

	public static void load()
	{
		try (InputStream is = new FileInputStream(new File(PLAYERS_FILE)))
		{
			Properties players = new Properties();
			players.load(is);
			is.close();

			STARTING_ADENA = Integer.parseInt(players.getProperty("StartingAdena", "100"));
			EFFECT_CANCELING = Boolean.parseBoolean(players.getProperty("CancelLesserEffect", "True"));
			HP_REGEN_MULTIPLIER = Double.parseDouble(players.getProperty("HpRegenMultiplier", "100")) / 100;
			MP_REGEN_MULTIPLIER = Double.parseDouble(players.getProperty("MpRegenMultiplier", "100")) / 100;
			CP_REGEN_MULTIPLIER = Double.parseDouble(players.getProperty("CpRegenMultiplier", "100")) / 100;
			PLAYER_SPAWN_PROTECTION = Integer.parseInt(players.getProperty("PlayerSpawnProtection", "0"));
			PLAYER_FAKEDEATH_UP_PROTECTION = Integer.parseInt(players.getProperty("PlayerFakeDeathUpProtection", "0"));
			RESPAWN_RESTORE_HP = Double.parseDouble(players.getProperty("RespawnRestoreHP", "70")) / 100;
			RESPAWN_RANDOM_ENABLED = Boolean.parseBoolean(players.getProperty("RespawnRandomInTown", "False"));
			RESPAWN_RANDOM_MAX_OFFSET = Integer.parseInt(players.getProperty("RespawnRandomMaxOffset", "50"));
			MAX_PVTSTORE_SLOTS_DWARF = Integer.parseInt(players.getProperty("MaxPvtStoreSlotsDwarf", "5"));
			MAX_PVTSTORE_SLOTS_OTHER = Integer.parseInt(players.getProperty("MaxPvtStoreSlotsOther", "4"));
			DEEPBLUE_DROP_RULES = Boolean.parseBoolean(players.getProperty("UseDeepBlueDropRules", "True"));
			ALT_GAME_DELEVEL = Boolean.parseBoolean(players.getProperty("Delevel", "True"));
			DEATH_PENALTY_CHANCE = Integer.parseInt(players.getProperty("DeathPenaltyChance", "20"));

			INVENTORY_MAXIMUM_NO_DWARF = Integer.parseInt(players.getProperty("MaximumSlotsForNoDwarf", "80"));
			INVENTORY_MAXIMUM_DWARF = Integer.parseInt(players.getProperty("MaximumSlotsForDwarf", "100"));
			INVENTORY_MAXIMUM_QUEST_ITEMS = Integer.parseInt(players.getProperty("MaximumSlotsForQuestItems", "100"));
			INVENTORY_MAXIMUM_PET = Integer.parseInt(players.getProperty("MaximumSlotsForPet", "12"));
			MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_NO_DWARF, INVENTORY_MAXIMUM_DWARF);
			ALT_WEIGHT_LIMIT = Double.parseDouble(players.getProperty("AltWeightLimit", "1"));
			WAREHOUSE_SLOTS_NO_DWARF = Integer.parseInt(players.getProperty("MaximumWarehouseSlotsForNoDwarf", "100"));
			WAREHOUSE_SLOTS_DWARF = Integer.parseInt(players.getProperty("MaximumWarehouseSlotsForDwarf", "120"));
			WAREHOUSE_SLOTS_CLAN = Integer.parseInt(players.getProperty("MaximumWarehouseSlotsForClan", "150"));
			FREIGHT_SLOTS = Integer.parseInt(players.getProperty("MaximumFreightSlots", "20"));
			ALT_GAME_FREIGHTS = Boolean.parseBoolean(players.getProperty("AltGameFreights", "False"));
			ALT_GAME_FREIGHT_PRICE = Integer.parseInt(players.getProperty("AltGameFreightPrice", "1000"));

			ENCHANT_CHANCE_WEAPON_MAGIC = Double.parseDouble(players.getProperty("EnchantChanceMagicWeapon", "0.4"));
			ENCHANT_CHANCE_WEAPON_MAGIC_15PLUS = Double.parseDouble(players.getProperty("EnchantChanceMagicWeapon15Plus", "0.2"));
			ENCHANT_CHANCE_WEAPON_NONMAGIC = Double.parseDouble(players.getProperty("EnchantChanceNonMagicWeapon", "0.7"));
			ENCHANT_CHANCE_WEAPON_NONMAGIC_15PLUS = Double.parseDouble(players.getProperty("EnchantChanceNonMagicWeapon15Plus", "0.35"));
			ENCHANT_CHANCE_ARMOR = Double.parseDouble(players.getProperty("EnchantChanceArmor", "0.66"));
			ENCHANT_MAX_WEAPON = Integer.parseInt(players.getProperty("EnchantMaxWeapon", "0"));
			ENCHANT_MAX_ARMOR = Integer.parseInt(players.getProperty("EnchantMaxArmor", "0"));
			ENCHANT_SAFE_MAX = Integer.parseInt(players.getProperty("EnchantSafeMax", "3"));
			ENCHANT_SAFE_MAX_FULL = Integer.parseInt(players.getProperty("EnchantSafeMaxFull", "4"));

			AUGMENTATION_NG_SKILL_CHANCE = Integer.parseInt(players.getProperty("AugmentationNGSkillChance", "15"));
			AUGMENTATION_NG_GLOW_CHANCE = Integer.parseInt(players.getProperty("AugmentationNGGlowChance", "0"));
			AUGMENTATION_MID_SKILL_CHANCE = Integer.parseInt(players.getProperty("AugmentationMidSkillChance", "30"));
			AUGMENTATION_MID_GLOW_CHANCE = Integer.parseInt(players.getProperty("AugmentationMidGlowChance", "40"));
			AUGMENTATION_HIGH_SKILL_CHANCE = Integer.parseInt(players.getProperty("AugmentationHighSkillChance", "45"));
			AUGMENTATION_HIGH_GLOW_CHANCE = Integer.parseInt(players.getProperty("AugmentationHighGlowChance", "70"));
			AUGMENTATION_TOP_SKILL_CHANCE = Integer.parseInt(players.getProperty("AugmentationTopSkillChance", "60"));
			AUGMENTATION_TOP_GLOW_CHANCE = Integer.parseInt(players.getProperty("AugmentationTopGlowChance", "100"));
			AUGMENTATION_BASESTAT_CHANCE = Integer.parseInt(players.getProperty("AugmentationBaseStatChance", "1"));

			KARMA_PLAYER_CAN_BE_KILLED_IN_PZ = Boolean.parseBoolean(players.getProperty("KarmaPlayerCanBeKilledInPeaceZone", "False"));
			KARMA_PLAYER_CAN_SHOP = Boolean.parseBoolean(players.getProperty("KarmaPlayerCanShop", "True"));
			KARMA_PLAYER_CAN_USE_GK = Boolean.parseBoolean(players.getProperty("KarmaPlayerCanUseGK", "False"));
			KARMA_PLAYER_CAN_TELEPORT = Boolean.parseBoolean(players.getProperty("KarmaPlayerCanTeleport", "True"));
			KARMA_PLAYER_CAN_TRADE = Boolean.parseBoolean(players.getProperty("KarmaPlayerCanTrade", "True"));
			KARMA_PLAYER_CAN_USE_WH = Boolean.parseBoolean(players.getProperty("KarmaPlayerCanUseWareHouse", "True"));
			KARMA_MIN_KARMA = Integer.parseInt(players.getProperty("MinKarma", "240"));
			KARMA_MAX_KARMA = Integer.parseInt(players.getProperty("MaxKarma", "10000"));
			KARMA_XP_DIVIDER = Integer.parseInt(players.getProperty("XPDivider", "260"));
			KARMA_LOST_BASE = Integer.parseInt(players.getProperty("BaseKarmaLost", "0"));
			KARMA_DROP_GM = Boolean.parseBoolean(players.getProperty("CanGMDropEquipment", "false"));
			KARMA_AWARD_PK_KILL = Boolean.parseBoolean(players.getProperty("AwardPKKillPVPPoint", "true"));
			KARMA_PK_LIMIT = Integer.parseInt(players.getProperty("MinimumPKRequiredToDrop", "5"));
			KARMA_NONDROPPABLE_PET_ITEMS = players.getProperty("ListOfPetItems", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650");
			KARMA_NONDROPPABLE_ITEMS = players.getProperty("ListOfNonDroppableItemsForPK", "1147,425,1146,461,10,2368,7,6,2370,2369");

			String[] array = KARMA_NONDROPPABLE_PET_ITEMS.split(",");
			KARMA_LIST_NONDROPPABLE_PET_ITEMS = new int[array.length];

			for (int i = 0; i < array.length; i++)
				KARMA_LIST_NONDROPPABLE_PET_ITEMS[i] = Integer.parseInt(array[i]);

			array = KARMA_NONDROPPABLE_ITEMS.split(",");
			KARMA_LIST_NONDROPPABLE_ITEMS = new int[array.length];

			for (int i = 0; i < array.length; i++)
				KARMA_LIST_NONDROPPABLE_ITEMS[i] = Integer.parseInt(array[i]);

			// sorting so binarySearch can be used later
			Arrays.sort(KARMA_LIST_NONDROPPABLE_PET_ITEMS);
			Arrays.sort(KARMA_LIST_NONDROPPABLE_ITEMS);

			PVP_NORMAL_TIME = Integer.parseInt(players.getProperty("PvPVsNormalTime", "15000"));
			PVP_PVP_TIME = Integer.parseInt(players.getProperty("PvPVsPvPTime", "30000"));

			PARTY_XP_CUTOFF_METHOD = players.getProperty("PartyXpCutoffMethod", "level");
			PARTY_XP_CUTOFF_PERCENT = Double.parseDouble(players.getProperty("PartyXpCutoffPercent", "3."));
			PARTY_XP_CUTOFF_LEVEL = Integer.parseInt(players.getProperty("PartyXpCutoffLevel", "20"));
			ALT_PARTY_RANGE = Integer.parseInt(players.getProperty("AltPartyRange", "1600"));
			ALT_PARTY_RANGE2 = Integer.parseInt(players.getProperty("AltPartyRange2", "1400"));

			EVERYBODY_HAS_ADMIN_RIGHTS = Boolean.parseBoolean(players.getProperty("EverybodyHasAdminRights", "False"));
			MASTERACCESS_LEVEL = Integer.parseInt(players.getProperty("MasterAccessLevel", "127"));
			MASTERACCESS_NAME_COLOR = Integer.decode(StringUtil.concat("0x", players.getProperty("MasterNameColor", "00FF00")));
			MASTERACCESS_TITLE_COLOR = Integer.decode(StringUtil.concat("0x", players.getProperty("MasterTitleColor", "00FF00")));
			GM_HERO_AURA = Boolean.parseBoolean(players.getProperty("GMHeroAura", "False"));
			GM_STARTUP_INVULNERABLE = Boolean.parseBoolean(players.getProperty("GMStartupInvulnerable", "True"));
			GM_STARTUP_INVISIBLE = Boolean.parseBoolean(players.getProperty("GMStartupInvisible", "True"));
			GM_STARTUP_SILENCE = Boolean.parseBoolean(players.getProperty("GMStartupSilence", "True"));
			GM_STARTUP_AUTO_LIST = Boolean.parseBoolean(players.getProperty("GMStartupAutoList", "True"));

			PETITIONING_ALLOWED = Boolean.parseBoolean(players.getProperty("PetitioningAllowed", "True"));
			MAX_PETITIONS_PER_PLAYER = Integer.parseInt(players.getProperty("MaxPetitionsPerPlayer", "5"));
			MAX_PETITIONS_PENDING = Integer.parseInt(players.getProperty("MaxPetitionsPending", "25"));

			IS_CRAFTING_ENABLED = Boolean.parseBoolean(players.getProperty("CraftingEnabled", "True"));
			DWARF_RECIPE_LIMIT = Integer.parseInt(players.getProperty("DwarfRecipeLimit", "50"));
			COMMON_RECIPE_LIMIT = Integer.parseInt(players.getProperty("CommonRecipeLimit", "50"));
			ALT_BLACKSMITH_USE_RECIPES = Boolean.parseBoolean(players.getProperty("AltBlacksmithUseRecipes", "True"));

			AUTO_LEARN_SKILLS = Boolean.parseBoolean(players.getProperty("AutoLearnSkills", "false"));
			ALT_GAME_MAGICFAILURES = Boolean.parseBoolean(players.getProperty("MagicFailures", "True"));
			ALT_GAME_SHIELD_BLOCKS = Boolean.parseBoolean(players.getProperty("AltShieldBlocks", "false"));
			ALT_PERFECT_SHLD_BLOCK = Integer.parseInt(players.getProperty("AltPerfectShieldBlockRate", "10"));
			LIFE_CRYSTAL_NEEDED = Boolean.parseBoolean(players.getProperty("LifeCrystalNeeded", "true"));
			SP_BOOK_NEEDED = Boolean.parseBoolean(players.getProperty("SpBookNeeded", "true"));
			ES_SP_BOOK_NEEDED = Boolean.parseBoolean(players.getProperty("EnchantSkillSpBookNeeded", "true"));
			DIVINE_SP_BOOK_NEEDED = Boolean.parseBoolean(players.getProperty("DivineInspirationSpBookNeeded", "true"));
			ALT_GAME_SUBCLASS_WITHOUT_QUESTS = Boolean.parseBoolean(players.getProperty("AltSubClassWithoutQuests", "False"));

			BUFFS_MAX_AMOUNT = Byte.parseByte(players.getProperty("MaxBuffsAmount", "20"));
			STORE_SKILL_COOLTIME = Boolean.parseBoolean(players.getProperty("StoreSkillCooltime", "true"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Server failed to load " + PLAYERS_FILE + " file.");
		}
	}
}