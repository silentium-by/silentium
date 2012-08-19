/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import silentium.gameserver.configs.ClansConfig;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.CastleManorManager;
import silentium.gameserver.instancemanager.CastleManorManager.CropProcure;
import silentium.gameserver.instancemanager.CastleManorManager.SeedProduction;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;

import java.util.StringTokenizer;

/**
 * Admin comand handler for Manor System This class handles following admin commands: - manor_info = shows info about current manor state -
 * manor_approve = approves settings for the next manor period - manor_setnext = changes manor settings to the next day's - manor_reset castle =
 * resets all manor data for specified castle (or all) - manor_setmaintenance = sets manor system under maintenance mode - manor_save = saves all
 * manor data into database - manor_disable = disables manor system
 *
 * @author l3x
 */
public class AdminManor implements IAdminCommandHandler {
	private static final String[] _adminCommands = { "admin_manor", "admin_manor_approve", "admin_manor_setnext", "admin_manor_reset", "admin_manor_setmaintenance", "admin_manor_save", "admin_manor_disable" };

	@Override
	public boolean useAdminCommand(String command, final L2PcInstance activeChar) {
		final StringTokenizer st = new StringTokenizer(command);
		command = st.nextToken();

		switch (command) {
			case "admin_manor":
				showMainPage(activeChar);
				break;
			case "admin_manor_setnext":
				CastleManorManager.getInstance().setNextPeriod();
				CastleManorManager.getInstance().setNewManorRefresh();
				CastleManorManager.getInstance().updateManorRefresh();
				activeChar.sendMessage("Manor System: set to next period");
				showMainPage(activeChar);
				break;
			case "admin_manor_approve":
				CastleManorManager.getInstance().approveNextPeriod();
				CastleManorManager.getInstance().setNewPeriodApprove();
				CastleManorManager.getInstance().updatePeriodApprove();
				activeChar.sendMessage("Manor System: next period approved");
				showMainPage(activeChar);
				break;
			case "admin_manor_reset":
				int castleId = 0;
				try {
					castleId = Integer.parseInt(st.nextToken());
				} catch (Exception e) {
				}

				if (castleId > 0) {
					final Castle castle = CastleManager.getInstance().getCastleById(castleId);
					castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
					castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
					castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
					castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);
					if (ClansConfig.ALT_MANOR_SAVE_ALL_ACTIONS) {
						castle.saveCropData();
						castle.saveSeedData();
					}
					activeChar.sendMessage("Manor data for " + castle.getName() + " was nulled");
				} else {
					for (final Castle castle : CastleManager.getInstance().getCastles()) {
						castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
						castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
						castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
						castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);
						if (ClansConfig.ALT_MANOR_SAVE_ALL_ACTIONS) {
							castle.saveCropData();
							castle.saveSeedData();
						}
					}
					activeChar.sendMessage("Manor data was nulled");
				}
				showMainPage(activeChar);
				break;
			case "admin_manor_setmaintenance": {
				final boolean mode = CastleManorManager.getInstance().isUnderMaintenance();
				CastleManorManager.getInstance().setUnderMaintenance(!mode);
				if (mode)
					activeChar.sendMessage("Manor System: not under maintenance");
				else
					activeChar.sendMessage("Manor System: under maintenance");
				showMainPage(activeChar);
				break;
			}
			case "admin_manor_save":
				CastleManorManager.getInstance().save();
				activeChar.sendMessage("Manor System: all data saved");
				showMainPage(activeChar);
				break;
			case "admin_manor_disable":
				final boolean mode = CastleManorManager.getInstance().isDisabled();
				CastleManorManager.getInstance().setDisabled(!mode);
				if (mode)
					activeChar.sendMessage("Manor System: enabled");
				else
					activeChar.sendMessage("Manor System: disabled");
				showMainPage(activeChar);
				break;
		}

		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return _adminCommands;
	}

	private static String formatTime(final long millis) {
		String s = "";
		int secs = (int) millis / 1000;
		int mins = secs / 60;
		secs -= mins * 60;
		final int hours = mins / 60;
		mins -= hours * 60;

		if (hours > 0)
			s += hours + ":";
		s += mins + ":";
		s += secs;
		return s;
	}

	private static void showMainPage(final L2PcInstance activeChar) {
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		final TextBuilder replyMSG = new TextBuilder("<html><body>");

		replyMSG.append("<center><font color=\"LEVEL\"> [Manor System] </font></center><br>");
		replyMSG.append("<table width=\"100%\"><tr><td>");
		replyMSG.append("Disabled: ").append(CastleManorManager.getInstance().isDisabled() ? "yes" : "no").append("</td><td>");
		replyMSG.append("Under Maintenance: ").append(CastleManorManager.getInstance().isUnderMaintenance() ? "yes" : "no").append("</td></tr><tr><td>");
		replyMSG.append("Time to refresh: ").append(formatTime(CastleManorManager.getInstance().getMillisToManorRefresh())).append("</td><td>");
		replyMSG.append("Time to approve: ").append(formatTime(CastleManorManager.getInstance().getMillisToNextPeriodApprove())).append("</td></tr>");
		replyMSG.append("</table>");

		replyMSG.append("<center><table><tr><td>");
		replyMSG.append("<button value=\"Set Next\" action=\"bypass -h admin_manor_setnext\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Approve Next\" action=\"bypass -h admin_manor_approve\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr><tr><td>");
		replyMSG.append("<button value=\"").append(CastleManorManager.getInstance().isUnderMaintenance() ? "Set normal" : "Set mainteance").append("\" action=\"bypass -h admin_manor_setmaintenance\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"").append(CastleManorManager.getInstance().isDisabled() ? "Enable" : "Disable").append("\" action=\"bypass -h admin_manor_disable\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr><tr><td>");
		replyMSG.append("<button value=\"Refresh\" action=\"bypass -h admin_manor\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Back\" action=\"bypass -h admin_admin\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("</table></center>");

		replyMSG.append("<br><center>Castle Information:<table width=\"100%\">");
		replyMSG.append("<tr><td></td><td>Current Period</td><td>Next Period</td></tr>");

		for (final Castle c : CastleManager.getInstance().getCastles()) {
			replyMSG.append("<tr><td>").append(c.getName()).append("</td>").append("<td>").append(c.getManorCost(CastleManorManager.PERIOD_CURRENT)).append("a</td>").append("<td>").append(c.getManorCost(CastleManorManager.PERIOD_NEXT)).append("a</td>").append("</tr>");
		}

		replyMSG.append("</table><br>");

		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
}