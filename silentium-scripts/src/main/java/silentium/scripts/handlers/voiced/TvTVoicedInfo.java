/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.voiced;

import silentium.gameserver.configs.TvTConfig;
import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.handler.IVoicedCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.TvTEvent;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Tvt info.
 * 
 * @author denser
 */
public class TvTVoicedInfo implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands = { "tvt" };

	/**
	 * Set this to false and recompile script if you don't want to use string cache.<br>
	 * This will decrease performance but will be more consistent against possible html editions during runtime Recompiling the script will get
	 * the new html would be enough too [DrHouse]
	 */
	private static final boolean USE_STATIC_HTML = true;
	private static final String HTML = HtmCache.getInstance().getHtm(StaticHtmPath.ModsHtmPath + "TvT/Status.htm");

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (command.equals("tvt"))
		{
			if (TvTEvent.isStarting() || TvTEvent.isStarted())
			{
				String htmContent = (USE_STATIC_HTML && !HTML.isEmpty()) ? HTML : HtmCache.getInstance().getHtm(StaticHtmPath.ModsHtmPath + "TvT/Status.htm");
				try
				{
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);

					npcHtmlMessage.setHtml(htmContent);
					// npcHtmlMessage.replace("%objectId%",
					// String.valueOf(getObjectId()));
					npcHtmlMessage.replace("%team1name%", TvTConfig.TVT_EVENT_TEAM_1_NAME);
					npcHtmlMessage.replace("%team1playercount%", String.valueOf(TvTEvent.getTeamsPlayerCounts()[0]));
					npcHtmlMessage.replace("%team1points%", String.valueOf(TvTEvent.getTeamsPoints()[0]));
					npcHtmlMessage.replace("%team2name%", TvTConfig.TVT_EVENT_TEAM_2_NAME);
					npcHtmlMessage.replace("%team2playercount%", String.valueOf(TvTEvent.getTeamsPlayerCounts()[1]));
					npcHtmlMessage.replace("%team2points%", String.valueOf(TvTEvent.getTeamsPoints()[1]));
					activeChar.sendPacket(npcHtmlMessage);
				}
				catch (Exception e)
				{
					_log.warn("wrong TvT voiced: " + e);
				}

			}
			else
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}