/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastMap;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.data.xml.MapRegionData;
import silentium.gameserver.instancemanager.AuctionManager;
import silentium.gameserver.instancemanager.ClanHallManager;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.entity.Auction;
import silentium.gameserver.model.entity.Auction.Bidder;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.templates.chars.L2NpcTemplate;
import silentium.gameserver.utils.Util;

public final class L2AuctioneerInstance extends L2NpcInstance
{
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_REGULAR = 3;

	private final Map<Integer, Auction> _pendingAuctions = new FastMap<>();

	public L2AuctioneerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
			return;
		else if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			String filename = StaticHtmPath.AuctionHtmPath + "auction-busy.htm";
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			return;
		}
		else if (condition == COND_REGULAR)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken();

			String val = "";
			if (st.countTokens() >= 1)
				val = st.nextToken();

			/* Only a few actions are possible for clanless people. */
			if (actualCommand.equalsIgnoreCase("list"))
				showAuctionsList(val, player);
			else if (actualCommand.equalsIgnoreCase("bidding"))
			{
				if (val.isEmpty())
					return;

				_log.debug("Auction: bidding show is successful.");

				try
				{
					int auctionId = Integer.parseInt(val);

					_log.debug("Auction: auction has started.");

					String filename = StaticHtmPath.AuctionHtmPath + "AgitAuctionInfo.htm";
					Auction a = AuctionManager.getInstance().getAuction(auctionId);

					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(filename);
					if (a != null)
					{
						html.replace("%AGIT_NAME%", a.getItemName());
						html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
						html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(a.getItemId()).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLease()));
						html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLocation());
						html.replace("%AGIT_AUCTION_END%", Util.formatDate(a.getEndDate(), "dd/MM/yyyy HH:mm"));
						html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + String.valueOf((((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60)) + " minutes");
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_COUNT%", String.valueOf(a.getBidders().size()));
						html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getDesc());
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_list");
						html.replace("%AGIT_LINK_BIDLIST%", "bypass -h npc_" + getObjectId() + "_bidlist " + a.getId());
						html.replace("%AGIT_LINK_RE%", "bypass -h npc_" + getObjectId() + "_bid1 " + a.getId());
					}
					else
						_log.warn("Auctioneer Auction null for AuctionId : " + auctionId);

					player.sendPacket(html);
				}
				catch (Exception e)
				{
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("location"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(StaticHtmPath.AuctionHtmPath + "location.htm");
				html.replace("%location%", MapRegionData.getInstance().getClosestTownName(player));
				html.replace("%LOCATION%", getPictureName(player));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("start"))
			{
				showChatWindow(player);
				return;
			}
			/* Clanless or clan members without enough power are kicked directly. */
			else
			{
				if (player.getClan() == null || !((player.getClanPrivileges() & L2Clan.CP_CH_AUCTION) == L2Clan.CP_CH_AUCTION))
				{
					showAuctionsList("1", player); // Force to display page 1.
					player.sendPacket(SystemMessageId.CANNOT_PARTICIPATE_IN_AUCTION);
					return;
				}

				if (actualCommand.equalsIgnoreCase("bid"))
				{
					if (val.isEmpty())
						return;

					try
					{
						int auctionId = Integer.parseInt(val);
						try
						{
							int bid = 0;
							if (st.countTokens() >= 1)
								bid = Math.min(Integer.parseInt(st.nextToken()), Integer.MAX_VALUE);

							AuctionManager.getInstance().getAuction(auctionId).setBid(player, bid);
						}
						catch (Exception e)
						{
						}
					}
					catch (Exception e)
					{
					}
					return;
				}
				else if (actualCommand.equalsIgnoreCase("bid1"))
				{
					if (val.isEmpty())
						return;

					if (player.getClan() == null || player.getClan().getLevel() < 2)
					{
						showAuctionsList("1", player); // Force to display page 1.
						player.sendPacket(SystemMessageId.AUCTION_ONLY_CLAN_LEVEL_2_HIGHER);
						return;
					}

					if (player.getClan().hasHideout())
					{
						showAuctionsList("1", player); // Force to display page 1.
						player.sendPacket(SystemMessageId.CANNOT_PARTICIPATE_IN_AUCTION);
						return;
					}

					if ((player.getClan().getAuctionBiddedAt() > 0 && player.getClan().getAuctionBiddedAt() != Integer.parseInt(val)))
					{
						showAuctionsList("1", player); // Force to display page 1.
						player.sendPacket(SystemMessageId.ALREADY_SUBMITTED_BID);
						return;
					}

					try
					{
						String filename = StaticHtmPath.AuctionHtmPath + "AgitBid1.htm";

						int minimumBid = AuctionManager.getInstance().getAuction(Integer.parseInt(val)).getHighestBidderMaxBid();
						if (minimumBid == 0)
							minimumBid = AuctionManager.getInstance().getAuction(Integer.parseInt(val)).getStartingBid();

						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(filename);
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_bidding " + val);
						html.replace("%PLEDGE_ADENA%", String.valueOf(player.getClan().getWarehouse().getAdena()));
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(minimumBid));
						html.replace("npc_%objectId%_bid", "npc_" + getObjectId() + "_bid " + val);
						player.sendPacket(html);
						return;
					}
					catch (Exception e)
					{
					}
					return;
				}
				else if (actualCommand.equalsIgnoreCase("bidlist"))
				{
					int auctionId = 0;
					if (val.isEmpty())
					{
						if (player.getClan().getAuctionBiddedAt() <= 0)
							return;

						auctionId = player.getClan().getAuctionBiddedAt();
					}
					else
						auctionId = Integer.parseInt(val);

					_log.debug("Auction: command bidlist has started.");

					String biders = "";
					final Map<Integer, Bidder> bidders = AuctionManager.getInstance().getAuction(auctionId).getBidders();
					for (Bidder b : bidders.values())
					{
						biders += "<tr>" + "<td>" + b.getClanName() + "</td><td>" + b.getName() + "</td><td>" + b.getTimeBid().get(Calendar.YEAR) + "/" + (b.getTimeBid().get(Calendar.MONTH) + 1) + "/" + b.getTimeBid().get(Calendar.DATE) + "</td>" + "</tr>";
					}
					String filename = StaticHtmPath.AuctionHtmPath + "AgitBidderList.htm";

					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(filename);
					html.replace("%AGIT_LIST%", biders);
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_bidding " + val);
					html.replace("%x%", val);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				else if (actualCommand.equalsIgnoreCase("selectedItems"))
				{
					showSelectedItems(player);
				}
				else if (actualCommand.equalsIgnoreCase("cancelBid"))
				{
					int bid = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()).getBidders().get(player.getClanId()).getBid();
					String filename = StaticHtmPath.AuctionHtmPath + "AgitBidCancel.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(filename);
					html.replace("%AGIT_BID%", String.valueOf(bid));
					html.replace("%AGIT_BID_REMAIN%", String.valueOf((int) (bid * 0.9)));
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				else if (actualCommand.equalsIgnoreCase("doCancelBid"))
				{
					if (AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()) != null)
					{
						AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()).cancelBid(player.getClanId());
						player.sendPacket(SystemMessageId.CANCELED_BID);
					}
					return;
				}
				else if (actualCommand.equalsIgnoreCase("cancelAuction"))
				{
					String filename = StaticHtmPath.AuctionHtmPath + "AgitSaleCancel.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(filename);
					html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				else if (actualCommand.equalsIgnoreCase("doCancelAuction"))
				{
					if (AuctionManager.getInstance().getAuction(player.getClan().getHideoutId()) != null)
					{
						AuctionManager.getInstance().getAuction(player.getClan().getHideoutId()).cancelAuction();
						player.sendPacket(SystemMessageId.CANCELED_BID);
					}
					showChatWindow(player);
					return;
				}
				else if (actualCommand.equalsIgnoreCase("sale"))
				{
					String filename = StaticHtmPath.AuctionHtmPath + "AgitSale1.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(filename);
					html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
					html.replace("%AGIT_PLEDGE_ADENA%", String.valueOf(player.getClan().getWarehouse().getAdena()));
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				else if (actualCommand.equalsIgnoreCase("rebid"))
				{
					try
					{
						String filename = StaticHtmPath.AuctionHtmPath + "AgitBid2.htm";
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(filename);
						Auction a = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt());
						if (a != null)
						{
							html.replace("%AGIT_AUCTION_BID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
							html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
							html.replace("%AGIT_AUCTION_END%", Util.formatDate(a.getEndDate(), "dd/MM/yyyy HH:mm"));
							html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
							html.replace("npc_%objectId%_bid1", "npc_" + getObjectId() + "_bid1 " + a.getId());
						}
						else
							_log.warn("Auctioneer Auction null for AuctionBiddedAt : " + player.getClan().getAuctionBiddedAt());

						player.sendPacket(html);
					}
					catch (Exception e)
					{
					}
					return;
				}
				/* Those bypasses check if CWH got enough adenas (case of sale auction type) */
				else
				{
					if (player.getClan().getWarehouse().getAdena() < ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease())
					{
						showSelectedItems(player);
						player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_IN_CWH);
						return;
					}

					if (actualCommand.equalsIgnoreCase("auction"))
					{
						if (val.isEmpty())
							return;

						try
						{
							int days = Integer.parseInt(val);
							try
							{
								int bid = 0;
								if (st.countTokens() >= 1)
									bid = Math.min(Integer.parseInt(st.nextToken()), Integer.MAX_VALUE);

								Auction a = new Auction(player.getClan().getHideoutId(), player.getClan(), days * 86400000L, bid, ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getName());
								if (_pendingAuctions.get(a.getId()) != null)
									_pendingAuctions.remove(a.getId());

								_pendingAuctions.put(a.getId(), a);

								String filename = StaticHtmPath.AuctionHtmPath + "AgitSale3.htm";
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile(filename);
								html.replace("%x%", val);
								html.replace("%AGIT_AUCTION_END%", Util.formatDate(a.getEndDate(), "dd/MM/yyyy HH:mm"));
								html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
								html.replace("%AGIT_AUCTION_MIN%", String.valueOf(a.getStartingBid()));
								html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getDesc());
								html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_sale2");
								html.replace("%objectId%", String.valueOf((getObjectId())));
								player.sendPacket(html);
							}
							catch (Exception e)
							{
							}
						}
						catch (Exception e)
						{
						}
						return;
					}
					else if (actualCommand.equalsIgnoreCase("confirmAuction"))
					{
						int chId = player.getClan().getHideoutId();
						if (chId <= 0)
							return;

						Auction auction = _pendingAuctions.get(chId);
						if (auction == null)
							return;

						if (Auction.takeItem(player, ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()))
						{
							auction.confirmAuction();

							_pendingAuctions.remove(chId);

							showSelectedItems(player);
							player.sendPacket(SystemMessageId.REGISTERED_FOR_CLANHALL);
						}
						return;
					}
					else if (actualCommand.equalsIgnoreCase("sale2"))
					{
						String filename = StaticHtmPath.AuctionHtmPath + "AgitSale2.htm";
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(filename);
						html.replace("%AGIT_LAST_PRICE%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_sale");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
						return;
					}
				}
			}
		}
		super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filename = StaticHtmPath.AuctionHtmPath + "auction-no.htm";

		int condition = validateCondition(player);
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			filename = StaticHtmPath.AuctionHtmPath + "auction-busy.htm";
		else
			filename = StaticHtmPath.AuctionHtmPath + "auction.htm";

		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		if (val == 0)
			return;

		super.showChatWindow(player, val);
	}

	private int validateCondition(L2PcInstance player)
	{
		if (getCastle() != null && getCastle().getCastleId() > 0)
		{
			if (getCastle().getSiege().getIsInProgress())
				return COND_BUSY_BECAUSE_OF_SIEGE;

			return COND_REGULAR;
		}

		return COND_ALL_FALSE;
	}

	private void showAuctionsList(String val, L2PcInstance player)
	{
		List<Auction> auctions = AuctionManager.getInstance().getAuctions();

		int limit = 15; // Limit to prevent client crash
		int start;
		int i = 1;
		double npage = Math.ceil((float) auctions.size() / limit);

		if (val.isEmpty())
			start = 1;
		else
		{
			start = limit * (Integer.parseInt(val) - 1) + 1;
			limit *= Integer.parseInt(val);
		}

		_log.debug("Auction: list command has started.");

		StringBuilder items = new StringBuilder();
		items.append("<table width=280 border=0><tr>");
		for (int j = 1; j <= npage; j++)
		{
			items.append("<td><center><a action=\"bypass -h npc_");
			items.append(getObjectId());
			items.append("_list ");
			items.append(j);
			items.append("\"> Page ");
			items.append(j);
			items.append(" </a></center></td>");
		}

		items.append("</tr></table>");
		items.append("<table width=280 border=0>");

		for (Auction a : auctions)
		{
			if (a == null)
				continue;

			if (i > limit)
				break;
			else if (i < start)
			{
				i++;
				continue;
			}
			else
				i++;

			items.append("<tr>");
			items.append("<td><font color=\"aaaaff\">");
			items.append(ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLocation());
			items.append("</font></td>");
			items.append("<td><font color=\"ffffaa\"><a action=\"bypass -h npc_");
			items.append(getObjectId());
			items.append("_bidding ");
			items.append(a.getId());
			items.append("\">");
			items.append(a.getItemName());
			items.append(" [");
			items.append(a.getBidders().size());
			items.append("]</a></font></td>");
			items.append("<td>").append(Util.formatDate(a.getEndDate(), "yy/MM/dd"));
			items.append("</td>");
			items.append("<td><font color=\"aaffff\">");
			items.append(a.getStartingBid());
			items.append("</font></td>");
			items.append("</tr>");
		}
		items.append("</table>");

		String filename = StaticHtmPath.AuctionHtmPath + "AgitAuctionList.htm";

		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
		html.replace("%itemsField%", items.toString());
		player.sendPacket(html);
		return;
	}

	private void showSelectedItems(L2PcInstance player)
	{
		final L2Clan clan = player.getClan();
		if (clan == null)
			return;

		if (!clan.hasHideout() && clan.getAuctionBiddedAt() > 0)
		{
			String filename = StaticHtmPath.AuctionHtmPath + "AgitBidInfo.htm";
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile(filename);
			Auction a = AuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt());
			if (a != null)
			{
				html.replace("%AGIT_NAME%", a.getItemName());
				html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
				html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
				html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(a.getItemId()).getGrade() * 10));
				html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLease()));
				html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLocation());
				html.replace("%AGIT_AUCTION_END%", Util.formatDate(a.getEndDate(), "dd/MM/yyyy HH:mm"));
				html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + String.valueOf((((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60)) + " minutes");
				html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
				html.replace("%AGIT_AUCTION_MYBID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
				html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getDesc());
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
			}
			else
				_log.warn("Auctioneer Auction null for AuctionBiddedAt : " + clan.getAuctionBiddedAt());

			player.sendPacket(html);
			return;
		}
		else if (AuctionManager.getInstance().getAuction(clan.getHideoutId()) != null)
		{
			String filename = StaticHtmPath.AuctionHtmPath + "AgitSaleInfo.htm";
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile(filename);
			Auction a = AuctionManager.getInstance().getAuction(clan.getHideoutId());
			if (a != null)
			{
				html.replace("%AGIT_NAME%", a.getItemName());
				html.replace("%AGIT_OWNER_PLEDGE_NAME%", a.getSellerClanName());
				html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
				html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(a.getItemId()).getGrade() * 10));
				html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLease()));
				html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLocation());
				html.replace("%AGIT_AUCTION_END%", Util.formatDate(a.getEndDate(), "dd/MM/yyyy HH:mm"));
				html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + String.valueOf((((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60)) + " minutes");
				html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
				html.replace("%AGIT_AUCTION_BIDCOUNT%", String.valueOf(a.getBidders().size()));
				html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getDesc());
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				html.replace("%id%", String.valueOf(a.getId()));
				html.replace("%objectId%", String.valueOf(getObjectId()));
			}
			else
				_log.warn("Auctioneer Auction null for getHasHideout : " + clan.getHideoutId());

			player.sendPacket(html);
			return;
		}
		else if (clan.hasHideout())
		{
			int ItemId = clan.getHideoutId();
			String filename = StaticHtmPath.AuctionHtmPath + "AgitInfo.htm";
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile(filename);
			if (ClanHallManager.getInstance().getClanHallById(ItemId) != null)
			{
				html.replace("%AGIT_NAME%", ClanHallManager.getInstance().getClanHallById(ItemId).getName());
				html.replace("%AGIT_OWNER_PLEDGE_NAME%", clan.getName());
				html.replace("%OWNER_PLEDGE_MASTER%", clan.getLeaderName());
				html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(ItemId).getGrade() * 10));
				html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(ItemId).getLease()));
				html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHallById(ItemId).getLocation());
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				html.replace("%objectId%", String.valueOf(getObjectId()));
			}
			else
				_log.warn("Clan Hall ID NULL : " + ItemId + " Can be caused by concurent write in ClanHallManager");

			player.sendPacket(html);
			return;
		}
		else if (!clan.hasHideout())
		{
			showAuctionsList("1", player); // Force to display page 1.
			player.sendPacket(SystemMessageId.NO_OFFERINGS_OWN_OR_MADE_BID_FOR);
			return;
		}
	}

	private static String getPictureName(L2PcInstance plyr)
	{
		int nearestTownId = MapRegionData.getInstance().getMapRegion(plyr.getX(), plyr.getY());
		String nearestTown;

		switch (nearestTownId)
		{
			case 5:
				nearestTown = "GLUDIO";
				break;
			case 6:
				nearestTown = "GLUDIN";
				break;
			case 7:
				nearestTown = "DION";
				break;
			case 8:
				nearestTown = "GIRAN";
				break;
			case 14:
				nearestTown = "RUNE";
				break;
			case 15:
				nearestTown = "GODARD";
				break;
			case 16:
				nearestTown = "SCHUTTGART";
				break;
			default:
				nearestTown = "ADEN";
				break;
		}

		return nearestTown;
	}
}
