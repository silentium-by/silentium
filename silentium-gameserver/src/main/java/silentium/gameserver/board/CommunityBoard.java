/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.board;

import silentium.gameserver.board.Manager.BaseBBSManager;
import silentium.gameserver.board.Manager.ClanBBSManager;
import silentium.gameserver.board.Manager.FriendsBBSManager;
import silentium.gameserver.board.Manager.MailBBSManager;
import silentium.gameserver.board.Manager.PostBBSManager;
import silentium.gameserver.board.Manager.RegionBBSManager;
import silentium.gameserver.board.Manager.TopBBSManager;
import silentium.gameserver.board.Manager.TopicBBSManager;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.L2GameClient;
import silentium.gameserver.network.SystemMessageId;

public class CommunityBoard
{
	protected CommunityBoard()
	{
	}

	public static CommunityBoard getInstance()
	{
		return SingletonHolder._instance;
	}

	public void handleCommands(L2GameClient client, String command)
	{
		final L2PcInstance activeChar = client.getActiveChar();
		if (activeChar == null)
			return;

		if (!MainConfig.ENABLE_COMMUNITY_BOARD)
		{
			activeChar.sendPacket(SystemMessageId.CB_OFFLINE);
			return;
		}

		if (command.startsWith("_bbshome"))
			TopBBSManager.getInstance().parseCmd(command, activeChar);
		else if (command.startsWith("_bbsloc"))
			RegionBBSManager.getInstance().parseCmd(command, activeChar);
		else if (command.startsWith("_bbsclan"))
			ClanBBSManager.getInstance().parseCmd(command, activeChar);
		else if (command.startsWith("_bbsmemo"))
			TopicBBSManager.getInstance().parseCmd(command, activeChar);
		else if (command.startsWith("_maillist_0_1_0_"))
			MailBBSManager.getInstance().parseCmd(command, activeChar);
		else if (command.startsWith("_friend") || command.startsWith("_block"))
			FriendsBBSManager.getInstance().parseCmd(command, activeChar);
		else if (command.startsWith("_bbstopics"))
			TopicBBSManager.getInstance().parseCmd(command, activeChar);
		else if (command.startsWith("_bbsposts"))
			PostBBSManager.getInstance().parseCmd(command, activeChar);
		else
			BaseBBSManager.separateAndSend("<html><body><br><br><center>The command: " + command + " isn't implemented.</center></body></html>", activeChar);
	}

	public void handleWriteCommands(L2GameClient client, String url, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		final L2PcInstance activeChar = client.getActiveChar();
		if (activeChar == null)
			return;

		if (!MainConfig.ENABLE_COMMUNITY_BOARD)
		{
			activeChar.sendPacket(SystemMessageId.CB_OFFLINE);
			return;
		}

		if (url.equals("Topic"))
			TopicBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		else if (url.equals("Post"))
			PostBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		else if (url.equals("Region"))
			RegionBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		else if (url.equals("Notice"))
			ClanBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		else if (url.equals("Mail"))
			MailBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		else
			BaseBBSManager.separateAndSend("<html><body><br><br><center>The command: " + url + " isn't implemented.</center></body></html>", activeChar);
	}

	private static class SingletonHolder
	{
		protected static final CommunityBoard _instance = new CommunityBoard();
	}
}