/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import silentium.commons.utils.StringUtil;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.CastleManorManager;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.entity.sevensigns.SevenSigns;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.ExShowCropInfo;
import silentium.gameserver.network.serverpackets.ExShowCropSetting;
import silentium.gameserver.network.serverpackets.ExShowManorDefaultInfo;
import silentium.gameserver.network.serverpackets.ExShowSeedInfo;
import silentium.gameserver.network.serverpackets.ExShowSeedSetting;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.ClanTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;
import silentium.gameserver.utils.Util;

/**
 * Castle Chamberlains implementation, used for: - tax rate control - regional manor system control - castle treasure control - ...
 */
public class L2CastleChamberlainInstance extends L2MerchantInstance
{
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;
	protected static final int COND_CLAN_MEMBER = 3;

	private int _preHour = 6;

	public L2CastleChamberlainInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// BypassValidation Exploit plug.
		if (player.getCurrentFolkNPC().getObjectId() != getObjectId())
			return;

		int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
			return;
		else if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			return;
		else if (condition == COND_OWNER)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command

			String val = "";
			if (st.countTokens() >= 1)
				val = st.nextToken();

			if (actualCommand.equalsIgnoreCase("banish_foreigner"))
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_DISMISS))
					return;

				if (siegeBlocksFunction(player))
					return;

				// Move non-clan members off castle area, and send html
				getCastle().banishForeigners();
				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-banishafter.htm");
				return;
			}
			else if (actualCommand.equalsIgnoreCase("banish_foreigner_show"))
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_DISMISS))
					return;

				if (siegeBlocksFunction(player))
					return;

				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-banishfore.htm");
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manage_functions"))
			{
				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-manage.htm");
				return;
			}
			else if (actualCommand.equalsIgnoreCase("products"))
			{
				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-products.htm");
				return;
			}
			else if (actualCommand.equalsIgnoreCase("list_siege_clans"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_MANAGE_SIEGE) == L2Clan.CP_CS_MANAGE_SIEGE)
				{
					getCastle().getSiege().listRegisterClan(player); // List current register clan
					return;
				}

				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-noprivs.htm");
				return;
			}
			else if (actualCommand.equalsIgnoreCase("receive_report"))
			{
				if (player.isClanLeader())
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(StaticHtmPath.ChamberlainHtmPath + "chamberlain-report.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
					html.replace("%clanname%", clan.getName());
					html.replace("%clanleadername%", clan.getLeaderName());
					html.replace("%castlename%", getCastle().getName());

					int currentPeriod = SevenSigns.getInstance().getCurrentPeriod();
					switch (currentPeriod)
					{
						case SevenSigns.PERIOD_COMP_RECRUITING:
							html.replace("%ss_event%", "Quest Event Initialization");
							break;
						case SevenSigns.PERIOD_COMPETITION:
							html.replace("%ss_event%", "Competition (Quest Event)");
							break;
						case SevenSigns.PERIOD_COMP_RESULTS:
							html.replace("%ss_event%", "Quest Event Results");
							break;
						case SevenSigns.PERIOD_SEAL_VALIDATION:
							html.replace("%ss_event%", "Seal Validation");
							break;
					}

					int sealOwner1 = SevenSigns.getInstance().getSealOwner(1);
					switch (sealOwner1)
					{
						case SevenSigns.CABAL_NULL:
							html.replace("%ss_avarice%", "Not in Possession");
							break;
						case SevenSigns.CABAL_DAWN:
							html.replace("%ss_avarice%", "Lords of Dawn");
							break;
						case SevenSigns.CABAL_DUSK:
							html.replace("%ss_avarice%", "Revolutionaries of Dusk");
							break;
					}

					int sealOwner2 = SevenSigns.getInstance().getSealOwner(2);
					switch (sealOwner2)
					{
						case SevenSigns.CABAL_NULL:
							html.replace("%ss_gnosis%", "Not in Possession");
							break;
						case SevenSigns.CABAL_DAWN:
							html.replace("%ss_gnosis%", "Lords of Dawn");
							break;
						case SevenSigns.CABAL_DUSK:
							html.replace("%ss_gnosis%", "Revolutionaries of Dusk");
							break;
					}

					int sealOwner3 = SevenSigns.getInstance().getSealOwner(3);
					switch (sealOwner3)
					{
						case SevenSigns.CABAL_NULL:
							html.replace("%ss_strife%", "Not in Possession");
							break;
						case SevenSigns.CABAL_DAWN:
							html.replace("%ss_strife%", "Lords of Dawn");
							break;
						case SevenSigns.CABAL_DUSK:
							html.replace("%ss_strife%", "Revolutionaries of Dusk");
							break;
					}
					player.sendPacket(html);
					return;
				}

				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-noprivs.htm");
				return;
			}
			else if (actualCommand.equalsIgnoreCase("items"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_USE_FUNCTIONS) == L2Clan.CP_CS_USE_FUNCTIONS)
				{
					if (val.isEmpty())
						return;

					player.tempInventoryDisable();

					_log.trace("Showing chamberlain buylist");

					showBuyWindow(player, Integer.parseInt(val + "1"));
					return;
				}

				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-noprivs.htm");
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manage_siege_defender"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_MANAGE_SIEGE) == L2Clan.CP_CS_MANAGE_SIEGE)
				{
					getCastle().getSiege().listRegisterClan(player);
					return;
				}

				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-noprivs.htm");
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manage_vault"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_TAXES) == L2Clan.CP_CS_TAXES)
				{
					String filename = StaticHtmPath.ChamberlainHtmPath + "chamberlain-vault.htm";
					int amount = 0;

					if (val.equalsIgnoreCase("deposit"))
					{
						try
						{
							amount = Integer.parseInt(st.nextToken());
						}
						catch (NoSuchElementException e)
						{
						}

						if (amount > 0 && getCastle().getTreasury() + amount < Integer.MAX_VALUE)
						{
							if (player.reduceAdena("Castle", amount, this, true))
								getCastle().addToTreasuryNoTax(amount);
							else
								sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
						}
					}
					else if (val.equalsIgnoreCase("withdraw"))
					{
						try
						{
							amount = Integer.parseInt(st.nextToken());
						}
						catch (NoSuchElementException e)
						{
						}

						if (amount > 0)
						{
							if (getCastle().getTreasury() < amount)
								filename = StaticHtmPath.ChamberlainHtmPath + "chamberlain-vault-no.htm";
							else
							{
								if (getCastle().addToTreasuryNoTax((-1) * amount))
									player.addAdena("Castle", amount, this, true);
							}
						}
					}
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					html.replace("%tax_income%", Util.formatAdena(getCastle().getTreasury()));
					html.replace("%withdraw_amount%", Util.formatAdena(amount));
					player.sendPacket(html);
					return;
				}

				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-noprivs.htm");
				return;
			}
			else if (actualCommand.equalsIgnoreCase("operate_door")) // door control
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_OPEN_DOOR) == L2Clan.CP_CS_OPEN_DOOR)
				{
					if (!val.isEmpty())
					{
						boolean open = (Integer.parseInt(val) == 1);
						while (st.hasMoreTokens())
							getCastle().openCloseDoor(player, Integer.parseInt(st.nextToken()), open);

						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						String file = StaticHtmPath.ChamberlainHtmPath + "doors-close.htm";
						if (open)
							file = StaticHtmPath.ChamberlainHtmPath + "doors-open.htm";
						html.setFile(file);
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
						return;
					}

					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(StaticHtmPath.ChamberlainHtmPath + getTemplate().getNpcId() + "-d.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
					return;
				}

				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-noprivs.htm");
				return;
			}
			else if (actualCommand.equalsIgnoreCase("tax_set")) // tax rates control
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_TAXES) == L2Clan.CP_CS_TAXES)
				{
					if (!val.isEmpty())
						getCastle().setTaxPercent(player, Integer.parseInt(val));

					final String msg = StringUtil.concat("<html><body>", getName(), ":<br>" + "Current tax rate: ", String.valueOf(getCastle().getTaxPercent()), "%<br>" + "<table>" + "<tr>" + "<td width=120>Change tax rate to:</td>" + "<td><edit var=\"value\" width=40></td>" + "</tr>" + "</table><br>" + "<center><button value=\"Adjust\" action=\"bypass -h npc_%objectId%_tax_set $value\" width=74 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\">" + "<button value=\"Cancel\" action=\"bypass -h npc_%objectId%_Link chamberlain/chamberlain.htm\" width=74 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\"></center>"
							+ "</body></html>");
					sendHtmlMessage(player, msg);
					return;
				}

				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(StaticHtmPath.ChamberlainHtmPath + "chamberlain-tax.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%tax%", String.valueOf(getCastle().getTaxPercent()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manor"))
			{
				String filename = "";
				if (CastleManorManager.getInstance().isDisabled())
					filename = StaticHtmPath.NpcHtmPath + "npcdefault.htm";
				else
				{
					int cmd = Integer.parseInt(val);
					switch (cmd)
					{
						case 0:
							filename = StaticHtmPath.ChamberlainHtmPath + "manor/manor.htm";
							break;
						// TODO: correct in html's to 1
						case 4:
							filename = StaticHtmPath.ChamberlainHtmPath + "manor/manor_help00" + st.nextToken() + ".htm";
							break;
						default:
							filename = StaticHtmPath.ChamberlainHtmPath + "chamberlain-no.htm";
							break;
					}
				}

				if (filename.length() != 0)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
				}
				return;
			}
			else if (command.startsWith("manor_menu_select"))
			{
				if (CastleManorManager.getInstance().isUnderMaintenance())
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					player.sendPacket(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE);
					return;
				}

				String params = command.substring(command.indexOf("?") + 1);
				StringTokenizer str = new StringTokenizer(params, "&");
				int ask = Integer.parseInt(str.nextToken().split("=")[1]);
				int state = Integer.parseInt(str.nextToken().split("=")[1]);
				int time = Integer.parseInt(str.nextToken().split("=")[1]);

				int castleId;
				if (state == -1) // info for current manor
					castleId = getCastle().getCastleId();
				else
					// info for requested manor
					castleId = state;

				switch (ask)
				{
					case 3: // Current seeds (Manor info)
						if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
							player.sendPacket(new ExShowSeedInfo(castleId, null));
						else
							player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
						break;
					case 4: // Current crops (Manor info)
						if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
							player.sendPacket(new ExShowCropInfo(castleId, null));
						else
							player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getCropProcure(time)));
						break;
					case 5: // Basic info (Manor info)
						player.sendPacket(new ExShowManorDefaultInfo());
						break;
					case 7: // Edit seed setup
						if (getCastle().isNextPeriodApproved())
							player.sendPacket(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM);
						else
							player.sendPacket(new ExShowSeedSetting(getCastle().getCastleId()));
						break;
					case 8: // Edit crop setup
						if (getCastle().isNextPeriodApproved())
							player.sendPacket(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM);
						else
							player.sendPacket(new ExShowCropSetting(getCastle().getCastleId()));
						break;
				}
			}
			else if (actualCommand.equalsIgnoreCase("siege_change")) // set siege time
			{
				if (player.isClanLeader())
				{
					if (getCastle().getSiege().getTimeRegistrationOverDate().getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
						sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "siegetime1.htm");
					else if (getCastle().getSiege().getIsTimeRegistrationOver())
						sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "siegetime2.htm");
					else
						sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "siegetime3.htm");
				}
				else
					sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-noprivs.htm");

				return;
			}
			else if (actualCommand.equalsIgnoreCase("siege_time_set")) // set preDay
			{
				switch (Integer.parseInt(val))
				{
					case 1:
						_preHour = Integer.parseInt(st.nextToken());
						break;
					default:
						break;
				}

				if (_preHour != 6)
				{
					getCastle().getSiegeDate().set(Calendar.HOUR_OF_DAY, _preHour + 12);

					// now store the changed time and finished next Siege Time registration
					getCastle().getSiege().endTimeRegistration(false);
					sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "siegetime8.htm");
					return;
				}

				sendFileMessage(player, StaticHtmPath.ChamberlainHtmPath + "siegetime6.htm");
				return;
			}
			else if (actualCommand.equals("give_crown"))
			{
				if (siegeBlocksFunction(player))
					return;

				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

				if (player.isClanLeader())
				{
					if (player.getInventory().getItemByItemId(6841) == null)
					{
						player.getInventory().addItem("Castle Crown", 6841, 1, player, this);

						SystemMessage ms = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
						ms.addItemName(6841);
						player.sendPacket(ms);

						html.setFile(StaticHtmPath.ChamberlainHtmPath + "chamberlain-gavecrown.htm");
						html.replace("%CharName%", String.valueOf(player.getName()));
						html.replace("%FeudName%", String.valueOf(getCastle().getName()));
					}
					else
						html.setFile(StaticHtmPath.ChamberlainHtmPath + "chamberlain-hascrown.htm");
				}
				else
					html.setFile(StaticHtmPath.ChamberlainHtmPath + "chamberlain-noprivs.htm");

				player.sendPacket(html);
				return;
			}
		}

		super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = StaticHtmPath.ChamberlainHtmPath + "chamberlain-no.htm";

		int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = StaticHtmPath.ChamberlainHtmPath + "chamberlain-busy.htm";
			else if (condition == COND_OWNER)
				filename = StaticHtmPath.ChamberlainHtmPath + "chamberlain.htm";
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	protected int validateCondition(L2PcInstance player)
	{
		if (getCastle() != null && getCastle().getCastleId() > 0)
		{
			if (player.getClan() != null)
			{
				if (getCastle().getSiege().getIsInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE;
				else if (getCastle().getOwnerId() == player.getClanId())
				{
					if (player.isClanLeader())
						return COND_OWNER;

					return COND_CLAN_MEMBER;
				}
			}
		}
		return COND_ALL_FALSE;
	}

	private boolean validatePrivileges(L2PcInstance player, int privilege)
	{
		if ((player.getClanPrivileges() & privilege) != privilege)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(StaticHtmPath.ChamberlainHtmPath + "chamberlain-noprivs.htm");
			player.sendPacket(html);
			return false;
		}
		return true;
	}

	private boolean siegeBlocksFunction(L2PcInstance player)
	{
		if (getCastle().getSiege().getIsInProgress())
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(StaticHtmPath.ChamberlainHtmPath + "chamberlain-busy.htm");
			html.replace("%npcname%", String.valueOf(getName()));
			player.sendPacket(html);
			return true;
		}
		return false;
	}

	private void sendFileMessage(L2PcInstance player, String htmlMessage)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(htmlMessage);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%time%", String.valueOf(getCastle().getSiegeDate().getTime()));
		player.sendPacket(html);
	}

	private void sendHtmlMessage(L2PcInstance player, String htmlMessage)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml(htmlMessage);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}