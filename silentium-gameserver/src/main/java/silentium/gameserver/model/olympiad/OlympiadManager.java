/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.olympiad;

import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import silentium.gameserver.configs.EventsConfig;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.TvTEvent;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.templates.StatsSet;

import com.google.common.collect.Lists;

/**
 * @author DS
 */
public class OlympiadManager
{
	private final List<Integer> _nonClassBasedRegisters;
	private final Map<Integer, List<Integer>> _classBasedRegisters;

	protected OlympiadManager()
	{
		_nonClassBasedRegisters = Lists.newCopyOnWriteArrayList();
		_classBasedRegisters = new FastMap<Integer, List<Integer>>().shared();
	}

	public static final OlympiadManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public final List<Integer> getRegisteredNonClassBased()
	{
		return _nonClassBasedRegisters;
	}

	public final Map<Integer, List<Integer>> getRegisteredClassBased()
	{
		return _classBasedRegisters;
	}

	protected final List<List<Integer>> hasEnoughRegisteredClassed()
	{
		List<List<Integer>> result = null;
		for (Map.Entry<Integer, List<Integer>> classList : _classBasedRegisters.entrySet())
		{
			if (classList.getValue() != null && classList.getValue().size() >= EventsConfig.ALT_OLY_CLASSED)
			{
				if (result == null)
					result = new FastList<>();

				result.add(classList.getValue());
			}
		}
		return result;
	}

	protected final boolean hasEnoughRegisteredNonClassed()
	{
		return _nonClassBasedRegisters.size() >= EventsConfig.ALT_OLY_NONCLASSED;
	}

	protected final void clearRegistered()
	{
		_nonClassBasedRegisters.clear();
		_classBasedRegisters.clear();
	}

	public final boolean isRegistered(L2PcInstance noble)
	{
		return isRegistered(noble, false);
	}

	private final boolean isRegistered(L2PcInstance player, boolean showMessage)
	{
		final Integer objId = Integer.valueOf(player.getObjectId());

		if (_nonClassBasedRegisters.contains(objId))
		{
			if (showMessage)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));

			return true;
		}

		final List<Integer> classed = _classBasedRegisters.get(player.getBaseClass());
		if (classed != null && classed.contains(objId))
		{
			if (showMessage)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));

			return true;
		}

		return false;
	}

	public final boolean isRegisteredInComp(L2PcInstance noble)
	{
		return isRegistered(noble, false) || isInCompetition(noble, false);
	}

	private final static boolean isInCompetition(L2PcInstance player, boolean showMessage)
	{
		if (!Olympiad._inCompPeriod)
			return false;

		AbstractOlympiadGame game;
		for (int i = OlympiadGameManager.getInstance().getNumberOfStadiums(); --i >= 0;)
		{
			game = OlympiadGameManager.getInstance().getOlympiadTask(i).getGame();
			if (game == null)
				continue;

			if (game.containsParticipant(player.getObjectId()))
			{
				if (!showMessage)
					return true;

				player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT);
				return true;
			}
		}
		return false;
	}

	public final boolean registerNoble(L2PcInstance player, CompetitionType type)
	{
		if (!Olympiad._inCompPeriod)
		{
			player.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}

		if (Olympiad.getInstance().getMillisToCompEnd() < 600000)
		{
			player.sendPacket(SystemMessageId.GAME_REQUEST_CANNOT_BE_MADE);
			return false;
		}

		switch (type)
		{
			case CLASSED:
			{
				if (!checkNoble(player))
					return false;

				List<Integer> classed = _classBasedRegisters.get(player.getBaseClass());
				if (classed != null)
					classed.add(player.getObjectId());
				else
				{
					classed = Lists.newCopyOnWriteArrayList();
					classed.add(player.getObjectId());
					_classBasedRegisters.put(player.getBaseClass(), classed);
				}

				player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
				break;
			}

			case NON_CLASSED:
			{
				if (!checkNoble(player))
					return false;

				_nonClassBasedRegisters.add(player.getObjectId());
				player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES);
				break;
			}
		}
		return true;
	}

	public final boolean unRegisterNoble(L2PcInstance noble)
	{
		if (!Olympiad._inCompPeriod)
		{
			noble.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}

		if (!noble.isNoble())
		{
			noble.sendPacket(SystemMessageId.NOBLESSE_ONLY);
			return false;
		}

		if (!isRegistered(noble, false))
		{
			noble.sendPacket(SystemMessageId.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
			return false;
		}

		if (isInCompetition(noble, false))
			return false;

		Integer objId = Integer.valueOf(noble.getObjectId());
		if (_nonClassBasedRegisters.remove(objId))
		{
			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			return true;
		}

		final List<Integer> classed = _classBasedRegisters.get(noble.getBaseClass());
		if (classed != null && classed.remove(objId))
		{
			_classBasedRegisters.remove(noble.getBaseClass());
			_classBasedRegisters.put(noble.getBaseClass(), classed);

			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			return true;
		}

		return false;
	}

	public final void removeDisconnectedCompetitor(L2PcInstance player)
	{
		final OlympiadGameTask task = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
		if (task != null && task.isGameStarted())
			task.getGame().handleDisconnect(player);

		final Integer objId = Integer.valueOf(player.getObjectId());
		if (_nonClassBasedRegisters.remove(objId))
			return;

		final List<Integer> classed = _classBasedRegisters.get(player.getBaseClass());
		if (classed != null && classed.remove(objId))
			return;
	}

	/**
	 * @param player
	 *            - messages will be sent to this L2PcInstance
	 * @return true if all requirements are met
	 */
	private final boolean checkNoble(L2PcInstance player)
	{
		if (!player.isNoble())
		{
			player.sendPacket(SystemMessageId.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			return false;
		}

		if (player.isSubClassActive())
		{
			player.sendPacket(SystemMessageId.YOU_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_JOB_CHARACTER);
			return false;
		}

		if (player.isCursedWeaponEquipped())
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_JOIN_OLYMPIAD_POSSESSING_S1).addItemName(player.getCursedWeaponEquippedId()));
			return false;
		}

		if (!player.isInventoryUnder80(true))
		{
			player.sendPacket(SystemMessageId.SINCE_80_PERCENT_OR_MORE_OF_YOUR_INVENTORY_SLOTS_ARE_FULL_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
			return false;
		}

		if (TvTEvent.isPlayerParticipant(player.getObjectId()))
		{
			player.sendMessage("You can't join olympiad while participating on TvT Event.");
			return false;
		}

		if (isRegistered(player, true))
			return false;

		if (isInCompetition(player, true))
			return false;

		StatsSet statDat = Olympiad.getNobleStats(player.getObjectId());
		if (statDat == null)
		{
			statDat = new StatsSet();
			statDat.set(Olympiad.CLASS_ID, player.getBaseClass());
			statDat.set(Olympiad.CHAR_NAME, player.getName());
			statDat.set(Olympiad.POINTS, Olympiad.DEFAULT_POINTS);
			statDat.set(Olympiad.COMP_DONE, 0);
			statDat.set(Olympiad.COMP_WON, 0);
			statDat.set(Olympiad.COMP_LOST, 0);
			statDat.set(Olympiad.COMP_DRAWN, 0);
			statDat.set("to_save", true);

			Olympiad.addNobleStats(player.getObjectId(), statDat);
		}

		final int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
		if (points <= 0)
		{
			NpcHtmlMessage message = new NpcHtmlMessage(0);
			message.setFile(StaticHtmPath.OlympiadHtmPath + "noble_nopoints1.htm", player);
			message.replace("%objectId%", String.valueOf(player.getTargetId()));
			player.sendPacket(message);
			return false;
		}

		return true;
	}

	private static class SingletonHolder
	{
		protected static final OlympiadManager _instance = new OlympiadManager();
	}
}