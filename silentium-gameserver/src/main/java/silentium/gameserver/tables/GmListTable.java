/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.tables;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.L2GameServerPacket;
import silentium.gameserver.network.serverpackets.PlaySound;
import silentium.gameserver.network.serverpackets.SystemMessage;

/**
 * This class stores references to all online game masters. (access level > 100)
 */
public class GmListTable
{
	private static Logger _log = LoggerFactory.getLogger(GmListTable.class.getName());

	private final FastMap<L2PcInstance, Boolean> _gmList;

	public static GmListTable getInstance()
	{
		return SingletonHolder._instance;
	}

	public FastList<L2PcInstance> getAllGms(boolean includeHidden)
	{
		FastList<L2PcInstance> tmpGmList = new FastList<>();

		for (FastMap.Entry<L2PcInstance, Boolean> n = _gmList.head(), end = _gmList.tail(); (n = n.getNext()) != end;)
			if (includeHidden || !n.getValue())
				tmpGmList.add(n.getKey());

		return tmpGmList;
	}

	public FastList<String> getAllGmNames(boolean includeHidden)
	{
		FastList<String> tmpGmList = new FastList<>();

		for (FastMap.Entry<L2PcInstance, Boolean> n = _gmList.head(), end = _gmList.tail(); (n = n.getNext()) != end;)
			if (!n.getValue())
				tmpGmList.add(n.getKey().getName());
			else if (includeHidden)
				tmpGmList.add(n.getKey().getName() + " (invis)");

		return tmpGmList;
	}

	protected GmListTable()
	{
		_gmList = new FastMap<L2PcInstance, Boolean>().shared();
	}

	/**
	 * Add a L2PcInstance player to the Set _gmList
	 *
	 * @param player
	 * @param hidden
	 */
	public void addGm(L2PcInstance player, boolean hidden)
	{
		_log.trace("added gm: " + player.getName());

		_gmList.put(player, hidden);
	}

	public void deleteGm(L2PcInstance player)
	{
		_log.trace("deleted gm: " + player.getName());

		_gmList.remove(player);
	}

	/**
	 * GM will be displayed on clients gmlist
	 *
	 * @param player
	 */
	public void showGm(L2PcInstance player)
	{
		FastMap.Entry<L2PcInstance, Boolean> gm = _gmList.getEntry(player);
		if (gm != null)
			gm.setValue(false);
	}

	/**
	 * GM will no longer be displayed on clients gmlist
	 *
	 * @param player
	 */
	public void hideGm(L2PcInstance player)
	{
		FastMap.Entry<L2PcInstance, Boolean> gm = _gmList.getEntry(player);
		if (gm != null)
			gm.setValue(true);
	}

	public boolean isGmOnline(boolean includeHidden)
	{
		for (FastMap.Entry<L2PcInstance, Boolean> n = _gmList.head(), end = _gmList.tail(); (n = n.getNext()) != end;)
		{
			if (includeHidden || !n.getValue())
				return true;
		}

		return false;
	}

	public void sendListToPlayer(L2PcInstance player)
	{
		if (isGmOnline(player.isGM()))
		{
			player.sendPacket(SystemMessageId.GM_LIST);

			for (String name : getAllGmNames(player.isGM()))
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.GM_S1).addString(name));
		}
		else
		{
			player.sendPacket(SystemMessageId.NO_GM_PROVIDING_SERVICE_NOW);
			player.sendPacket(new PlaySound("systemmsg_e.702"));
		}
	}

	public static void broadcastToGMs(L2GameServerPacket packet)
	{
		for (L2PcInstance gm : getInstance().getAllGms(true))
			gm.sendPacket(packet);
	}

	public static void broadcastMessageToGMs(String message)
	{
		for (L2PcInstance gm : getInstance().getAllGms(true))
			gm.sendMessage(message);
	}

	private static class SingletonHolder
	{
		protected static final GmListTable _instance = new GmListTable();
	}
}
