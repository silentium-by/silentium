/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.ai;

import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Character.AIAccessor;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.network.SystemMessageId;

/**
 * This class manages AI of L2Playable.<BR>
 * <BR>
 * PlayableAI :<BR>
 * <BR>
 * <li>SummonAI</li> <li>PlayerAI</li> <BR>
 * <BR>
 * 
 * @author JIV
 */
public abstract class PlayableAI extends CharacterAI
{
	public PlayableAI(AIAccessor accessor)
	{
		super(accessor);
	}

	/**
	 * @see silentium.gameserver.ai.CharacterAI#onIntentionAttack(silentium.gameserver.model.actor.L2Character)
	 */
	@Override
	protected void onIntentionAttack(L2Character target)
	{
		if (target instanceof L2Playable)
		{
			if (target.getActingPlayer().getProtectionBlessing() && (_actor.getActingPlayer().getLevel() - target.getActingPlayer().getLevel()) >= 10 && _actor.getActingPlayer().getKarma() > 0 && !(target.isInsideZone(L2Character.ZONE_PVP)))
			{
				// If attacker have karma, level >= 10 and target have Newbie Protection Buff
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}

			if (_actor.getActingPlayer().getProtectionBlessing() && (target.getActingPlayer().getLevel() - _actor.getActingPlayer().getLevel()) >= 10 && target.getActingPlayer().getKarma() > 0 && !(target.isInsideZone(L2Character.ZONE_PVP)))
			{
				// If target have karma, level >= 10 and actor have Newbie Protection Buff
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}

			if (target.getActingPlayer().isCursedWeaponEquipped() && _actor.getActingPlayer().getLevel() <= 20)
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}

			if (_actor.getActingPlayer().isCursedWeaponEquipped() && target.getActingPlayer().getLevel() <= 20)
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}
		}
		super.onIntentionAttack(target);
	}

	/**
	 * @see silentium.gameserver.ai.CharacterAI#onIntentionCast(silentium.gameserver.model.L2Skill, silentium.gameserver.model.L2Object)
	 */
	@Override
	protected void onIntentionCast(L2Skill skill, L2Object target)
	{
		if (target instanceof L2Playable && skill.isOffensive())
		{
			if (target.getActingPlayer().getProtectionBlessing() && (_actor.getActingPlayer().getLevel() - target.getActingPlayer().getLevel()) >= 10 && _actor.getActingPlayer().getKarma() > 0 && !(((L2Playable) target).isInsideZone(L2Character.ZONE_PVP)))
			{
				// If attacker have karma, level >= 10 and target have Newbie Protection Buff
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}

			if (_actor.getActingPlayer().getProtectionBlessing() && (target.getActingPlayer().getLevel() - _actor.getActingPlayer().getLevel()) >= 10 && target.getActingPlayer().getKarma() > 0 && !(((L2Playable) target).isInsideZone(L2Character.ZONE_PVP)))
			{
				// If target have karma, level >= 10 and actor have Newbie Protection Buff
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}

			if (target.getActingPlayer().isCursedWeaponEquipped() && _actor.getActingPlayer().getLevel() <= 20)
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}

			if (_actor.getActingPlayer().isCursedWeaponEquipped() && target.getActingPlayer().getLevel() <= 20)
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}
		}
		super.onIntentionCast(skill, target);
	}
}