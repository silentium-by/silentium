/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor;

import silentium.gameserver.ai.CtrlEvent;
import silentium.gameserver.model.CharEffectList;
import silentium.gameserver.model.L2Effect;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.knownlist.PlayableKnownList;
import silentium.gameserver.model.actor.stat.PlayableStat;
import silentium.gameserver.model.actor.status.PlayableStatus;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.templates.chars.L2CharTemplate;
import silentium.gameserver.templates.skills.L2EffectType;

/**
 * This class represents all Playable characters in the world.<BR>
 * <BR>
 * L2Playable :<BR>
 * <BR>
 * <li>L2PcInstance</li> <li>L2Summon</li><BR>
 * <BR>
 */
public abstract class L2Playable extends L2Character
{
	/**
	 * Constructor of L2Playable (use L2Character constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and link copy basic Calculator set to this L2Playable</li> <BR>
	 * <BR>
	 * 
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param template
	 *            The L2CharTemplate to apply to the L2Playable
	 */
	public L2Playable(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void initKnownList()
	{
		setKnownList(new PlayableKnownList(this));
	}

	@Override
	public PlayableKnownList getKnownList()
	{
		return (PlayableKnownList) super.getKnownList();
	}

	@Override
	public void initCharStat()
	{
		setStat(new PlayableStat(this));
	}

	@Override
	public PlayableStat getStat()
	{
		return (PlayableStat) super.getStat();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new PlayableStatus(this));
	}

	@Override
	public PlayableStatus getStatus()
	{
		return (PlayableStatus) super.getStatus();
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
				return false;

			// now reset currentHp to zero
			setCurrentHp(0);

			setIsDead(true);
		}

		// Set target to null and cancel Attack or Cast
		setTarget(null);

		// Stop movement
		stopMove(null);

		// Stop HP/MP/CP Regeneration task
		getStatus().stopHpMpRegeneration();

		// Stop all active skills effects in progress
		if (isPhoenixBlessed())
		{
			// remove Lucky Charm if player has SoulOfThePhoenix/Salvation buff
			if (getCharmOfLuck())
				stopCharmOfLuck(null);
			if (isNoblesseBlessed())
				stopNoblesseBlessing(null);
		}
		// Same thing if the Character isn't a Noblesse Blessed L2Playable
		else if (isNoblesseBlessed())
		{
			stopNoblesseBlessing(null);

			// remove Lucky Charm if player have Nobless blessing buff
			if (getCharmOfLuck())
				stopCharmOfLuck(null);
		}
		else
			stopAllEffectsExceptThoseThatLastThroughDeath();

		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		broadcastStatusUpdate();

		if (getWorldRegion() != null)
			getWorldRegion().onDeath(this);

		// Notify Quest of L2Playable's death
		L2PcInstance actingPlayer = getActingPlayer();
		if (!actingPlayer.isNotifyQuestOfDeathEmpty())
		{
			for (QuestState qs : actingPlayer.getNotifyQuestOfDeath())
				qs.getQuest().notifyDeath((killer == null ? this : killer), this, qs);
		}

		if (killer != null)
		{
			L2PcInstance player = killer.getActingPlayer();

			if (player != null)
				player.onKillUpdatePvPKarma(this);
		}
		// Notify L2Character AI
		getAI().notifyEvent(CtrlEvent.EVT_DEAD);

		return true;
	}

	public boolean checkIfPvP(L2Character target)
	{
		if (target == null || target == this)
			return false;

		if (!(target instanceof L2Playable))
			return false;

		L2PcInstance player = getActingPlayer();
		if (player == null || player.getKarma() != 0)
			return false;

		L2PcInstance targetPlayer = target.getActingPlayer();
		if (targetPlayer == null || targetPlayer == this)
			return false;

		if (targetPlayer.getKarma() != 0 || targetPlayer.getPvpFlag() == 0)
			return false;

		return true;
	}

	/**
	 * Return True.<BR>
	 * <BR>
	 */
	@Override
	public boolean isAttackable()
	{
		return true;
	}

	// Support for Noblesse Blessing skill, where buffs are retained after resurrect
	public final boolean isNoblesseBlessed()
	{
		return _effects.isAffected(CharEffectList.EFFECT_FLAG_NOBLESS_BLESSING);
	}

	public final void stopNoblesseBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.NOBLESSE_BLESSING);
		else
			removeEffect(effect);
		updateAbnormalEffect();
	}

	// Support for Soul of the Phoenix and Salvation skills
	public final boolean isPhoenixBlessed()
	{
		return _effects.isAffected(CharEffectList.EFFECT_FLAG_PHOENIX_BLESSING);
	}

	public final void stopPhoenixBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PHOENIX_BLESSING);
		else
			removeEffect(effect);

		updateAbnormalEffect();
	}

	/**
	 * @return True if the Silent Moving mode is active.
	 */
	public boolean isSilentMoving()
	{
		return _effects.isAffected(CharEffectList.EFFECT_FLAG_SILENT_MOVE);
	}

	// for Newbie Protection Blessing skill, keeps you safe from an attack by a chaotic character >= 10 levels apart from you
	public final boolean getProtectionBlessing()
	{
		return _effects.isAffected(CharEffectList.EFFECT_FLAG_PROTECTION_BLESSING);
	}

	public void stopProtectionBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PROTECTION_BLESSING);
		else
			removeEffect(effect);

		updateAbnormalEffect();
	}

	// Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
	public final boolean getCharmOfLuck()
	{
		return _effects.isAffected(CharEffectList.EFFECT_FLAG_CHARM_OF_LUCK);
	}

	public final void stopCharmOfLuck(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.CHARM_OF_LUCK);
		else
			removeEffect(effect);

		updateAbnormalEffect();
	}

	@Override
	public void updateEffectIcons(boolean partyOnly)
	{
		_effects.updateEffectIcons(partyOnly);
	}

	/**
	 * This method allows to easily send relations. Overridden in L2Summon and L2PcInstance.
	 */
	public void broadcastRelationsChanges()
	{
	}

	public abstract int getKarma();

	public abstract byte getPvpFlag();

	public abstract boolean useMagic(L2Skill skill, boolean forceUse, boolean dontMove);
}