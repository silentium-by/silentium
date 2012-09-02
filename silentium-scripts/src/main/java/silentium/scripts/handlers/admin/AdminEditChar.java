/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
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
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.utils.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

public class AdminEditChar implements IAdminCommandHandler {
	private static final Logger _log = LoggerFactory.getLogger(AdminEditChar.class.getName());

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
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_current_player".equals(command))
			showCharacterInfo(activeChar, null);
		else if (command.startsWith("admin_character_list") || command.startsWith("admin_character_info")) {
			try {
				final String val = command.substring(21);
				final L2PcInstance target = L2World.getInstance().getPlayer(val);
				if (target != null)
					showCharacterInfo(activeChar, target);
				else
					activeChar.sendPacket(SystemMessageId.CHARACTER_DOES_NOT_EXIST);
			} catch (StringIndexOutOfBoundsException e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //character_info <player_name>");
			}
		} else if (command.startsWith("admin_show_characters")) {
			try {
				final String val = command.substring(22);
				final int page = Integer.parseInt(val);
				listCharacters(activeChar, page);
			} catch (StringIndexOutOfBoundsException e) {
				// Case of empty page number
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //show_characters <page_number>");
			}
		} else if (command.startsWith("admin_find_character")) {
			try {
				final String val = command.substring(21);
				findCharacter(activeChar, val);
			} catch (StringIndexOutOfBoundsException e) { // Case of empty character name
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //find_character <character_name>");
				listCharacters(activeChar, 0);
			}
		} else if (command.startsWith("admin_find_ip")) {
			try {
				final String val = command.substring(14);
				findCharactersPerIp(activeChar, val);
			} catch (Exception e) {
				// Case of empty or malformed IP number
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //find_ip <www.xxx.yyy.zzz>");
				listCharacters(activeChar, 0);
			}
		} else if (command.startsWith("admin_find_account")) {
			try {
				final String val = command.substring(19);
				findCharactersPerAccount(activeChar, val);
			} catch (Exception e) {
				// Case of empty or malformed player name
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //find_account <player_name>");
				listCharacters(activeChar, 0);
			}
		} else if (command.startsWith("admin_find_dualbox")) {
			int multibox = 2;
			try {
				final String val = command.substring(19);
				multibox = Integer.parseInt(val);
				if (multibox < 1) {
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //find_dualbox [number > 0]");
					return false;
				}
			} catch (Exception e) {
			}
			findDualbox(activeChar, multibox);
		} else if ("admin_edit_character".equals(command))
			editCharacter(activeChar);
			// Karma control commands
		else if ("admin_nokarma".equals(command))
			setTargetKarma(activeChar, 0);
		else if (command.startsWith("admin_setkarma")) {
			try {
				final String val = command.substring(15);
				final int karma = Integer.parseInt(val);
				setTargetKarma(activeChar, karma);
			} catch (Exception e) {
				if (MainConfig.DEVELOPER)
					_log.warn("Set karma error: " + e);
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //setkarma <new_karma_value>");
			}
		} else if (command.startsWith("admin_rec")) {
			try {
				final String val = command.substring(10);
				final int recVal = Integer.parseInt(val);
				final L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				player.setRecomHave(recVal);
				player.sendMessage("You have been recommended by a GM");
				player.broadcastUserInfo();
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //rec number");
			}
		} else if (command.startsWith("admin_setclass")) {
			try {
				final String val = command.substring(15);
				final int classidval = Integer.parseInt(val);
				final L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				boolean valid = false;
				for (final ClassId classid : ClassId.values())
					if (classidval == classid.getId())
						valid = true;

				if (valid && player.getClassId().getId() != classidval) {
					player.setClassId(classidval);
					if (!player.isSubClassActive())
						player.setBaseClass(classidval);

					final String newclass = player.getTemplate().className;

					player.store();
					player.broadcastUserInfo();

					// Messages
					if (player != activeChar)
						player.sendMessage("A GM changed your class to " + newclass + '.');
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", player.getName() + " is now a " + newclass + '.');
				} else
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //setclass <valid classid>");
			} catch (StringIndexOutOfBoundsException e) {
				AdminHelpPage.showHelpPage(activeChar, "charclasses.htm");
			}
		} else if (command.startsWith("admin_settitle")) {
			try {
				final String val = command.substring(15);

				final L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				player.setTitle(val);
				player.sendMessage("Your title has been changed by a GM.");
				player.broadcastUserInfo();
			} catch (StringIndexOutOfBoundsException e) {
				// Case of empty character title
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //settitle title");
			}
		} else if (command.startsWith("admin_setname")) {
			try {
				final String val = command.substring(14);
				if (!Util.isValidPlayerName(val)) {
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "The new name doesn't fit with the regex pattern.");
					return false;
				}

				final L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				player.setName(val);
				player.sendMessage("Your name has been changed by a GM.");
				player.broadcastUserInfo();
				player.store();
			} catch (StringIndexOutOfBoundsException e) {
				// Case of empty character name
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //setname name");
			}
		} else if (command.startsWith("admin_setsex")) {
			final L2Object target = activeChar.getTarget();
			L2PcInstance player = null;

			if (target instanceof L2PcInstance)
				player = (L2PcInstance) target;
			else
				return false;

			player.getAppearance().setSex(!player.getAppearance().getSex());
			player.sendMessage("Your gender has been changed by a GM");
			player.broadcastUserInfo();
			player.decayMe();
			player.spawnMe(player.getX(), player.getY(), player.getZ());
		} else if (command.startsWith("admin_setcolor")) {
			try {
				final String val = command.substring(15);
				final L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				player.getAppearance().setNameColor(Integer.decode("0x" + val));
				player.sendMessage("Your name color has been changed by a GM.");
				player.broadcastUserInfo();
			} catch (Exception e) {
				// Case of empty color or invalid hex string
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "You need to specify a valid new color.");
			}
		} else if (command.startsWith("admin_settcolor")) {
			try {
				final String val = command.substring(16);
				final L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;

				player.getAppearance().setTitleColor(Integer.decode("0x" + val));
				player.sendMessage("Your title color has been changed by a GM.");
				player.broadcastUserInfo();
			} catch (Exception e) {
				// Case of empty color or invalid hex string
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "You need to specify a valid new color.");
			}
		} else if (command.startsWith("admin_summon_info")) {
			final L2Object target = activeChar.getTarget();
			if (target instanceof L2Summon)
				gatherSummonInfo((L2Summon) target, activeChar);
				// Allow to target a player to find his pet - target the pet then.
			else if (target instanceof L2PcInstance) {
				final L2Summon pet = ((L2PcInstance) target).getPet();
				if (pet != null) {
					gatherSummonInfo(pet, activeChar);
					activeChar.setTarget(pet);
				} else
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			} else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		} else if (command.startsWith("admin_unsummon")) {
			final L2Object target = activeChar.getTarget();
			if (target instanceof L2Summon)
				((L2Summon) target).unSummon(((L2Summon) target).getOwner());
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		} else if (command.startsWith("admin_summon_setlvl")) {
			final L2Object target = activeChar.getTarget();
			if (target instanceof L2PetInstance) {
				final L2PetInstance pet = (L2PetInstance) target;
				try {
					final String val = command.substring(20);
					final int level = Integer.parseInt(val);
					final long newexp;
					long oldexp = 0;
					oldexp = pet.getStat().getExp();
					newexp = pet.getStat().getExpForLevel(level);
					if (oldexp > newexp)
						pet.getStat().removeExp(oldexp - newexp);
					else if (oldexp < newexp)
						pet.getStat().addExp(newexp - oldexp);
				} catch (Exception e) {
				}
			} else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		} else if (command.startsWith("admin_show_pet_inv")) {
			final String val;
			final int objId;
			L2Object target;
			try {
				val = command.substring(19);
				objId = Integer.parseInt(val);
				target = L2World.getInstance().getPet(objId);
			} catch (Exception e) {
				target = activeChar.getTarget();
			}

			if (target instanceof L2PetInstance)
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Currently undone."); // FIXME activeChar.sendPacket(new GMViewItemList((L2PetInstance)
				// target));
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);

		} else if (command.startsWith("admin_fullfood")) {
			final L2Object target = activeChar.getTarget();
			if (target instanceof L2PetInstance) {
				final L2PetInstance targetPet = (L2PetInstance) target;
				targetPet.setCurrentFed(targetPet.getMaxFed());
			} else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		} else if (command.startsWith("admin_party_info")) {
			final String val;
			L2Object target;
			try {
				val = command.substring(17);
				target = L2World.getInstance().getPlayer(val);
				if (target == null)
					target = activeChar.getTarget();
			} catch (Exception e) {
				target = activeChar.getTarget();
			}

			if (target instanceof L2PcInstance) {
				if (((L2PcInstance) target).isInParty())
					gatherPartyInfo((L2PcInstance) target, activeChar);
				else
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", target.getName() + " isn't in a party.");
			} else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		} else if (command.startsWith("admin_clan_info")) {
			final String val;
			final L2PcInstance player;
			try {
				val = command.substring(16);
				player = L2World.getInstance().getPlayer(val);
				if (player != null) {
					final L2Clan clan = player.getClan();
					if (clan != null) {
						try {
							final NpcHtmlMessage msg = new NpcHtmlMessage(0);
							final String htm = HtmCache.getInstance().getHtm(StaticHtmPath.AdminHtmPath + "claninfo.htm");
							msg.setHtml(htm);
							msg.replace("%clan_name%", clan.getName());
							msg.replace("%clan_leader%", clan.getLeaderName());
							msg.replace("%clan_level%", String.valueOf(clan.getLevel()));
							msg.replace("%clan_has_castle%", clan.hasCastle() ? CastleManager.getInstance().getCastleById(clan.getCastleId()).getName() : "No");
							msg.replace("%clan_has_clanhall%", clan.hasHideout() ? ClanHallManager.getInstance().getClanHallById(clan.getHideoutId()).getName() : "No");
							msg.replace("%clan_points%", String.valueOf(clan.getReputationScore()));
							msg.replace("%clan_players_count%", String.valueOf(clan.getMembersCount()));
							msg.replace("%clan_ally%", clan.getAllyId() > 0 ? clan.getAllyName() : "Not in ally");
							activeChar.sendPacket(msg);
						} catch (NullPointerException npe) {
							npe.printStackTrace();
						}
					} else {
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "This player isn't in a clan.");
						return false;
					}
				} else {
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Player is offline.");
					return false;
				}
			} catch (NumberFormatException nfe) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "This shouldn't happening.");
				return false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (command.startsWith("admin_remove_clan_penalty")) {
			try {
				final StringTokenizer st = new StringTokenizer(command, " ");
				if (st.countTokens() != 3) {
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //remove_clan_penalty join|create charname");
					return false;
				}

				st.nextToken();

				final boolean changeCreateExpiryTime = "create".equalsIgnoreCase(st.nextToken());

				final String playerName = st.nextToken();
				L2PcInstance player = null;
				player = L2World.getInstance().getPlayer(playerName);

				if (player == null) {
					final Connection con = DatabaseFactory.getConnection();
					final PreparedStatement ps = con.prepareStatement("UPDATE characters SET " + (changeCreateExpiryTime ? "clan_create_expiry_time" : "clan_join_expiry_time") + " WHERE char_name=? LIMIT 1");

					ps.setString(1, playerName);
					ps.execute();
				} else {
					// removing penalty
					if (changeCreateExpiryTime)
						player.setClanCreateExpiryTime(0);
					else
						player.setClanJoinExpiryTime(0);
				}
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Clan penalty is successfully removed for " + playerName + '.');
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private static void listCharacters(final L2PcInstance activeChar, int page) {
		final Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		final L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		final int MaxCharactersPerPage = 20;
		int MaxPages = players.length / MaxCharactersPerPage;

		if (players.length > MaxCharactersPerPage * MaxPages)
			MaxPages++;

		// Check if number of users changed
		if (page > MaxPages)
			page = MaxPages;

		final int CharactersStart = MaxCharactersPerPage * page;
		int CharactersEnd = players.length;
		if (CharactersEnd - CharactersStart > MaxCharactersPerPage)
			CharactersEnd = CharactersStart + MaxCharactersPerPage;

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "charlist.htm", activeChar);
		final TextBuilder replyMSG = new TextBuilder();
		for (int x = 0; x < MaxPages; x++) {
			final int pagenr = x + 1;
			replyMSG.append("<center><a action=\"bypass -h admin_show_characters ").append(x).append("\">Page ").append(pagenr).append("</a></center>");
		}
		adminReply.replace("%pages%", replyMSG.toString());
		replyMSG.clear();
		for (int i = CharactersStart; i < CharactersEnd; i++) { // Add player info into new Table row
			replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_info ").append(players[i].getName()).append("\">").append(players[i].getName()).append("</a></td><td width=110>").append(players[i].getTemplate().className).append("</td><td width=40>").append(players[i].getLevel()).append("</td></tr>");
		}
		adminReply.replace("%players%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private static void showCharacterInfo(final L2PcInstance activeChar, L2PcInstance player) {
		if (player == null) {
			final L2Object target = activeChar.getTarget();
			if (target instanceof L2PcInstance)
				player = (L2PcInstance) target;
			else
				return;
		} else
			activeChar.setTarget(player);
		gatherCharacterInfo(activeChar, player, "charinfo.htm");
	}

	/**
	 * Gather character informations.
	 *
	 * @param activeChar The player who requested that action.
	 * @param player     The target to gather informations from.
	 * @param filename   The name of the HTM to send.
	 */
	private static void gatherCharacterInfo(final L2PcInstance activeChar, final L2PcInstance player, final String filename) {
		String ip = "N/A";
		String account = "N/A";
		try {
			final String clientInfo = player.getClient().toString();
			account = clientInfo.substring(clientInfo.indexOf("Account: ") + 9, clientInfo.indexOf(" - IP: "));
			ip = clientInfo.substring(clientInfo.indexOf(" - IP: ") + 7, clientInfo.lastIndexOf(']'));
		} catch (Exception e) {
			_log.warn(e.getLocalizedMessage(), e);
		}

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + filename, activeChar);
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
		adminReply.replace("%percent%", String.valueOf(Util.roundTo((float) player.getCurrentLoad() / (float) player.getMaxLoad() * 100, 2)));
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

	private static void setTargetKarma(final L2PcInstance activeChar, final int newKarma) {
		// function to change karma of selected char
		final L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else
			return;

		if (newKarma >= 0) {
			// for display
			final int oldKarma = player.getKarma();
			// update karma
			player.setKarma(newKarma);
			// Admin information
			activeChar.sendChatMessage(0, Say2.ALL, "SYS", "You changed " + player.getName() + "'s karma from " + oldKarma + " to " + newKarma + '.');

			_log.debug("[SET KARMA] [GM]" + activeChar.getName() + " changed " + player.getName() + "'s karma from " + oldKarma + " to " + newKarma + '.');
		} else {
			// tell admin of mistake
			activeChar.sendChatMessage(0, Say2.ALL, "SYS", "The karma value must be greater or equal to 0.");

			_log.debug("[SET KARMA] ERROR: [GM]" + activeChar.getName() + " entered an incorrect value for new karma: " + newKarma + " for " + player.getName() + '.');
		}
	}

	private static void editCharacter(final L2PcInstance activeChar) {
		final L2Object target = activeChar.getTarget();
		if (!(target instanceof L2PcInstance))
			return;

		final L2PcInstance player = (L2PcInstance) target;
		gatherCharacterInfo(activeChar, player, "charedit.htm");
	}

	/**
	 * @param activeChar
	 * @param CharacterToFind
	 */
	private static void findCharacter(final L2PcInstance activeChar, final String CharacterToFind) {
		int CharactersFound = 0;
		String name;

		final Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		final L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "charfind.htm", activeChar);
		final TextBuilder replyMSG = new TextBuilder();
		for (final L2PcInstance player : players) { // Add player info into new Table row
			name = player.getName();
			if (name.toLowerCase().contains(CharacterToFind.toLowerCase())) {
				CharactersFound += 1;
				replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_list ").append(name).append("\">").append(name).append("</a></td><td width=110>").append(player.getTemplate().className).append("</td><td width=40>").append(player.getLevel()).append("</td></tr>");
			}
			if (CharactersFound > 20)
				break;
		}
		adminReply.replace("%results%", replyMSG.toString());
		replyMSG.clear();
		if (CharactersFound == 0)
			replyMSG.append("s. Please try again.");
		else if (CharactersFound > 20) {
			adminReply.replace("%number%", " more than 20");
			replyMSG.append("s.<br>Please refine your search to see all of the results.");
		} else if (CharactersFound == 1)
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
	private static void findCharactersPerIp(final L2PcInstance activeChar, final String IpAdress) throws IllegalArgumentException {
		boolean findDisconnected = false;

		if ("disconnected".equals(IpAdress))
			findDisconnected = true;
		else {
			if (!IpAdress.matches("^(?:(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))$"))
				throw new IllegalArgumentException("Malformed IPv4 number");
		}

		final Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		final L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		int CharactersFound = 0;
		L2GameClient client;
		String name, ip = "0.0.0.0";
		final StringBuilder replyMSG = new StringBuilder(1000);
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "ipfind.htm", activeChar);

		for (final L2PcInstance player : players) {
			client = player.getClient();
			if (client.isDetached()) {
				if (!findDisconnected)
					continue;
			} else {
				if (findDisconnected)
					continue;

				ip = client.getConnection().getInetAddress().getHostAddress();
				if (!ip.equals(IpAdress))
					continue;
			}

			name = player.getName();
			CharactersFound += 1;
			StringUtil.append(replyMSG, "<tr><td width=80><a action=\"bypass -h admin_character_list ", name, "\">", name, "</a></td><td width=110>", player.getTemplate().className, "</td><td width=40>", String.valueOf(player.getLevel()), "</td></tr>");

			if (CharactersFound > 20)
				break;
		}
		adminReply.replace("%results%", replyMSG.toString());

		final String replyMSG2;

		if (CharactersFound == 0)
			replyMSG2 = "s. Maybe they got d/c? :)";
		else if (CharactersFound > 20) {
			adminReply.replace("%number%", " more than " + CharactersFound);
			replyMSG2 = "s.<br>In order to avoid you a client crash I won't <br1>display results beyond the 20th character.";
		} else replyMSG2 = CharactersFound == 1 ? "." : "s.";

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
	private static void findCharactersPerAccount(final L2PcInstance activeChar, final String characterName) throws IllegalArgumentException {
		if (Util.isValidPlayerName(characterName)) {
			String account = null;
			final Map<Integer, String> chars;

			final L2PcInstance player = L2World.getInstance().getPlayer(characterName);
			if (player == null)
				throw new IllegalArgumentException("Player doesn't exist.");

			chars = player.getAccountChars();
			account = player.getAccountName();
			final TextBuilder replyMSG = new TextBuilder();
			final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

			adminReply.setFile(StaticHtmPath.AdminHtmPath + "accountinfo.htm", activeChar);

			for (final String charname : chars.values())
				replyMSG.append(charname).append("<br1>");

			adminReply.replace("%characters%", replyMSG.toString());
			adminReply.replace("%account%", account);
			adminReply.replace("%player%", characterName);

			activeChar.sendPacket(adminReply);
		} else
			throw new IllegalArgumentException("Malformed character name.");
	}

	/**
	 * @param activeChar
	 * @param multibox
	 */
	private static void findDualbox(final L2PcInstance activeChar, final int multibox) {
		final Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		final L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		final Map<String, List<L2PcInstance>> ipMap = new HashMap<>();

		String ip = "0.0.0.0";
		L2GameClient client;

		final Map<String, Integer> dualboxIPs = new HashMap<>();

		for (final L2PcInstance player : players) {
			client = player.getClient();
			if (client == null || client.isDetached())
				continue;

			ip = client.getConnection().getInetAddress().getHostAddress();
			if (ipMap.get(ip) == null)
				ipMap.put(ip, new ArrayList<L2PcInstance>());
			ipMap.get(ip).add(player);

			if (ipMap.get(ip).size() >= multibox) {
				final Integer count = dualboxIPs.get(ip);
				if (count == null)
					dualboxIPs.put(ip, multibox);
				else
					dualboxIPs.put(ip, count + 1);
			}
		}

		final List<String> keys = new ArrayList<>(dualboxIPs.keySet());
		Collections.sort(keys, new Comparator<String>() {
			@Override
			public int compare(final String left, final String right) {
				return dualboxIPs.get(left).compareTo(dualboxIPs.get(right));
			}
		});
		Collections.reverse(keys);

		final StringBuilder results = new StringBuilder();
		for (final String dualboxIP : keys)
			StringUtil.append(results, "<a action=\"bypass -h admin_find_ip " + dualboxIP + "\">" + dualboxIP + " (" + dualboxIPs.get(dualboxIP) + ")</a><br1>");

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "dualbox.htm", activeChar);
		adminReply.replace("%multibox%", String.valueOf(multibox));
		adminReply.replace("%results%", results.toString());
		adminReply.replace("%strict%", "");
		activeChar.sendPacket(adminReply);
	}

	private static void gatherSummonInfo(final L2Summon target, final L2PcInstance activeChar) {
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(StaticHtmPath.AdminHtmPath + "petinfo.htm", activeChar);
		final String name = target.getName();
		html.replace("%name%", name == null ? "N/A" : name);
		html.replace("%level%", Integer.toString(target.getLevel()));
		html.replace("%exp%", Long.toString(target.getStat().getExp()));
		final String owner = target.getActingPlayer().getName();
		html.replace("%owner%", " <a action=\"bypass -h admin_character_info " + owner + "\">" + owner + "</a>");
		html.replace("%class%", target.getClass().getSimpleName());
		html.replace("%ai%", target.hasAI() ? String.valueOf(target.getAI().getIntention().name()) : "NULL");
		html.replace("%hp%", (int) target.getStatus().getCurrentHp() + "/" + target.getStat().getMaxHp());
		html.replace("%mp%", (int) target.getStatus().getCurrentMp() + "/" + target.getStat().getMaxMp());
		html.replace("%karma%", Integer.toString(target.getKarma()));
		html.replace("%undead%", target.isUndead() ? "yes" : "no");

		if (target instanceof L2PetInstance) {
			final int objId = target.getActingPlayer().getObjectId();
			html.replace("%inv%", " <a action=\"bypass admin_show_pet_inv " + objId + "\">view</a>");
		} else
			html.replace("%inv%", "none");

		if (target instanceof L2PetInstance) {
			html.replace("%food%", ((L2PetInstance) target).getCurrentFed() + "/" + ((L2PetInstance) target).getPetLevelData().getPetMaxFeed());
			html.replace("%load%", target.getInventory().getTotalWeight() + "/" + target.getMaxLoad());
		} else {
			html.replace("%food%", "N/A");
			html.replace("%load%", "N/A");
		}
		activeChar.sendPacket(html);
	}

	private static void gatherPartyInfo(final L2PcInstance target, final L2PcInstance activeChar) {
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(StaticHtmPath.AdminHtmPath + "partyinfo.htm", activeChar);
		final StringBuilder text = new StringBuilder(400);
		for (final L2PcInstance member : target.getParty().getPartyMembers()) {
			if (member.getParty().getPartyLeaderOID() != member.getObjectId()) {
				text.append("<tr><td><table width=270 border=0 cellpadding=2><tr><td width=30 align=right>");
				text.append(member.getLevel()).append("</td><td width=130><a action=\"bypass -h admin_character_info ").append(member.getName()).append("\">").append(member.getName()).append("</a>");
				text.append("</td><td width=110 align=right>").append(member.getClassId().toString()).append("</td></tr></table></td></tr>");
			} else {
				text.append("<tr><td><table width=270 border=0 cellpadding=2><tr><td width=30 align=right><font color=\"LEVEL\">");
				text.append(member.getLevel()).append("</td><td width=130><a action=\"bypass -h admin_character_info ").append(member.getName()).append("\">").append(member.getName()).append(" (Party leader)</a>");
				text.append("</td><td width=110 align=right>").append(member.getClassId().toString()).append("</font></td></tr></table></td></tr>");
			}
		}
		html.replace("%party%", text.toString());
		activeChar.sendPacket(html);
	}
}
