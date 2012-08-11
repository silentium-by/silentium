/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import gnu.trove.map.hash.TIntObjectHashMap;
import silentium.gameserver.GameTimeController;
import silentium.gameserver.ai.CtrlEvent;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.ai.NextAction;
import silentium.gameserver.ai.NextAction.NextActionCallback;
import silentium.gameserver.ai.SummonAI;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.model.L2CharPosition;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.*;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.ChairSit;
import silentium.gameserver.utils.Util;

public final class RequestActionUse extends L2GameClientPacket
{
	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;

	@Override
	protected void readImpl()
	{
		_actionId = readD();
		_ctrlPressed = (readD() == 1);
		_shiftPressed = (readC() == 1);
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		log.debug(activeChar.getName() + " request Action use: id " + _actionId + " 2:" + _ctrlPressed + " 3:" +
				_shiftPressed);

		// dont do anything if player is dead, or use fakedeath using another action than sit.
		if ((activeChar.isFakeDeath() && _actionId != 0) || activeChar.isDead())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// don't do anything if player is confused
		if (activeChar.isOutOfControl())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Summon pet = activeChar.getPet();
		L2Object target = activeChar.getTarget();

		log.info("Requested Action ID: " + _actionId);

		switch (_actionId)
		{
			case 0:
				final L2PcInstance ch = activeChar;
				final L2Object targ = target;

				if (activeChar.getMountType() != 0)
					break;

				if (activeChar.isFakeDeath())
				{
					activeChar.stopFakeDeath(true);
					break;
				}

				if (activeChar.isSitting() || !activeChar.isMoving())
					useSit(ch, targ);
				else
				{
					// Sit when arrive using next action, creating next action class.
					NextAction nextAction = new NextAction(CtrlEvent.EVT_ARRIVED, CtrlIntention.AI_INTENTION_MOVE_TO, new NextActionCallback()
					{
						@Override
						public void doWork()
						{
							useSit(ch, targ);
						}
					});

					// Binding next action to AI.
					activeChar.getAI().setNextAction(nextAction);
				}
				break;
			case 1:
				if (activeChar.isRunning())
					activeChar.setWalking();
				else
					activeChar.setRunning();

				log.trace("new move type: " + (activeChar.isRunning() ? "RUNNING" : "WALKING"));
				break;
			case 10: // Private Store - Sell
				activeChar.tryOpenPrivateSellStore(false);
				break;
			case 28: // Private Store - Buy
				activeChar.tryOpenPrivateBuyStore();
				break;
			case 15:
			case 21: // Change Movement Mode (pet follow/stop)
				if (pet != null)
				{
					// You can't order anymore your pet to stop if distance is superior to 2000.
					if (pet.getFollowStatus() && Util.calculateDistance(activeChar, pet, true) > 2000)
						return;

					if (!activeChar.isBetrayed())
						((SummonAI) pet.getAI()).notifyFollowStatusChange();
				}
				break;
			case 16:
			case 22: // Attack (pet attack)
				if (target != null && pet != null && pet != target && activeChar != target && !pet.isBetrayed())
				{
					if (pet.isAttackingDisabled())
					{
						if (pet.getAttackEndTime() > GameTimeController.getGameTicks())
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						else
							return;
					}

					if (pet instanceof L2PetInstance && (pet.getLevel() - activeChar.getLevel() > 20))
					{
						activeChar.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
						return;
					}

					if (activeChar.isInOlympiadMode() && !activeChar.isOlympiadStart())
					{
						// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet
						// ActionFailed
						activeChar.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}

					if (!activeChar.getAccessLevel().allowPeaceAttack() && L2Character.isInsidePeaceZone(pet, target))
					{
						activeChar.sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
						return;
					}

					pet.setTarget(target);
					if (target.isAutoAttackable(activeChar) || _ctrlPressed)
					{
						if (target instanceof L2DoorInstance)
						{
							if (((L2DoorInstance) target).isAttackable(activeChar) && pet.getNpcId() != L2SiegeSummonInstance.SWOOP_CANNON_ID)
								pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						}
						// siege golem AI doesn't support attacking other than doors at the moment
						else if (pet.getNpcId() != L2SiegeSummonInstance.SIEGE_GOLEM_ID)
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
					}
					else
					{
						pet.setFollowStatus(false);
						pet.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target);
					}
				}
				break;
			case 17:
			case 23: // Stop (pet - cancel action)
				if (pet != null && !pet.isMovementDisabled() && !pet.isBetrayed())
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				break;
			case 19: // Returns pet to control item
				if (pet != null && pet instanceof L2PetInstance)
				{
					if (pet.isDead())
						activeChar.sendPacket(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED);
					else if (pet.isBetrayed() || pet.isMovementDisabled())
						activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
					else if (pet.isAttackingNow() || pet.isInCombat())
						activeChar.sendPacket(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE);
					else
					{
						// if the pet is hungry, you can't unsummon it
						if (((L2PetInstance) pet).isHungry())
							activeChar.sendPacket(SystemMessageId.YOU_CANNOT_RESTORE_HUNGRY_PETS);
						else
							pet.unSummon(activeChar);
					}
				}
				break;
			case 38: // pet mount/dismount
				activeChar.mountPlayer(pet);
				break;
			case 32: // Wild Hog Cannon - Mode Change
				useSkill(4230);
				break;
			case 36: // Soulless - Toxic Smoke
				useSkill(4259);
				break;
			case 37: // Dwarven Manufacture
				activeChar.tryOpenWorkshop(true);
				break;
			case 39: // Soulless - Parasite Burst
				useSkill(4138);
				break;
			case 41: // Wild Hog Cannon - Attack
				useSkill(4230);
				break;
			case 42: // Kai the Cat - Self Damage Shield
				useSkill(4378, activeChar);
				break;
			case 43: // Unicorn Merrow - Hydro Screw
				useSkill(4137);
				break;
			case 44: // Big Boom - Boom Attack
				useSkill(4139);
				break;
			case 45: // Unicorn Boxer - Master Recharge
				useSkill(4025, activeChar);
				break;
			case 46: // Mew the Cat - Mega Storm Strike
				useSkill(4261);
				break;
			case 47: // Silhouette - Steal Blood
				useSkill(4260);
				break;
			case 48: // Mechanic Golem - Mech. Cannon
				useSkill(4068);
				break;
			case 51: // General Manufacture
				activeChar.tryOpenWorkshop(false);
				break;
			case 52: // Unsummon a servitor
				if (pet != null && pet instanceof L2SummonInstance)
				{
					if (pet.isDead())
						activeChar.sendPacket(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED);
					else if (pet.isBetrayed() || pet.isMovementDisabled())
						activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
					else if (pet.isAttackingNow() || pet.isInCombat())
						activeChar.sendPacket(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE);
					else
						pet.unSummon(activeChar);
				}
				break;
			case 53: // move to target
			case 54: // move to target hatch/strider
				if (target != null && pet != null && pet != target && !pet.isMovementDisabled() && !pet.isBetrayed())
				{
					pet.setFollowStatus(false);
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			case 61: // Private Store Package Sell
				activeChar.tryOpenPrivateSellStore(true);
			case 1000: // Siege Golem - Siege Hammer
				if (target instanceof L2DoorInstance)
					useSkill(4079);
				break;
			case 1001: // Sin Eater - Ultimate Bombastic Buster
				// useSkill();
				break;
			case 1003: // Wind Hatchling/Strider - Wild Stun
				useSkill(4710);
				break;
			case 1004: // Wind Hatchling/Strider - Wild Defense
				useSkill(4711, activeChar);
				break;
			case 1005: // Star Hatchling/Strider - Bright Burst
				useSkill(4712);
				break;
			case 1006: // Star Hatchling/Strider - Bright Heal
				useSkill(4713, activeChar);
				break;
			case 1007: // Cat Queen - Blessing of Queen
				useSkill(4699, activeChar);
				break;
			case 1008: // Cat Queen - Gift of Queen
				useSkill(4700, activeChar);
				break;
			case 1009: // Cat Queen - Cure of Queen
				useSkill(4701);
				break;
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
				useSkill(4702, activeChar);
				break;
			case 1011: // Unicorn Seraphim - Gift of Seraphim
				useSkill(4703, activeChar);
				break;
			case 1012: // Unicorn Seraphim - Cure of Seraphim
				useSkill(4704);
				break;
			case 1013: // Nightshade - Curse of Shade
				useSkill(4705);
				break;
			case 1014: // Nightshade - Mass Curse of Shade
				useSkill(4706, activeChar);
				break;
			case 1015: // Nightshade - Shade Sacrifice
				useSkill(4707);
				break;
			case 1016: // Cursed Man - Cursed Blow
				useSkill(4709);
				break;
			case 1017: // Cursed Man - Cursed Strike/Stun
				useSkill(4708);
				break;
			case 1031: // Feline King - Slash
				useSkill(5135);
				break;
			case 1032: // Feline King - Spinning Slash
				useSkill(5136);
				break;
			case 1033: // Feline King - Grip of the Cat
				useSkill(5137);
				break;
			case 1034: // Magnus the Unicorn - Whiplash
				useSkill(5138);
				break;
			case 1035: // Magnus the Unicorn - Tridal Wave
				useSkill(5139);
				break;
			case 1036: // Spectral Lord - Corpse Kaboom
				useSkill(5142);
				break;
			case 1037: // Spectral Lord - Dicing Death
				useSkill(5141);
				break;
			case 1038: // Spectral Lord - Force Curse
				useSkill(5140);
				break;
			case 1039: // Swoop Cannon - Cannon Fodder
				if (!(target instanceof L2DoorInstance))
					useSkill(5110);
				break;
			case 1040: // Swoop Cannon - Big Bang
				if (!(target instanceof L2DoorInstance))
					useSkill(5111);
				break;
			default:
				log.warn(activeChar.getName() + ": unhandled action type " + _actionId);
		}
	}

	public static boolean useSit(L2PcInstance activeChar, L2Object target)
	{
		if (activeChar.getMountType() != 0)
			return false;

		if (target != null && !activeChar.isSitting() && target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1 && CastleManager.getInstance().getCastle(target) != null && activeChar.isInsideRadius(target, L2StaticObjectInstance.INTERACTION_DISTANCE, false, false))
		{
			final ChairSit cs = new ChairSit(activeChar, ((L2StaticObjectInstance) target).getStaticObjectId());
			activeChar.sendPacket(cs);
			activeChar.sitDown();
			activeChar.broadcastPacket(cs);
			return false;
		}

		if (activeChar.isSitting())
			activeChar.standUp();
		else
			activeChar.sitDown();

		log.trace("new wait type: " + (activeChar.isSitting() ? "SITTING" : "STANDING"));

		return true;
	}

	/*
	 * Cast a skill for active pet/servitor. Target is specified as a parameter but can be overwrited or ignored depending on
	 * skill type.
	 */
	private void useSkill(int skillId, L2Object target)
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null || activeChar.getPrivateStoreType() != 0)
			return;

		final L2Summon activeSummon = activeChar.getPet();
		if (activeSummon != null && !activeSummon.isBetrayed())
		{
			if (activeSummon instanceof L2PetInstance)
			{
				if (activeSummon.getLevel() - activeChar.getLevel() > 20)
				{
					activeChar.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
					return;
				}
			}

			TIntObjectHashMap<L2Skill> _skills = activeSummon.getTemplate().getSkills();
			if (_skills == null || _skills.isEmpty())
				return;

			L2Skill skill = _skills.get(skillId);
			if (skill == null)
			{
				log.warn(activeSummon.getName() + " does not have the skill id " + skillId + " assigned.");
				return;
			}

			if (skill.isOffensive() && activeChar == target)
				return;

			activeSummon.setTarget(target);
			activeSummon.useMagic(skill, _ctrlPressed, _shiftPressed);
		}
	}

	/*
	 * Cast a skill for active pet/servitor. Target is retrieved from owner' target, then validated by overloaded method
	 * useSkill(int, L2Object).
	 */
	private void useSkill(int skillId)
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		useSkill(skillId, activeChar.getTarget());
	}
}
