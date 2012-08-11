/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.utils.Rnd;
import silentium.commons.utils.StringUtil;
import silentium.gameserver.SevenSigns;
import silentium.gameserver.SevenSignsFestival;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.NPCConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.instancemanager.ClanHallManager;
import silentium.gameserver.instancemanager.SiegeManager;
import silentium.gameserver.instancemanager.ZoneManager;
import silentium.gameserver.model.L2SiegeClan;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2CubicInstance;
import silentium.gameserver.model.actor.instance.L2DoorInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.ClanHall;
import silentium.gameserver.model.entity.Siege;
import silentium.gameserver.model.zone.type.L2MotherTreeZone;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.effects.EffectTemplate;
import silentium.gameserver.skills.funcs.*;
import silentium.gameserver.templates.item.L2Armor;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.templates.item.L2Weapon;
import silentium.gameserver.templates.item.L2WeaponType;
import silentium.gameserver.templates.skills.L2SkillType;
import silentium.gameserver.utils.Util;

/**
 * Global calculations, can be modified by server admins
 */
public final class Formulas
{
	protected static final Logger _log = LoggerFactory.getLogger(Formulas.class.getName());

	private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs

	public static final byte SHIELD_DEFENSE_FAILED = 0; // no shield defense
	public static final byte SHIELD_DEFENSE_SUCCEED = 1; // normal shield defense
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2; // perfect block

	public static final byte SKILL_REFLECT_FAILED = 0; // no reflect
	public static final byte SKILL_REFLECT_SUCCEED = 1; // normal reflect, some damage reflected some other not
	public static final byte SKILL_REFLECT_VENGEANCE = 2; // 100% of the damage affect both

	private static final byte MELEE_ATTACK_RANGE = 40;

	public static final int MAX_STAT_VALUE = 100;

	public static final int BASENPCSTR = 40;
	public static final int BASENPCCON = 43;
	public static final int BASENPCDEX = 30;
	public static final int BASENPCINT = 21;
	public static final int BASENPCWIT = 20;
	public static final int BASENPCMEN = 20;

	private static final double[] STRCompute = new double[] { 1.036, 34.845 };
	private static final double[] INTCompute = new double[] { 1.020, 31.375 };
	private static final double[] DEXCompute = new double[] { 1.009, 19.360 };
	private static final double[] WITCompute = new double[] { 1.050, 20.000 };
	private static final double[] CONCompute = new double[] { 1.030, 27.632 };
	private static final double[] MENCompute = new double[] { 1.010, -0.060 };

	public static final double[] WITbonus = new double[MAX_STAT_VALUE];
	public static final double[] MENbonus = new double[MAX_STAT_VALUE];
	public static final double[] INTbonus = new double[MAX_STAT_VALUE];
	public static final double[] STRbonus = new double[MAX_STAT_VALUE];
	public static final double[] DEXbonus = new double[MAX_STAT_VALUE];
	public static final double[] CONbonus = new double[MAX_STAT_VALUE];

	protected static final double[] sqrtMENbonus = new double[MAX_STAT_VALUE];
	protected static final double[] sqrtCONbonus = new double[MAX_STAT_VALUE];

	static
	{
		for (int i = 0; i < STRbonus.length; i++)
			STRbonus[i] = Math.floor(Math.pow(STRCompute[0], i - STRCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < INTbonus.length; i++)
			INTbonus[i] = Math.floor(Math.pow(INTCompute[0], i - INTCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < DEXbonus.length; i++)
			DEXbonus[i] = Math.floor(Math.pow(DEXCompute[0], i - DEXCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < WITbonus.length; i++)
			WITbonus[i] = Math.floor(Math.pow(WITCompute[0], i - WITCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < CONbonus.length; i++)
			CONbonus[i] = Math.floor(Math.pow(CONCompute[0], i - CONCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < MENbonus.length; i++)
			MENbonus[i] = Math.floor(Math.pow(MENCompute[0], i - MENCompute[1]) * 100 + .5d) / 100;

		// Precompute square root values
		for (int i = 0; i < sqrtCONbonus.length; i++)
			sqrtCONbonus[i] = Math.sqrt(CONbonus[i]);
		for (int i = 0; i < sqrtMENbonus.length; i++)
			sqrtMENbonus[i] = Math.sqrt(MENbonus[i]);
	}

	/**
	 * @param cha
	 *            The character to make checks on.
	 * @return the period between 2 regenerations task (3s for L2Character, 5 min for L2DoorInstance).
	 */
	public static int getRegeneratePeriod(L2Character cha)
	{
		if (cha instanceof L2DoorInstance)
			return HP_REGENERATE_PERIOD * 100; // 5 mins

		return HP_REGENERATE_PERIOD; // 3s
	}

	/**
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP,
	 * REGENERATE_HP_RATE...). In fact, each calculator is a table of Func object in which each Func represents a mathematic
	 * function : <BR>
	 * <BR>
	 * To reduce cache memory use, L2Npcs who don't have skills share the same Calculator set called <B>NPC_STD_CALCULATOR</B>.<BR>
	 * <BR>
	 *
	 * @return the standard NPC Calculator set.
	 */
	public static Calculator[] getStdNPCCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];

		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());

		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());

		return std;
	}

	/**
	 * Add basics Func objects to L2PcInstance and L2Summon.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP,
	 * REGENERATE_HP_RATE...). In fact, each calculator is a table of Func object in which each Func represents a mathematic
	 * function : <BR>
	 * <BR>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR>
	 * <BR>
	 *
	 * @param cha
	 *            L2PcInstance or L2Summon that must obtain basic Func objects
	 */
	public static void addFuncsToNewCharacter(L2Character cha)
	{
		// Summons and players.
		if (cha instanceof L2Playable)
		{
			cha.addStatFunc(FuncMaxHpMul.getInstance());
			cha.addStatFunc(FuncMaxMpMul.getInstance());

			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());

			cha.addStatFunc(FuncMoveSpeed.getInstance());

			// Players only.
			if (cha instanceof L2PcInstance)
			{
				cha.addStatFunc(FuncMaxHpAdd.getInstance());
				cha.addStatFunc(FuncMaxCpAdd.getInstance());
				cha.addStatFunc(FuncMaxCpMul.getInstance());
				cha.addStatFunc(FuncMaxMpAdd.getInstance());

				cha.addStatFunc(FuncBowAtkRange.getInstance());

				cha.addStatFunc(FuncHennaSTR.getInstance());
				cha.addStatFunc(FuncHennaDEX.getInstance());
				cha.addStatFunc(FuncHennaINT.getInstance());
				cha.addStatFunc(FuncHennaMEN.getInstance());
				cha.addStatFunc(FuncHennaCON.getInstance());
				cha.addStatFunc(FuncHennaWIT.getInstance());
			}
		}
	}

	/**
	 * @param cha
	 *            The character to make checks on.
	 * @return the HP regen rate (base + modifiers).
	 */
	public static final double calcHpRegen(L2Character cha)
	{
		double init = cha.getTemplate().getBaseHpReg();
		double hpRegenMultiplier = cha.isRaid() ? NPCConfig.RAID_HP_REGEN_MULTIPLIER : PlayersConfig.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;

		if (NPCConfig.CHAMPION_ENABLE && cha.isChampion())
			hpRegenMultiplier *= NPCConfig.CHAMPION_HP_REGEN;

		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;

			// Calculate correct baseHpReg value for certain level of PC
			init += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;

			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
				hpRegenMultiplier *= calcFestivalRegenModifier(player);
			else
			{
				double siegeModifier = calcSiegeRegenModifer(player);
				if (siegeModifier > 0)
					hpRegenMultiplier *= siegeModifier;
			}

			if (player.isInsideZone(L2Character.ZONE_CLANHALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getHideoutId();
				if (clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
							hpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100;
				}
			}

			// Mother Tree effect is calculated at last
			if (player.isInsideZone(L2Character.ZONE_MOTHERTREE))
			{
				L2MotherTreeZone zone = ZoneManager.getInstance().getZone(player, L2MotherTreeZone.class);
				int hpBonus = zone == null ? 0 : zone.getHpRegenBonus();
				hpRegenBonus += hpBonus;
			}

			// Calculate Movement bonus
			if (player.isSitting())
				hpRegenMultiplier *= 1.5; // Sitting
			else if (!player.isMoving())
				hpRegenMultiplier *= 1.1; // Staying
			else if (player.isRunning())
				hpRegenMultiplier *= 0.7; // Running

			// Add CON bonus
			init *= cha.getLevelMod() * Formulas.CONbonus[cha.getCON()];
		}

		if (init < 1)
			init = 1;

		return cha.calcStat(Stats.REGENERATE_HP_RATE, init, null, null) * hpRegenMultiplier + hpRegenBonus;
	}

	/**
	 * @param cha
	 *            The character to make checks on.
	 * @return the MP regen rate (base + modifiers).
	 */
	public static final double calcMpRegen(L2Character cha)
	{
		double init = cha.getTemplate().getBaseMpReg();
		double mpRegenMultiplier = cha.isRaid() ? NPCConfig.RAID_MP_REGEN_MULTIPLIER : PlayersConfig.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;

		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;

			// Calculate correct baseMpReg value for certain level of PC
			init += 0.3 * ((player.getLevel() - 1) / 10.0);

			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
				mpRegenMultiplier *= calcFestivalRegenModifier(player);

			// Mother Tree effect is calculated at last
			if (player.isInsideZone(L2Character.ZONE_MOTHERTREE))
			{
				L2MotherTreeZone zone = ZoneManager.getInstance().getZone(player, L2MotherTreeZone.class);
				int mpBonus = zone == null ? 0 : zone.getMpRegenBonus();
				mpRegenBonus += mpBonus;
			}

			if (player.isInsideZone(L2Character.ZONE_CLANHALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getHideoutId();
				if (clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
							mpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100;
				}
			}

			// Calculate Movement bonus
			if (player.isSitting())
				mpRegenMultiplier *= 1.5; // Sitting
			else if (!player.isMoving())
				mpRegenMultiplier *= 1.1; // Staying
			else if (player.isRunning())
				mpRegenMultiplier *= 0.7; // Running

			// Add MEN bonus
			init *= cha.getLevelMod() * Formulas.MENbonus[cha.getMEN()];
		}

		if (init < 1)
			init = 1;

		return cha.calcStat(Stats.REGENERATE_MP_RATE, init, null, null) * mpRegenMultiplier + mpRegenBonus;
	}

	/**
	 * @param cha
	 *            The character to make checks on.
	 * @return the CP regen rate (base + modifiers).
	 */
	public static final double calcCpRegen(L2Character cha)
	{
		double init = cha.getTemplate().getBaseHpReg();
		double cpRegenMultiplier = PlayersConfig.CP_REGEN_MULTIPLIER;
		double cpRegenBonus = 0;

		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;

			// Calculate correct baseHpReg value for certain level of PC
			init += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;

			// Calculate Movement bonus
			if (player.isSitting())
				cpRegenMultiplier *= 1.5; // Sitting
			else if (!player.isMoving())
				cpRegenMultiplier *= 1.1; // Staying
			else if (player.isRunning())
				cpRegenMultiplier *= 0.7; // Running
		}
		else
		{
			// Calculate Movement bonus
			if (!cha.isMoving())
				cpRegenMultiplier *= 1.1; // Staying
			else if (cha.isRunning())
				cpRegenMultiplier *= 0.7; // Running
		}

		// Apply CON bonus
		init *= cha.getLevelMod() * Formulas.CONbonus[cha.getCON()];
		if (init < 1)
			init = 1;

		return cha.calcStat(Stats.REGENERATE_CP_RATE, init, null, null) * cpRegenMultiplier + cpRegenBonus;
	}

	public static final double calcFestivalRegenModifier(L2PcInstance activeChar)
	{
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
		final int oracle = festivalInfo[0];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;

		// If the player isn't found in the festival, leave the regen rate as it is.
		if (festivalId < 0)
			return 0;

		// Retrieve the X and Y coords for the center of the festival arena the player is in.
		if (oracle == SevenSigns.CABAL_DAWN)
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		else
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];

		// Check the distance between the player and the player spawn point, in the center of the arena.
		double distToCenter = activeChar.getPlanDistanceSq(festivalCenter[0], festivalCenter[1]);

		_log.debug("Distance: " + distToCenter + ", RegenMulti: " + (distToCenter * 2.5) / 50);

		return 1.0 - (distToCenter * 0.0005); // Maximum Decreased Regen of ~ -65%;
	}

	public static final double calcSiegeRegenModifer(L2PcInstance activeChar)
	{
		if (activeChar == null || activeChar.getClan() == null)
			return 0;

		Siege siege = SiegeManager.getSiege(activeChar.getPosition().getX(), activeChar.getPosition().getY(), activeChar.getPosition().getZ());
		if (siege == null || !siege.getIsInProgress())
			return 0;

		L2SiegeClan siegeClan = siege.getAttackerClan(activeChar.getClan().getClanId());
		if (siegeClan == null || siegeClan.getFlag().isEmpty() || !Util.checkIfInRange(200, activeChar, siegeClan.getFlag().get(0), true))
			return 0;

		return 1.5; // If all is true, then modifer will be 50% more
	}

	/**
	 * @param attacker
	 *            The attacker, from where the blow comes from.
	 * @param target
	 *            The victim of the blow.
	 * @param skill
	 *            The skill used.
	 * @param shld
	 *            True if victim was wearign a shield.
	 * @param ss
	 *            True if ss were activated.
	 * @return blow damage based on cAtk
	 */
	public static double calcBlowDamage(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss)
	{
		final boolean isPvP = (attacker instanceof L2Playable) && (target instanceof L2Playable);
		double power = skill.getPower();
		double defence = target.getPDef(attacker);
		double damage = 0;
		damage += calcValakasAttribute(attacker, target, skill);

		if (ss)
			damage *= 2.;

		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				defence += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}

		if (ss && skill.getSSBoost() > 0)
			power *= skill.getSSBoost();

		damage += attacker.calcStat(Stats.CRITICAL_DAMAGE, (damage + power), target, skill);
		damage += attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 6.5;
		damage *= target.calcStat(Stats.CRIT_VULN, 1, target, skill);

		// get the vulnerability for the instance due to skills (buffs, passives, toggles, etc)
		damage = target.calcStat(Stats.DAGGER_WPN_VULN, damage, target, null);
		damage *= 70. / defence;

		// Random weapon damage
		damage *= attacker.getRandomDamageMultiplier();

		// Dmg bonusses in PvP fight
		if (isPvP)
			damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);

		return damage < 1 ? 1. : damage;
	}

	/**
	 * Calculated damage caused by ATTACK of attacker on target, called separatly for each weapon, if dual-weapon is used.
	 *
	 * @param attacker
	 *            player or NPC that makes ATTACK
	 * @param target
	 *            player or NPC, target of ATTACK
	 * @param skill
	 *            skill used.
	 * @param shld
	 *            target was using a shield or not.
	 * @param crit
	 *            if the ATTACK have critical success
	 * @param dual
	 *            if dual weapon is used
	 * @param ss
	 *            if weapon item was charged by soulshot
	 * @return damage points
	 */
	public static final double calcPhysDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean crit, boolean dual, boolean ss)
	{
		if (attacker instanceof L2PcInstance)
		{
			L2PcInstance pcInst = (L2PcInstance) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
				return 0;
		}

		final boolean isPvP = (attacker instanceof L2Playable) && (target instanceof L2Playable);
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		damage += calcValakasAttribute(attacker, target, skill);

		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				if (!PlayersConfig.ALT_GAME_SHIELD_BLOCKS)
					defence += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1.;
		}

		if (ss)
			damage *= 2;

		if (skill != null)
		{
			double skillpower = skill.getPower(attacker, target);
			float ssboost = skill.getSSBoost();
			if (ssboost <= 0)
				damage += skillpower;
			else if (ssboost > 0)
			{
				if (ss)
				{
					skillpower *= ssboost;
					damage += skillpower;
				}
				else
					damage += skillpower;
			}
		}

		// defence modifier depending of the attacker weapon
		L2Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		if (weapon != null)
		{
			switch (weapon.getItemType())
			{
				case BOW:
					stat = Stats.BOW_WPN_VULN;
					break;
				case BLUNT:
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case BIGSWORD:
					stat = Stats.BIGSWORD_WPN_VULN;
					break;
				case BIGBLUNT:
					stat = Stats.BIGBLUNT_WPN_VULN;
					break;
				case DAGGER:
					stat = Stats.DAGGER_WPN_VULN;
					break;
				case DUAL:
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST:
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case ETC:
					stat = Stats.ETC_WPN_VULN;
					break;
				case FIST:
					stat = Stats.FIST_WPN_VULN;
					break;
				case POLE:
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
			}
		}

		if (crit)
		{
			// Finally retail like formula
			damage = 2 * attacker.calcStat(Stats.CRITICAL_DAMAGE, 1, target, skill) * target.calcStat(Stats.CRIT_VULN, 1, target, null) * (70 * damage / defence);
			// Crit dmg add is almost useless in normal hits...
			damage += (attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 70 / defence);
		}
		else
			damage = 70 * damage / defence;

		if (stat != null)
			damage = target.calcStat(stat, damage, target, null);

		// Weapon random damage
		damage *= attacker.getRandomDamageMultiplier();

		if (shld > 0 && PlayersConfig.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0)
				damage = 0;
		}

		if (target instanceof L2Npc)
		{
			double multiplier;
			switch (((L2Npc) target).getTemplate().getRace())
			{
				case BEAST:
					multiplier = 1 + ((attacker.getPAtkMonsters(target) - target.getPDefMonsters(target)) / 100);
					damage *= multiplier;
					break;
				case ANIMAL:
					multiplier = 1 + ((attacker.getPAtkAnimals(target) - target.getPDefAnimals(target)) / 100);
					damage *= multiplier;
					break;
				case PLANT:
					multiplier = 1 + ((attacker.getPAtkPlants(target) - target.getPDefPlants(target)) / 100);
					damage *= multiplier;
					break;
				case DRAGON:
					multiplier = 1 + ((attacker.getPAtkDragons(target) - target.getPDefDragons(target)) / 100);
					damage *= multiplier;
					break;
				case BUG:
					multiplier = 1 + ((attacker.getPAtkInsects(target) - target.getPDefInsects(target)) / 100);
					damage *= multiplier;
					break;
				case GIANT:
					multiplier = 1 + ((attacker.getPAtkGiants(target) - target.getPDefGiants(target)) / 100);
					damage *= multiplier;
					break;
				case MAGICCREATURE:
					multiplier = 1 + ((attacker.getPAtkMagicCreatures(target) - target.getPDefMagicCreatures(target)) / 100);
					damage *= multiplier;
					break;
				default:
					// nothing
					break;
			}
		}

		if (damage > 0 && damage < 1)
			damage = 1;
		else if (damage < 0)
			damage = 0;

		// Dmg bonuses in PvP fight
		if (isPvP)
		{
			if (skill == null)
				damage *= attacker.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null);
			else
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
		}

		return damage;
	}

	/**
	 * Calculated damage caused by charges skills types. The special thing is about the multiplier (56 and not 70), and about the
	 * fixed amount of damages
	 *
	 * @param attacker
	 *            player or NPC that makes ATTACK
	 * @param target
	 *            player or NPC, target of ATTACK
	 * @param skill
	 *            skill used.
	 * @param shld
	 *            target was using a shield or not.
	 * @param crit
	 *            if the ATTACK have critical success
	 * @param dual
	 *            if dual weapon is used
	 * @param ss
	 *            if weapon item was charged by soulshot
	 * @return damage points
	 */
	public static final double calcChargeSkillsDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean crit, boolean dual, boolean ss)
	{
		if (attacker instanceof L2PcInstance)
		{
			L2PcInstance pcInst = (L2PcInstance) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
				return 0;
		}

		final boolean isPvP = (attacker instanceof L2Playable) && (target instanceof L2Playable);
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		damage += calcValakasAttribute(attacker, target, skill);

		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				if (!PlayersConfig.ALT_GAME_SHIELD_BLOCKS)
					defence += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1.;
		}

		if (ss)
			damage *= 2;

		if (skill != null)
		{
			double skillpower = skill.getPower(attacker, target);
			float ssboost = skill.getSSBoost();
			if (ssboost <= 0)
				damage += skillpower;
			else if (ssboost > 0)
			{
				if (ss)
				{
					skillpower *= ssboost;
					damage += skillpower;
				}
				else
					damage += skillpower;
			}
		}

		// defence modifier depending of the attacker weapon
		L2Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		if (weapon != null)
		{
			switch (weapon.getItemType())
			{
				case BOW:
					stat = Stats.BOW_WPN_VULN;
					break;
				case BLUNT:
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case BIGSWORD:
					stat = Stats.BIGSWORD_WPN_VULN;
					break;
				case BIGBLUNT:
					stat = Stats.BIGBLUNT_WPN_VULN;
					break;
				case DAGGER:
					stat = Stats.DAGGER_WPN_VULN;
					break;
				case DUAL:
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST:
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case ETC:
					stat = Stats.ETC_WPN_VULN;
					break;
				case FIST:
					stat = Stats.FIST_WPN_VULN;
					break;
				case POLE:
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
			}
		}

		if (crit)
		{
			// Finally retail like formula
			damage = 2 * attacker.calcStat(Stats.CRITICAL_DAMAGE, 1, target, skill) * target.calcStat(Stats.CRIT_VULN, 1, target, null) * (56 * damage / defence);
			// Crit dmg add is almost useless in normal hits...
			damage += (attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 56 / defence);
		}
		else
			damage = 56 * damage / defence;

		if (stat != null)
			damage = target.calcStat(stat, damage, target, null);

		// Weapon random damage
		damage *= attacker.getRandomDamageMultiplier();

		if (shld > 0 && PlayersConfig.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0)
				damage = 0;
		}

		if (target instanceof L2Npc)
		{
			double multiplier;
			switch (((L2Npc) target).getTemplate().getRace())
			{
				case BEAST:
					multiplier = 1 + ((attacker.getPAtkMonsters(target) - target.getPDefMonsters(target)) / 100);
					damage *= multiplier;
					break;
				case ANIMAL:
					multiplier = 1 + ((attacker.getPAtkAnimals(target) - target.getPDefAnimals(target)) / 100);
					damage *= multiplier;
					break;
				case PLANT:
					multiplier = 1 + ((attacker.getPAtkPlants(target) - target.getPDefPlants(target)) / 100);
					damage *= multiplier;
					break;
				case DRAGON:
					multiplier = 1 + ((attacker.getPAtkDragons(target) - target.getPDefDragons(target)) / 100);
					damage *= multiplier;
					break;
				case BUG:
					multiplier = 1 + ((attacker.getPAtkInsects(target) - target.getPDefInsects(target)) / 100);
					damage *= multiplier;
					break;
				case GIANT:
					multiplier = 1 + ((attacker.getPAtkGiants(target) - target.getPDefGiants(target)) / 100);
					damage *= multiplier;
					break;
				case MAGICCREATURE:
					multiplier = 1 + ((attacker.getPAtkMagicCreatures(target) - target.getPDefMagicCreatures(target)) / 100);
					damage *= multiplier;
					break;
				default:
					// nothing
					break;
			}
		}

		if (damage > 0 && damage < 1)
			damage = 1;
		else if (damage < 0)
			damage = 0;

		// Dmg bonuses in PvP fight
		if (isPvP)
		{
			if (skill == null)
				damage *= attacker.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null);
			else
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
		}

		return damage;
	}

	public static final double calcMagicDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss, boolean bss, boolean mcrit)
	{
		if (attacker instanceof L2PcInstance)
		{
			L2PcInstance pcInst = (L2PcInstance) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
				return 0;
		}

		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);

		// AI SpiritShot
		if (attacker instanceof L2Npc)
		{
			if (((L2Npc) attacker)._spiritshotcharged)
				ss = true;
			else
				ss = false;

			((L2Npc) attacker)._spiritshotcharged = false;
		}

		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}

		if (bss)
			mAtk *= 4;
		else if (ss)
			mAtk *= 2;

		double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker, target);

		// Failure calculation
		if (PlayersConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker instanceof L2PcInstance)
			{
				if (calcMagicSuccess(attacker, target, skill) && (target.getLevel() - attacker.getLevel()) <= 9)
				{
					if (skill.getSkillType() == L2SkillType.DRAIN)
						attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DRAIN_HALF_SUCCESFUL));
					else
						attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));

					damage /= 2;
				}
				else
				{
					attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					damage = 1;
				}
			}

			if (target instanceof L2PcInstance)
			{
				if (skill.getSkillType() == L2SkillType.DRAIN)
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_DRAIN).addCharName(attacker));
				else
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_MAGIC).addCharName(attacker));
			}
		}
		else if (mcrit)
			damage *= 4;

		// Pvp bonuses for dmg
		if (attacker instanceof L2Playable && target instanceof L2Playable)
		{
			if (skill.isMagic())
				damage *= attacker.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null);
			else
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
		}

		damage *= calcElemental(attacker, target, skill);

		return damage;
	}

	public static final double calcMagicDam(L2CubicInstance attacker, L2Character target, L2Skill skill, boolean mcrit, byte shld)
	{
		// Current info include mAtk in the skill power.
		double mDef = target.getMDef(attacker.getOwner(), skill);

		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}

		double damage = 91 / mDef * skill.getPower();
		L2PcInstance owner = attacker.getOwner();

		// Failure calculation
		if (PlayersConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(owner, target, skill))
		{
			if (calcMagicSuccess(owner, target, skill) && (target.getLevel() - skill.getMagicLevel()) <= 9)
			{
				if (skill.getSkillType() == L2SkillType.DRAIN)
					owner.sendPacket(SystemMessageId.DRAIN_HALF_SUCCESFUL);
				else
					owner.sendPacket(SystemMessageId.ATTACK_FAILED);

				damage /= 2;
			}
			else
			{
				owner.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
				damage = 1;
			}

			if (target instanceof L2PcInstance)
			{
				if (skill.getSkillType() == L2SkillType.DRAIN)
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_DRAIN).addCharName(owner));
				else
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_MAGIC).addCharName(owner));
			}
		}
		else if (mcrit)
			damage *= 4;

		damage *= calcElemental(owner, target, skill);

		return damage;
	}

	/**
	 * @param rate
	 *            The value to make check on.
	 * @return true in case of critical hit
	 */
	public static final boolean calcCrit(double rate)
	{
		return rate > Rnd.get(1000);
	}

	/**
	 * Calcul value of blow success
	 *
	 * @param activeChar
	 *            The character delaing the blow.
	 * @param target
	 *            The victim.
	 * @param chance
	 *            The base chance of landing a blow.
	 * @return true if successful, false otherwise
	 */
	public static final boolean calcBlow(L2Character activeChar, L2Character target, int chance)
	{
		return activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 20) / 100), target, null) > Rnd.get(100);
	}

	/**
	 * Calcul value of lethal chance
	 *
	 * @param activeChar
	 *            The character delaing the blow.
	 * @param target
	 *            The victim.
	 * @param baseLethal
	 *            The base lethal chance of the skill.
	 * @param magiclvl
	 * @return
	 */
	public static final double calcLethal(L2Character activeChar, L2Character target, int baseLethal, int magiclvl)
	{
		double chance = 0;
		if (magiclvl > 0)
		{
			int delta = ((magiclvl + activeChar.getLevel()) / 2) - 1 - target.getLevel();

			if (delta >= -3)
				chance = (baseLethal * ((double) activeChar.getLevel() / target.getLevel()));
			else if (delta < -3 && delta >= -9)
				chance = (-3) * (baseLethal / (delta));
			else
				chance = baseLethal / 15;
		}
		else
			chance = (baseLethal * ((double) activeChar.getLevel() / target.getLevel()));

		return 10 * activeChar.calcStat(Stats.LETHAL_RATE, chance, target, null);
	}

	public static final boolean calcLethalHit(L2Character activeChar, L2Character target, L2Skill skill)
	{
		if (!target.isRaid() && !(target instanceof L2DoorInstance))
		{
			// If one of following IDs is found, return false (Tyrannosaurus x 3, Headquarters)
			if (target instanceof L2Npc)
			{
				int npcId = ((L2Npc) target).getNpcId();
				switch (npcId)
				{
					case 22215:
					case 22216:
					case 22217:
					case 35062:
						return false;
				}
			}

			// 2nd lethal effect activate (cp,hp to 1 or if target is npc then hp to 1)
			if (skill.getLethalChance2() > 0 && Rnd.get(1000) < calcLethal(activeChar, target, skill.getLethalChance2(), skill.getMagicLevel()))
			{
				if (target instanceof L2Npc)
					target.reduceCurrentHp(target.getCurrentHp() - 1, activeChar, skill);
				else if (target instanceof L2PcInstance) // If is a active player set his HP and CP to 1
				{
					L2PcInstance player = (L2PcInstance) target;
					if (!player.isInvul())
					{
						if (!(activeChar instanceof L2PcInstance && (((L2PcInstance) activeChar).isGM() && !((L2PcInstance) activeChar).getAccessLevel().canGiveDamage())))
						{
							player.setCurrentHp(1);
							player.setCurrentCp(1);
							player.sendPacket(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL);
						}
					}
				}
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LETHAL_STRIKE));
			}
			else if (skill.getLethalChance1() > 0 && Rnd.get(1000) < calcLethal(activeChar, target, skill.getLethalChance1(), skill.getMagicLevel()))
			{
				if (target instanceof L2PcInstance)
				{
					L2PcInstance player = (L2PcInstance) target;
					if (!player.isInvul())
					{
						if (!(activeChar instanceof L2PcInstance && (((L2PcInstance) activeChar).isGM() && !((L2PcInstance) activeChar).getAccessLevel().canGiveDamage())))
							player.setCurrentCp(1); // Set CP to 1
					}
				}
				else if (target instanceof L2Npc) // If is a monster remove first damage and after 50% of current hp
					target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar, skill);

				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LETHAL_STRIKE));
			}
			else
				return false;
		}
		else
			return false;

		return true;
	}

	public static final boolean calcMCrit(int mRate)
	{
		if (MainConfig.DEVELOPER)
			_log.info("Current mCritRate: " + mRate + "/1000");

		return mRate > Rnd.get(1000);
	}

	/**
	 * Check if casting process is canceled due to hit.
	 *
	 * @param target
	 *            The target to make checks on.
	 * @param dmg
	 *            The amount of dealt damages.
	 */
	public static final void calcCastBreak(L2Character target, double dmg)
	{
		// Don't go further for invul characters or raid bosses.
		if (target.isRaid() || target.isInvul())
			return;

		// Break automatically the skill cast if under attack
		if (target instanceof L2PcInstance)
		{
			if (((L2PcInstance) target).getFusionSkill() != null)
			{
				target.breakCast();
				return;
			}
		}

		double init = 0;

		// Initialization to 15% for magical skills
		if (target.isCastingNow() && (target.getLastSkillCast() != null && target.getLastSkillCast().isMagic()))
			init = 15;

		// Don't go further for ppl casting a physical skill
		if (init <= 0)
			return;

		// Chance to break is higher with higher dmg.
		init += Math.sqrt(13 * dmg);

		// Special treatement for monsters, else their casts are often broken.
		// Use a modifier according to AI type.
		if (target instanceof L2Attackable)
		{
			double mod = 0.1;

			// FIGHTER and ARCHER types use default mod, 0.1
			switch (((L2Attackable) target).getAiType())
			{
				case MAGE:
				case HEALER:
					mod = 0.3;
					break;
				case BALANCED:
					mod = 0.2;
					break;
			}
			init *= mod;
		}

		// Chance is affected by target MEN
		init -= (MENbonus[target.getMEN()] * 100 - 100);

		// Calculate all modifiers for ATTACK_CANCEL
		double rate = target.calcStat(Stats.ATTACK_CANCEL, init, null, null);

		// Adjust the rate to be between 1 and 99
		if (rate > 99)
			rate = 99;
		else if (rate < 1)
			rate = 1;

		if (MainConfig.DEVELOPER)
			_log.info("calcCastBreak rate: " + (int) rate + "%");

		if (Rnd.get(100) < rate)
			target.breakCast();
	}

	/**
	 * Calculate delay (in milliseconds) before next ATTACK.
	 *
	 * @param attacker
	 * @param target
	 * @param rate
	 * @return delay in ms.
	 */
	public static final int calcPAtkSpd(L2Character attacker, L2Character target, double rate)
	{
		if (rate < 2)
			return 2700;

		return (int) (470000 / rate);
	}

	/**
	 * Calculate delay (in milliseconds) for skills cast.
	 *
	 * @param attacker
	 * @param skill
	 *            used to know if skill is magic or no.
	 * @param skillTime
	 * @return delay in ms.
	 */
	public static final int calcAtkSpd(L2Character attacker, L2Skill skill, double skillTime)
	{
		if (skill.isMagic())
			return (int) (skillTime * 333 / attacker.getMAtkSpd());

		return (int) (skillTime * 333 / attacker.getPAtkSpd());
	}

	/**
	 * Calculate the hit/miss chance. Take in consideration the attacker's accuracy, the target's evasion, and the difference of
	 * levels between both.
	 *
	 * @param attacker
	 *            Take accuracy from the attacker.
	 * @param target
	 *            Take evasion from the target.
	 * @return true if hit missed (target evaded)
	 */
	public static boolean calcHitMiss(L2Character attacker, L2Character target)
	{
		int delta = attacker.getAccuracy() - target.getEvasionRate(attacker);
		int chance;
		if (delta >= 10)
			chance = 980;
		else
		{
			switch (delta)
			{
				case 9:
					chance = 975;
					break;
				case 8:
					chance = 970;
					break;
				case 7:
					chance = 965;
					break;
				case 6:
					chance = 960;
					break;
				case 5:
					chance = 955;
					break;
				case 4:
					chance = 945;
					break;
				case 3:
					chance = 935;
					break;
				case 2:
					chance = 925;
					break;
				case 1:
					chance = 915;
					break;
				case 0:
					chance = 905;
					break;
				case -1:
					chance = 890;
					break;
				case -2:
					chance = 875;
					break;
				case -3:
					chance = 860;
					break;
				case -4:
					chance = 845;
					break;
				case -5:
					chance = 830;
					break;
				case -6:
					chance = 815;
					break;
				case -7:
					chance = 800;
					break;
				case -8:
					chance = 785;
					break;
				case -9:
					chance = 770;
					break;
				case -10:
					chance = 755;
					break;
				case -11:
					chance = 735;
					break;
				case -12:
					chance = 715;
					break;
				case -13:
					chance = 695;
					break;
				case -14:
					chance = 675;
					break;
				case -15:
					chance = 655;
					break;
				case -16:
					chance = 625;
					break;
				case -17:
					chance = 595;
					break;
				case -18:
					chance = 565;
					break;
				case -19:
					chance = 535;
					break;
				case -20:
					chance = 505;
					break;
				case -21:
					chance = 455;
					break;
				case -22:
					chance = 405;
					break;
				case -23:
					chance = 355;
					break;
				case -24:
					chance = 305;
					break;
				default:
					chance = 275;
			}

			if (!attacker.isInFrontOfTarget())
			{
				if (attacker.isBehindTarget())
					chance *= 1.2;
				else
					// side
					chance *= 1.1;

				if (chance > 980)
					chance = 980;
			}
		}
		return chance < Rnd.get(1000);
	}

	/**
	 * Test the shield use.
	 *
	 * @param attacker
	 *            The attacker.
	 * @param target
	 *            The victim ; make check about his shield.
	 * @param skill
	 *            The skill the attacker has used.
	 * @param sendSysMsg
	 *            Send or no a system message.
	 * @return 0 = shield defense doesn't succeed<br>
	 *         1 = shield defense succeed<br>
	 *         2 = perfect block
	 */
	public static byte calcShldUse(L2Character attacker, L2Character target, L2Skill skill, boolean sendSysMsg)
	{
		// Ignore shield skills types bypass the shield use.
		if (skill != null && skill.ignoreShield())
			return 0;

		L2Item item = target.getSecondaryWeaponItem();
		if (item == null || !(item instanceof L2Armor))
			return 0;

		double shldRate = target.calcStat(Stats.SHIELD_RATE, 0, attacker, null) * Formulas.DEXbonus[target.getDEX()];
		if (shldRate == 0.0)
			return 0;

		int degreeside = (int) target.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 0, null, null) + 120;
		if (degreeside < 360 && (!target.isFacing(attacker, degreeside)))
			return 0;

		byte shldSuccess = SHIELD_DEFENSE_FAILED;

		// if attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%)
		L2Weapon at_weapon = attacker.getActiveWeaponItem();
		if (at_weapon != null && at_weapon.getItemType() == L2WeaponType.BOW)
			shldRate *= 1.3;

		if (shldRate > 0 && 100 - PlayersConfig.ALT_PERFECT_SHLD_BLOCK < Rnd.get(100))
			shldSuccess = SHIELD_DEFENSE_PERFECT_BLOCK;
		else if (shldRate > Rnd.get(100))
			shldSuccess = SHIELD_DEFENSE_SUCCEED;

		if (sendSysMsg && target instanceof L2PcInstance)
		{
			switch (shldSuccess)
			{
				case SHIELD_DEFENSE_SUCCEED:
					((L2PcInstance) target).sendPacket(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL);
					break;
				case SHIELD_DEFENSE_PERFECT_BLOCK:
					((L2PcInstance) target).sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
			}
		}

		return shldSuccess;
	}

	public static byte calcShldUse(L2Character attacker, L2Character target, L2Skill skill)
	{
		return calcShldUse(attacker, target, skill, true);
	}

	public static byte calcShldUse(L2Character attacker, L2Character target)
	{
		return calcShldUse(attacker, target, null, true);
	}

	public static boolean calcMagicAffected(L2Character actor, L2Character target, L2Skill skill)
	{
		L2SkillType type = skill.getSkillType();
		if (target.isRaid() && !calcRaidAffected(type))
			return false;

		double defence = 0;

		if (skill.isActive() && skill.isOffensive())
			defence = target.getMDef(actor, skill);

		double attack = 2 * actor.getMAtk(target, skill) * calcSkillVulnerability(actor, target, skill, type);
		double d = (attack - defence) / (attack + defence);

		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}

	public static double calcSkillVulnerability(L2Character attacker, L2Character target, L2Skill skill, L2SkillType type)
	{
		double multiplier = 1;

		// Get the elemental damages.
		if (skill.getElement() > 0)
			multiplier *= Math.sqrt(calcElemental(attacker, target, skill));

		// Get the skillType to calculate its effect in function of base stats of the target.
		switch (type)
		{
			case BLEED:
				multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, target, null);
				break;
			case POISON:
				multiplier = target.calcStat(Stats.POISON_VULN, multiplier, target, null);
				break;
			case STUN:
				multiplier = target.calcStat(Stats.STUN_VULN, multiplier, target, null);
				break;
			case PARALYZE:
				multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
				break;
			case ROOT:
				multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, target, null);
				break;
			case SLEEP:
				multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, target, null);
				break;
			case MUTE:
			case FEAR:
			case BETRAY:
			case AGGDEBUFF:
			case AGGREDUCE_CHAR:
			case ERASE:
			case CONFUSION:
				multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
				break;
			case DEBUFF:
			case WEAKNESS:
				multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
				break;
			case CANCEL:
				multiplier = target.calcStat(Stats.CANCEL_VULN, multiplier, target, null);
				break;
			default:
		}

		// Return a multiplier (exemple with resist shock : 1 + (-0,4 stun vuln) = 0,6%
		return 1 + (multiplier / 100);
	}

	private static double calcSkillStatModifier(L2SkillType type, L2Character target)
	{
		double multiplier = 1;

		switch (type)
		{
			case STUN:
			case BLEED:
			case POISON:
				multiplier = 2 - sqrtCONbonus[target.getStat().getCON()];
				break;
			case SLEEP:
			case DEBUFF:
			case WEAKNESS:
			case ERASE:
			case ROOT:
			case MUTE:
			case FEAR:
			case BETRAY:
			case CONFUSION:
			case AGGREDUCE_CHAR:
			case PARALYZE:
				multiplier = 2 - sqrtMENbonus[target.getStat().getMEN()];
				break;
		}

		return Math.max(0, multiplier);
	}

	public static double getSTRBonus(L2Character activeChar)
	{
		return STRbonus[activeChar.getSTR()];
	}

	private static double getLevelModifier(L2Character attacker, L2Character target, L2Skill skill)
	{
		if (skill.getLevelDepend() == 0)
			return 1;

		int delta = (skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel()) + skill.getLevelDepend() - target.getLevel();
		return 1 + ((delta < 0 ? 0.04 : 0.02) * delta);
	}

	public static boolean calcEffectSuccess(L2Character attacker, L2Character target, EffectTemplate effect, L2Skill skill, byte shld, boolean ss, boolean sps, boolean bss)
	{
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK) // perfect block
			return false;

		final L2SkillType type = effect.effectType;
		final int value = (int) effect.effectPower;

		if (type == null)
			return Rnd.get(100) < value;
		else if (type.equals(L2SkillType.CANCEL)) // CANCEL type lands always
			return true;

		final double statModifier = calcSkillStatModifier(type, target);
		final double skillModifier = calcSkillVulnerability(attacker, target, skill, type);
		final double ssModifier = (bss ? 150 : (sps || ss ? 125 : 100));
		final double lvlModifier = getLevelModifier(attacker, target, skill);

		// Calculate BaseRate.
		double rate = value * statModifier;
		double mAtkModifier = 0;

		// Add Matk/Mdef Bonus
		if (skill.isMagic())
		{
			mAtkModifier = attacker.getMAtk(target, skill) / (2.0 * (target.getMDef(attacker, skill) + (shld == 1 ? target.getShldDef() : 0)));
			rate *= Math.pow(mAtkModifier, mAtkModifier < 1 ? 0.8 : 0.4);
		}

		// Add Bonus for Sps/SS
		if (ssModifier != 100)
		{
			if (rate > 10000 / (100 + ssModifier))
				rate = 100 - (100 - rate) * 100 / ssModifier;
			else
				rate = rate * ssModifier / 100;
		}

		// Apply level modifier.
		rate *= lvlModifier;

		// Apply skill resist.
		rate *= skillModifier;

		rate = Math.max(1, Math.min(rate, 99));

		if (MainConfig.DEVELOPER)
		{
			final StringBuilder stat = new StringBuilder(140);
			StringUtil.append(stat, "calcEffectSuccess(): Name:", skill.getName(), " eff.type:", type.toString(), " power:", String.valueOf(value), " statMod:", String.format("%1.2f", statModifier), " skillMod:", String.format("%1.2f", skillModifier), " mAtkMod:", String.format("%1.2f", mAtkModifier), " ssMod:", String.valueOf(ssModifier), " lvlMod:", String.format("%1.2f", lvlModifier), " total:", String.format("%1.2f", rate), "%");

			final String result = stat.toString();
			_log.info(result);
		}

		return (Rnd.get(100) < rate);
	}

	public static boolean calcSkillSuccess(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss, boolean sps, boolean bss)
	{
		final double baseChance = skill.getEffectPower();

		return calcSkillSuccess(baseChance, attacker, target, skill, shld, ss, sps, bss);
	}

	public static boolean calcSkillSuccess(final double baseChance, L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss, boolean sps, boolean bss)
	{
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK) // perfect block
			return false;

		final L2SkillType type = skill.getEffectType();

		if (target.isRaid() && !calcRaidAffected(type))
			return false;

		if (skill.ignoreResists())
			return (Rnd.get(100) < baseChance);

		final double statModifier = calcSkillStatModifier(type, target);
		final double skillModifier = calcSkillVulnerability(attacker, target, skill, type);
		final double ssModifier = (bss ? 150 : (sps || ss ? 125 : 100));
		final double lvlModifier = getLevelModifier(attacker, target, skill);

		// Calculate BaseRate.
		double rate = baseChance * statModifier;
		double mAtkModifier = 0;

		// Add Matk/Mdef Bonus
		if (skill.isMagic())
		{
			mAtkModifier = attacker.getMAtk(target, skill) / (2.0 * (target.getMDef(attacker, skill) + (shld == 1 ? target.getShldDef() : 0)));
			rate *= Math.pow(mAtkModifier, mAtkModifier < 1 ? 0.8 : 0.4);
		}

		// Add Bonus for Sps/SS
		if (ssModifier != 100)
		{
			if (rate > 10000 / (100 + ssModifier))
				rate = 100 - (100 - rate) * 100 / ssModifier;
			else
				rate = rate * ssModifier / 100;
		}

		// Apply level modifier.
		rate *= lvlModifier;

		// Apply skill resist.
		rate *= skillModifier;

		rate = Math.max(1, Math.min(rate, 99));

		if (MainConfig.DEVELOPER)
		{
			final StringBuilder stat = new StringBuilder(140);
			StringUtil.append(stat, "calcSkillSuccess(): Name:", skill.getName(), " type:", skill.getSkillType().toString(), " power:", String.valueOf(baseChance), " statMod:", String.format("%1.2f", statModifier), " skillMod:", String.format("%1.2f", skillModifier), " mAtkMod:", String.format("%1.2f", mAtkModifier), " ssMod:", String.valueOf(ssModifier), " lvlMod:", String.format("%1.2f", lvlModifier), " total:", String.format("%1.2f", rate), "%");

			final String result = stat.toString();
			_log.info(result);
		}

		return (Rnd.get(100) < rate);
	}

	public static boolean calcCubicSkillSuccess(L2CubicInstance attacker, L2Character target, L2Skill skill, byte shld)
	{
		// if target reflect this skill then the effect will fail
		if (calcSkillReflect(target, skill) != SKILL_REFLECT_FAILED)
			return false;

		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK) // perfect block
			return false;

		final L2SkillType type = skill.getEffectType();

		if (target.isRaid() && !calcRaidAffected(type))
			return false;

		final double baseChance = skill.getEffectPower();

		if (skill.ignoreResists())
			return Rnd.get(100) < baseChance;

		final double statModifier = calcSkillStatModifier(type, target);
		final double skillModifier = calcSkillVulnerability(attacker.getOwner(), target, skill, type);
		final double lvlModifier = getLevelModifier(attacker.getOwner(), target, skill);

		double rate = baseChance * statModifier * skillModifier;
		double mAtkModifier = 0;

		// Add Matk/Mdef Bonus
		if (skill.isMagic())
		{
			mAtkModifier = attacker.getMAtk() / (2.0 * (target.getMDef(attacker.getOwner(), skill) + (shld == 1 ? target.getShldDef() : 0)));
			rate *= Math.pow(mAtkModifier, mAtkModifier < 1 ? 0.8 : 0.4) * 100 - 100;
		}

		rate *= lvlModifier;

		rate = Math.max(1, Math.min(rate, 99));

		if (MainConfig.DEVELOPER)
		{
			final StringBuilder stat = new StringBuilder(140);
			StringUtil.append(stat, "calcCubicSkillSuccess(): Name:", skill.getName(), " type:", skill.getSkillType().toString(), " power:", String.valueOf(baseChance), " statMod:", String.format("%1.2f", statModifier), " skillMod:", String.format("%1.2f", skillModifier), " mAtkMod:", String.format("%1.2f", mAtkModifier), " lvlMod:", String.format("%1.2f", lvlModifier), " total:", String.format("%1.2f", rate), "%");

			final String result = stat.toString();
			_log.info(result);
		}

		return (Rnd.get(100) < rate);
	}

	public static boolean calcMagicSuccess(L2Character attacker, L2Character target, L2Skill skill)
	{
		int lvlDifference = target.getLevel() - ((skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel()) + skill.getLevelDepend());
		double rate = 100;

		if (lvlDifference > 0)
			rate = (Math.pow(1.3, lvlDifference)) * 100;

		if (MainConfig.DEVELOPER)
		{
			final StringBuilder stat = new StringBuilder(80);
			StringUtil.append(stat, "calcMagicSuccess(): Name:", skill.getName(), " lvlDiff:", String.valueOf(lvlDifference), " fail:", String.format("%1.2f", rate / 100), "%");

			final String result = stat.toString();
			_log.info(result);
		}

		rate = Math.min(rate, 9900);

		return (Rnd.get(10000) > rate);
	}

	public static boolean calculateUnlockChance(L2Skill skill)
	{
		int level = skill.getLevel();
		int chance = 0;
		switch (level)
		{
			case 1:
				chance = 30;
				break;
			case 2:
				chance = 50;
				break;
			case 3:
				chance = 75;
				break;
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
				chance = 100;
				break;
		}

		if (Rnd.get(100) > chance)
			return false;

		return true;
	}

	public static double calcManaDam(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean bss)
	{
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		double mp = target.getMaxMp();

		if (bss)
			mAtk *= 4;
		else if (ss)
			mAtk *= 2;

		double damage = (Math.sqrt(mAtk) * skill.getPower(attacker, target) * (mp / 97)) / mDef;
		damage *= calcSkillVulnerability(attacker, target, skill, skill.getSkillType());
		return damage;
	}

	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, L2Character caster)
	{
		if (baseRestorePercent == 0 || baseRestorePercent == 100)
			return baseRestorePercent;

		double restorePercent = baseRestorePercent * Formulas.WITbonus[caster.getWIT()];
		if (restorePercent - baseRestorePercent > 20.0)
			restorePercent += 20.0;

		restorePercent = Math.max(restorePercent, baseRestorePercent);
		restorePercent = Math.min(restorePercent, 90.0);

		return restorePercent;
	}

	public static boolean calcPhysicalSkillEvasion(L2Character target, L2Skill skill)
	{
		if (skill.isMagic())
			return false;

		return Rnd.get(100) < target.calcStat(Stats.P_SKILL_EVASION, 0, null, skill);
	}

	public static boolean calcSkillMastery(L2Character actor, L2Skill sk)
	{
		// Pointless check for L2Character other than players, as initial value will stay 0.
		if (!(actor instanceof L2PcInstance))
			return false;

		if (sk.getSkillType() == L2SkillType.FISHING)
			return false;

		double val = actor.getStat().calcStat(Stats.SKILL_MASTERY, 0, null, null);

		if (((L2PcInstance) actor).isMageClass())
			val *= INTbonus[actor.getINT()];
		else
			val *= STRbonus[actor.getSTR()];

		return Rnd.get(100) < val;
	}

	public static double calcValakasAttribute(L2Character attacker, L2Character target, L2Skill skill)
	{
		double calcPower = 0;
		double calcDefen = 0;

		if (skill != null && skill.getAttributeName().contains("valakas"))
		{
			calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
			calcDefen = target.calcStat(Stats.VALAKAS_RES, calcDefen, target, skill);
		}
		else
		{
			calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
			if (calcPower > 0)
			{
				calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
				calcDefen = target.calcStat(Stats.VALAKAS_RES, calcDefen, target, skill);
			}
		}
		return calcPower - calcDefen;
	}

	public static double calcElemental(L2Character attacker, L2Character target, L2Skill skill)
	{
		if (skill != null)
		{
			final byte element = skill.getElement();
			if (element >= 0)
			{
				// power is basically put to 20
				int calcPower = 20;
				int calcDefen = target.getDefenseElementValue(element);

				/*
				 * if (attacker.getAttackElement() == element) FIXME implementation elemental to a skill. calcPower +=
				 * attacker.getAttackElementValue(element);
				 */

				int calcTotal = calcPower - calcDefen;
				if (calcTotal > 0)
				{
					if (calcTotal < 75)
						return 1 + calcTotal * 0.0052;
					else if (calcTotal < 150)
						return 1.4;
					else if (calcTotal < 290)
						return 1.7;
					else if (calcTotal < 300)
						return 1.8;
					else
						return 2.0;
				}
			}
		}/*
		 * else { final byte element = attacker.getAttackElement(); if (element >= 0) { int calcPower =
		 * attacker.getAttackElementValue(element); int calcDefen = target.getDefenseElementValue(element); return 1 +
		 * L2Math.limit(-20, calcPower - calcDefen, 100) * 0.007; } }
		 */

		return 1;
	}

	/**
	 * Calculate skill reflection according these three possibilities: <li>Reflect failed</li> <li>Mormal reflect (just effects).
	 * <U>Only possible for skilltypes: BUFF, REFLECT, HEAL_PERCENT, MANAHEAL_PERCENT, HOT, CPHOT, MPHOT</U></li> <li>vengEance
	 * reflect (100% damage reflected but damage is also dealt to actor). <U>This is only possible for skills with skilltype PDAM,
	 * BLOW, CHARGEDAM, MDAM or DEATHLINK</U></li>
	 *
	 * @param target
	 * @param skill
	 * @return SKILL_REFLECTED_FAILED, SKILL_REFLECT_SUCCEED or SKILL_REFLECT_VENGEANCE
	 */
	public static byte calcSkillReflect(L2Character target, L2Skill skill)
	{
		/*
		 * Neither some special skills (like hero debuffs...) or those skills ignoring resistances can be reflected
		 */
		if (skill.ignoreResists() || !skill.canBeReflected())
			return SKILL_REFLECT_FAILED;

		// only magic and melee skills can be reflected
		if (!skill.isMagic() && (skill.getCastRange() == -1 || skill.getCastRange() > MELEE_ATTACK_RANGE))
			return SKILL_REFLECT_FAILED;

		byte reflect = SKILL_REFLECT_FAILED;
		// check for non-reflected skilltypes, need additional retail check
		switch (skill.getSkillType())
		{
			case BUFF:
			case REFLECT:
			case HEAL_PERCENT:
			case MANAHEAL_PERCENT:
			case HOT:
			case CPHOT:
			case MPHOT:
			case UNDEAD_DEFENSE:
			case AGGDEBUFF:
			case CONT:
				return SKILL_REFLECT_FAILED;
				// these skill types can deal damage
			case PDAM:
			case BLOW:
			case MDAM:
			case DEATHLINK:
			case CHARGEDAM:
				final Stats stat = skill.isMagic() ? Stats.VENGEANCE_SKILL_MAGIC_DAMAGE : Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE;
				final double venganceChance = target.getStat().calcStat(stat, 0, target, skill);
				if (venganceChance > Rnd.get(100))
					reflect |= SKILL_REFLECT_VENGEANCE;
				break;
		}

		final double reflectChance = target.calcStat(skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, skill);
		if (Rnd.get(100) < reflectChance)
			reflect |= SKILL_REFLECT_SUCCEED;

		return reflect;
	}

	/**
	 * Calculate damage caused by falling
	 *
	 * @param cha
	 * @param fallHeight
	 * @return damage
	 */
	public static double calcFallDam(L2Character cha, int fallHeight)
	{
		if (!MainConfig.ENABLE_FALLING_DAMAGE || fallHeight < 0)
			return 0;

		final double damage = cha.calcStat(Stats.FALL, fallHeight * cha.getMaxHp() / 1000, null, null);
		return damage;
	}

	public static int calcNpcHpBonus(int CON, int baseHpMax)
	{
		try
		{
			baseHpMax = (int) (baseHpMax * CONbonus[CON]);
			return baseHpMax;
		}
		catch (Exception e)
		{
			_log.warn("Formulas: Error in NPC Data!", e);
			return baseHpMax;
		}
	}

	public static int calcNpcMpBonus(int MEN, int baseMpMax)
	{
		try
		{
			baseMpMax = (int) (baseMpMax * MENbonus[MEN]);
			return baseMpMax;
		}
		catch (Exception e)
		{
			_log.warn("Formulas: Error in NPC Data!", e);
			return baseMpMax;
		}
	}

	public static double calcNpcMdefBonus(int MEN, int baseMdef, int level)
	{
		try
		{

			return (0.00905984 * level + 0.811133) * baseMdef * MENbonus[MEN];
		}
		catch (Exception e)
		{
			_log.warn("Formulas: Error in NPC Data!", e);
			return baseMdef;
		}
	}

	public static double calcNpcPdefBonus(int basePdef, int level)
	{
		try
		{
			return (0.0100095 * level + 0.890021) * basePdef;
		}
		catch (Exception e)
		{
			_log.warn("Formulas: Error in NPC Data!", e);
			return basePdef;
		}
	}

	public static final double calcNpcMatkBonus(int INT, int baseMatk, int level)
	{
		try
		{
			return (0.0100095 * level + 0.890021) * baseMatk * INTbonus[INT];
		}
		catch (Exception e)
		{
			_log.warn("Formulas: Error in NPC Data!", e);
			return baseMatk;
		}
	}

	public static final double calcNpcPatkBonus(int STR, int basePatk, int level)
	{
		try
		{

			return (0.00986325 * level + 0.885473) * basePatk * STRbonus[STR];
		}
		catch (Exception e)
		{
			_log.warn("Formulas: Error in NPC Data!", e);
			return basePatk;
		}
	}

	public static final int calcNpcPatkSpdBonus(int DEX, int basePatkSpd)
	{
		try
		{
			basePatkSpd = (int) (basePatkSpd * DEXbonus[DEX]);
			return basePatkSpd;
		}
		catch (Exception e)
		{
			_log.warn("Formulas: Error in NPC Data!", e);
			return basePatkSpd;
		}
	}

	public static final int calcNpcMoveBonus(int DEX, int baseMoveSpd)
	{
		try
		{
			baseMoveSpd = (int) (baseMoveSpd * DEXbonus[DEX]);
			return baseMoveSpd;
		}
		catch (Exception e)
		{
			_log.warn("Formulas: Error in NPC Data!", e);
			return baseMoveSpd;
		}
	}

	public static final int calcNpcMatkSpdBonus(int WIT, int baseMatkSpd)
	{
		try
		{
			baseMatkSpd = (int) (baseMatkSpd * WITbonus[WIT]);
			return baseMatkSpd;
		}
		catch (Exception e)
		{
			_log.warn("Formulas: Error in NPC Data!", e);
			return baseMatkSpd;
		}
	}

	public static boolean calcRaidAffected(L2SkillType type)
	{
		switch (type)
		{
			case MANADAM:
			case MDOT:
				return true;
			case CONFUSION:
			case ROOT:
			case STUN:
			case MUTE:
			case FEAR:
			case DEBUFF:
			case PARALYZE:
			case SLEEP:
			case AGGDEBUFF:
			case AGGREDUCE_CHAR:
				if (Rnd.get(1000) == 1)
					return true;
		}
		return false;
	}
}
