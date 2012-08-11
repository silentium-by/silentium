/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.configs.EventsConfig;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.Announcements;
import silentium.gameserver.GameTimeController;
import silentium.gameserver.SevenSigns;
import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.data.xml.AdminCommandAccessRightsData;
import silentium.gameserver.data.xml.MapRegionData;
import silentium.gameserver.instancemanager.ClanHallManager;
import silentium.gameserver.instancemanager.CoupleManager;
import silentium.gameserver.instancemanager.DimensionalRiftManager;
import silentium.gameserver.instancemanager.PetitionManager;
import silentium.gameserver.instancemanager.QuestManager;
import silentium.gameserver.instancemanager.SiegeManager;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.ClanHall;
import silentium.gameserver.model.entity.Couple;
import silentium.gameserver.model.entity.Siege;
import silentium.gameserver.model.entity.TvTEvent;
import silentium.gameserver.model.olympiad.Olympiad;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.Die;
import silentium.gameserver.network.serverpackets.EtcStatusUpdate;
import silentium.gameserver.network.serverpackets.ExStorageMaxCount;
import silentium.gameserver.network.serverpackets.FriendList;
import silentium.gameserver.network.serverpackets.HennaInfo;
import silentium.gameserver.network.serverpackets.ItemList;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.PledgeShowMemberListAll;
import silentium.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import silentium.gameserver.network.serverpackets.PledgeSkillList;
import silentium.gameserver.network.serverpackets.PledgeStatusChanged;
import silentium.gameserver.network.serverpackets.QuestList;
import silentium.gameserver.network.serverpackets.ShortCutInit;
import silentium.gameserver.network.serverpackets.SkillCoolTime;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.network.serverpackets.UserInfo;
import silentium.gameserver.tables.GmListTable;
import silentium.gameserver.tables.SkillTable.FrequentSkill;

public class EnterWorld extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
		// this is just a trigger packet. it has no content
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			log.warn("EnterWorld failed! activeChar is null...");
			getClient().closeNow();
			return;
		}

		if (L2World.getInstance().findObject(activeChar.getObjectId()) != null)
		{
			log.warn("User already exist in OID map! User " + activeChar.getName() + " is character clone.");
		}

		if (activeChar.isGM())
		{
			if (PlayersConfig.GM_STARTUP_INVULNERABLE && AdminCommandAccessRightsData.getInstance().hasAccess("admin_invul", activeChar.getAccessLevel()))
				activeChar.setIsInvul(true);

			if (PlayersConfig.GM_STARTUP_INVISIBLE && AdminCommandAccessRightsData.getInstance().hasAccess("admin_hide", activeChar.getAccessLevel()))
				activeChar.getAppearance().setInvisible();

			if (PlayersConfig.GM_STARTUP_SILENCE && AdminCommandAccessRightsData.getInstance().hasAccess("admin_silence", activeChar.getAccessLevel()))
				activeChar.setInRefusalMode(true);

			if (PlayersConfig.GM_STARTUP_AUTO_LIST && AdminCommandAccessRightsData.getInstance().hasAccess("admin_gmliston", activeChar.getAccessLevel()))
				GmListTable.getInstance().addGm(activeChar, false);
			else
				GmListTable.getInstance().addGm(activeChar, true);
		}

		// Set dead status if applies
		if (activeChar.getCurrentHp() < 0.5)
			activeChar.setIsDead(true);

		if (activeChar.getClan() != null)
		{
			activeChar.sendPacket(new PledgeSkillList(activeChar.getClan()));
			notifyClanMembers(activeChar);
			notifySponsorOrApprentice(activeChar);

			// Add message at connexion if clanHall not paid.
			ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan());
			if (clanHall != null)
			{
				if (!clanHall.getPaid())
					activeChar.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
			}

			for (Siege siege : SiegeManager.getSieges())
			{
				if (!siege.getIsInProgress())
					continue;

				if (siege.checkIsAttacker(activeChar.getClan()))
					activeChar.setSiegeState((byte) 1);
				else if (siege.checkIsDefender(activeChar.getClan()))
					activeChar.setSiegeState((byte) 2);
			}

			activeChar.sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
			activeChar.sendPacket(new PledgeStatusChanged(activeChar.getClan()));
		}

		// Updating Seal of Strife Buff/Debuff
		if (SevenSigns.getInstance().isSealValidationPeriod() && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL)
		{
			int cabal = SevenSigns.getInstance().getPlayerCabal(activeChar.getObjectId());
			if (cabal != SevenSigns.CABAL_NULL)
			{
				if (cabal == SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
					activeChar.addSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill());
				else
					activeChar.addSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill());
			}
		}
		else
		{
			activeChar.removeSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill());
			activeChar.removeSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill());
		}

		if (PlayersConfig.PLAYER_SPAWN_PROTECTION > 0)
			activeChar.setProtection(true);

		activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());

		// buff and status icons
		if (PlayersConfig.STORE_SKILL_COOLTIME)
			activeChar.restoreEffects();

		// engage and notify Partner
		if (EventsConfig.ALLOW_WEDDING)
			engage(activeChar);

		// Announcements, welcome & Seven signs period messages
		activeChar.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);
		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
		Announcements.getInstance().showAnnouncements(activeChar);

		// if player is DE, check for shadow sense skill at night
		if (activeChar.getRace().ordinal() == 2)
		{
			// If player got the skill (exemple : low level DEs haven't it)
			if (activeChar.getSkillLevel(294) == 1)
			{
				if (GameTimeController.getInstance().isNowNight())
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NIGHT_EFFECT_APPLIES).addSkillName(294));
				else
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DAY_EFFECT_DISAPPEARS).addSkillName(294));
			}
		}

		activeChar.getMacroses().sendUpdate();
		activeChar.sendPacket(new UserInfo(activeChar));
		activeChar.sendPacket(new HennaInfo(activeChar));
		activeChar.sendPacket(new FriendList(activeChar));
		// activeChar.queryGameGuard();
		activeChar.sendPacket(new ItemList(activeChar, false));
		activeChar.sendPacket(new ShortCutInit(activeChar));
		activeChar.sendPacket(new ExStorageMaxCount(activeChar));
		activeChar.sendPacket(new EtcStatusUpdate(activeChar));
		activeChar.sendSkillList();

		Quest.playerEnter(activeChar);
		if (!MainConfig.DISABLE_TUTORIAL)
			loadTutorial(activeChar);

		for (Quest quest : QuestManager.getInstance().getAllManagedScripts())
		{
			if (quest != null && quest.getOnEnterWorld())
				quest.notifyEnterWorld(activeChar);
		}
		activeChar.sendPacket(new QuestList());

		if (MainConfig.SERVER_NEWS)
		{
			String serverNews = HtmCache.getInstance().getHtm(StaticHtmPath.NpcHtmPath + "servnews.htm");
			if (serverNews != null)
				sendPacket(new NpcHtmlMessage(1, serverNews));
		}

		PetitionManager.getInstance().checkPetitionMessages(activeChar);

		// no broadcast needed since the player will already spawn dead to others
		if (activeChar.isAlikeDead())
			sendPacket(new Die(activeChar));

		activeChar.onPlayerEnter();

		sendPacket(new SkillCoolTime(activeChar));

		// If player logs back in a stadium, port him in nearest town.
		if (Olympiad.getInstance().playerInStadia(activeChar))
			activeChar.teleToLocation(MapRegionData.TeleportWhereType.Town);

		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false))
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(activeChar);

		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
			activeChar.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);

		// Attacker or spectator logging into a siege zone will be ported at town.
		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(L2Character.ZONE_SIEGE))
			activeChar.teleToLocation(MapRegionData.TeleportWhereType.Town);

		TvTEvent.onLogin(activeChar);
	}

	private static void engage(L2PcInstance cha)
	{
		int _chaid = cha.getObjectId();

		for (Couple cl : CoupleManager.getInstance().getCouples())
		{
			if (cl.getPlayer1Id() == _chaid || cl.getPlayer2Id() == _chaid)
			{
				if (cl.getMaried())
					cha.setMarried(true);

				cha.setCoupleId(cl.getId());
			}
		}
	}

	private static void notifyClanMembers(L2PcInstance activeChar)
	{
		L2Clan clan = activeChar.getClan();
		clan.getClanMember(activeChar.getName()).setPlayerInstance(activeChar);

		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN);
		msg.addPcName(activeChar);

		clan.broadcastToOtherOnlineMembers(msg, activeChar);
		clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);

		msg = null;
	}

	private static void notifySponsorOrApprentice(L2PcInstance activeChar)
	{
		if (activeChar.getSponsor() != 0)
		{
			L2PcInstance sponsor = L2World.getInstance().getPlayer(activeChar.getSponsor());
			if (sponsor != null)
				sponsor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN).addPcName(activeChar));
		}
		else if (activeChar.getApprentice() != 0)
		{
			L2PcInstance apprentice = L2World.getInstance().getPlayer(activeChar.getApprentice());
			if (apprentice != null)
				apprentice.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN).addPcName(activeChar));
		}
	}

	private static void loadTutorial(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent("UC", null, player);
	}

	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}
