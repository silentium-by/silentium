/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import java.util.StringTokenizer;

import silentium.commons.utils.StringUtil;
import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.data.xml.MapRegionData;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.instancemanager.ZoneManager;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.L2WorldRegion;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.zone.L2ZoneType;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminZone implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS = { "admin_zone_check", "admin_zone_visual", "admin_zone_visual_clear" };

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (activeChar == null)
			return false;

		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if (actualCommand.equalsIgnoreCase("admin_zone_check"))
			showHtml(activeChar);
		else if (actualCommand.equalsIgnoreCase("admin_zone_visual"))
		{
			String next = st.nextToken();
			if (next.equalsIgnoreCase("all"))
			{
				for (L2ZoneType zone : ZoneManager.getInstance().getZones(activeChar))
					zone.visualizeZone(activeChar.getZ());

				showHtml(activeChar);
			}
			else
			{
				int zoneId = Integer.parseInt(next);
				ZoneManager.getInstance().getZoneById(zoneId).visualizeZone(activeChar.getZ());
			}
		}
		else if (actualCommand.equalsIgnoreCase("admin_zone_visual_clear"))
		{
			ZoneManager.getInstance().clearDebugItems();
			showHtml(activeChar);
		}

		return true;
	}

	private static void showHtml(L2PcInstance activeChar)
	{
		int worldX = activeChar.getX();
		int worldY = activeChar.getY();
		int geoX = ((((worldX - (-327680)) >> 4) >> 11) + 10);
		int geoY = ((((worldY - (-262144)) >> 4) >> 11) + 10);

		final String htmContent = HtmCache.getInstance().getHtm(StaticHtmPath.AdminHtmPath + "zone.htm");
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(htmContent);

		adminReply.replace("%MAPREGION%", "[x:" + MapRegionData.getMapRegionX(activeChar.getX()) + " y:" + MapRegionData.getMapRegionX(activeChar.getY()) + "]");
		adminReply.replace("%GEOREGION%", "" + geoX + "_" + geoY);
		adminReply.replace("%CLOSESTTOWN%", MapRegionData.getInstance().getClosestTownName(activeChar));
		adminReply.replace("%CURRENTLOC%", "" + activeChar.getX() + ", " + activeChar.getY() + ", " + activeChar.getZ());

		adminReply.replace("%PVP%", (activeChar.isInsideZone(L2Character.ZONE_PVP) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%PEACE%", (activeChar.isInsideZone(L2Character.ZONE_PEACE) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%SIEGE%", (activeChar.isInsideZone(L2Character.ZONE_SIEGE) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%MOTHERTREE%", (activeChar.isInsideZone(L2Character.ZONE_MOTHERTREE) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%CLANHALL%", (activeChar.isInsideZone(L2Character.ZONE_CLANHALL) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%NOLANDING%", (activeChar.isInsideZone(L2Character.ZONE_NOLANDING) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%WATER%", (activeChar.isInsideZone(L2Character.ZONE_WATER) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%JAIL%", (activeChar.isInsideZone(L2Character.ZONE_JAIL) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%MONSTERTRACK%", (activeChar.isInsideZone(L2Character.ZONE_MONSTERTRACK) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%CASTLE%", (activeChar.isInsideZone(L2Character.ZONE_CASTLE) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%SWAMP%", (activeChar.isInsideZone(L2Character.ZONE_SWAMP) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%NOSUMMONFRIEND%", (activeChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%NOSTORE%", (activeChar.isInsideZone(L2Character.ZONE_NOSTORE) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%TOWN%", (activeChar.isInsideZone(L2Character.ZONE_TOWN) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%SCRIPT%", (activeChar.isInsideZone(L2Character.ZONE_SCRIPT) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%HQ%", (activeChar.isInsideZone(L2Character.ZONE_HQ) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%DANGERAREA%", (activeChar.isInsideZone(L2Character.ZONE_DANGERAREA) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%CASTONARTIFACT%", (activeChar.isInsideZone(L2Character.ZONE_CASTONARTIFACT) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%NORESTART%", (activeChar.isInsideZone(L2Character.ZONE_NORESTART) ? "<font color=\"LEVEL\">YES</font>" : "NO"));

		StringBuilder zones = new StringBuilder(100);
		L2WorldRegion region = L2World.getInstance().getRegion(activeChar.getX(), activeChar.getY());
		for (L2ZoneType zone : region.getZones())
		{
			if (zone.isCharacterInZone(activeChar))
			{
				StringUtil.append(zones, String.valueOf(zone.getId()));
				StringUtil.append(zones, " ");
			}
		}
		adminReply.replace("%ZLIST%", zones.toString());
		activeChar.sendPacket(adminReply);
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}