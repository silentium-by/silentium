/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.board.BB.Forum;
import silentium.gameserver.board.Manager.ForumsBBSManager;
import silentium.gameserver.configs.ClansConfig;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.crest.CrestCache;
import silentium.gameserver.data.crest.CrestCache.CrestType;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.SiegeManager;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance.TimeStamp;
import silentium.gameserver.model.itemcontainer.ClanWarehouse;
import silentium.gameserver.model.itemcontainer.ItemContainer;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ItemList;
import silentium.gameserver.network.serverpackets.L2GameServerPacket;
import silentium.gameserver.network.serverpackets.PledgeReceiveSubPledgeCreated;
import silentium.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import silentium.gameserver.network.serverpackets.PledgeShowMemberListAll;
import silentium.gameserver.network.serverpackets.PledgeShowMemberListDeleteAll;
import silentium.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import silentium.gameserver.network.serverpackets.PledgeSkillListAdd;
import silentium.gameserver.network.serverpackets.SkillCoolTime;
import silentium.gameserver.network.serverpackets.StatusUpdate;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.network.serverpackets.UserInfo;
import silentium.gameserver.tables.ClanTable;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.utils.Util;

public class L2Clan
{
	private static final Logger _log = LoggerFactory.getLogger(L2Clan.class.getName());

	private String _name;
	private int _clanId;
	private L2ClanMember _leader;
	private final Map<Integer, L2ClanMember> _members = new FastMap<>();

	private String _allyName;
	private int _allyId;
	private int _level;
	private int _hasCastle;
	private int _hasHideout;
	private int _hiredGuards;
	private int _crestId;
	private int _crestLargeId;
	private int _allyCrestId;
	private int _auctionBiddedAt = 0;
	private long _allyPenaltyExpiryTime;
	private int _allyPenaltyType;
	private long _charPenaltyExpiryTime;
	private long _dissolvingExpiryTime;

	// Ally Penalty Types
	public static final int PENALTY_TYPE_CLAN_LEAVED = 1;
	public static final int PENALTY_TYPE_CLAN_DISMISSED = 2;
	public static final int PENALTY_TYPE_DISMISS_CLAN = 3;
	public static final int PENALTY_TYPE_DISSOLVE_ALLY = 4;

	private final ItemContainer _warehouse = new ClanWarehouse(this);
	private final List<Integer> _atWarWith = new FastList<>();
	private final List<Integer> _atWarAttackers = new FastList<>();

	private Forum _forum;

	private final List<L2Skill> _skillList = new FastList<>();

	// Clan Privileges
	public static final int CP_NOTHING = 0;
	public static final int CP_CL_JOIN_CLAN = 2;
	public static final int CP_CL_GIVE_TITLE = 4;
	public static final int CP_CL_VIEW_WAREHOUSE = 8;
	public static final int CP_CL_MANAGE_RANKS = 16;
	public static final int CP_CL_PLEDGE_WAR = 32;
	public static final int CP_CL_DISMISS = 64;
	public static final int CP_CL_REGISTER_CREST = 128;
	public static final int CP_CL_MASTER_RIGHTS = 256;
	public static final int CP_CL_MANAGE_LEVELS = 512;
	public static final int CP_CH_OPEN_DOOR = 1024;
	public static final int CP_CH_OTHER_RIGHTS = 2048;
	public static final int CP_CH_AUCTION = 4096;
	public static final int CP_CH_DISMISS = 8192;
	public static final int CP_CH_SET_FUNCTIONS = 16384;
	public static final int CP_CS_OPEN_DOOR = 32768;
	public static final int CP_CS_MANOR_ADMIN = 65536;
	public static final int CP_CS_MANAGE_SIEGE = 131072;
	public static final int CP_CS_USE_FUNCTIONS = 262144;
	public static final int CP_CS_DISMISS = 524288;
	public static final int CP_CS_TAXES = 1048576;
	public static final int CP_CS_MERCENARIES = 2097152;
	public static final int CP_CS_SET_FUNCTIONS = 4194304;
	public static final int CP_ALL = 8388606;

	// Sub-unit types
	public static final int SUBUNIT_ACADEMY = -1;
	public static final int SUBUNIT_ROYAL1 = 100;
	public static final int SUBUNIT_ROYAL2 = 200;
	public static final int SUBUNIT_KNIGHT1 = 1001;
	public static final int SUBUNIT_KNIGHT2 = 1002;
	public static final int SUBUNIT_KNIGHT3 = 2001;
	public static final int SUBUNIT_KNIGHT4 = 2002;

	/** FastMap(Integer, L2Skill) containing all skills of the L2Clan */
	protected final Map<Integer, L2Skill> _skills = new FastMap<>();
	protected final Map<Integer, RankPrivs> _privs = new FastMap<>();
	protected final Map<Integer, SubPledge> _subPledges = new FastMap<>();

	private int _reputationScore = 0;
	private int _rank = 0;

	private String _notice;
	private boolean _noticeEnabled = false;
	private static final int MAX_NOTICE_LENGTH = 8192;

	/**
	 * Called if a clan is referenced only by id. In this case all other data needs to be fetched from db
	 * 
	 * @param clanId
	 *            A valid clan Id to create and restore
	 */
	public L2Clan(int clanId)
	{
		_clanId = clanId;
		initializePrivs();
		restore();
		getWarehouse().restore();
	}

	/**
	 * Called only if a new clan is created
	 * 
	 * @param clanId
	 *            A valid clan Id to create
	 * @param clanName
	 *            A valid clan name
	 */
	public L2Clan(int clanId, String clanName)
	{
		_clanId = clanId;
		_name = clanName;
		initializePrivs();
	}

	/**
	 * @return Returns the clanId.
	 */
	public int getClanId()
	{
		return _clanId;
	}

	/**
	 * @param clanId
	 *            The clanId to set.
	 */
	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}

	/**
	 * @return Returns the leaderId.
	 */
	public int getLeaderId()
	{
		return (_leader != null ? _leader.getObjectId() : 0);
	}

	/**
	 * @return L2ClanMember of clan leader.
	 */
	public L2ClanMember getLeader()
	{
		return _leader;
	}

	/**
	 * @param leader
	 *            The leader to set.
	 */
	public void setLeader(L2ClanMember leader)
	{
		_leader = leader;
		_members.put(leader.getObjectId(), leader);
	}

	public void setNewLeader(L2ClanMember member)
	{
		if (!getLeader().isOnline())
			return;

		if (member == null || !member.isOnline())
			return;

		L2PcInstance exLeader = getLeader().getPlayerInstance();
		SiegeManager.removeSiegeSkills(exLeader);
		exLeader.setClan(this);
		exLeader.setClanPrivileges(L2Clan.CP_NOTHING);
		exLeader.broadcastUserInfo();

		setLeader(member);
		updateClanInDB();

		exLeader.setPledgeClass(exLeader.getClan().getClanMember(exLeader.getObjectId()).calculatePledgeClass(exLeader));
		exLeader.broadcastUserInfo();

		L2PcInstance newLeader = member.getPlayerInstance();
		newLeader.setClan(this);
		newLeader.setPledgeClass(member.calculatePledgeClass(newLeader));
		newLeader.setClanPrivileges(L2Clan.CP_ALL);
		if (getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel())
		{
			SiegeManager.addSiegeSkills(newLeader);

			// Transfering siege skills TimeStamps from old leader to new leader to prevent unlimited headquarters
			if (!exLeader.getReuseTimeStamp().isEmpty())
			{
				for (L2Skill sk : SkillTable.getInstance().getSiegeSkills(newLeader.isNoble()))
				{
					if (exLeader.getReuseTimeStamp().containsKey(sk.getReuseHashCode()))
					{
						TimeStamp t = exLeader.getReuseTimeStamp().get(sk.getReuseHashCode());
						newLeader.addTimeStamp(sk, t.getReuse(), t.getStamp());
					}
				}
				newLeader.sendPacket(new SkillCoolTime(newLeader));
			}
		}
		newLeader.broadcastUserInfo();

		broadcastClanStatus();
		broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_LEADER_PRIVILEGES_HAVE_BEEN_TRANSFERRED_TO_S1).addPcName(newLeader));
	}

	/**
	 * @return the leaderName.
	 */
	public String getLeaderName()
	{
		if (_leader == null)
		{
			_log.warn("Clan named" + getName() + " is without clan leader.");
			return "";
		}
		return _leader.getName();
	}

	/**
	 * @return the name.
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * @param name
	 *            : The name to set.
	 */
	public void setName(String name)
	{
		_name = name;
	}

	private void addClanMember(L2ClanMember member)
	{
		_members.put(member.getObjectId(), member);
	}

	public void addClanMember(L2PcInstance player)
	{
		final L2ClanMember member = new L2ClanMember(this, player.getName(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getTitle(), player.getAppearance().getSex(), player.getRace().ordinal());
		addClanMember(member);
		member.setPlayerInstance(player);
		player.setClan(this);
		player.setPledgeClass(member.calculatePledgeClass(player));

		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new UserInfo(player));
	}

	public void updateClanMember(L2PcInstance player)
	{
		final L2ClanMember member = new L2ClanMember(player);
		if (player.isClanLeader())
			setLeader(member);

		addClanMember(member);
	}

	public L2ClanMember getClanMember(String name)
	{
		for (L2ClanMember temp : _members.values())
		{
			if (temp.getName().equals(name))
				return temp;
		}
		return null;
	}

	/**
	 * @param objectID
	 *            : the required clan member object Id.
	 * @return the clan member for a given {@code objectID}.
	 */
	public L2ClanMember getClanMember(int objectID)
	{
		return _members.get(objectID);
	}

	/**
	 * @param objectId
	 *            : the object Id of the member that will be removed.
	 * @param clanJoinExpiryTime
	 *            : time penalty to join a clan.
	 */
	public void removeClanMember(int objectId, long clanJoinExpiryTime)
	{
		final L2ClanMember exMember = _members.remove(objectId);
		if (exMember == null)
		{
			_log.warn("Member Object ID: " + objectId + " not found in clan while trying to remove");
			return;
		}

		final int leadssubpledge = getLeaderSubPledge(objectId);
		if (leadssubpledge != 0)
		{
			// Sub-unit leader withdraws, position becomes vacant and leader should appoint new via NPC
			getSubPledge(leadssubpledge).setLeaderId(0);
			updateSubPledgeInDB(leadssubpledge);
		}

		if (exMember.getApprentice() != 0)
		{
			final L2ClanMember apprentice = getClanMember(exMember.getApprentice());
			if (apprentice != null)
			{
				if (apprentice.getPlayerInstance() != null)
					apprentice.getPlayerInstance().setSponsor(0);
				else
					apprentice.initApprenticeAndSponsor(0, 0);

				apprentice.saveApprenticeAndSponsor(0, 0);
			}
		}

		if (exMember.getSponsor() != 0)
		{
			final L2ClanMember sponsor = getClanMember(exMember.getSponsor());
			if (sponsor != null)
			{
				if (sponsor.getPlayerInstance() != null)
					sponsor.getPlayerInstance().setApprentice(0);
				else
					sponsor.initApprenticeAndSponsor(0, 0);

				sponsor.saveApprenticeAndSponsor(0, 0);
			}
		}
		exMember.saveApprenticeAndSponsor(0, 0);
		if (ClansConfig.REMOVE_CASTLE_CIRCLETS)
			CastleManager.getInstance().removeCircletsAndCrown(exMember, getCastleId());

		if (exMember.isOnline())
		{
			L2PcInstance player = exMember.getPlayerInstance();

			// Clean title only for non nobles.
			if (!player.isNoble())
				player.setTitle("");

			player.setApprentice(0);
			player.setSponsor(0);

			if (player.isClanLeader())
			{
				SiegeManager.removeSiegeSkills(player);
				player.setClanCreateExpiryTime(System.currentTimeMillis() + ClansConfig.ALT_CLAN_CREATE_DAYS * 86400000L); // 24*60*60*1000
																															// =
																															// 86400000
			}

			for (L2Skill skill : player.getClan().getAllSkills())
				player.removeSkill(skill, false);

			player.sendSkillList();
			player.setClan(null);

			// players leaving from clan academy have no penalty
			if (exMember.getPledgeType() != -1)
				player.setClanJoinExpiryTime(clanJoinExpiryTime);

			player.setPledgeClass(exMember.calculatePledgeClass(player));
			player.broadcastUserInfo();
			// disable clan tab
			player.sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
		}
		else
			removeMemberInDatabase(exMember, clanJoinExpiryTime, getLeaderId() == objectId ? System.currentTimeMillis() + ClansConfig.ALT_CLAN_CREATE_DAYS * 86400000L : 0);
	}

	public L2ClanMember[] getMembers()
	{
		return _members.values().toArray(new L2ClanMember[_members.size()]);
	}

	public int getMembersCount()
	{
		return _members.size();
	}

	public int getSubPledgeMembersCount(int subpl)
	{
		int result = 0;
		for (L2ClanMember temp : _members.values())
		{
			if (temp.getPledgeType() == subpl)
				result++;
		}
		return result;
	}

	/**
	 * @param pledgeType
	 *            the Id of the pledge type.
	 * @return the maximum number of members allowed for a given {@code pledgeType}.
	 */
	public int getMaxNrOfMembers(int pledgeType)
	{
		int limit = 0;

		switch (pledgeType)
		{
			case 0:
				switch (getLevel())
				{
					case 4:
						limit = 40;
						break;
					case 3:
						limit = 30;
						break;
					case 2:
						limit = 20;
						break;
					case 1:
						limit = 15;
						break;
					case 0:
						limit = 10;
						break;
					default:
						limit = 40;
						break;
				}
				break;
			case -1:
			case 100:
			case 200:
				limit = 20;
				break;
			case 1001:
			case 1002:
			case 2001:
			case 2002:
				limit = 10;
				break;
			default:
				break;
		}

		return limit;
	}

	public L2PcInstance[] getOnlineMembers(int exclude)
	{
		FastList<L2PcInstance> list = FastList.newInstance();
		for (L2ClanMember temp : _members.values())
		{
			if (temp != null && temp.isOnline() && !(temp.getObjectId() == exclude))
				list.add(temp.getPlayerInstance());
		}

		L2PcInstance[] result = list.toArray(new L2PcInstance[list.size()]);
		FastList.recycle(list);
		return result;
	}

	/**
	 * @return the online clan member count.
	 */
	public int getOnlineMembersCount()
	{
		int count = 0;
		for (L2ClanMember temp : _members.values())
		{
			if (temp == null || !temp.isOnline())
				continue;

			count++;
		}
		return count;
	}

	/**
	 * @return the alliance Id.
	 */
	public int getAllyId()
	{
		return _allyId;
	}

	/**
	 * @return the alliance name.
	 */
	public String getAllyName()
	{
		return _allyName;
	}

	/**
	 * @param allyCrestId
	 *            the alliance crest Id to be set.
	 */
	public void setAllyCrestId(int allyCrestId)
	{
		_allyCrestId = allyCrestId;
	}

	/**
	 * @return the alliance crest Id.
	 */
	public int getAllyCrestId()
	{
		return _allyCrestId;
	}

	/**
	 * @return the clan level.
	 */
	public int getLevel()
	{
		return _level;
	}

	/**
	 * Sets the clan level and updates the clan forum if it's needed.
	 * 
	 * @param level
	 *            the clan level to be set.
	 */
	public void setLevel(int level)
	{
		_level = level;
		if (MainConfig.ENABLE_COMMUNITY_BOARD && _level >= 2 && _forum == null)
		{
			final Forum forum = ForumsBBSManager.getInstance().getForumByName("ClanRoot");
			if (forum != null)
			{
				_forum = forum.getChildByName(_name);
				if (_forum == null)
					_forum = ForumsBBSManager.getInstance().createNewForum(_name, ForumsBBSManager.getInstance().getForumByName("ClanRoot"), Forum.CLAN, Forum.CLANMEMBERONLY, getClanId());
			}
		}
	}

	/**
	 * @return clan castle id.
	 */
	public int getCastleId()
	{
		return _hasCastle;
	}

	/**
	 * @return clan hideout id.
	 */
	public int getHideoutId()
	{
		return _hasHideout;
	}

	/**
	 * @return {code true} if the clan has a castle.
	 */
	public boolean hasCastle()
	{
		return _hasCastle > 0;
	}

	/**
	 * @return {code true} if the clan has a hideout.
	 */
	public boolean hasHideout()
	{
		return _hasHideout > 0;
	}

	/**
	 * @param crestId
	 *            : The id of pledge crest.
	 */
	public void setCrestId(int crestId)
	{
		_crestId = crestId;
	}

	/**
	 * @return the clanCrestId.
	 */
	public int getCrestId()
	{
		return _crestId;
	}

	/**
	 * @param crestLargeId
	 *            : The id of pledge LargeCrest.
	 */
	public void setCrestLargeId(int crestLargeId)
	{
		_crestLargeId = crestLargeId;
	}

	/**
	 * @return the clan CrestLargeId
	 */
	public int getCrestLargeId()
	{
		return _crestLargeId;
	}

	/**
	 * @param allyId
	 *            : The allyId to set.
	 */
	public void setAllyId(int allyId)
	{
		_allyId = allyId;
	}

	/**
	 * @param allyName
	 *            : The allyName to set.
	 */
	public void setAllyName(String allyName)
	{
		_allyName = allyName;
	}

	/**
	 * @param hasCastle
	 *            : The hasCastle to set.
	 */
	public void setHasCastle(int hasCastle)
	{
		_hasCastle = hasCastle;
	}

	/**
	 * @param hasHideout
	 *            : The hasHideout to set.
	 */
	public void setHasHideout(int hasHideout)
	{
		_hasHideout = hasHideout;
	}

	/**
	 * @param id
	 *            the member id.
	 * @return true if the member id given as parameter is in the _members list.
	 */
	public boolean isMember(int id)
	{
		return (id == 0 ? false : _members.containsKey(id));
	}

	/**
	 * Store in database current clan's reputation.
	 */
	public void updateClanScoreInDB()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET reputation_score=? WHERE clan_id=?");
			statement.setInt(1, getReputationScore());
			statement.setInt(2, getClanId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Exception on updateClanScoreInDb(): " + e.getMessage(), e);
		}
	}

	public void updateClanInDB()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET leader_id=?,ally_id=?,ally_name=?,reputation_score=?,ally_penalty_expiry_time=?,ally_penalty_type=?,char_penalty_expiry_time=?,dissolving_expiry_time=? WHERE clan_id=?");
			statement.setInt(1, getLeaderId());
			statement.setInt(2, getAllyId());
			statement.setString(3, getAllyName());
			statement.setInt(4, getReputationScore());
			statement.setLong(5, getAllyPenaltyExpiryTime());
			statement.setInt(6, getAllyPenaltyType());
			statement.setLong(7, getCharPenaltyExpiryTime());
			statement.setLong(8, getDissolvingExpiryTime());
			statement.setInt(9, getClanId());
			statement.execute();
			statement.close();

			_log.debug("New clan leader saved in db: " + getClanId());
		}
		catch (Exception e)
		{
			_log.warn("error while saving new clan leader to db " + e);
		}
	}

	public void store()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id) values (?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, getClanId());
			statement.setString(2, getName());
			statement.setInt(3, getLevel());
			statement.setInt(4, getCastleId());
			statement.setInt(5, getAllyId());
			statement.setString(6, getAllyName());
			statement.setInt(7, getLeaderId());
			statement.setInt(8, getCrestId());
			statement.setInt(9, getCrestLargeId());
			statement.setInt(10, getAllyCrestId());
			statement.execute();
			statement.close();

			_log.debug("New clan saved in db: " + getClanId());
		}
		catch (Exception e)
		{
			_log.warn("error while saving new clan to db " + e);
		}
	}

	private void removeMemberInDatabase(L2ClanMember member, long clanJoinExpiryTime, long clanCreateExpiryTime)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET clanid=0, title=?, clan_join_expiry_time=?, clan_create_expiry_time=?, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0 WHERE obj_Id=?");
			statement.setString(1, "");
			statement.setLong(2, clanJoinExpiryTime);
			statement.setLong(3, clanCreateExpiryTime);
			statement.setInt(4, member.getObjectId());
			statement.execute();
			statement.close();

			_log.debug("clan member removed in db: " + getClanId());

			statement = con.prepareStatement("UPDATE characters SET apprentice=0 WHERE apprentice=?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			statement.close();

			statement = con.prepareStatement("UPDATE characters SET sponsor=0 WHERE sponsor=?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("error while removing clan member in db " + e);
		}
	}

	private void restore()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			L2ClanMember member;

			PreparedStatement statement = con.prepareStatement("SELECT clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id,reputation_score,auction_bid_at,ally_penalty_expiry_time,ally_penalty_type,char_penalty_expiry_time,dissolving_expiry_time FROM clan_data where clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet clanData = statement.executeQuery();

			if (clanData.next())
			{
				setName(clanData.getString("clan_name"));
				setLevel(clanData.getInt("clan_level"));
				setHasCastle(clanData.getInt("hasCastle"));
				setAllyId(clanData.getInt("ally_id"));
				setAllyName(clanData.getString("ally_name"));
				setAllyPenaltyExpiryTime(clanData.getLong("ally_penalty_expiry_time"), clanData.getInt("ally_penalty_type"));
				if (getAllyPenaltyExpiryTime() < System.currentTimeMillis())
				{
					setAllyPenaltyExpiryTime(0, 0);
				}
				setCharPenaltyExpiryTime(clanData.getLong("char_penalty_expiry_time"));
				if (getCharPenaltyExpiryTime() + ClansConfig.ALT_CLAN_JOIN_DAYS * 86400000L < System.currentTimeMillis()) // 24*60*60*1000
																															// =
																															// 86400000
				{
					setCharPenaltyExpiryTime(0);
				}
				setDissolvingExpiryTime(clanData.getLong("dissolving_expiry_time"));

				setCrestId(clanData.getInt("crest_id"));
				setCrestLargeId(clanData.getInt("crest_large_id"));
				setAllyCrestId(clanData.getInt("ally_crest_id"));

				setReputationScore(clanData.getInt("reputation_score"));
				setAuctionBiddedAt(clanData.getInt("auction_bid_at"), false);

				int leaderId = (clanData.getInt("leader_id"));

				PreparedStatement statement2 = con.prepareStatement("SELECT char_name,level,classid,obj_Id,title,power_grade,subpledge,apprentice,sponsor,sex,race FROM characters WHERE clanid=?");
				statement2.setInt(1, getClanId());
				ResultSet clanMembers = statement2.executeQuery();

				while (clanMembers.next())
				{
					member = new L2ClanMember(this, clanMembers.getString("char_name"), clanMembers.getInt("level"), clanMembers.getInt("classid"), clanMembers.getInt("obj_id"), clanMembers.getInt("subpledge"), clanMembers.getInt("power_grade"), clanMembers.getString("title"), (clanMembers.getInt("sex") != 0), clanMembers.getInt("race"));
					if (member.getObjectId() == leaderId)
						setLeader(member);
					else
						addClanMember(member);
					member.initApprenticeAndSponsor(clanMembers.getInt("apprentice"), clanMembers.getInt("sponsor"));
				}
				clanMembers.close();
				statement2.close();
			}

			clanData.close();
			statement.close();

			if (_log.isDebugEnabled() && getName() != null)
				_log.debug("Restored clan data for \"" + getName() + "\" from database.");

			restoreSubPledges();
			restoreRankPrivs();
			restoreSkills();
			restoreNotice();
			checkCrests();
		}
		catch (Exception e)
		{
			_log.warn("error while restoring clan " + e);
		}
	}

	private void restoreNotice()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT enabled,notice FROM clan_notices WHERE clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet noticeData = statement.executeQuery();

			while (noticeData.next())
			{
				_noticeEnabled = noticeData.getBoolean("enabled");
				_notice = noticeData.getString("notice");
			}

			noticeData.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Error restoring clan notice: " + e.getMessage(), e);
		}
	}

	private void storeNotice(String notice, boolean enabled)
	{
		if (notice == null)
			notice = "";

		if (notice.length() > MAX_NOTICE_LENGTH)
			notice = notice.substring(0, MAX_NOTICE_LENGTH - 1);

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_notices (clan_id,notice,enabled) values (?,?,?) ON DUPLICATE KEY UPDATE notice=?,enabled=?");
			statement.setInt(1, getClanId());
			statement.setString(2, notice);

			if (enabled)
			{
				statement.setString(3, "true");
				statement.setString(4, notice);
				statement.setString(5, "true");
			}
			else
			{
				statement.setString(3, "false");
				statement.setString(4, notice);
				statement.setString(5, "false");
			}

			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Error could not store clan notice: " + e.getMessage(), e);
		}

		_notice = notice;
		_noticeEnabled = enabled;
	}

	public void setNoticeEnabled(boolean enabled)
	{
		storeNotice(_notice, enabled);
	}

	public void setNotice(String notice)
	{
		storeNotice(notice, _noticeEnabled);
	}

	public boolean isNoticeEnabled()
	{
		return _noticeEnabled;
	}

	public String getNotice()
	{
		return (_notice == null) ? "" : _notice;
	}

	/**
	 * Restore skills of that clan, and feed _skills Map.
	 */
	private void restoreSkills()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT skill_id,skill_level FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");

				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				if (skill == null)
					continue;

				_skills.put(id, skill);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not restore clan skills: " + e);
		}
	}

	/**
	 * @return an array with all clan skills that clan knows.
	 */
	public final L2Skill[] getAllSkills()
	{
		if (_skills == null)
			return new L2Skill[0];

		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}

	/**
	 * Replace old skill by new skill or add the new skill.
	 * 
	 * @param newSkill
	 *            the skill to add.
	 * @return the skill object or null.
	 */
	public L2Skill addSkill(L2Skill newSkill)
	{
		if (newSkill != null)
			return _skills.put(newSkill.getId(), newSkill);

		return null;
	}

	/**
	 * Add a new skill to the list, send a packet to all online clan members, update their stats and store it in db
	 * 
	 * @param newSkill
	 *            The skill to add
	 * @return null if the newSkill was null, else the old skill.
	 */
	public L2Skill addNewSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;

		if (newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);

			try (Connection con = DatabaseFactory.getConnection())
			{
				PreparedStatement statement;

				if (oldSkill != null)
				{
					statement = con.prepareStatement("UPDATE clan_skills SET skill_level=? WHERE skill_id=? AND clan_id=?");
					statement.setInt(1, newSkill.getLevel());
					statement.setInt(2, oldSkill.getId());
					statement.setInt(3, getClanId());
					statement.execute();
					statement.close();
				}
				else
				{
					statement = con.prepareStatement("INSERT INTO clan_skills (clan_id,skill_id,skill_level,skill_name) VALUES (?,?,?,?)");
					statement.setInt(1, getClanId());
					statement.setInt(2, newSkill.getId());
					statement.setInt(3, newSkill.getLevel());
					statement.setString(4, newSkill.getName());
					statement.execute();
					statement.close();
				}
			}
			catch (Exception e)
			{
				_log.warn("Error could not store char skills: " + e);
			}

			for (L2ClanMember temp : _members.values())
			{
				if (temp != null && temp.getPlayerInstance() != null && temp.isOnline())
				{
					if (newSkill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass())
					{
						temp.getPlayerInstance().addSkill(newSkill, false); // Skill is not saved to player DB
						temp.getPlayerInstance().sendPacket(new PledgeSkillListAdd(newSkill.getId(), newSkill.getLevel()));
					}
				}
			}
		}

		return oldSkill;
	}

	public void addSkillEffects()
	{
		for (L2Skill skill : _skills.values())
		{
			for (L2ClanMember temp : _members.values())
			{
				if (temp != null && temp.isOnline())
				{
					if (skill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass())
						temp.getPlayerInstance().addSkill(skill, false); // Skill is not saved to player DB
				}
			}
		}
	}

	public void addSkillEffects(L2PcInstance member)
	{
		if (member == null)
			return;

		for (L2Skill skill : _skills.values())
		{
			// TODO add skills according to members class( in ex. don't add Clan Agillity skill's effect to lower class then
			// Baron)
			if (skill.getMinPledgeClass() <= member.getPledgeClass())
				member.addSkill(skill, false); // Skill is not saved to player DB
		}
	}

	public void broadcastToOnlineAllyMembers(L2GameServerPacket packet)
	{
		for (L2Clan clan : ClanTable.getInstance().getClanAllies(getAllyId()))
			clan.broadcastToOnlineMembers(packet);
	}

	public void broadcastToOnlineMembers(L2GameServerPacket packet)
	{
		for (L2ClanMember member : _members.values())
		{
			if (member != null && member.isOnline())
				member.getPlayerInstance().sendPacket(packet);
		}
	}

	public void broadcastToOtherOnlineMembers(L2GameServerPacket packet, L2PcInstance player)
	{
		for (L2ClanMember member : _members.values())
		{
			if (member != null && member.isOnline() && member.getPlayerInstance() != player)
				member.getPlayerInstance().sendPacket(packet);
		}
	}

	@Override
	public String toString()
	{
		return getName() + "[" + getClanId() + "]";
	}

	public ItemContainer getWarehouse()
	{
		return _warehouse;
	}

	public boolean isAtWarWith(Integer id)
	{
		if (!_atWarWith.isEmpty() && _atWarWith.contains(id))
			return true;

		return false;
	}

	public boolean isAtWarAttacker(Integer id)
	{
		if (!_atWarAttackers.isEmpty() && _atWarAttackers.contains(id))
			return true;

		return false;
	}

	public void setEnemyClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarWith.add(id);
	}

	public void setEnemyClan(Integer clan)
	{
		_atWarWith.add(clan);
	}

	public void setAttackerClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarAttackers.add(id);
	}

	public void setAttackerClan(Integer clan)
	{
		_atWarAttackers.add(clan);
	}

	public void deleteEnemyClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarWith.remove(id);
	}

	public void deleteAttackerClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarAttackers.remove(id);
	}

	public int getHiredGuards()
	{
		return _hiredGuards;
	}

	public void incrementHiredGuards()
	{
		_hiredGuards++;
	}

	public boolean isAtWar()
	{
		if (!_atWarWith.isEmpty())
			return true;

		return false;
	}

	public List<Integer> getWarList()
	{
		return _atWarWith;
	}

	public List<Integer> getAttackerList()
	{
		return _atWarAttackers;
	}

	public void broadcastClanStatus()
	{
		for (L2PcInstance member : getOnlineMembers(0))
		{
			member.sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
			member.sendPacket(new PledgeShowMemberListAll(this, member));
		}
	}

	public void removeSkill(int id)
	{
		L2Skill deleteSkill = null;
		for (L2Skill sk : _skillList)
		{
			if (sk.getId() == id)
			{
				deleteSkill = sk;
				return;
			}
		}
		_skillList.remove(deleteSkill);
	}

	public void removeSkill(L2Skill deleteSkill)
	{
		_skillList.remove(deleteSkill);
	}

	/**
	 * @return the clan skills list.
	 */
	public List<L2Skill> getSkills()
	{
		return _skillList;
	}

	public static class SubPledge
	{
		private final int _id;
		private String _subPledgeName;
		private int _leaderId;

		public SubPledge(int id, String name, int leaderId)
		{
			_id = id;
			_subPledgeName = name;
			_leaderId = leaderId;
		}

		public int getId()
		{
			return _id;
		}

		public String getName()
		{
			return _subPledgeName;
		}

		public void setName(String name)
		{
			_subPledgeName = name;
		}

		public int getLeaderId()
		{
			return _leaderId;
		}

		public void setLeaderId(int leaderId)
		{
			_leaderId = leaderId;
		}
	}

	public class RankPrivs
	{
		private final int _rankId;
		private final int _party;// TODO find out what this stuff means and implement it
		private int _rankPrivs;

		public RankPrivs(int rank, int party, int privs)
		{
			_rankId = rank;
			_party = party;
			_rankPrivs = privs;
		}

		public int getRank()
		{
			return _rankId;
		}

		public int getParty()
		{
			return _party;
		}

		public int getPrivs()
		{
			return _rankPrivs;
		}

		public void setPrivs(int privs)
		{
			_rankPrivs = privs;
		}
	}

	private void restoreSubPledges()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT sub_pledge_id,name,leader_id FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				int id = rset.getInt("sub_pledge_id");
				_subPledges.put(id, new SubPledge(id, rset.getString("name"), rset.getInt("leader_id")));
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not restore clan sub-units: " + e.getMessage(), e);
		}
	}

	/**
	 * Retrieve subPledge by type
	 * 
	 * @param pledgeType
	 * @return the subpledge object.
	 */
	public final SubPledge getSubPledge(int pledgeType)
	{
		if (_subPledges == null)
			return null;

		return _subPledges.get(pledgeType);
	}

	/**
	 * Retrieve subPledge by name
	 * 
	 * @param pledgeName
	 * @return the subpledge object.
	 */
	public final SubPledge getSubPledge(String pledgeName)
	{
		if (_subPledges == null)
			return null;

		for (SubPledge sp : _subPledges.values())
		{
			if (sp.getName().equalsIgnoreCase(pledgeName))
				return sp;
		}
		return null;
	}

	/**
	 * Retrieve all subPledges.
	 * 
	 * @return an array containing all subpledge objects.
	 */
	public final SubPledge[] getAllSubPledges()
	{
		if (_subPledges == null)
			return new SubPledge[0];

		return _subPledges.values().toArray(new SubPledge[_subPledges.values().size()]);
	}

	public SubPledge createSubPledge(L2PcInstance player, int pledgeType, int leaderId, String subPledgeName)
	{
		pledgeType = getAvailablePledgeTypes(pledgeType);
		if (pledgeType == 0)
		{
			if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				player.sendPacket(SystemMessageId.CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY);
			else
				player.sendMessage("You can't create any more sub-units of this type");
			return null;
		}

		if (_leader.getObjectId() == leaderId)
		{
			player.sendMessage("Leader is not correct");
			return null;
		}

		// Royal Guard 5000 points per each
		// Order of Knights 10000 points per each
		if (pledgeType != -1 && ((getReputationScore() < 5000 && pledgeType < L2Clan.SUBUNIT_KNIGHT1) || (getReputationScore() < 10000 && pledgeType > L2Clan.SUBUNIT_ROYAL2)))
		{
			player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
			return null;
		}

		SubPledge subPledge = null;
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_subpledges (clan_id,sub_pledge_id,name,leader_id) values (?,?,?,?)");
			statement.setInt(1, getClanId());
			statement.setInt(2, pledgeType);
			statement.setString(3, subPledgeName);
			statement.setInt(4, (pledgeType != -1) ? leaderId : 0);
			statement.execute();
			statement.close();

			subPledge = new SubPledge(pledgeType, subPledgeName, leaderId);
			_subPledges.put(pledgeType, subPledge);

			if (pledgeType != -1)
				takeReputationScore(2500);

			_log.debug("New sub_clan saved in db: " + getClanId() + "; " + pledgeType);
		}
		catch (Exception e)
		{
			_log.warn("error while saving new sub_clan to db " + e);
		}

		broadcastToOnlineMembers(new PledgeShowInfoUpdate(_leader.getClan()));
		broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge, _leader.getClan()));

		return subPledge;
	}

	public int getAvailablePledgeTypes(int pledgeType)
	{
		if (_subPledges.get(pledgeType) != null)
		{
			switch (pledgeType)
			{
				case SUBUNIT_ACADEMY:
					return 0;

				case SUBUNIT_ROYAL1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_ROYAL2);
					break;

				case SUBUNIT_ROYAL2:
					return 0;

				case SUBUNIT_KNIGHT1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT2);
					break;

				case SUBUNIT_KNIGHT2:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT3);
					break;

				case SUBUNIT_KNIGHT3:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT4);
					break;

				case SUBUNIT_KNIGHT4:
					return 0;
			}
		}
		return pledgeType;
	}

	public void updateSubPledgeInDB(int pledgeType)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_subpledges SET leader_id=?, name=? WHERE clan_id=? AND sub_pledge_id=?");
			statement.setInt(1, getSubPledge(pledgeType).getLeaderId());
			statement.setString(2, getSubPledge(pledgeType).getName());
			statement.setInt(3, getClanId());
			statement.setInt(4, pledgeType);
			statement.execute();
			statement.close();

			_log.debug("Subpledge updated in db: " + getClanId());
		}
		catch (Exception e)
		{
			_log.error("Error updating subpledge: " + e.getMessage(), e);
		}
	}

	private void restoreRankPrivs()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT privs,rank,party FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while (rset.next())
			{
				_privs.get(rset.getInt("rank")).setPrivs(rset.getInt("privs"));
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not restore clan privs by rank: " + e);
		}
	}

	public void initializePrivs()
	{
		RankPrivs privs;
		for (int i = 1; i < 10; i++)
		{
			privs = new RankPrivs(i, 0, CP_NOTHING);
			_privs.put(i, privs);
		}
	}

	public int getRankPrivs(int rank)
	{
		if (_privs.get(rank) != null)
			return _privs.get(rank).getPrivs();

		return CP_NOTHING;
	}

	/**
	 * Retrieve all skills of this L2PcInstance from the database
	 * 
	 * @param rank
	 * @param privs
	 */
	public void setRankPrivs(int rank, int privs)
	{
		if (_privs.get(rank) != null)
		{
			_privs.get(rank).setPrivs(privs);

			try (Connection con = DatabaseFactory.getConnection())
			{
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,party,privs) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE privs = ?");
				statement.setInt(1, getClanId());
				statement.setInt(2, rank);
				statement.setInt(3, 0);
				statement.setInt(4, privs);
				statement.setInt(5, privs);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("Could not store clan privs for rank: " + e);
			}

			for (L2ClanMember cm : getMembers())
			{
				if (cm.isOnline() && cm.getPowerGrade() == rank && cm.getPlayerInstance() != null)
				{
					cm.getPlayerInstance().setClanPrivileges(privs);
					cm.getPlayerInstance().sendPacket(new UserInfo(cm.getPlayerInstance()));
				}
			}
			broadcastClanStatus();
		}
		else
		{
			_privs.put(rank, new RankPrivs(rank, 0, privs));

			try (Connection con = DatabaseFactory.getConnection())
			{
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,party,privs) VALUES (?,?,?,?)");
				statement.setInt(1, getClanId());
				statement.setInt(2, rank);
				statement.setInt(3, 0);
				statement.setInt(4, privs);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("Could not create new rank and store clan privs for rank: " + e);
			}
		}
	}

	/**
	 * Retrieve all RankPrivs
	 * 
	 * @return an array containing all RankPrivs objects.
	 */
	public final RankPrivs[] getAllRankPrivs()
	{
		if (_privs == null)
			return new RankPrivs[0];

		return _privs.values().toArray(new RankPrivs[_privs.values().size()]);
	}

	public int getLeaderSubPledge(int leaderId)
	{
		int id = 0;
		for (SubPledge sp : _subPledges.values())
		{
			if (sp.getLeaderId() == 0)
				continue;

			if (sp.getLeaderId() == leaderId)
				id = sp.getId();
		}
		return id;
	}

	/**
	 * Add the value to the total amount of the clan's reputation score.<br>
	 * <b>This method updates the database.</b>
	 * 
	 * @param value
	 *            : The value to add to current amount.
	 */
	public synchronized void addReputationScore(int value)
	{
		setReputationScore(getReputationScore() + value);
		updateClanScoreInDB();
	}

	/**
	 * Removes the value to the total amount of the clan's reputation score.<br>
	 * <b>This method updates the database.</b>
	 * 
	 * @param value
	 *            : The value to remove to current amount.
	 */
	public synchronized void takeReputationScore(int value)
	{
		setReputationScore(getReputationScore() - value);
		updateClanScoreInDB();
	}

	/**
	 * Launch behaviors following how big or low is the actual reputation.<br>
	 * <b>This method DOESN'T update the database.</b>
	 * 
	 * @param value
	 *            : The total amount to set to _reputationScore.
	 */
	private void setReputationScore(int value)
	{
		if (_reputationScore >= 0 && value < 0)
		{
			broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.REPUTATION_POINTS_0_OR_LOWER_CLAN_SKILLS_DEACTIVATED));
			L2Skill[] skills = getAllSkills();
			for (L2ClanMember member : _members.values())
			{
				if (member.isOnline() && member.getPlayerInstance() != null)
				{
					for (L2Skill sk : skills)
						member.getPlayerInstance().removeSkill(sk, false);
				}
			}
		}
		else if (_reputationScore < 0 && value >= 0)
		{
			broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_SKILLS_WILL_BE_ACTIVATED_SINCE_REPUTATION_IS_0_OR_HIGHER));
			L2Skill[] skills = getAllSkills();
			for (L2ClanMember member : _members.values())
			{
				if (member.isOnline() && member.getPlayerInstance() != null)
				{
					for (L2Skill sk : skills)
					{
						if (sk.getMinPledgeClass() <= member.getPlayerInstance().getPledgeClass())
							member.getPlayerInstance().addSkill(sk, false);
					}
				}
			}
		}
		_reputationScore = value;

		if (_reputationScore > 100000000)
			_reputationScore = 100000000;

		if (_reputationScore < -100000000)
			_reputationScore = -100000000;

		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
	}

	public int getReputationScore()
	{
		return _reputationScore;
	}

	public void setRank(int rank)
	{
		_rank = rank;
	}

	public int getRank()
	{
		return _rank;
	}

	public int getAuctionBiddedAt()
	{
		return _auctionBiddedAt;
	}

	public void setAuctionBiddedAt(int id, boolean storeInDb)
	{
		_auctionBiddedAt = id;

		if (storeInDb)
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET auction_bid_at=? WHERE clan_id=?");
				statement.setInt(1, id);
				statement.setInt(2, getClanId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("Could not store auction for clan: " + e);
			}
		}
	}

	/**
	 * Checks if activeChar and target meet various conditions to join a clan
	 * 
	 * @param activeChar
	 * @param target
	 * @param pledgeType
	 * @return
	 */
	public boolean checkClanJoinCondition(L2PcInstance activeChar, L2PcInstance target, int pledgeType)
	{
		if (activeChar == null)
			return false;

		if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_JOIN_CLAN) != L2Clan.CP_CL_JOIN_CLAN)
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}

		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return false;
		}

		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_YOURSELF);
			return false;
		}

		if (getCharPenaltyExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(SystemMessageId.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER);
			return false;
		}

		if (target.getClanId() != 0)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_WORKING_WITH_ANOTHER_CLAN).addPcName(target));
			return false;
		}

		if (target.getClanId() != 0)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_WORKING_WITH_ANOTHER_CLAN).addPcName(target));
			return false;
		}

		if (target.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN).addPcName(target));
			return false;
		}

		if ((target.getLevel() > 40 || target.getClassId().level() >= 2) && pledgeType == -1)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DOESNOT_MEET_REQUIREMENTS_TO_JOIN_ACADEMY).addPcName(target));
			activeChar.sendPacket(SystemMessageId.ACADEMY_REQUIREMENTS);
			return false;
		}

		if (getSubPledgeMembersCount(pledgeType) >= getMaxNrOfMembers(pledgeType))
		{
			if (pledgeType == 0)
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CLAN_IS_FULL).addPcName(target));
			else
				activeChar.sendPacket(SystemMessageId.SUBCLAN_IS_FULL);
			return false;
		}
		return true;
	}

	/**
	 * Checks if activeChar and target meet various conditions to join a clan
	 * 
	 * @param activeChar
	 * @param target
	 * @return
	 */
	public boolean checkAllyJoinCondition(L2PcInstance activeChar, L2PcInstance target)
	{
		if (activeChar == null)
			return false;

		if (activeChar.getAllyId() == 0 || !activeChar.isClanLeader() || activeChar.getClanId() != activeChar.getAllyId())
		{
			activeChar.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
			return false;
		}

		L2Clan leaderClan = activeChar.getClan();
		if (leaderClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (leaderClan.getAllyPenaltyType() == PENALTY_TYPE_DISMISS_CLAN)
			{
				activeChar.sendPacket(SystemMessageId.CANT_INVITE_CLAN_WITHIN_1_DAY);
				return false;
			}
		}

		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.SELECT_USER_TO_INVITE);
			return false;
		}

		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_YOURSELF);
			return false;
		}

		if (target.getClan() == null)
		{
			activeChar.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
			return false;
		}

		if (!target.isClanLeader())
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addPcName(target));
			return false;
		}

		L2Clan targetClan = target.getClan();
		if (target.getAllyId() != 0)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CLAN_ALREADY_MEMBER_OF_S2_ALLIANCE).addString(targetClan.getName()).addString(targetClan.getAllyName()));
			return false;
		}

		if (targetClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_LEAVED)
			{
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANT_ENTER_ALLIANCE_WITHIN_1_DAY).addString(target.getClan().getName()).addString(target.getClan().getAllyName()));
				return false;
			}

			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_DISMISSED)
			{
				activeChar.sendPacket(SystemMessageId.CANT_ENTER_ALLIANCE_WITHIN_1_DAY);
				return false;
			}
		}

		if (activeChar.isInsideZone(L2Character.ZONE_SIEGE) && target.isInsideZone(L2Character.ZONE_SIEGE))
		{
			activeChar.sendPacket(SystemMessageId.OPPOSING_CLAN_IS_PARTICIPATING_IN_SIEGE);
			return false;
		}

		if (leaderClan.isAtWarWith(targetClan.getClanId()))
		{
			activeChar.sendPacket(SystemMessageId.MAY_NOT_ALLY_CLAN_BATTLE);
			return false;
		}

		if (ClanTable.getInstance().getClanAllies(activeChar.getAllyId()).size() >= ClansConfig.ALT_MAX_NUM_OF_CLANS_IN_ALLY)
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_LIMIT);
			return false;
		}

		return true;
	}

	public long getAllyPenaltyExpiryTime()
	{
		return _allyPenaltyExpiryTime;
	}

	public int getAllyPenaltyType()
	{
		return _allyPenaltyType;
	}

	public void setAllyPenaltyExpiryTime(long expiryTime, int penaltyType)
	{
		_allyPenaltyExpiryTime = expiryTime;
		_allyPenaltyType = penaltyType;
	}

	public long getCharPenaltyExpiryTime()
	{
		return _charPenaltyExpiryTime;
	}

	public void setCharPenaltyExpiryTime(long time)
	{
		_charPenaltyExpiryTime = time;
	}

	public long getDissolvingExpiryTime()
	{
		return _dissolvingExpiryTime;
	}

	public void setDissolvingExpiryTime(long time)
	{
		_dissolvingExpiryTime = time;
	}

	public void createAlly(L2PcInstance player, String allyName)
	{
		if (player == null)
			return;

		_log.debug(player.getObjectId() + "(" + player.getName() + ") requested ally creation from ");

		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
			return;
		}

		if (getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.ALREADY_JOINED_ALLIANCE);
			return;
		}

		if (getLevel() < 5)
		{
			player.sendPacket(SystemMessageId.TO_CREATE_AN_ALLY_YOU_CLAN_MUST_BE_LEVEL_5_OR_HIGHER);
			return;
		}

		if (getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (getAllyPenaltyType() == L2Clan.PENALTY_TYPE_DISSOLVE_ALLY)
			{
				player.sendPacket(SystemMessageId.CANT_CREATE_ALLIANCE_10_DAYS_DISOLUTION);
				return;
			}
		}

		if (getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_CREATE_ALLY_WHILE_DISSOLVING);
			return;
		}

		if (!Util.isAlphaNumeric(allyName))
		{
			player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME);
			return;
		}

		if (allyName.length() > 16 || allyName.length() < 2)
		{
			player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME_LENGTH);
			return;
		}

		if (ClanTable.getInstance().isAllyExists(allyName))
		{
			player.sendPacket(SystemMessageId.ALLIANCE_ALREADY_EXISTS);
			return;
		}

		setAllyId(getClanId());
		setAllyName(allyName.trim());
		setAllyPenaltyExpiryTime(0, 0);
		updateClanInDB();

		player.sendPacket(new UserInfo(player));
		player.sendMessage("Alliance " + allyName + " has been created.");
	}

	public void dissolveAlly(L2PcInstance player)
	{
		if (getAllyId() == 0)
		{
			player.sendPacket(SystemMessageId.NO_CURRENT_ALLIANCES);
			return;
		}

		if (!player.isClanLeader() || getClanId() != getAllyId())
		{
			player.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
			return;
		}

		if (player.isInsideZone(L2Character.ZONE_SIEGE))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_ALLY_WHILE_IN_SIEGE);
			return;
		}

		broadcastToOnlineAllyMembers(SystemMessage.getSystemMessage(SystemMessageId.ALLIANCE_DISOLVED));

		long currentTime = System.currentTimeMillis();
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == getAllyId() && clan.getClanId() != getClanId())
			{
				clan.setAllyId(0);
				clan.setAllyName(null);
				clan.setAllyPenaltyExpiryTime(0, 0);
				clan.updateClanInDB();
			}
		}

		setAllyId(0);
		setAllyName(null);
		changeAllyCrest(0, false);
		setAllyPenaltyExpiryTime(currentTime + ClansConfig.ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED * 86400000L, L2Clan.PENALTY_TYPE_DISSOLVE_ALLY);
		updateClanInDB();

		// The clan leader should take the XP penalty of a full death.
		player.deathPenalty(false);
	}

	public boolean levelUpClan(L2PcInstance player)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}

		if (System.currentTimeMillis() < getDissolvingExpiryTime())
		{
			player.sendPacket(SystemMessageId.CANNOT_RISE_LEVEL_WHILE_DISSOLUTION_IN_PROGRESS);
			return false;
		}

		boolean increaseClanLevel = false;

		switch (getLevel())
		{
			case 0:
			{
				// upgrade to 1
				if (player.getSp() >= 30000 && player.getAdena() >= 650000)
				{
					if (player.reduceAdena("ClanLvl", 650000, player.getTarget(), true))
					{
						player.setSp(player.getSp() - 30000);
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(30000));
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 1:
			{
				// upgrade to 2
				if (player.getSp() >= 150000 && player.getAdena() >= 2500000)
				{
					if (player.reduceAdena("ClanLvl", 2500000, player.getTarget(), true))
					{
						player.setSp(player.getSp() - 150000);
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(150000));
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 2:
			{
				// upgrade to 3
				if (player.getSp() >= 500000 && player.getInventory().getItemByItemId(1419) != null)
				{
					// itemid 1419 == proof of blood
					if (player.destroyItemByItemId("ClanLvl", 1419, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 500000);
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(500000));
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(1419).addNumber(1));
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 3:
			{
				// upgrade to 4
				if (player.getSp() >= 1400000 && player.getInventory().getItemByItemId(3874) != null)
				{
					// itemid 3874 == proof of alliance
					if (player.destroyItemByItemId("ClanLvl", 3874, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 1400000);
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(1400000));
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(3874).addNumber(1));
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 4:
			{
				// upgrade to 5
				if (player.getSp() >= 3500000 && player.getInventory().getItemByItemId(3870) != null)
				{
					// itemid 3870 == proof of aspiration
					if (player.destroyItemByItemId("ClanLvl", 3870, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 3500000);
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(3500000));
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(3870).addNumber(1));
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 5:
				if (getReputationScore() >= 10000 && getMembersCount() >= 30)
				{
					takeReputationScore(10000);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(10000));
					increaseClanLevel = true;
				}
				break;

			case 6:
				if (getReputationScore() >= 20000 && getMembersCount() >= 80)
				{
					takeReputationScore(20000);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(20000));
					increaseClanLevel = true;
				}
				break;
			case 7:
				if (getReputationScore() >= 40000 && getMembersCount() >= 120)
				{
					takeReputationScore(40000);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(40000));
					increaseClanLevel = true;
				}
				break;
			default:
				return false;
		}

		if (!increaseClanLevel)
		{
			player.sendPacket(SystemMessageId.FAILED_TO_INCREASE_CLAN_LEVEL);
			return false;
		}

		// the player should know that he has less sp now :p
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.SP, player.getSp());
		player.sendPacket(su);

		player.sendPacket(new ItemList(player, false));

		changeLevel(getLevel() + 1);
		return true;
	}

	public void changeLevel(int level)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET clan_level = ? WHERE clan_id = ?");
			statement.setInt(1, level);
			statement.setInt(2, getClanId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not increase clan level:" + e.getMessage(), e);
		}

		setLevel(level);

		if (getLeader().isOnline())
		{
			L2PcInstance leader = getLeader().getPlayerInstance();
			if (3 < level)
				SiegeManager.addSiegeSkills(leader);
			else if (4 > level)
				SiegeManager.removeSiegeSkills(leader);

			if (4 < level)
				leader.sendPacket(SystemMessageId.CLAN_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS);
		}

		// notify all the members about it
		broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_LEVEL_INCREASED));
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
	}

	/**
	 * Change the clan crest. If crest id is 0, crest is removed. New crest id is saved to database.
	 * 
	 * @param crestId
	 *            if 0, crest is removed, else new crest id is set and saved to database
	 */
	public void changeClanCrest(int crestId)
	{
		if (getCrestId() != 0)
			CrestCache.removeCrest(CrestType.PLEDGE, getCrestId());

		setCrestId(crestId);

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?");
			statement.setInt(1, crestId);
			statement.setInt(2, getClanId());
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn("Could not update crest for clan " + getName() + " [" + getClanId() + "] : " + e.getMessage(), e);
		}

		for (L2PcInstance member : getOnlineMembers(0))
			member.broadcastUserInfo();
	}

	/**
	 * Change the ally crest. If crest id is 0, crest is removed. New crest id is saved to database.
	 * 
	 * @param crestId
	 *            if 0, crest is removed, else new crest id is set and saved to database
	 * @param onlyThisClan
	 *            Do it for the ally aswell.
	 */
	public void changeAllyCrest(int crestId, boolean onlyThisClan)
	{
		String sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE clan_id = ?";
		int allyId = getClanId();
		if (!onlyThisClan)
		{
			if (getAllyCrestId() != 0)
				CrestCache.removeCrest(CrestType.ALLY, getAllyCrestId());

			sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE ally_id = ?";
			allyId = getAllyId();
		}

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(sqlStatement);
			statement.setInt(1, crestId);
			statement.setInt(2, allyId);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn("Could not update ally crest for ally/clan id " + allyId + " : " + e.getMessage(), e);
		}

		if (onlyThisClan)
		{
			setAllyCrestId(crestId);
			for (L2PcInstance member : getOnlineMembers(0))
				member.broadcastUserInfo();
		}
		else
		{
			for (L2Clan clan : ClanTable.getInstance().getClans())
			{
				if (clan.getAllyId() == getAllyId())
				{
					clan.setAllyCrestId(crestId);
					for (L2PcInstance member : clan.getOnlineMembers(0))
						member.broadcastUserInfo();
				}
			}
		}
	}

	/**
	 * Change the large crest. If crest id is 0, crest is removed. New crest id is saved to database.
	 * 
	 * @param crestId
	 *            if 0, crest is removed, else new crest id is set and saved to database
	 */
	public void changeLargeCrest(int crestId)
	{
		if (getCrestLargeId() != 0)
			CrestCache.removeCrest(CrestType.PLEDGE_LARGE, getCrestLargeId());

		setCrestLargeId(crestId);

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_large_id = ? WHERE clan_id = ?");
			statement.setInt(1, crestId);
			statement.setInt(2, getClanId());
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn("Could not update large crest for clan " + getName() + " [" + getClanId() + "] : " + e.getMessage(), e);
		}

		for (L2PcInstance member : getOnlineMembers(0))
			member.broadcastUserInfo();
	}

	private void checkCrests()
	{
		if (getCrestId() != 0)
		{
			if (CrestCache.getCrest(CrestType.PLEDGE, getCrestId()) == null)
			{
				_log.info("Removing non-existent crest for clan " + getName() + " [" + getClanId() + "], crestId:" + getCrestId());
				setCrestId(0);
				changeClanCrest(0);
			}
		}

		if (getCrestLargeId() != 0)
		{
			if (CrestCache.getCrest(CrestType.PLEDGE_LARGE, getCrestLargeId()) == null)
			{
				_log.info("Removing non-existent large crest for clan " + getName() + " [" + getClanId() + "], crestLargeId:" + getCrestLargeId());
				setCrestLargeId(0);
				changeLargeCrest(0);
			}
		}

		if (getAllyCrestId() != 0)
		{
			if (CrestCache.getCrest(CrestType.ALLY, getAllyCrestId()) == null)
			{
				_log.info("Removing non-existent ally crest for clan " + getName() + " [" + getClanId() + "], allyCrestId:" + getAllyCrestId());
				setAllyCrestId(0);
				changeAllyCrest(0, true);
			}
		}
	}
}
