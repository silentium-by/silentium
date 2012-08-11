/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import silentium.gameserver.configs.NPCConfig;
import silentium.commons.utils.StringUtil;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.data.xml.CharTemplateData;
import silentium.gameserver.model.base.ClassId;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.MagicSkillUse;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.UserInfo;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;

/**
 * Custom class allowing you to choose your class. You can customize class rewards as needed items. Check npc.properties for more
 * informations. This NPC type got 2 differents ways to level : - the normal one, where you have to be at least of the good level
 * NOTE : you have to take 1st class then 2nd, if you try to take 2nd directly it won't work. - the "allow_entire_tree" version,
 * where you can take class depending of your current path. NOTE : you don't need to be of the good level. Added to the
 * "change class" function, this NPC can noblesse and give available skills (related to your current class and level).
 */
public final class L2ClassMasterInstance extends L2NpcInstance
{
	public L2ClassMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = StaticHtmPath.ClassmasterHtmPath + "disabled.htm";

		if (NPCConfig.ALLOW_CLASS_MASTERS)
			filename = StaticHtmPath.ClassmasterHtmPath + getNpcId() + ".htm";

		// Send a Server->Client NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (!NPCConfig.ALLOW_CLASS_MASTERS)
			return;

		if (command.startsWith("1stClass"))
			showHtmlMenu(player, getObjectId(), 1);
		else if (command.startsWith("2ndClass"))
			showHtmlMenu(player, getObjectId(), 2);
		else if (command.startsWith("3rdClass"))
			showHtmlMenu(player, getObjectId(), 3);
		else if (command.startsWith("change_class"))
		{
			int val = Integer.parseInt(command.substring(13));

			if (checkAndChangeClass(player, val))
			{
				// self-animation
				player.broadcastPacket(new MagicSkillUse(player, player, 5103, 1, 0, 0));

				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(StaticHtmPath.ClassmasterHtmPath + "ok.htm");
				html.replace("%name%", CharTemplateData.getInstance().getClassNameById(val));
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("become_noble"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

			if (!player.isNoble())
			{
				player.setNoble(true, true);
				player.sendPacket(new UserInfo(player));
				html.setFile(StaticHtmPath.ClassmasterHtmPath + "nobleok.htm");
				player.sendPacket(html);
			}
			else
			{
				html.setFile(StaticHtmPath.ClassmasterHtmPath + "alreadynoble.htm");
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("learn_skills"))
		{
			player.giveAvailableSkills();
			player.sendSkillList();
		}
		else
			super.onBypassFeedback(player, command);
	}

	private static final void showHtmlMenu(L2PcInstance player, int objectId, int level)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(objectId);

		if (!NPCConfig.CLASS_MASTER_SETTINGS.isAllowed(level))
		{
			int jobLevel = player.getClassId().level();
			final StringBuilder sb = new StringBuilder(100);
			sb.append("<html><body>");
			switch (jobLevel)
			{
				case 0:
					if (NPCConfig.CLASS_MASTER_SETTINGS.isAllowed(1))
						sb.append("Come back here when you reached level 20 to change your class.<br>");
					else if (NPCConfig.CLASS_MASTER_SETTINGS.isAllowed(2))
						sb.append("Come back after your first occupation change.<br>");
					else if (NPCConfig.CLASS_MASTER_SETTINGS.isAllowed(3))
						sb.append("Come back after your second occupation change.<br>");
					else
						sb.append("I can't change your occupation.<br>");
					break;
				case 1:
					if (NPCConfig.CLASS_MASTER_SETTINGS.isAllowed(2))
						sb.append("Come back here when you reached level 40 to change your class.<br>");
					else if (NPCConfig.CLASS_MASTER_SETTINGS.isAllowed(3))
						sb.append("Come back after your second occupation change.<br>");
					else
						sb.append("I can't change your occupation.<br>");
					break;
				case 2:
					if (NPCConfig.CLASS_MASTER_SETTINGS.isAllowed(3))
						sb.append("Come back here when you reached level 76 to change your class.<br>");
					else
						sb.append("I can't change your occupation.<br>");
					break;
				case 3:
					sb.append("There is no class change available for you anymore.<br>");
					break;
			}
			sb.append("</body></html>");
			html.setHtml(sb.toString());
		}
		else
		{
			final ClassId currentClassId = player.getClassId();
			if (currentClassId.level() >= level)
				html.setFile(StaticHtmPath.ClassmasterHtmPath + "nomore.htm");
			else
			{
				final int minLevel = getMinLevel(currentClassId.level());
				if (player.getLevel() >= minLevel || NPCConfig.ALLOW_ENTIRE_TREE)
				{
					final StringBuilder menu = new StringBuilder(100);
					for (ClassId cid : ClassId.values())
					{
						if (validateClassId(currentClassId, cid) && cid.level() == level)
							StringUtil.append(menu, "<a action=\"bypass -h npc_%objectId%_change_class ", String.valueOf(cid.getId()), "\">", CharTemplateData.getInstance().getClassNameById(cid.getId()), "</a><br>");
					}

					if (menu.length() > 0)
					{
						html.setFile(StaticHtmPath.ClassmasterHtmPath + "template.htm");
						html.replace("%name%", CharTemplateData.getInstance().getClassNameById(currentClassId.getId()));
						html.replace("%menu%", menu.toString());
					}
					else
					{
						html.setFile(StaticHtmPath.ClassmasterHtmPath + "comebacklater.htm");
						html.replace("%level%", String.valueOf(getMinLevel(level - 1)));
					}
				}
				else
				{
					if (minLevel < Integer.MAX_VALUE)
					{
						html.setFile(StaticHtmPath.ClassmasterHtmPath + "comebacklater.htm");
						html.replace("%level%", String.valueOf(minLevel));
					}
					else
						html.setFile(StaticHtmPath.ClassmasterHtmPath + "nomore.htm");
				}
			}
		}

		html.replace("%objectId%", String.valueOf(objectId));
		html.replace("%req_items%", getRequiredItems(level));
		player.sendPacket(html);
	}

	private static final boolean checkAndChangeClass(L2PcInstance player, int val)
	{
		final ClassId currentClassId = player.getClassId();
		if (getMinLevel(currentClassId.level()) > player.getLevel() && !NPCConfig.ALLOW_ENTIRE_TREE)
			return false;

		if (!validateClassId(currentClassId, val))
			return false;

		int newJobLevel = currentClassId.level() + 1;

		// Weight/Inventory check
		if (!NPCConfig.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).isEmpty())
		{
			if (player.getWeightPenalty() >= 3)
			{
				player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				return false;
			}
		}

		// check if player have all required items for class transfer
		for (int _itemId : NPCConfig.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).keys())
		{
			int _count = NPCConfig.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).get(_itemId);
			if (player.getInventory().getInventoryItemCount(_itemId, -1) < _count)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return false;
			}
		}

		// get all required items for class transfer
		for (int _itemId : NPCConfig.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).keys())
		{
			int _count = NPCConfig.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).get(_itemId);
			if (!player.destroyItemByItemId("ClassMaster", _itemId, _count, player, true))
				return false;
		}

		// reward player with items
		for (int _itemId : NPCConfig.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).keys())
		{
			int _count = NPCConfig.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).get(_itemId);
			player.addItem("ClassMaster", _itemId, _count, player, true);
		}

		player.setClassId(val);

		if (player.isSubClassActive())
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		else
			player.setBaseClass(player.getActiveClass());

		player.broadcastUserInfo();
		return true;
	}

	/**
	 * @param level
	 *            - current skillId level (0 - start, 1 - first, etc)
	 * @return minimum player level required for next class transfer
	 */
	private static final int getMinLevel(int level)
	{
		switch (level)
		{
			case 0:
				return 20;
			case 1:
				return 40;
			case 2:
				return 76;
			default:
				return Integer.MAX_VALUE;
		}
	}

	/**
	 * Returns true if class change is possible
	 *
	 * @param oldCID
	 *            current player ClassId
	 * @param val
	 *            new class index
	 * @return
	 */
	private static final boolean validateClassId(ClassId oldCID, int val)
	{
		try
		{
			return validateClassId(oldCID, ClassId.values()[val]);
		}
		catch (Exception e)
		{
			// possible ArrayOutOfBoundsException
		}
		return false;
	}

	/**
	 * Returns true if class change is possible
	 *
	 * @param oldCID
	 *            current player ClassId
	 * @param newCID
	 *            new ClassId
	 * @return true if class change is possible
	 */
	private static final boolean validateClassId(ClassId oldCID, ClassId newCID)
	{
		if (newCID == null || newCID.getRace() == null)
			return false;

		if (oldCID.equals(newCID.getParent()))
			return true;

		if (NPCConfig.ALLOW_ENTIRE_TREE && newCID.childOf(oldCID))
			return true;

		return false;
	}

	private static String getRequiredItems(int level)
	{
		if (NPCConfig.CLASS_MASTER_SETTINGS.getRequireItems(level) == null || NPCConfig.CLASS_MASTER_SETTINGS.getRequireItems(level).isEmpty())
			return "<tr><td>none</td></r>";

		StringBuilder sb = new StringBuilder();
		for (int _itemId : NPCConfig.CLASS_MASTER_SETTINGS.getRequireItems(level).keys())
		{
			int _count = NPCConfig.CLASS_MASTER_SETTINGS.getRequireItems(level).get(_itemId);
			sb.append("<tr><td><font color=\"LEVEL\">" + _count + "</font></td><td>" + ItemTable.getInstance().getTemplate(_itemId).getName() + "</td></tr>");
		}
		return sb.toString();
	}
}