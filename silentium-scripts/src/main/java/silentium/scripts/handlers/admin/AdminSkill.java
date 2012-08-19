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
import silentium.commons.utils.StringUtil;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.MagicSkillUse;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.PledgeSkillList;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.SkillTable;

import java.util.StringTokenizer;

/**
 * This class handles following admin commands: - show_skills - remove_skills - skill_list - skill_index - add_skill - remove_skill - get_skills
 * - reset_skills - give_all_skills - remove_all_skills - add_clan_skills
 */
public class AdminSkill implements IAdminCommandHandler {
	private static final Logger _log = LoggerFactory.getLogger(AdminSkill.class.getName());
	private static final String[] ADMIN_COMMANDS = { "admin_show_skills", "admin_remove_skills", "admin_skill_list", "admin_skill_index", "admin_add_skill", "admin_remove_skill", "admin_get_skills", "admin_reset_skills", "admin_give_all_skills", "admin_remove_all_skills", "admin_add_clan_skill", "admin_st" };

	private static L2Skill[] adminSkills;

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_show_skills".equals(command))
			showMainPage(activeChar);
		else if (command.startsWith("admin_remove_skills")) {
			try {
				final String val = command.substring(20);
				removeSkillsPage(activeChar, Integer.parseInt(val));
			} catch (StringIndexOutOfBoundsException e) {
			}
		} else if (command.startsWith("admin_skill_list")) {
			AdminHelpPage.showHelpPage(activeChar, "skills.htm");
		} else if (command.startsWith("admin_skill_index")) {
			try {
				final String val = command.substring(18);
				AdminHelpPage.showHelpPage(activeChar, "skills/" + val + ".htm");
			} catch (StringIndexOutOfBoundsException e) {
			}
		} else if (command.startsWith("admin_add_skill")) {
			try {
				final String val = command.substring(15);
				adminAddSkill(activeChar, val);
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //add_skill <skill_id> <level>");
			}
		} else if (command.startsWith("admin_remove_skill")) {
			try {
				final String id = command.substring(19);
				final int idval = Integer.parseInt(id);
				adminRemoveSkill(activeChar, idval);
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //remove_skill <skill_id>");
			}
		} else if ("admin_get_skills".equals(command)) {
			adminGetSkills(activeChar);
		} else if ("admin_reset_skills".equals(command))
			adminResetSkills(activeChar);
		else if ("admin_give_all_skills".equals(command))
			adminGiveAllSkills(activeChar);
		else if ("admin_remove_all_skills".equals(command)) {
			if (activeChar.getTarget() instanceof L2PcInstance) {
				final L2PcInstance player = (L2PcInstance) activeChar.getTarget();

				for (final L2Skill skill : player.getAllSkills())
					player.removeSkill(skill);

				activeChar.sendMessage("You removed all skills from " + player.getName() + '.');
				if (player != activeChar)
					player.sendMessage("Admin removed all skills from you.");

				player.sendSkillList();
			}
		} else if (command.startsWith("admin_add_clan_skill")) {
			try {
				final String[] val = command.split(" ");
				adminAddClanSkill(activeChar, Integer.parseInt(val[1]), Integer.parseInt(val[2]));
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //add_clan_skill <skill_id> <level>");
			}
		} else if (command.startsWith("admin_st")) {
			try {
				final StringTokenizer st = new StringTokenizer(command);
				st.nextToken();

				final int id = Integer.parseInt(st.nextToken());
				adminTestSkill(activeChar, id);
			} catch (Exception e) {
				activeChar.sendMessage("Used to test skills' visual effect, format : //st <ID>");
			}
		}

		return true;
	}

	private static void adminTestSkill(final L2PcInstance activeChar, final int id) {
		final L2Character player;
		final L2Object target = activeChar.getTarget();

		player = !(target instanceof L2Character) ? activeChar : (L2Character) target;

		player.broadcastPacket(new MagicSkillUse(activeChar, player, id, 1, 1, 1));
	}

	/**
	 * This function will give all the skills that the target can learn at his/her level
	 *
	 * @param activeChar The GM char.
	 */
	private static void adminGiveAllSkills(final L2PcInstance activeChar) {
		final L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else {
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		// Notify player and admin
		activeChar.sendMessage("You gave " + player.giveAvailableSkills() + " skills to " + player.getName() + '.');
		player.sendSkillList();
	}

	private static void removeSkillsPage(final L2PcInstance activeChar, int page) {
		final L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else {
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}

		final L2Skill[] skills = player.getAllSkills();

		final int maxSkillsPerPage = 10;
		int maxPages = skills.length / maxSkillsPerPage;
		if (skills.length > maxSkillsPerPage * maxPages)
			maxPages++;

		if (page > maxPages)
			page = maxPages;

		final int skillsStart = maxSkillsPerPage * page;
		int skillsEnd = skills.length;
		if (skillsEnd - skillsStart > maxSkillsPerPage)
			skillsEnd = skillsStart + maxSkillsPerPage;

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		final StringBuilder replyMSG = StringUtil.startAppend(500 + maxPages * 50 + (skillsEnd - skillsStart + 1) * 50, "<html><body>" + "<table width=270><tr>" + "<td width=45><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>" + "<td width=180><center>Delete Skills Menu</center></td>" + "<td width=45><button value=\"Back\" action=\"bypass -h admin_show_skills\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>" + "</tr></table>" + "<br><br>" + "<center>Editing <font color=\"LEVEL\">", player.getName(), "</font>" + ", ", player.getTemplate().className, " lvl ",
				String.valueOf(player.getLevel()), ".<br><center><table width=270><tr>");

		for (int x = 0; x < maxPages; x++) {
			final int pagenr = x + 1;
			StringUtil.append(replyMSG, "<td><a action=\"bypass -h admin_remove_skills ", String.valueOf(x), "\">Page ", String.valueOf(pagenr), "</a></td>");
		}

		replyMSG.append("</tr></table></center>" + "<br><table width=270>" + "<tr><td width=80>Name:</td><td width=60>Level:</td><td width=40>Id:</td></tr>");

		for (int i = skillsStart; i < skillsEnd; i++) {
			StringUtil.append(replyMSG, "<tr><td width=80><a action=\"bypass -h admin_remove_skill ", String.valueOf(skills[i].getId()), "\">", skills[i].getName(), "</a></td><td width=60>", String.valueOf(skills[i].getLevel()), "</td><td width=40>", String.valueOf(skills[i].getId()), "</td></tr>");
		}

		replyMSG.append("</table>" + "<br><center><table width=200>" + "<tr><td width=50 align=right>Id: </td>" + "<td><edit var=\"id_to_remove\" width=55></td>" + "<td width=100><button value=\"Remove skill\" action=\"bypass -h admin_remove_skill $id_to_remove\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>" + "<tr><td></td><td></td>" + "<td><button value=\"Back to stats\" action=\"bypass -h admin_current_player\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td>" + "</tr></table></center>" + "</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private static void showMainPage(final L2PcInstance activeChar) {
		final L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else {
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "charskills.htm");
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%class%", player.getTemplate().className);
		activeChar.sendPacket(adminReply);
	}

	private static void adminGetSkills(final L2PcInstance activeChar) {
		final L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else {
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		if (player == activeChar)
			player.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
		else {
			L2Skill[] skills = player.getAllSkills();
			adminSkills = activeChar.getAllSkills();

			for (final L2Skill skill : adminSkills)
				activeChar.removeSkill(skill);

			for (final L2Skill skill : skills)
				activeChar.addSkill(skill, true);

			activeChar.sendMessage("You ninjaed " + player.getName() + "'s skills list.");
			activeChar.sendSkillList();
			skills = null;
		}
	}

	private static void adminResetSkills(final L2PcInstance activeChar) {
		if (adminSkills == null)
			activeChar.sendMessage("Ninja first skills of someone to use that command.");
		else {
			L2Skill[] skills = activeChar.getAllSkills();

			for (final L2Skill skill : skills)
				activeChar.removeSkill(skill);

			for (final L2Skill skill : adminSkills)
				activeChar.addSkill(skill, true);

			activeChar.sendMessage("All your skills have been returned back.");
			activeChar.sendSkillList();
			adminSkills = null;
			skills = null;
		}
	}

	private static void adminAddSkill(final L2PcInstance activeChar, final String val) {
		final L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else {
			showMainPage(activeChar);
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		final StringTokenizer st = new StringTokenizer(val);
		if (st.countTokens() != 2)
			showMainPage(activeChar);
		else {
			L2Skill skill = null;
			try {
				final String id = st.nextToken();
				final String level = st.nextToken();
				final int idval = Integer.parseInt(id);
				final int levelval = Integer.parseInt(level);
				skill = SkillTable.getInstance().getInfo(idval, levelval);
			} catch (Exception e) {
			}

			if (skill != null) {
				final String name = skill.getName();

				player.addSkill(skill, true);
				player.sendMessage("Admin gave you the skill " + name + '.');
				if (player != activeChar)
					activeChar.sendMessage("You gave the skill " + name + " to " + player.getName() + '.');

				_log.warn("[GM]" + activeChar.getName() + " gave skill " + name + " to " + player.getName() + '.');

				player.sendSkillList();
			} else
				activeChar.sendMessage("Error: there is no such skill.");

			showMainPage(activeChar); // Back to start
		}
	}

	private static void adminRemoveSkill(final L2PcInstance activeChar, final int idval) {
		final L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else {
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		final L2Skill skill = SkillTable.getInstance().getInfo(idval, player.getSkillLevel(idval));
		if (skill != null) {
			final String skillname = skill.getName();

			player.removeSkill(skill);
			activeChar.sendMessage("You removed the skill " + skillname + " from " + player.getName() + '.');
			if (player != activeChar)
				player.sendMessage("Admin removed the skill " + skillname + " from your skills list.");

			_log.warn("[GM]" + activeChar.getName() + " removed skill " + skillname + " from " + player.getName() + '.');

			player.sendSkillList();
		} else
			activeChar.sendMessage("Error: there is no such skill.");

		removeSkillsPage(activeChar, 0); // Back to previous page
	}

	private static void adminAddClanSkill(final L2PcInstance activeChar, final int id, final int level) {
		final L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else {
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			showMainPage(activeChar);
			return;
		}

		if (!player.isClanLeader()) {
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addPcName(player));
			showMainPage(activeChar);
			return;
		}

		if (id < 370 || id > 391 || level < 1 || level > 3) {
			activeChar.sendMessage("Usage: //add_clan_skill <skill_id> <level>");
			showMainPage(activeChar);
			return;
		}

		final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
		if (skill == null) {
			activeChar.sendMessage("Error: there is no such skill.");
			return;
		}

		// The previous check on CL checks already if player is/isn't in a clan.
		final L2Clan clanTarget = player.getClan();

		clanTarget.addNewSkill(skill);
		clanTarget.broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED).addSkillName(id));
		clanTarget.broadcastToOnlineMembers(new PledgeSkillList(clanTarget));
		for (final L2PcInstance member : clanTarget.getOnlineMembers(0))
			member.sendSkillList();

		activeChar.sendMessage("You gave " + skill.getName() + " Clan Skill to " + clanTarget.getName() + " clan.");

		showMainPage(activeChar);
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}
