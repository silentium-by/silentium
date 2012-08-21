/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.board.Manager;

import java.util.List;
import java.util.StringTokenizer;

import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.tables.CharNameTable;

public class FriendsBBSManager extends BaseBBSManager
{
	protected FriendsBBSManager()
	{
	}

	public static FriendsBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}

	@Override
	public void parseCmd(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command, "_");
		String cmd = st.nextToken();

		if (cmd.equalsIgnoreCase("friendlist"))
			showFriendsList(activeChar, false);
		else if (cmd.startsWith("friendselect"))
		{
			Integer friendId = Integer.valueOf(cmd.split(";")[1]);
			if (!activeChar.getSelectedFriendList().contains(friendId))
				activeChar.selectFriend(friendId);

			showFriendsList(activeChar, false);
		}
		else if (cmd.startsWith("frienddeselect"))
		{
			activeChar.deselectFriend(Integer.valueOf(cmd.split(";")[1]));
			showFriendsList(activeChar, false);
		}
		else if (cmd.startsWith("friendmail"))
			showMailWrite(activeChar);
		else
			separateAndSend("<html><body><br><br><center>The command: " + command + " isn't implemented.</center></body></html>", activeChar);
	}

	@Override
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{

	}

	private static void showFriendsList(L2PcInstance activeChar, boolean delMsg)
	{
		String content = HtmCache.getInstance().getHtm(StaticHtmPath.BoardHtmPath + "friend/list.htm");
		if (content == null)
			return;

		// Retrieve activeChar's friendlist and selected
		final List<Integer> list = activeChar.getFriendList();
		final List<Integer> slist = activeChar.getSelectedFriendList();

		// Friendlist
		if (list.isEmpty())
			content = content.replaceAll("%friendslist%", "");
		else
		{
			final StringBuilder flString = new StringBuilder(list.size() * 100);

			for (int id : list)
			{
				String friendName = CharNameTable.getInstance().getNameById(id);
				if (friendName == null)
					continue;

				L2PcInstance friend = L2World.getInstance().getPlayer(friendName);
				flString.append("<a action=\"bypass _friendselect;").append(id).append("\">").append(friendName).append("</a>&nbsp;").append(friend == null ? "(off)" : "(on)").append("<br1>");
			}

			content = content.replaceAll("%friendslist%", flString.toString());
		}

		// Selected friendlist
		if (slist.isEmpty())
			content = content.replaceAll("%selectedFriendsList%", "");
		else
		{
			final StringBuilder sflString = new StringBuilder(slist.size() * 80);

			for (int id : slist)
			{
				String friendName = CharNameTable.getInstance().getNameById(id);
				if (friendName == null)
					continue;

				sflString.append("<a action=\"bypass _frienddeselect;").append(id).append("\">").append(friendName).append("</a>").append("<br1>");
			}

			content = content.replaceAll("%selectedFriendsList%", sflString.toString());
		}

		// Delete button.
		content = content.replaceAll("%deleteMSG%", (delMsg) ? "<br>\nAre you sure you want to delete all messages from your Friends List? <button value = \"OK\" action=\"bypass _bssfriend;delall\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\">" : "");

		separateAndSend(content, activeChar);
	}

	public static final void showMailWrite(L2PcInstance activeChar)
	{
		String content = HtmCache.getInstance().getHtm(StaticHtmPath.BoardHtmPath + "mail/mail-write.htm");
		if (content == null)
			return;

		content = content.replaceAll("%maillink%", "<a action=\"bypass _friendlist\">&\\$904;</a> > &\\$915;");
		content = content.replaceAll("%playerObjId%", String.valueOf(activeChar.getObjectId()));
		content = content.replaceAll("%postId%", "-1");

		StringBuilder toList = new StringBuilder();
		for (int id : activeChar.getSelectedFriendList())
		{
			String friendName = CharNameTable.getInstance().getNameById(id);
			if (friendName == null)
				continue;

			if (!toList.equals(""))
				toList.append(";");

			toList.append(friendName);
		}

		separateAndSend(content, activeChar);
	}

	@Override
	protected String getFolder()
	{
		return "friend/";
	}

	private static class SingletonHolder
	{
		protected static final FriendsBBSManager _instance = new FriendsBBSManager();
	}
}