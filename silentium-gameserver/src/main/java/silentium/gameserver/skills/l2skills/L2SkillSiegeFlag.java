/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.l2skills;

import silentium.gameserver.idfactory.IdFactory;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.SiegeManager;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2SiegeFlagInstance;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.NpcTable;
import silentium.gameserver.templates.StatsSet;

public class L2SkillSiegeFlag extends L2Skill
{
	private final boolean _isAdvanced;

	public L2SkillSiegeFlag(StatsSet set)
	{
		super(set);
		_isAdvanced = set.getBool("isAdvanced", false);
	}

	/**
	 * @see silentium.gameserver.model.L2Skill#useSkill(silentium.gameserver.model.actor.L2Character, silentium.gameserver.model.L2Object[])
	 */
	@Override
	public void useSkill(L2Character activeChar, L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		final L2PcInstance player = (L2PcInstance) activeChar;

		if (!player.isClanLeader())
			return;

		if (!checkIfOkToPlaceFlag(player, true))
			return;

		try
		{
			// Spawn a new flag
			L2SiegeFlagInstance flag = new L2SiegeFlagInstance(player, IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(35062), _isAdvanced);
			flag.setTitle(player.getClan().getName());
			flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
			flag.setHeading(player.getHeading());
			flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);

			final Castle castle = CastleManager.getInstance().getCastle(activeChar);
			if (castle != null)
				castle.getSiege().getFlag(player.getClan()).add(flag);
		}
		catch (Exception e)
		{
			_log.warn("Error placing flag: " + e.getMessage(), e);
		}
	}

	/**
	 * @param activeChar
	 *            The L2Character of the character placing the flag
	 * @param isCheckOnly
	 *            if false, it will send a notification to the player telling him why it failed
	 * @return true if character clan place a flag
	 */
	public static boolean checkIfOkToPlaceFlag(L2Character activeChar, boolean isCheckOnly)
	{
		final Castle castle = CastleManager.getInstance().getCastle(activeChar);
		if (castle == null)
			return false;

		if (!(activeChar instanceof L2PcInstance))
			return false;

		SystemMessage sm;
		final L2PcInstance player = (L2PcInstance) activeChar;

		if (castle.getCastleId() <= 0 || (!castle.getSiege().getIsInProgress()) || (castle.getSiege().getAttackerClan(player.getClan()) == null))
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(247);
		else if (player.getClan() == null || !player.isClanLeader())
			sm = SystemMessage.getSystemMessage(SystemMessageId.ONLY_CLAN_LEADER_CAN_ISSUE_COMMANDS);
		else if (castle.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= SiegeManager.getInstance().getFlagMaxCount())
			sm = SystemMessage.getSystemMessage(SystemMessageId.NOT_ANOTHER_HEADQUARTERS);
		else if (!player.isInsideZone(L2Character.ZONE_HQ))
			sm = SystemMessage.getSystemMessage(SystemMessageId.NOT_SET_UP_BASE_HERE);
		else if (isNearAnotherHeadquarter(player))
			sm = SystemMessage.getSystemMessage(SystemMessageId.HEADQUARTERS_TOO_CLOSE);
		else
			return true;

		if (!isCheckOnly)
			player.sendPacket(sm);

		return false;
	}

	private static boolean isNearAnotherHeadquarter(L2PcInstance player)
	{
		// Search all flag instances in the knownlist of the player in a defined radius
		for (L2Object target : player.getKnownList().getKnownCharactersInRadius(400))
		{
			if (target instanceof L2SiegeFlagInstance)
				return true;
		}
		return false;
	}
}
