/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import javolution.text.TextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.database.DatabaseFactory;
import silentium.commons.utils.StringUtil;
import silentium.gameserver.TradeController;
import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2DropCategory;
import silentium.gameserver.model.L2DropData;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.L2TradeList;
import silentium.gameserver.model.L2TradeList.L2TradeItem;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2MerchantInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.tables.NpcTable;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.templates.StatsSet;
import silentium.gameserver.templates.chars.L2NpcTemplate;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.templates.skills.L2SkillType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author terry
 */
public class AdminEditNpc implements IAdminCommandHandler {
	private static final Logger _log = LoggerFactory.getLogger(AdminEditNpc.class.getName());
	private static final int PAGE_LIMIT = 20;

	private static final String[] ADMIN_COMMANDS = { "admin_edit_npc", "admin_save_npc", "admin_show_droplist", "admin_edit_drop", "admin_add_drop", "admin_del_drop", "admin_showShop", "admin_showShopList", "admin_addShopItem", "admin_delShopItem", "admin_editShopItem", "admin_close_window", "admin_show_skilllist_npc", "admin_add_skill_npc", "admin_edit_skill_npc", "admin_del_skill_npc" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if (command.startsWith("admin_showShop ")) {
			final String[] args = command.split(" ");
			if (args.length > 1)
				showShop(activeChar, Integer.parseInt(command.split(" ")[1]));
		} else if (command.startsWith("admin_showShopList ")) {
			final String[] args = command.split(" ");
			if (args.length > 2)
				showShopList(activeChar, Integer.parseInt(command.split(" ")[1]), Integer.parseInt(command.split(" ")[2]));
		} else if (command.startsWith("admin_edit_npc ")) {
			try {
				final String[] commandSplit = command.split(" ");
				final int npcId = Integer.parseInt(commandSplit[1]);
				final L2NpcTemplate npc = NpcTable.getInstance().getTemplate(npcId);
				showNpcProperty(activeChar, npc);
			} catch (Exception e) {
				activeChar.sendMessage("Wrong usage: //edit_npc <npcId>");
			}
		} else if (command.startsWith("admin_show_droplist ")) {
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			try {
				final int npcId = Integer.parseInt(st.nextToken());
				int page = 1;
				if (st.hasMoreTokens())
					page = Integer.parseInt(st.nextToken());
				showNpcDropList(activeChar, npcId, page);
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //show_droplist <npc_id> [<page>]");
			}
		} else if (command.startsWith("admin_addShopItem ")) {
			final String[] args = command.split(" ");
			if (args.length > 1)
				addShopItem(activeChar, args);
		} else if (command.startsWith("admin_delShopItem ")) {
			final String[] args = command.split(" ");
			if (args.length > 2)
				delShopItem(activeChar, args);
		} else if (command.startsWith("admin_editShopItem ")) {
			final String[] args = command.split(" ");
			if (args.length > 2)
				editShopItem(activeChar, args);
		} else if (command.startsWith("admin_save_npc ")) {
			try {
				saveNpcProperty(activeChar, command);
			} catch (StringIndexOutOfBoundsException e) {
			}
		} else if (command.startsWith("admin_edit_drop ")) {
			int npcId = -1, itemId = 0, category = -1000;
			try {
				final StringTokenizer st = new StringTokenizer(command.substring(16).trim());
				if (st.countTokens() == 3) {
					try {
						npcId = Integer.parseInt(st.nextToken());
						itemId = Integer.parseInt(st.nextToken());
						category = Integer.parseInt(st.nextToken());
						showEditDropData(activeChar, npcId, itemId, category);
					} catch (Exception e) {
					}
				} else if (st.countTokens() == 6) {
					try {
						npcId = Integer.parseInt(st.nextToken());
						itemId = Integer.parseInt(st.nextToken());
						category = Integer.parseInt(st.nextToken());
						final int min = Integer.parseInt(st.nextToken());
						final int max = Integer.parseInt(st.nextToken());
						final int chance = Integer.parseInt(st.nextToken());

						updateDropData(activeChar, npcId, itemId, min, max, category, chance);
					} catch (Exception e) {
						_log.debug("admin_edit_drop parements error: " + command);
					}
				} else
					activeChar.sendMessage("Usage: //edit_drop <npc_id> <item_id> <category> [<min> <max> <chance>]");
			} catch (StringIndexOutOfBoundsException e) {
				activeChar.sendMessage("Usage: //edit_drop <npc_id> <item_id> <category> [<min> <max> <chance>]");
			}
		} else if (command.startsWith("admin_add_drop ")) {
			int npcId = -1;
			try {
				final StringTokenizer st = new StringTokenizer(command.substring(15).trim());
				if (st.countTokens() == 1) {
					try {
						final String[] input = command.substring(15).split(" ");
						if (input.length < 1)
							return true;
						npcId = Integer.parseInt(input[0]);
					} catch (Exception e) {
					}

					if (npcId > 0) {
						final L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
						showAddDropData(activeChar, npcData);
					}
				} else if (st.countTokens() == 6) {
					try {
						npcId = Integer.parseInt(st.nextToken());
						final int itemId = Integer.parseInt(st.nextToken());
						final int category = Integer.parseInt(st.nextToken());
						final int min = Integer.parseInt(st.nextToken());
						final int max = Integer.parseInt(st.nextToken());
						final int chance = Integer.parseInt(st.nextToken());

						addDropData(activeChar, npcId, itemId, min, max, category, chance);
					} catch (Exception e) {
						_log.debug("admin_add_drop parements error: " + command);
					}
				} else
					activeChar.sendMessage("Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
			} catch (StringIndexOutOfBoundsException e) {
				activeChar.sendMessage("Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
			}
		} else if (command.startsWith("admin_del_drop ")) {
			int npcId = -1, itemId = -1, category = -1000;
			try {
				final String[] input = command.substring(15).split(" ");
				if (input.length >= 3) {
					npcId = Integer.parseInt(input[0]);
					itemId = Integer.parseInt(input[1]);
					category = Integer.parseInt(input[2]);
				}
			} catch (Exception e) {
			}

			if (npcId > 0)
				deleteDropData(activeChar, npcId, itemId, category);
			else
				activeChar.sendMessage("Usage: //del_drop <npc_id> <item_id> <category>");
		} else if (command.startsWith("admin_show_skilllist_npc ")) {
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			try {
				final int npcId = Integer.parseInt(st.nextToken());
				int page = 0;
				if (st.hasMoreTokens())
					page = Integer.parseInt(st.nextToken());
				showNpcSkillList(activeChar, npcId, page);
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //show_skilllist_npc <npc_id> <page>");
			}
		} else if (command.startsWith("admin_edit_skill_npc ")) {
			try {
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int npcId = Integer.parseInt(st.nextToken());
				final int skillId = Integer.parseInt(st.nextToken());
				if (!st.hasMoreTokens())
					showNpcSkillEdit(activeChar, npcId, skillId);
				else {
					final int level = Integer.parseInt(st.nextToken());
					updateNpcSkillData(activeChar, npcId, skillId, level);
				}
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //edit_skill_npc <npc_id> <item_id> [<level>]");
			}
		} else if (command.startsWith("admin_add_skill_npc ")) {
			try {
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int npcId = Integer.parseInt(st.nextToken());
				if (!st.hasMoreTokens()) {
					showNpcSkillAdd(activeChar, npcId);
				} else {
					final int skillId = Integer.parseInt(st.nextToken());
					final int level = Integer.parseInt(st.nextToken());
					addNpcSkillData(activeChar, npcId, skillId, level);
				}
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //add_skill_npc <npc_id> [<skill_id> <level>]");
			}
		} else if (command.startsWith("admin_del_skill_npc ")) {
			try {
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int npcId = Integer.parseInt(st.nextToken());
				final int skillId = Integer.parseInt(st.nextToken());
				deleteNpcSkillData(activeChar, npcId, skillId);
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //del_skill_npc <npc_id> <skill_id>");
			}
		}

		return true;
	}

	private static void editShopItem(final L2PcInstance activeChar, final String... args) {
		final int tradeListID = Integer.parseInt(args[1]);
		final int itemID = Integer.parseInt(args[2]);
		final L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);

		final L2Item item = ItemTable.getInstance().getTemplate(itemID);
		if (tradeList.getPriceForItemId(itemID) < 0)
			return;

		if (args.length > 3) {
			final int price = Integer.parseInt(args[3]);
			final int order = findOrderTradeList(itemID, tradeList.getPriceForItemId(itemID), tradeListID);

			tradeList.replaceItem(itemID, Integer.parseInt(args[3]));
			updateTradeList(itemID, price, tradeListID, order);

			activeChar.sendMessage("Updated price for " + item.getName() + " in Trade List " + tradeListID);
			showShopList(activeChar, tradeListID, 1);
			return;
		}

		final String replyMSG = StringUtil.concat("<html><title>Merchant Shop Item Edit</title><body><center><font color=\"LEVEL\">", NpcTable.getInstance().getTemplate(Integer.parseInt(tradeList.getNpcId())).getName(), " (", tradeList.getNpcId(), ") -> ", Integer.toString(tradeListID), "</font></center><table width=\"100%\"><tr><td>Item</td><td>", item.getName(), " (", Integer.toString(item.getItemId()), ")", "</td></tr><tr><td>Price (", String.valueOf(tradeList.getPriceForItemId(itemID)), ")</td><td><edit var=\"price\" width=80></td></tr></table><center><br><button value=\"Save\" action=\"bypass -h admin_editShopItem ", String.valueOf(tradeListID), " ",
				String.valueOf(itemID), " $price\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"Back to Shop List\" action=\"bypass -h admin_showShopList ", String.valueOf(tradeListID), " 1\"  width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG);
		activeChar.sendPacket(adminReply);
	}

	private static void delShopItem(final L2PcInstance activeChar, final String... args) {
		final int tradeListID = Integer.parseInt(args[1]);
		final int itemID = Integer.parseInt(args[2]);
		final L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);

		if (tradeList.getPriceForItemId(itemID) < 0)
			return;

		if (args.length > 3) {
			final int order = findOrderTradeList(itemID, tradeList.getPriceForItemId(itemID), tradeListID);

			tradeList.removeItem(itemID);
			deleteTradeList(tradeListID, order);

			activeChar.sendMessage("Deleted " + ItemTable.getInstance().getTemplate(itemID).getName() + " from Trade List " + tradeListID);
			showShopList(activeChar, tradeListID, 1);
			return;
		}

		final String replyMSG = StringUtil.concat("<html><title>Merchant Shop Item Delete</title><body><br>Delete entry in trade list ", String.valueOf(tradeListID), "<table width=\"100%\"><tr><td>Item</td><td>", ItemTable.getInstance().getTemplate(itemID).getName(), " (", Integer.toString(itemID), ")</td></tr><tr><td>Price</td><td>", String.valueOf(tradeList.getPriceForItemId(itemID)), "</td></tr></table><center><br><button value=\"Delete\" action=\"bypass -h admin_delShopItem ", String.valueOf(tradeListID), " ", String.valueOf(itemID),
				" 1\"  width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"Back to Shop List\" action=\"bypass -h admin_showShopList ", String.valueOf(tradeListID), " 1\"  width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG);
		activeChar.sendPacket(adminReply);
	}

	private static void addShopItem(final L2PcInstance activeChar, final String... args) {
		final int tradeListID = Integer.parseInt(args[1]);

		final L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
		if (tradeList == null) {
			activeChar.sendMessage("TradeList not found!");
			return;
		}

		if (args.length > 3) {
			final int order = tradeList.getItems().size() + 1; // last item order + 1
			final int itemID = Integer.parseInt(args[2]);
			final int price = Integer.parseInt(args[3]);

			final L2TradeItem newItem = new L2TradeItem(tradeListID, itemID);
			newItem.setPrice(price);
			newItem.setMaxCount(-1);
			tradeList.addItem(newItem);
			final boolean stored = storeTradeList(itemID, price, tradeListID, order);

			if (stored)
				activeChar.sendMessage("Added " + ItemTable.getInstance().getTemplate(itemID).getName() + " to Trade List " + tradeList.getListId());
			else
				activeChar.sendMessage("Could not add " + ItemTable.getInstance().getTemplate(itemID).getName() + " to Trade List " + tradeList.getListId() + '!');

			showShopList(activeChar, tradeListID, 1);
			return;
		}

		final String replyMSG = StringUtil.concat("<html><title>Merchant Shop Item Add</title><body><br>Add a new entry in merchantList.<table width=\"100%\"><tr><td>ItemID</td><td><edit var=\"itemID\" width=80></td></tr><tr><td>Price</td><td><edit var=\"price\" width=80></td></tr></table><center><br><button value=\"Add\" action=\"bypass -h admin_addShopItem ", String.valueOf(tradeListID), " $itemID $price\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"Back to Shop List\" action=\"bypass -h admin_showShopList ", String.valueOf(tradeListID),
				" 1\"  width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG);
		activeChar.sendPacket(adminReply);
	}

	private static void showShopList(final L2PcInstance activeChar, final int tradeListID, final int page) {
		final L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
		if (page > tradeList.getItems().size() / PAGE_LIMIT + 1 || page < 1)
			return;

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(itemListHtml(tradeList, page));
		activeChar.sendPacket(adminReply);
	}

	private static String itemListHtml(final L2TradeList tradeList, final int page) {
		final StringBuilder replyMSG = new StringBuilder();

		int max = tradeList.getItems().size() / PAGE_LIMIT;
		if (tradeList.getItems().size() > PAGE_LIMIT * max)
			max++;

		StringUtil.append(replyMSG, "<html><title>Merchant Shop List Page: ", String.valueOf(page), " of ", Integer.toString(max), "</title><body><br><center><font color=\"LEVEL\">", NpcTable.getInstance().getTemplate(Integer.parseInt(tradeList.getNpcId())).getName(), " (", tradeList.getNpcId(), ") Shop ID: ", Integer.toString(tradeList.getListId()), "</font></center><table width=300 bgcolor=666666><tr>");

		for (int x = 0; x < max; x++) {
			final int pagenr = x + 1;
			if (page == pagenr) {
				replyMSG.append("<td>Page ");
				replyMSG.append(pagenr);
				replyMSG.append("</td>");
			} else {
				replyMSG.append("<td><a action=\"bypass -h admin_showShopList ");
				replyMSG.append(tradeList.getListId());
				replyMSG.append(' ');
				replyMSG.append(x + 1);
				replyMSG.append("\"> Page ");
				replyMSG.append(pagenr);
				replyMSG.append(" </a></td>");
			}
		}

		replyMSG.append("</tr></table><table width=\"100%\"><tr><td width=150>Item</td><td width=60>Price</td><td width=40>Delete</td></tr>");

		final int start = (page - 1) * PAGE_LIMIT;
		final int end = Math.min((page - 1) * PAGE_LIMIT + PAGE_LIMIT, tradeList.getItems().size());
		// System.out.println("page: " + page + "; tradeList.getItems().size(): " + tradeList.getItems().size() + "; start: " +
		// start + "; end: " + end + "; max: " + max);
		for (final L2TradeItem item : tradeList.getItems(start, end)) {
			StringUtil.append(replyMSG, "<tr><td><a action=\"bypass -h admin_editShopItem ", String.valueOf(tradeList.getListId()), " ", String.valueOf(item.getItemId()), "\">", ItemTable.getInstance().getTemplate(item.getItemId()).getName(), "</a></td><td>", String.valueOf(item.getPrice()), "</td><td><a action=\"bypass -h admin_delShopItem ", String.valueOf(tradeList.getListId()), " ", String.valueOf(item.getItemId()), "\">Delete</a></td></tr>");
		}
		StringUtil.append(replyMSG, "<tr><td><br><br></td><td> </td><td> </td></tr><tr>");

		StringUtil.append(replyMSG, "</tr></table><center><br><button value=\"Add Shop Item\" action=\"bypass -h admin_addShopItem ", String.valueOf(tradeList.getListId()), "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");

		return replyMSG.toString();
	}

	private static void showShop(final L2PcInstance activeChar, final int merchantID) {
		final List<L2TradeList> tradeLists = TradeController.getInstance().getBuyListByNpcId(merchantID);
		if (tradeLists == null) {
			activeChar.sendMessage("Unknown npc template Id: " + merchantID);
			return;
		}

		final StringBuilder replyMSG = new StringBuilder();
		StringUtil.append(replyMSG, "<html><title>Merchant Shop Lists</title><body>");

		if (activeChar.getTarget() instanceof L2MerchantInstance) {
			final L2Npc merchant = (L2Npc) activeChar.getTarget();
			final int taxRate = merchant.getCastle().getTaxPercent();

			StringUtil.append(replyMSG, "<br>NPC: ", merchant.getName(), " (", Integer.toString(merchantID), ") <br>Tax Rate: ", Integer.toString(taxRate), "%");
		}

		StringUtil.append(replyMSG, "<table width=\"100%\">");

		for (final L2TradeList tradeList : tradeLists) {
			if (tradeList != null) {
				StringUtil.append(replyMSG, "<tr><td><a action=\"bypass -h admin_showShopList ", String.valueOf(tradeList.getListId()), " 1\">Merchant List ID ", String.valueOf(tradeList.getListId()), "</a></td></tr>");
			}
		}

		StringUtil.append(replyMSG, "</table><center><br><button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private static boolean storeTradeList(final int itemID, final int price, final int tradeListID, final int order) {
		try (Connection con = DatabaseFactory.getConnection()) {
			final PreparedStatement stmt = con.prepareStatement("INSERT INTO merchant_buylists (`item_id`,`price`,`shop_id`,`order`) VALUES (?,?,?,?)");
			stmt.setInt(1, itemID);
			stmt.setInt(2, price);
			stmt.setInt(3, tradeListID);
			stmt.setInt(4, order);
			stmt.execute();
			stmt.close();
		} catch (Exception e) {
			_log.warn("Could not store trade list (" + itemID + ", " + price + ", " + tradeListID + ", " + order + "): " + e);
			return false;
		}
		return true;
	}

	private static void updateTradeList(final int itemID, final int price, final int tradeListID, final int order) {
		try (Connection con = DatabaseFactory.getConnection()) {
			final PreparedStatement stmt = con.prepareStatement("UPDATE `merchant_buylists` SET `price` = ? WHERE `shop_id` = ? AND `order` = ?");
			stmt.setInt(1, price);
			stmt.setInt(2, tradeListID);
			stmt.setInt(3, order);
			stmt.close();
		} catch (Exception e) {
			_log.warn("Could not update trade list (" + itemID + ", " + price + ", " + tradeListID + ", " + order + "): " + e);
		}
	}

	private static void deleteTradeList(final int tradeListID, final int order) {
		try (Connection con = DatabaseFactory.getConnection()) {
			final PreparedStatement stmt = con.prepareStatement("DELETE FROM `merchant_buylists` WHERE `shop_id` = ? AND `order` = ?");
			stmt.setInt(1, tradeListID);
			stmt.setInt(2, order);
			stmt.close();
		} catch (Exception e) {
			_log.warn("Could not delete trade list (" + tradeListID + ", " + order + "): " + e);
		}
	}

	private static int findOrderTradeList(final int itemID, final int price, final int tradeListID) {
		int order = -1;
		try (Connection con = DatabaseFactory.getConnection()) {
			final PreparedStatement stmt = con.prepareStatement("SELECT `order` FROM `merchant_buylists` WHERE `shop_id` = ? AND `item_id` = ? AND `price` = ?");
			stmt.setInt(1, tradeListID);
			stmt.setInt(2, itemID);
			stmt.setInt(3, price);
			final ResultSet rs = stmt.executeQuery();

			if (rs.first())
				order = rs.getInt("order");

			stmt.close();
			rs.close();
		} catch (Exception e) {
			_log.warn("Could not get order for (" + itemID + ", " + price + ", " + tradeListID + "): " + e);
		}
		return order;
	}

	private static void showNpcProperty(final L2PcInstance activeChar, final L2NpcTemplate npc) {
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		final String content = HtmCache.getInstance().getHtm(StaticHtmPath.AdminHtmPath + "editnpc.htm");

		if (content != null) {
			adminReply.setHtml(content);
			adminReply.replace("%npcId%", String.valueOf(npc.getNpcId()));
			adminReply.replace("%templateId%", String.valueOf(npc.getIdTemplate()));
			adminReply.replace("%name%", npc.getName());
			adminReply.replace("%serverSideName%", npc.isServerSideName() ? "1" : "0");
			adminReply.replace("%title%", npc.getTitle());
			adminReply.replace("%serverSideTitle%", npc.isServerSideTitle() ? "1" : "0");
			adminReply.replace("%collisionRadius%", String.valueOf(npc.getCollisionRadius()));
			adminReply.replace("%collisionHeight%", String.valueOf(npc.getCollisionHeight()));
			adminReply.replace("%level%", String.valueOf(npc.getLevel()));
			adminReply.replace("%sex%", String.valueOf(npc.getSex()));
			adminReply.replace("%type%", String.valueOf(npc.getType()));
			adminReply.replace("%attackRange%", String.valueOf(npc.getBaseAtkRange()));
			adminReply.replace("%hp%", String.valueOf(npc.getBaseHpMax()));
			adminReply.replace("%mp%", String.valueOf(npc.getBaseMpMax()));
			adminReply.replace("%hpRegen%", String.valueOf(npc.getBaseHpReg()));
			adminReply.replace("%mpRegen%", String.valueOf(npc.getBaseMpReg()));
			adminReply.replace("%str%", String.valueOf(npc.getBaseSTR()));
			adminReply.replace("%con%", String.valueOf(npc.getBaseCON()));
			adminReply.replace("%dex%", String.valueOf(npc.getBaseDEX()));
			adminReply.replace("%int%", String.valueOf(npc.getBaseINT()));
			adminReply.replace("%wit%", String.valueOf(npc.getBaseWIT()));
			adminReply.replace("%men%", String.valueOf(npc.getBaseMEN()));
			adminReply.replace("%exp%", String.valueOf(npc.getRewardExp()));
			adminReply.replace("%sp%", String.valueOf(npc.getRewardSp()));
			adminReply.replace("%pAtk%", String.valueOf(npc.getBasePAtk()));
			adminReply.replace("%pDef%", String.valueOf(npc.getBasePDef()));
			adminReply.replace("%mAtk%", String.valueOf(npc.getBaseMAtk()));
			adminReply.replace("%mDef%", String.valueOf(npc.getBaseMDef()));
			adminReply.replace("%pAtkSpd%", String.valueOf(npc.getBasePAtkSpd()));
			adminReply.replace("%aggro%", String.valueOf(npc.getAIDataStatic().getAggroRange()));
			adminReply.replace("%mAtkSpd%", String.valueOf(npc.getBaseMAtkSpd()));
			adminReply.replace("%rHand%", String.valueOf(npc.getRightHand()));
			adminReply.replace("%lHand%", String.valueOf(npc.getLeftHand()));
			adminReply.replace("%enchant%", String.valueOf(npc.getEnchantEffect()));
			adminReply.replace("%walkSpd%", String.valueOf(npc.getBaseWalkSpd()));
			adminReply.replace("%runSpd%", String.valueOf(npc.getBaseRunSpd()));
			adminReply.replace("%factionId%", npc.getAIDataStatic().getClan() == null ? "" : npc.getAIDataStatic().getClan());
			adminReply.replace("%factionRange%", String.valueOf(npc.getAIDataStatic().getClanRange()));
		} else
			adminReply.setHtml("<html><head><body>File not found: " + StaticHtmPath.AdminHtmPath + "editnpc.htm</body></html>");
		activeChar.sendPacket(adminReply);
	}

	private static void saveNpcProperty(final L2PcInstance activeChar, final String command) {
		final String[] commandSplit = command.split(" ");

		if (commandSplit.length < 4)
			return;

		final StatsSet newNpcData = new StatsSet();

		try {
			newNpcData.set("npcId", commandSplit[1]);

			final String statToSet = commandSplit[2];
			String value = commandSplit[3];

			if (commandSplit.length > 4) {
				for (int i = 0; i < commandSplit.length - 3; i++)
					value += ' ' + commandSplit[i + 4];
			}

			switch (statToSet) {
				case "templateId":
					newNpcData.set("idTemplate", Integer.parseInt(value));
					break;
				case "name":
					newNpcData.set("name", value);
					break;
				case "serverSideName":
					newNpcData.set("serverSideName", Integer.parseInt(value));
					break;
				case "title":
					newNpcData.set("title", value);
					break;
				case "serverSideTitle":
					newNpcData.set("serverSideTitle", Integer.parseInt(value) == 1 ? 1 : 0);
					break;
				case "collisionRadius":
					newNpcData.set("collision_radius", Integer.parseInt(value));
					break;
				case "collisionHeight":
					newNpcData.set("collision_height", Integer.parseInt(value));
					break;
				case "level":
					newNpcData.set("level", Integer.parseInt(value));
					break;
				case "sex":
					final int intValue = Integer.parseInt(value);
					newNpcData.set("sex", intValue == 0 ? "male" : intValue == 1 ? "female" : "etc");
					break;
				case "type":
					Class.forName("silentium.gameserver.model.actor.instance." + value + "Instance");
					newNpcData.set("type", value);
					break;
				case "attackRange":
					newNpcData.set("attackrange", Integer.parseInt(value));
					break;
				case "hp":
					newNpcData.set("hp", Integer.parseInt(value));
					break;
				case "mp":
					newNpcData.set("mp", Integer.parseInt(value));
					break;
				case "hpRegen":
					newNpcData.set("hpreg", Integer.parseInt(value));
					break;
				case "mpRegen":
					newNpcData.set("mpreg", Integer.parseInt(value));
					break;
				case "str":
					newNpcData.set("str", Integer.parseInt(value));
					break;
				case "con":
					newNpcData.set("con", Integer.parseInt(value));
					break;
				case "dex":
					newNpcData.set("dex", Integer.parseInt(value));
					break;
				case "int":
					newNpcData.set("int", Integer.parseInt(value));
					break;
				case "wit":
					newNpcData.set("wit", Integer.parseInt(value));
					break;
				case "men":
					newNpcData.set("men", Integer.parseInt(value));
					break;
				case "exp":
					newNpcData.set("exp", Integer.parseInt(value));
					break;
				case "sp":
					newNpcData.set("sp", Integer.parseInt(value));
					break;
				case "pAtk":
					newNpcData.set("patk", Integer.parseInt(value));
					break;
				case "pDef":
					newNpcData.set("pdef", Integer.parseInt(value));
					break;
				case "mAtk":
					newNpcData.set("matk", Integer.parseInt(value));
					break;
				case "mDef":
					newNpcData.set("mdef", Integer.parseInt(value));
					break;
				case "pAtkSpd":
					newNpcData.set("atkspd", Integer.parseInt(value));
					break;
				case "aggro":
					newNpcData.set("aggro", Integer.parseInt(value));
					break;
				case "mAtkSpd":
					newNpcData.set("matkspd", Integer.parseInt(value));
					break;
				case "rHand":
					newNpcData.set("rhand", Integer.parseInt(value));
					break;
				case "lHand":
					newNpcData.set("lhand", Integer.parseInt(value));
					break;
				case "enchant":
					newNpcData.set("enchant", Integer.parseInt(value));
					break;
				case "runSpd":
					newNpcData.set("runspd", Integer.parseInt(value));
					break;
				case "factionId":
					newNpcData.set("faction_id", value);
					break;
				case "factionRange":
					newNpcData.set("faction_range", Integer.parseInt(value));
					break;
			}
		} catch (Exception e) {
			_log.warn("Error saving new npc value: " + e);
		}

		NpcTable.getInstance().saveNpc(newNpcData);

		final int npcId = newNpcData.getInteger("npcId");

		NpcTable.getInstance().reloadNpc(npcId);
		showNpcProperty(activeChar, NpcTable.getInstance().getTemplate(npcId));
	}

	private static void showNpcDropList(final L2PcInstance activeChar, final int npcId, final int page) {
		final L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if (npcData == null) {
			activeChar.sendMessage("Unknown npc template id " + npcId);
			return;
		}

		final StringBuilder replyMSG = new StringBuilder(2000);
		replyMSG.append("<html><title>Show droplist page ");
		replyMSG.append(page);
		replyMSG.append("</title><body><br1><center><font color=\"LEVEL\">");
		replyMSG.append(npcData.getName());
		replyMSG.append(" (");
		replyMSG.append(npcId);
		replyMSG.append(")</font></center><br>Drop type legend: <font color=\"3BB9FF\">Drop</font> | <font color=\"00ff00\">Sweep</font> | <font color=\"C12869\">Quest</font><br1><table width=\"100%\" border=0><tr><td width=35>cat.</td><td width=240>item</td><td width=25>del</td></tr>");

		int myPage = 1;
		int i = 0;
		int shown = 0;
		boolean hasMore = false;
		if (npcData.getDropData() != null) {
			for (final L2DropCategory cat : npcData.getDropData()) {

				if (shown == PAGE_LIMIT) {
					hasMore = true;
					break;
				}
				for (final L2DropData drop : cat.getAllDrops()) {
					final String color = drop.isQuestDrop() ? "C12869" : cat.isSweep() ? "00ff00" : "3BB9FF";

					if (myPage != page) {
						i++;
						if (i == PAGE_LIMIT) {
							myPage++;
							i = 0;
						}
						continue;
					}
					if (shown == PAGE_LIMIT) {
						hasMore = true;
						break;
					}

					replyMSG.append("<tr><td><font color=\"");
					replyMSG.append(color);
					replyMSG.append("\">");
					replyMSG.append(cat.getCategoryType());
					replyMSG.append("</td><td><a action=\"bypass -h admin_edit_drop ");
					replyMSG.append(npcId);
					replyMSG.append(' ');
					replyMSG.append(drop.getItemId());
					replyMSG.append(' ');
					replyMSG.append(cat.getCategoryType());
					replyMSG.append("\">");
					replyMSG.append(ItemTable.getInstance().getTemplate(drop.getItemId()).getName());
					replyMSG.append(" (");
					replyMSG.append(drop.getItemId());
					replyMSG.append(")</a></td><td><a action=\"bypass -h admin_del_drop ");
					replyMSG.append(npcId);
					replyMSG.append(' ');
					replyMSG.append(drop.getItemId());
					replyMSG.append(' ');
					replyMSG.append(cat.getCategoryType());
					replyMSG.append("\">del</a></font></td></tr>");
					shown++;
				}
			}
		}

		replyMSG.append("</table><table width=300 bgcolor=666666 border=0><tr>");

		if (page > 1) {
			replyMSG.append("<td width=120><a action=\"bypass -h admin_show_droplist ");
			replyMSG.append(npcId);
			replyMSG.append(' ');
			replyMSG.append(page - 1);
			replyMSG.append("\">Prev Page</a></td>");
			if (!hasMore) {
				replyMSG.append("<td width=100>Page ");
				replyMSG.append(page);
				replyMSG.append("</td><td width=70></td></tr>");
			}
		}
		if (hasMore) {
			if (page <= 1)
				replyMSG.append("<td width=120></td>");
			replyMSG.append("<td width=100>Page ");
			replyMSG.append(page);
			replyMSG.append("</td><td width=70><a action=\"bypass -h admin_show_droplist ");
			replyMSG.append(npcId);
			replyMSG.append(' ');
			replyMSG.append(page + 1);
			replyMSG.append("\">Next Page</a></td></tr>");
		}

		replyMSG.append("</table><center><br><button value=\"Add Drop Data\" action=\"bypass -h admin_add_drop ");
		replyMSG.append(npcId);
		replyMSG.append("\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private static void showEditDropData(final L2PcInstance activeChar, final int npcId, final int itemId, final int category) {
		final L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if (npcData == null) {
			activeChar.sendMessage("Unknown npc template id " + npcId);
			return;
		}

		final L2Item itemData = ItemTable.getInstance().getTemplate(itemId);
		if (itemData == null) {
			activeChar.sendMessage("Unknown item template id " + itemId);
			return;
		}

		final StringBuilder replyMSG = new StringBuilder();
		replyMSG.append("<html><title>Edit drop data</title><body>");

		List<L2DropData> dropDatas = null;
		if (npcData.getDropData() != null) {
			for (final L2DropCategory dropCat : npcData.getDropData()) {
				if (dropCat.getCategoryType() == category) {
					dropDatas = dropCat.getAllDrops();
					break;
				}
			}
		}

		L2DropData dropData = null;
		if (dropDatas != null) {
			for (final L2DropData drop : dropDatas) {
				if (drop.getItemId() == itemId) {
					dropData = drop;
					break;
				}
			}
		}

		if (dropData != null) {
			replyMSG.append("<table width=\"100%\"><tr><td>Npc</td><td>");
			replyMSG.append(npcData.getName());
			replyMSG.append(" (");
			replyMSG.append(npcId);
			replyMSG.append(")</td></tr><tr><td>Item</td><td>");
			replyMSG.append(itemData.getName());
			replyMSG.append(" (");
			replyMSG.append(itemId);
			replyMSG.append(")</td></tr><tr><td>Category</td><td>");
			replyMSG.append(category == -1 ? "-1 (sweep)" : Integer.toString(category));
			replyMSG.append("</td></tr>");
			replyMSG.append("<tr><td>Min count (");
			replyMSG.append(dropData.getMinDrop());
			replyMSG.append(")</td><td><edit var=\"min\" width=80></td></tr><tr><td>Max count (");
			replyMSG.append(dropData.getMaxDrop());
			replyMSG.append(")</td><td><edit var=\"max\" width=80></td></tr><tr><td>Chance (");
			replyMSG.append(dropData.getChance());
			replyMSG.append(")</td><td><edit var=\"chance\" width=80></td></tr></table><br>");

			replyMSG.append("<center><br><button value=\"Save\" action=\"bypass -h admin_edit_drop ");
			replyMSG.append(npcId);
			replyMSG.append(' ');
			replyMSG.append(itemId);
			replyMSG.append(' ');
			replyMSG.append(category);
			replyMSG.append(" $min $max $chance\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		} else {
			replyMSG.append("No drop data detail found.<center><br>");
		}
		replyMSG.append("<button value=\"Back to Droplist\" action=\"bypass -h admin_show_droplist ");
		replyMSG.append(npcId);
		replyMSG.append("\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
		replyMSG.append("</body></html>");

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private static void showAddDropData(final L2PcInstance activeChar, final L2NpcTemplate npcData) {
		final String replyMSG = StringUtil.concat("<html><title>Add drop data</title><body><table width=\"100%\"><tr><td>Npc</td><td>", npcData.getName(), " (", Integer.toString(npcData.getNpcId()), ")", "</td></tr><tr><td>Item Id</td><td><edit var=\"itemId\" width=80></td></tr><tr><td>Min count</td><td><edit var=\"min\" width=80></td></tr><tr><td>Max count</td><td><edit var=\"max\" width=80></td></tr><tr><td>Category (sweep=-1)</td><td><edit var=\"category\" width=80></td></tr><tr><td>Chance (0-1000000)</td><td><edit var=\"chance\" width=80></td></tr></table><center><br><button value=\"Add\" action=\"bypass -h admin_add_drop ",
				Integer.toString(npcData.getNpcId()), " $itemId $category $min $max $chance\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"Back to Droplist\" action=\"bypass -h admin_show_droplist ", Integer.toString(npcData.getNpcId()), "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG);
		activeChar.sendPacket(adminReply);
	}

	private static void updateDropData(final L2PcInstance activeChar, int npcId, final int itemId, final int min, final int max, final int category, final int chance) {
		try (Connection con = DatabaseFactory.getConnection()) {
			final PreparedStatement statement = con.prepareStatement("UPDATE droplist SET min=?, max=?, chance=? WHERE mobId=? AND itemId=? AND category=?");
			statement.setInt(1, min);
			statement.setInt(2, max);
			statement.setInt(3, chance);
			statement.setInt(4, npcId);
			statement.setInt(5, itemId);
			statement.setInt(6, category);

			statement.execute();
			statement.close();

			final PreparedStatement statement2 = con.prepareStatement("SELECT mobId FROM droplist WHERE mobId=? AND itemId=? AND category=?");
			statement2.setInt(1, npcId);
			statement2.setInt(2, itemId);
			statement2.setInt(3, category);

			final ResultSet npcIdRs = statement2.executeQuery();
			if (npcIdRs.next())
				npcId = npcIdRs.getInt("mobId");
			npcIdRs.close();
			statement2.close();

			if (npcId > 0) {
				reLoadNpcDropList(npcId);

				final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				final TextBuilder replyMSG = new TextBuilder("<html><title>Drop data modify complete!</title>");
				replyMSG.append("<body>");
				replyMSG.append("<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist ").append(npcId).append("\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
				replyMSG.append("</body></html>");

				adminReply.setHtml(replyMSG.toString());
				activeChar.sendPacket(adminReply);
			} else
				activeChar.sendMessage("unknown error!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void addDropData(final L2PcInstance activeChar, final int npcId, final int itemId, final int min, final int max, final int category, final int chance) {
		try (Connection con = DatabaseFactory.getConnection()) {
			final PreparedStatement statement = con.prepareStatement("INSERT INTO droplist(mobId, itemId, min, max, category, chance) values(?,?,?,?,?,?)");
			statement.setInt(1, npcId);
			statement.setInt(2, itemId);
			statement.setInt(3, min);
			statement.setInt(4, max);
			statement.setInt(5, category);
			statement.setInt(6, chance);
			statement.execute();
			statement.close();

			reLoadNpcDropList(npcId);

			final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			final TextBuilder replyMSG = new TextBuilder("<html><title>Add drop data complete!</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Continue add\" action=\"bypass -h admin_add_drop ").append(npcId).append("\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("<br><br><button value=\"DropList\" action=\"bypass -h admin_show_droplist ").append(npcId).append("\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("</center></body></html>");

			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		} catch (Exception e) {
		}
	}

	private static void deleteDropData(final L2PcInstance activeChar, final int npcId, final int itemId, final int category) {
		try (Connection con = DatabaseFactory.getConnection()) {
			if (npcId > 0) {
				final PreparedStatement statement2 = con.prepareStatement("DELETE FROM droplist WHERE mobId=? AND itemId=? AND category=?");
				statement2.setInt(1, npcId);
				statement2.setInt(2, itemId);
				statement2.setInt(3, category);
				statement2.execute();
				statement2.close();

				reLoadNpcDropList(npcId);

				final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				final TextBuilder replyMSG = new TextBuilder("<html><title>Delete drop data(" + npcId + ", " + itemId + ", " + category + ")complete</title>");
				replyMSG.append("<body>");
				replyMSG.append("<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist ").append(npcId).append("\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
				replyMSG.append("</body></html>");

				adminReply.setHtml(replyMSG.toString());
				activeChar.sendPacket(adminReply);
			}
		} catch (Exception e) {
		}
	}

	private static void reLoadNpcDropList(final int npcId) {
		final L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if (npcData == null)
			return;

		// reset the drop lists
		npcData.clearAllDropData();

		// get the drops
		try (Connection con = DatabaseFactory.getConnection()) {
			L2DropData dropData = null;

			npcData.getDropData().clear();

			final PreparedStatement statement = con.prepareStatement("SELECT mobId, itemId, min, max, category, chance FROM droplist WHERE mobId=?");
			statement.setInt(1, npcId);
			final ResultSet dropDataList = statement.executeQuery();

			while (dropDataList.next()) {
				dropData = new L2DropData();

				dropData.setItemId(dropDataList.getInt("itemId"));
				dropData.setMinDrop(dropDataList.getInt("min"));
				dropData.setMaxDrop(dropDataList.getInt("max"));
				dropData.setChance(dropDataList.getInt("chance"));

				final int category = dropDataList.getInt("category");
				npcData.addDropData(dropData, category);
			}
			dropDataList.close();
			statement.close();
		} catch (Exception e) {
		}
	}

	private static void showNpcSkillList(final L2PcInstance activeChar, final int npcId, int page) {
		final L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if (npcData == null) {
			activeChar.sendMessage("Template id unknown: " + npcId);
			return;
		}

		TIntObjectHashMap<L2Skill> skills = new TIntObjectHashMap<>();
		if (npcData.getSkills() != null)
			skills = npcData.getSkills();

		final int _skillsize = skills.size();

		final int MaxSkillsPerPage = PAGE_LIMIT;
		int MaxPages = _skillsize / MaxSkillsPerPage;
		if (_skillsize > MaxSkillsPerPage * MaxPages)
			MaxPages++;

		if (page > MaxPages)
			page = MaxPages;

		final int SkillsStart = MaxSkillsPerPage * page;
		int SkillsEnd = _skillsize;
		if (SkillsEnd - SkillsStart > MaxSkillsPerPage)
			SkillsEnd = SkillsStart + MaxSkillsPerPage;

		final StringBuilder replyMSG = new StringBuilder("<html><title>Show NPC Skill List</title><body><center><font color=\"LEVEL\">");
		replyMSG.append(npcData.getName());
		replyMSG.append(" (");
		replyMSG.append(npcData.getNpcId());
		replyMSG.append("): ");
		replyMSG.append(_skillsize);
		replyMSG.append(" skills</font></center><table width=300 bgcolor=666666><tr>");

		for (int x = 0; x < MaxPages; x++) {
			final int pagenr = x + 1;
			if (page == x) {
				replyMSG.append("<td>Page ");
				replyMSG.append(pagenr);
				replyMSG.append("</td>");
			} else {
				replyMSG.append("<td><a action=\"bypass -h admin_show_skilllist_npc ");
				replyMSG.append(npcData.getNpcId());
				replyMSG.append(' ');
				replyMSG.append(x);
				replyMSG.append("\"> Page ");
				replyMSG.append(pagenr);
				replyMSG.append(" </a></td>");
			}
		}
		replyMSG.append("</tr></table><table width=\"100%\" border=0><tr><td>Skill name [skill id-skill lvl]</td><td>Delete</td></tr>");

		final TIntObjectIterator<L2Skill> skillite = skills.iterator();
		for (int i = 0; i < SkillsStart; i++) {
			if (skillite.hasNext())
				skillite.advance();
		}

		int cnt = SkillsStart;
		while (skillite.hasNext()) {
			cnt++;
			if (cnt > SkillsEnd)
				break;

			skillite.advance();
			replyMSG.append("<tr><td width=240><a action=\"bypass -h admin_edit_skill_npc ");
			replyMSG.append(npcData.getNpcId());
			replyMSG.append(' ');
			replyMSG.append(skillite.value().getId());
			replyMSG.append("\">");
			if (skillite.value().getSkillType() == L2SkillType.NOTDONE)
				replyMSG.append("<font color=\"777777\">").append(skillite.value().getName()).append("</font>");
			else
				replyMSG.append(skillite.value().getName());
			replyMSG.append(" [");
			replyMSG.append(skillite.value().getId());
			replyMSG.append('-');
			replyMSG.append(skillite.value().getLevel());
			replyMSG.append("]</a></td><td width=60><a action=\"bypass -h admin_del_skill_npc ");
			replyMSG.append(npcData.getNpcId());
			replyMSG.append(' ');
			replyMSG.append(skillite.key());
			replyMSG.append("\">Delete</a></td></tr>");
		}
		replyMSG.append("</table><br><center><button value=\"Add Skill\" action=\"bypass -h admin_add_skill_npc ");
		replyMSG.append(npcId);
		replyMSG.append("\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private static void showNpcSkillEdit(final L2PcInstance activeChar, final int npcId, final int skillId) {
		try {
			final StringBuilder replyMSG = new StringBuilder("<html><title>NPC Skill Edit</title><body>");

			final L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
			if (npcData == null) {
				activeChar.sendMessage("Template id unknown: " + npcId);
				return;
			}
			if (npcData.getSkills() == null)
				return;

			final L2Skill npcSkill = npcData.getSkills().get(skillId);

			if (npcSkill != null) {
				replyMSG.append("<table width=\"100%\"><tr><td>NPC: </td><td>");
				replyMSG.append(NpcTable.getInstance().getTemplate(npcId).getName());
				replyMSG.append(" (");
				replyMSG.append(npcId);
				replyMSG.append(")</td></tr><tr><td>Skill: </td><td>");
				replyMSG.append(npcSkill.getName());
				replyMSG.append(" (");
				replyMSG.append(skillId);
				replyMSG.append(")</td></tr><tr><td>Skill Lvl: (");
				replyMSG.append(npcSkill.getLevel());
				replyMSG.append(") </td><td><edit var=\"level\" width=50></td></tr></table><br><center><button value=\"Save\" action=\"bypass -h admin_edit_skill_npc ");
				replyMSG.append(npcId);
				replyMSG.append(' ');
				replyMSG.append(skillId);
				replyMSG.append(" $level\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br1><button value=\"Back to SkillList\" action=\"bypass -h admin_show_skilllist_npc ");
				replyMSG.append(npcId);
				replyMSG.append("\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
			}

			replyMSG.append("</body></html>");

			final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		} catch (Exception e) {
			activeChar.sendMessage("Could not edit npc skills!");
			_log.warn("Error while editing npc skills (" + npcId + ", " + skillId + "): " + e);
		}
	}

	private static void updateNpcSkillData(final L2PcInstance activeChar, final int npcId, final int skillId, final int level) {
		final L2Skill skillData = SkillTable.getInstance().getInfo(skillId, level);
		if (skillData == null) {
			activeChar.sendMessage("Could not update npc skill: not existing skill id with that level!");
			showNpcSkillEdit(activeChar, npcId, skillId);
			return;
		}

		if (skillData.getLevel() != level) {
			activeChar.sendMessage("Skill id with requested level doesn't exist! Skill level not changed.");
			showNpcSkillEdit(activeChar, npcId, skillId);
			return;
		}

		try (Connection con = DatabaseFactory.getConnection()) {
			final PreparedStatement statement = con.prepareStatement("UPDATE `npc_skills` SET `level`=? WHERE `npcid`=? AND `skillid`=?");
			statement.setInt(1, level);
			statement.setInt(2, npcId);
			statement.setInt(3, skillId);

			statement.execute();
			statement.close();

			reloadNpcSkillList(npcId);

			showNpcSkillList(activeChar, npcId, 0);
			activeChar.sendMessage("Updated skill id " + skillId + " for npc id " + npcId + " to level " + level + '.');
		} catch (Exception e) {
			activeChar.sendMessage("Could not update npc skill!");
			_log.warn("Error while updating npc skill (" + npcId + ", " + skillId + ", " + level + "): " + e);
		}
	}

	private static void showNpcSkillAdd(final L2PcInstance activeChar, final int npcId) {
		final L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);

		final StringBuilder replyMSG = new StringBuilder("<html><title>NPC Skill Add</title><body><table width=\"100%\"><tr><td>NPC: </td><td>");
		replyMSG.append(npcData.getName());
		replyMSG.append(" (");
		replyMSG.append(npcData.getNpcId());
		replyMSG.append(")</td></tr><tr><td>SkillId: </td><td><edit var=\"skillId\" width=80></td></tr><tr><td>Level: </td><td><edit var=\"level\" width=80></td></tr></table><br><center><button value=\"Add Skill\" action=\"bypass -h admin_add_skill_npc ");
		replyMSG.append(npcData.getNpcId());
		replyMSG.append(" $skillId $level\"  width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br1><button value=\"Back to SkillList\" action=\"bypass -h admin_show_skilllist_npc ");
		replyMSG.append(npcData.getNpcId());
		replyMSG.append("\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private static void addNpcSkillData(final L2PcInstance activeChar, final int npcId, final int skillId, final int level) {
		// skill check
		final L2Skill skillData = SkillTable.getInstance().getInfo(skillId, level);
		if (skillData == null) {
			activeChar.sendMessage("Could not add npc skill: not existing skill id with that level!");
			showNpcSkillAdd(activeChar, npcId);
			return;
		}

		try (Connection con = DatabaseFactory.getConnection()) {
			final PreparedStatement statement = con.prepareStatement("INSERT INTO `npc_skills`(`npcid`, `skillid`, `level`) VALUES(?,?,?)");
			statement.setInt(1, npcId);
			statement.setInt(2, skillId);
			statement.setInt(3, level);
			statement.execute();
			statement.close();

			reloadNpcSkillList(npcId);

			showNpcSkillList(activeChar, npcId, 0);
			activeChar.sendMessage("Added skill " + skillId + '-' + level + " to npc id " + npcId + '.');
		} catch (Exception e) {
			activeChar.sendMessage("Could not add npc skill!");
			_log.warn("Error while adding a npc skill (" + npcId + ", " + skillId + ", " + level + "): " + e);
		}
	}

	private static void deleteNpcSkillData(final L2PcInstance activeChar, final int npcId, final int skillId) {
		try (Connection con = DatabaseFactory.getConnection()) {
			if (npcId > 0) {
				final PreparedStatement statement = con.prepareStatement("DELETE FROM `npc_skills` WHERE `npcid`=? AND `skillid`=?");
				statement.setInt(1, npcId);
				statement.setInt(2, skillId);
				statement.execute();
				statement.close();

				reloadNpcSkillList(npcId);

				showNpcSkillList(activeChar, npcId, 0);
				activeChar.sendMessage("Deleted skill id " + skillId + " from npc id " + npcId + '.');
			}
		} catch (Exception e) {
			activeChar.sendMessage("Could not delete npc skill!");
			_log.warn("Error while deleting npc skill (" + npcId + ", " + skillId + "): " + e);
		}
	}

	private static void reloadNpcSkillList(final int npcId) {
		try (Connection con = DatabaseFactory.getConnection()) {
			final L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);

			L2Skill skillData = null;
			if (npcData.getSkills() != null)
				npcData.getSkills().clear();

			// without race
			final PreparedStatement statement = con.prepareStatement("SELECT `skillid`, `level` FROM `npc_skills` WHERE `npcid`=? AND `skillid` <> 4416");
			statement.setInt(1, npcId);
			final ResultSet skillDataList = statement.executeQuery();

			while (skillDataList.next()) {
				final int idval = skillDataList.getInt("skillid");
				final int levelval = skillDataList.getInt("level");
				skillData = SkillTable.getInstance().getInfo(idval, levelval);
				if (skillData != null)
					npcData.addSkill(skillData);
			}
			skillDataList.close();
			statement.close();
		} catch (Exception e) {
			_log.warn("Error while reloading npc skill list (" + npcId + "): " + e);
		}
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}
