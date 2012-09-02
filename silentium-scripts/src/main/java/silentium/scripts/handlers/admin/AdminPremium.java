/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.clientpackets.Say2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

public class AdminPremium implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_premium_menu", "admin_premium_add1", "admin_premium_add2", "admin_premium_add3" };
	private static final String UPDATE_PREMIUMSERVICE = "UPDATE character_premium SET premium_service=?,enddate=? WHERE account_name=?";
	private static final Logger _log = LoggerFactory.getLogger(AdminPremium.class.getName());

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_premium_menu".equals(command)) {
			AdminHelpPage.showHelpPage(activeChar, "premium_menu.htm");
		} else if (command.startsWith("admin_premium_add1")) {
			try {
				final String val = command.substring(19);
				addPremiumServices(1, val);
			} catch (StringIndexOutOfBoundsException e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Err");
			}
		} else if (command.startsWith("admin_premium_add2")) {
			try {
				final String val = command.substring(19);
				addPremiumServices(2, val);
			} catch (StringIndexOutOfBoundsException e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Err");
			}
		} else if (command.startsWith("admin_premium_add3")) {
			try {
				final String val = command.substring(19);
				addPremiumServices(3, val);
			} catch (StringIndexOutOfBoundsException e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Err");
			}
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private void addPremiumServices(final int Months, final String AccName) {
		Connection con = null;
		try {
			final Calendar finishtime = Calendar.getInstance();
			finishtime.setTimeInMillis(System.currentTimeMillis());
			finishtime.set(Calendar.SECOND, 0);
			finishtime.add(Calendar.MONTH, Months);

			con = DatabaseFactory.getConnection();
			final PreparedStatement statement = con.prepareStatement(UPDATE_PREMIUMSERVICE);
			statement.setInt(1, 1);
			statement.setLong(2, finishtime.getTimeInMillis());
			statement.setString(3, AccName);
			statement.execute();
			statement.close();
		} catch (SQLException e) {
			_log.info("PremiumService:  Could not increase data");
		} finally {
			try {
				if (con != null)
					con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
