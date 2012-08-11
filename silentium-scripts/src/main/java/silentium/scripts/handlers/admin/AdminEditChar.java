/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import javolution.text.TextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.database.DatabaseFactory;
import silentium.commons.utils.StringUtil;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.data.xml.CharTemplateData;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.ClanHallManager;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.model.base.ClassId;
import silentium.gameserver.network.L2GameClient;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.utils.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

public class AdminEditChar implements IAdminCommandHandler
{
	private static Logger _log = LoggerFactory.getLogger(AdminEditChar.class.getName());

	private static final String[] ADMIN_COMMANDS = { "admin_edit_character", "admin_current_player", "admin_nokarma", // this is
																														// to
																														// remove
																														// karma
																														// from
																														// selected
																														// char...
			"admin_setkarma", // sets karma of target char to any amount. //setkarma <karma>
			"admin_character_list", // same as character_info, kept for compatibility purposes
			"admin_character_info", // given a player name, displays an information window
			"admin_show_characters", // list of characters
			"admin_find_character", // find a player by his name or a part of it (case-insensitive)
			"admin_find_ip", // find all the player connections from a given IPv4 number
			"admin_find_account", // list all the characters from an account (useful for GMs w/o DB access)
			"admin_find_dualbox", // list all IPs with more than 1 char logged in (dualbox)
			"admin_rec", // gives recommendation points
			"admin_settitle", // changes char's title
			"admin_setname", // changes char's name
			"admin_setsex", // changes char's sex
			"admin_setcolor", // change char name's color
			"admin_settcolor", // change char title's color
			"admin_setclass", // changes char's classId

			"admin_summon_info", // displays an information window about target summon
			"admin_unsummon", // unsummon target's pet/summon
			"admin_summon_setlvl", // set the pet's level
			"admin_show_pet_inv", // show pet's inventory
			"admin_fullfood", // fulfills a pet's food bar

			"admin_party_info", // find party infos of targeted character, if any
			"admin_clan_info", // find clan infos of the character, if any
			"admin_remove_clan_penalty" // removes clan penalties
	};

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_current_player"))
			showCharacterInfo(activeChar, null);
		else if ((command.startsWith("admin_character_list")) || (command.startsWith("admin_character_info")))
		{
			try
			{
				String val = command.substring(21);
				L2PcInstance target = L2World.getInstance().getPlayer(val);
				if (target != null)
					showCharacterInfo(activeChar, target);
				else
					activeChar.sendPacket(SystemMessageId.CHARACTER_DOES_NOT_EXIST);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //character_info <player_name>");
			}
		}
		else if (command.startsWith("admin_show_characters"))
		{
			try
			{
				String val = command.substring(22);
				int page = Integer.parseInt(val);
				listCharacters(activeChar, page);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Case of empty page number
				activeChar.sendMessage("Usage: //show_characters <page_number>");
			}
		}
		else if (command.startsWith("admin_find_character"))
		{
			try
			{
				String val = command.substring(21);
				findCharacter(activeChar, val);
			}
			catch (StringIndexOutOfBoundsException e)
			{ // Case of empty character name
				activeChar.sendMessage("Usage: //find_character <character_name>");
				listCharacters(activeChar, 0);
			}
		}
		else if (command.startsWith("admin_find_ip"))
		{
			try
			{
				String val = command.substring(14);
				findCharactersPerIp(activeChar, val);
			}
			catch (Exception e)
			{
				// Case of empty or malformed IP number
				activeChar.sendMessage("Usage: //find_ip <www.xxx.yyy.zzz>");
				listCharacters(activeChar, 0);
			}
		}
		else if (command.startsWith("admin_find_account"))
		{
			try
			{
				String val = command.substring(19);
				findCharactersPerAccount(activeChar, val);
			}
			catch (Exception e)
			{
				// Case of empty or malformed player name
				activeChar.sendMessage("Usage: //find_account <player_name>");
				listCharacters(activeChar, 0);
			}
		}
		else if (command.startsWith("admin_find_dualbox"))
		{
			int multibox = 2;
			try
			{
				String val = command.substring(19);
				multibox = Integer.parseInt(val);
				if (multibox < 1)
				{
					activeChar.sendMessage("Usage: //find_dualbox [number > 0]");
					return false;
				}
			}
			catch (Exception e)
			{
			}
			findDualbox(activeChar, multibox);
		}
		else if (command.equals("admin_edit_character"))
			editCharacter(activeChar);
		// Karma control commands
		else if (command.equals("admin_nokarma"))
			setTargetKarma(activeChar, 0);
		else if (command.startsWith("admin_setkarma"))
		{
			try
			{
				String val = command.substring(15);
				int karma = Integer.parseInt(val);
				setTargetKarma(activeChar, karma);
			}
			catch (Exception e)
			{
				if (MainConfig.DEVELOPER)
					_log.warn("Set karma error: " + e);
				activeChar.sendMessage("Usage: //setkarma <new_karma_value>");
			}
		}
		else if (command.startsWith("admin_rec"))
		{
			try
			{
				String val = command.substring(10);
				int recVal = Integer.parseInt(val);
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				player.setRecomHave(recVal);
				player.sendMessage("You have been recommended by a GM");
				player.broadcastUserInfo();
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //rec number");
			}
		}
		else if (command.startsWith("admin_setclass"))
		{
			try
			{
				String val = command.substring(15);
				int classidval = Integer.parseInt(val);
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				boolean valid = false;
				for (ClassId classid : ClassId.values())
					if (classidval == classid.getId())
						valid = true;

				if (valid && (player.getClassId().getId() != classidval))
				{
					player.setClassId(classidval);
					if (!player.isSubClassActive())
						player.setBaseClass(classidval);

					String newclass = player.getTemplate().className;

					player.store();
					player.broadcastUserInfo();

					// Messages
					if (player != activeChar)
						player.sendMessage("A GM changed your class to " + newclass + ".");
					activeChar.sendMessage(player.getName() + " is now a " + newclass + ".");
				}
				else
					activeChar.sendMessage("Usage: //setclass <valid classid>");
			}
			catch (StringIndexOutOfBoundsException e)
			{
				AdminHelpPage.showHelpPage(activeChar, "charclasses.htm");
			}
		}
		else if (command.startsWith("admin_settitle"))
		{
			try
			{
				String val = command.substring(15);

				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				player.setTitle(val);
				player.sendMessage("Your title has been changed by a GM.");
				player.broadcastUserInfo();
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Case of empty character title
				activeChar.sendMessage("Usage: //settitle title");
			}
		}
		else if (command.startsWith("admin_setname"))
		{
			try
			{
				String val = command.substring(14);
				if (!Util.isValidPlayerName(val))
				{
					activeChar.sendMessage("The new name doesn't fit with the regex pattern.");
					return false;
				}

				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				player.setName(val);
				player.sendMessage("Your name has been changed by a GM.");
				player.broadcastUserInfo();
				player.store();
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Case of empty character name
				activeChar.sendMessage("Usage: //setname name");
			}
		}
		else if (command.startsWith("admin_setsex"))
		{
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;

			if (target instanceof L2PcInstance)
				player = (L2PcInstance) target;
			else
				return false;

			player.getAppearance().setSex(player.getAppearance().getSex() ? false : true);
			player.sendMessage("Your gender has been changed by a GM");
			player.broadcastUserInfo();
			player.decayMe();
			player.spawnMe(player.getX(), player.getY(), player.getZ());
		}
		else if (command.startsWith("admin_setcolor"))
		{
			try
			{
				String val = command.substring(15);
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				player.getAppearance().setNameColor(Integer.decode("0x" + val));
				player.sendMessage("Your name color has been changed by a GM.");
				player.broadcastUserInfo();
			}
			catch (Exception e)
			{
				// Case of empty color or invalid hex string
				activeChar.sendMessage("You need to specify a valid new color.");
			}
		}
		else if (command.startsWith("admin_settcolor"))
		{
			try
			{
				String val = command.substring(16);
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				player.getAppearance().setTitleColor(Integer.decode("0x" + val));
				player.sendMessage("Your title color has been changed by a GM.");
				player.broadcastUserInfo();
			}
			catch (Exception e)
			{
				// Case of empty color or invalid hex string
				activeChar.sendMessage("You need to specify a valid new color.");
			}
		}
		else if (command.startsWith("admin_summon_info"))
		{
			L2Object target = activeChar.getTarget();
			if (target instanceof L2Summon)
				gatherSummonInfo((L2Summon) target, activeChar);
			// Allow to target a player to find his pet - target the pet then.
			else if (target instanceof L2PcInstance)
			{
				L2Summon pet = ((L2PcInstance) target).getPet();
				if (pet != null)
				{
					gatherSummonInfo(pet, activeChar);
					activeChar.setTarget(pet);
				}
				else
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			}
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
		else if (command.startsWith("admin_unsummon"))
		{
			L2Object target = activeChar.getTarget();
			if (target instanceof L2Summon)
				((L2Summon) target).unSummon(((L2Summon) target).getOwner());
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
		else if (command.startsWith("admin_summon_setlvl"))
		{
			L2Object target = activeChar.getTarget();
			if (target instanceof L2PetInstance)
			{
				L2PetInstance pet = (L2PetInstance) target;
				try
				{
					String val = command.substring(20);
					int level = Integer.parseInt(val);
					long newexp, oldexp = 0;
					oldexp = pet.getStat().getExp();
					newexp = pet.getStat().getExpForLevel(level);
					if (oldexp > newexp)
						pet.getStat().removeExp(oldexp - newexp);
					else if (oldexp < newexp)
						pet.getStat().addExp(newexp - oldexp);
				}
				catch (Exception e)
				{
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
		else if (command.startsWith("admin_show_pet_inv"))
		{
			String val;
			int objId;
			L2Object target;
			try
			{
				val = command.substring(19);
				objId = Integer.parseInt(val);
				target = L2World.getInstance().getPet(objId);
			}
			catch (Exception e)
			{
				target = activeChar.getTarget();
			}

			if (target instanceof L2PetInstance)
				activeChar.sendMessage("Currently undone."); // FIXME activeChar.sendPacket(new GMViewItemList((L2PetInstance)
																// target));
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);

		}
		else if (command.startsWith("admin_fullfood"))
		{
			L2Object target = activeChar.getTarget();
			if (target instanceof L2PetInstance)
			{
				L2PetInstance targetPet = (L2PetInstance) target;
				targetPet.setCurrentFed(targetPet.getMaxFed());
			}
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
		else if (command.startsWith("admin_party_info"))
		{
			String val;
			L2Object target;
			try
			{
				val = command.substring(17);
				target = L2World.getInstance().getPlayer(val);
				if (target == null)
					target = activeChar.getTarget();
			}
			catch (Exception e)
			{
				target = activeChar.getTarget();
			}

			if (target instanceof L2PcInstance)
			{
				if (((L2PcInstance) target).isInParty())
					gatherPartyInfo((L2PcInstance) target, activeChar);
				else
					activeChar.sendMessage(((L2PcInstance) target).getName() + " isn't in a party.");
			}
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
		else if (command.startsWith("admin_clan_info"))
		{
			String val;
			L2PcInstance player;
			try
			{
				val = command.substring(16);
				player = L2World.getInstance().getPlayer(val);
				if (player != null)
				{
					L2Clan clan = player.getClan();
					if (clan != null)
					{
						try
						{
							NpcHtmlMessage msg = new NpcHtmlMessage(0);
							String htm = HtmCache.getInstance().getHtm(StaticHtmPath.AdminHtmPath + "claninfo.htm");
							msg.setHtml(htm.toString());
							msg.replace("%clan_name%", clan.getName());
							msg.replace("%clan_leader%", clan.getLeaderName());
							msg.replace("%clan_level%", String.valueOf(clan.getLevel()));
							msg.replace("%clan_has_castle%", clan.hasCastle() ? CastleManager.getInstance().getCastleById(clan.getCastleId()).getName() : "No");
							msg.replace("%clan_has_clanhall%", clan.hasHideout() ? ClanHallManager.getInstance().getClanHallById(clan.getHideoutId()).getName() : "No");
							msg.replace("%clan_points%", String.valueOf(clan.getReputationScore()));
							msg.replace("%clan_players_count%", String.valueOf(clan.getMembersCount()));
							msg.replace("%clan_ally%", clan.getAllyId() > 0 ? clan.getAllyName() : "Not in ally");
							activeChar.sendPacket(msg);
						}
						catch (NullPointerException npe)
						{
							npe.printStackTrace();
						}
					}
					else
					{
						activeChar.sendMessage("This player isn't in a clan.");
						return false;
					}
				}
				else
				{
					activeChar.sendMessage("Player is offline.");
					return false;
				}
			}
			catch (NumberFormatException nfe)
			{
				activeChar.sendMessage("This shouldn't happening.");
				return false;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (command.startsWith("admin_remove_clan_penalty"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				if (st.countTokens() != 3)
				{
					activeChar.sendMessage("Usage: //remove_clan_penalty join|create charname");
					return false;
				}

				st.nextToken();

				boolean changeCreateExpiryTime = st.nextToken().equalsIgnoreCase("create");

				String playerName = st.nextToken();
				L2PcInstance player = null;
				player = L2World.getInstance().getPlayer(playerName);

				if (player == null)
				{
					Connection con = DatabaseFactory.getConnection();
					PreparedStatement ps = con.prepareStatement("UPDATE characters SET " + (changeCreateExpiryTime ? "clan_create_expiry_time" : "clan_join_expiry_time") + " WHERE char_name=? LIMIT 1");

					ps.setString(1, playerName);
					ps.execute();
				}
				else
				{
					// removing penalty
					if (changeCreateExpiryTime)
						player.setClanCreateExpiryTime(0);
					else
						player.setClanJoinExpiryTime(0);
				}
				activeChar.sendMessage("Clan penalty is successfully removed for " + playerName + ".");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private static void listCharacters(L2PcInstance activeChar, int page)
	{
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		int MaxCharactersPerPage = 20;
		int MaxPages = players.length / MaxCharactersPerPage;

		if (players.length > MaxCharactersPerPage * MaxPages)
			MaxPages++;

		// Check if number of users changed
		if (page > MaxPages)
			page = MaxPages;

		int CharactersStart = MaxCharactersPerPage * page;
		int CharactersEnd = players.length;
		if (CharactersEnd - CharactersStart > MaxCharactersPerPage)
			CharactersEnd = CharactersStart + MaxCharactersPerPage;

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "charlist.htm");
		TextBuilder replyMSG = new TextBuilder();
		for (int x = 0; x < MaxPages; x++)
		{
			int pagenr = x + 1;
			replyMSG.append("<center><a action=\"bypass -h admin_show_characters " + x + "\">Page " + pagenr + "</a></center>");
		}
		adminReply.replace("%pages%", replyMSG.toString());
		replyMSG.clear();
		for (int i = CharactersStart; i < CharactersEnd; i++)
		{ // Add player info into new Table row
			replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_info " + players[i].getName() + "\">" + players[i].getName() + "</a></td><td width=110>" + players[i].getTemplate().className + "</td><td width=40>" + players[i].getLevel() + "</td></tr>");
		}
		adminReply.replace("%players%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private static void showCharacterInfo(L2PcInstance activeChar, L2PcInstance player)
	{
		if (player == null)
		{
			L2Object target = activeChar.getTarget();
			if (target instanceof L2PcInstance)
				player = (L2PcInstance) target;
			else
				return;
		}
		else
			activeChar.setTarget(player);
		gatherCharacterInfo(activeChar, player, "charinfo.htm");
	}

	/**
	 * Gather character informations.
	 *
	 * @param activeChar
	 *            The player who requested that action.
	 * @param player
	 *            The target to gather informations from.
	 * @param filename
	 *            The name of the HTM to send.
	 */
	private static void gatherCharacterInfo(L2PcInstance activeChar, L2PcInstance player, String filename)
	{
		String ip = "N/A";
		String account = "N/A";
		try
		{
			String clientInfo = player.getClient().toString();
			account = clientInfo.substring(clientInfo.indexOf("Account: ") + 9, clientInfo.indexOf(" - IP: "));
			ip = clientInfo.substring(clientInfo.indexOf(" - IP: ") + 7, clientInfo.lastIndexOf("]"));
		}
		catch (Exception e)
		{
			_log.warn(e.getLocalizedMessage(), e);
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + filename);
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%clan%", String.valueOf(player.getClan() != null ? "<a action=\"bypass -h admin_clan_info " + player.getName() + "\">" + player.getClan().getName() + "</a>" : "none"));
		adminReply.replace("%xp%", String.valueOf(player.getExp()));
		adminReply.replace("%sp%", String.valueOf(player.getSp()));
		adminReply.replace("%class%", player.getTemplate().className);
		adminReply.replace("%ordinal%", String.valueOf(player.getClassId().ordinal()));
		adminReply.replace("%classid%", String.valueOf(player.getClassId()));
		adminReply.replace("%baseclass%", CharTemplateData.getInstance().getClassNameById(player.getBaseClass()));
		adminReply.replace("%x%", String.valueOf(player.getX()));
		adminReply.replace("%y%", String.valueOf(player.getY()));
		adminReply.replace("%z%", String.valueOf(player.getZ()));
		adminReply.replace("%currenthp%", String.valueOf((int) player.getCurrentHp()));
		adminReply.replace("%maxhp%", String.valueOf(player.getMaxHp()));
		adminReply.replace("%karma%", String.valueOf(player.getKarma()));
		adminReply.replace("%currentmp%", String.valueOf((int) player.getCurrentMp()));
		adminReply.replace("%maxmp%", String.valueOf(player.getMaxMp()));
		adminReply.replace("%pvpflag%", String.valueOf(player.getPvpFlag()));
		adminReply.replace("%currentcp%", String.valueOf((int) player.getCurrentCp()));
		adminReply.replace("%maxcp%", String.valueOf(player.getMaxCp()));
		adminReply.replace("%pvpkills%", String.valueOf(player.getPvpKills()));
		adminReply.replace("%pkkills%", String.valueOf(player.getPkKills()));
		adminReply.replace("%currentload%", String.valueOf(player.getCurrentLoad()));
		adminReply.replace("%maxload%", String.valueOf(player.getMaxLoad()));
		adminReply.replace("%percent%", String.valueOf(Util.roundTo(((float) player.getCurrentLoad() / (float) player.getMaxLoad()) * 100, 2)));
		adminReply.replace("%patk%", String.valueOf(player.getPAtk(null)));
		adminReply.replace("%matk%", String.valueOf(player.getMAtk(null, null)));
		adminReply.replace("%pdef%", String.valueOf(player.getPDef(null)));
		adminReply.replace("%mdef%", String.valueOf(player.getMDef(null, null)));
		adminReply.replace("%accuracy%", String.valueOf(player.getAccuracy()));
		adminReply.replace("%evasion%", String.valueOf(player.getEvasionRate(null)));
		adminReply.replace("%critical%", String.valueOf(player.getCriticalHit(null, null)));
		adminReply.replace("%runspeed%", String.valueOf(player.getRunSpeed()));
		adminReply.replace("%patkspd%", String.valueOf(player.getPAtkSpd()));
		adminReply.replace("%matkspd%", String.valueOf(player.getMAtkSpd()));
		adminReply.replace("%account%", account);
		adminReply.replace("%ip%", ip);
		adminReply.replace("%ai%", String.valueOf(player.getAI().getIntention().name()));
		activeChar.sendPacket(adminReply);
	}

	private static void setTargetKarma(L2PcInstance activeChar, int newKarma)
	{
		// function to change karma of selected char
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else
			return;

		if (newKarma >= 0)
		{
			// for display
			int oldKarma = player.getKarma();
			// update karma
			player.setKarma(newKarma);
			// Admin information
			activeChar.sendMessage("You changed " + player.getName() + "'s karma from " + oldKarma + " to " + newKarma + ".");

			_log.debug("[SET KARMA] [GM]" + activeChar.getName() + " changed " + player.getName() + "'s karma from " +
					oldKarma + " to " + newKarma + ".");
		}
		else
		{
			// tell admin of mistake
			activeChar.sendMessage("The karma value must be greater or equal to 0.");

			_log.debug("[SET KARMA] ERROR: [GM]" + activeChar.getName() + " entered an incorrect value for new karma: " +
					newKarma + " for " + player.getName() + ".");
		}
	}

	private static void editCharacter(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		if (!(target instanceof L2PcInstance))
			return;

		L2PcInstance player = (L2PcInstance) target;
		gatherCharacterInfo(activeChar, player, "charedit.htm");
	}

	/**
	 * @param activeChar
	 * @param CharacterToFind
	 */
	private static void findCharacter(L2PcInstance activeChar, String CharacterToFind)
	{
		int CharactersFound = 0;
		String name;

		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "charfind.htm");
		TextBuilder replyMSG = new TextBuilder();
		for (L2PcInstance player : players)
		{ // Add player info into new Table row
			name = player.getName();
			if (name.toLowerCase().contains(CharacterToFind.toLowerCase()))
			{
				CharactersFound = CharactersFound + 1;
				replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_list " + name + "\">" + name + "</a></td><td width=110>" + player.getTemplate().className + "</td><td width=40>" + player.getLevel() + "</td></tr>");
			}
			if (CharactersFound > 20)
				break;
		}
		adminReply.replace("%results%", replyMSG.toString());
		replyMSG.clear();
		if (CharactersFound == 0)
			replyMSG.append("s. Please try again.");
		else if (CharactersFound > 20)
		{
			adminReply.replace("%number%", " more than 20");
			replyMSG.append("s.<br>Please refine your search to see all of the results.");
		}
		else if (CharactersFound == 1)
			replyMSG.append(".");
		else
			replyMSG.append("s.");
		adminReply.replace("%number%", String.valueOf(CharactersFound));
		adminReply.replace("%end%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	/**
	 * @param activeChar
	 * @param IpAdress
	 * @throws IllegalArgumentException
	 */
	private static void findCharactersPerIp(L2PcInstance activeChar, String IpAdress) throws IllegalArgumentException
	{
		boolean findDisconnected = false;

		if (IpAdress.equals("disconnected"))
			findDisconnected = true;
		else
		{
			if (!IpAdress.matches("^(?:(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))$"))
				throw new IllegalArgumentException("Malformed IPv4 number");
		}

		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		int CharactersFound = 0;
		L2GameClient client;
		String name, ip = "0.0.0.0";
		final StringBuilder replyMSG = new StringBuilder(1000);
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "ipfind.htm");

		for (L2PcInstance player : players)
		{
			client = player.getClient();
			if (client.isDetached())
			{
				if (!findDisconnected)
					continue;
			}
			else
			{
				if (findDisconnected)
					continue;

				ip = client.getConnection().getInetAddress().getHostAddress();
				if (!ip.equals(IpAdress))
					continue;
			}

			name = player.getName();
			CharactersFound = CharactersFound + 1;
			StringUtil.append(replyMSG, "<tr><td width=80><a action=\"bypass -h admin_character_list ", name, "\">", name, "</a></td><td width=110>", player.getTemplate().className, "</td><td width=40>", String.valueOf(player.getLevel()), "</td></tr>");

			if (CharactersFound > 20)
				break;
		}
		adminReply.replace("%results%", replyMSG.toString());

		final String replyMSG2;

		if (CharactersFound == 0)
			replyMSG2 = "s. Maybe they got d/c? :)";
		else if (CharactersFound > 20)
		{
			adminReply.replace("%number%", " more than " + String.valueOf(CharactersFound));
			replyMSG2 = "s.<br>In order to avoid you a client crash I won't <br1>display results beyond the 20th character.";
		}
		else if (CharactersFound == 1)
			replyMSG2 = ".";
		else
			replyMSG2 = "s.";

		adminReply.replace("%ip%", IpAdress);
		adminReply.replace("%number%", String.valueOf(CharactersFound));
		adminReply.replace("%end%", replyMSG2);
		activeChar.sendPacket(adminReply);
	}

	/**
	 * @param activeChar
	 * @param characterName
	 * @throws IllegalArgumentException
	 */
	private static void findCharactersPerAccount(L2PcInstance activeChar, String characterName) throws IllegalArgumentException
	{
		if (Util.isValidPlayerName(characterName))
		{
			String account = null;
			Map<Integer, String> chars;

			L2PcInstance player = L2World.getInstance().getPlayer(characterName);
			if (player == null)
				throw new IllegalArgumentException("Player doesn't exist.");

			chars = player.getAccountChars();
			account = player.getAccountName();
			TextBuilder replyMSG = new TextBuilder();
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

			adminReply.setFile(StaticHtmPath.AdminHtmPath + "accountinfo.htm");

			for (String charname : chars.values())
				replyMSG.append(charname + "<br1>");

			adminReply.replace("%characters%", replyMSG.toString());
			adminReply.replace("%account%", account);
			adminReply.replace("%player%", characterName);

			activeChar.sendPacket(adminReply);
		}
		else
			throw new IllegalArgumentException("Malformed character name.");
	}

	/**
	 * @param activeChar
	 * @param multibox
	 */
	private static void findDualbox(L2PcInstance activeChar, int multibox)
	{
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		Map<String, List<L2PcInstance>> ipMap = new HashMap<>();

		String ip = "0.0.0.0";
		L2GameClient client;

		final Map<String, Integer> dualboxIPs = new HashMap<>();

		for (L2PcInstance player : players)
		{
			client = player.getClient();
			if (client == null || client.isDetached())
				continue;

			ip = client.getConnection().getInetAddress().getHostAddress();
			if (ipMap.get(ip) == null)
				ipMap.put(ip, new ArrayList<L2PcInstance>());
			ipMap.get(ip).add(player);

			if (ipMap.get(ip).size() >= multibox)
			{
				Integer count = dualboxIPs.get(ip);
				if (count == null)
					dualboxIPs.put(ip, multibox);
				else
					dualboxIPs.put(ip, count + 1);
			}
		}

		List<String> keys = new ArrayList<>(dualboxIPs.keySet());
		Collections.sort(keys, new Comparator<String>()
		{
			@Override
			public int compare(String left, String right)
			{
				return dualboxIPs.get(left).compareTo(dualboxIPs.get(right));
			}
		});
		Collections.reverse(keys);

		final StringBuilder results = new StringBuilder();
		for (String dualboxIP : keys)
			StringUtil.append(results, "<a action=\"bypass -h admin_find_ip " + dualboxIP + "\">" + dualboxIP + " (" + dualboxIPs.get(dualboxIP) + ")</a><br1>");

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "dualbox.htm");
		adminReply.replace("%multibox%", String.valueOf(multibox));
		adminReply.replace("%results%", results.toString());
		adminReply.replace("%strict%", "");
		activeChar.sendPacket(adminReply);
	}

	private static void gatherSummonInfo(L2Summon target, L2PcInstance activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(StaticHtmPath.AdminHtmPath + "petinfo.htm");
		String name = target.getName();
		html.replace("%name%", name == null ? "N/A" : name);
		html.replace("%level%", Integer.toString(target.getLevel()));
		html.replace("%exp%", Long.toString(target.getStat().getExp()));
		String owner = target.getActingPlayer().getName();
		html.replace("%owner%", " <a action=\"bypass -h admin_character_info " + owner + "\">" + owner + "</a>");
		html.replace("%class%", target.getClass().getSimpleName());
		html.replace("%ai%", target.hasAI() ? String.valueOf(target.getAI().getIntention().name()) : "NULL");
		html.replace("%hp%", (int) target.getStatus().getCurrentHp() + "/" + target.getStat().getMaxHp());
		html.replace("%mp%", (int) target.getStatus().getCurrentMp() + "/" + target.getStat().getMaxMp());
		html.replace("%karma%", Integer.toString(target.getKarma()));
		html.replace("%undead%", target.isUndead() ? "yes" : "no");

		if (target instanceof L2PetInstance)
		{
			int objId = target.getActingPlayer().getObjectId();
			html.replace("%inv%", " <a action=\"bypass admin_show_pet_inv " + objId + "\">view</a>");
		}
		else
			html.replace("%inv%", "none");

		if (target instanceof L2PetInstance)
		{
			html.replace("%food%", ((L2PetInstance) target).getCurrentFed() + "/" + ((L2PetInstance) target).getPetLevelData().getPetMaxFeed());
			html.replace("%load%", ((L2PetInstance) target).getInventory().getTotalWeight() + "/" + ((L2PetInstance) target).getMaxLoad());
		}
		else
		{
			html.replace("%food%", "N/A");
			html.replace("%load%", "N/A");
		}
		activeChar.sendPacket(html);
	}

	private static void gatherPartyInfo(L2PcInstance target, L2PcInstance activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(StaticHtmPath.AdminHtmPath + "partyinfo.htm");
		StringBuilder text = new StringBuilder(400);
		for (L2PcInstance member : target.getParty().getPartyMembers())
		{
			if (member.getParty().getPartyLeaderOID() != member.getObjectId())
			{
				text.append("<tr><td><table width=270 border=0 cellpadding=2><tr><td width=30 align=right>");
				text.append(member.getLevel() + "</td><td width=130><a action=\"bypass -h admin_character_info " + member.getName() + "\">" + member.getName() + "</a>");
				text.append("</td><td width=110 align=right>" + member.getClassId().toString() + "</td></tr></table></td></tr>");
			}
			else
			{
				text.append("<tr><td><table width=270 border=0 cellpadding=2><tr><td width=30 align=right><font color=\"LEVEL\">");
				text.append(member.getLevel() + "</td><td width=130><a action=\"bypass -h admin_character_info " + member.getName() + "\">" + member.getName() + " (Party leader)</a>");
				text.append("</td><td width=110 align=right>" + member.getClassId().toString() + "</font></td></tr></table></td></tr>");
			}
		}
		html.replace("%party%", text.toString());
		activeChar.sendPacket(html);
	}
}
