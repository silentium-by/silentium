/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.idfactory;

import gnu.trove.list.array.TIntArrayList;
import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.configs.MainConfig;

import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IdFactory
{
	private static Logger _log = LoggerFactory.getLogger(IdFactory.class.getName());

	protected static final String[] ID_CHECKS = { "SELECT owner_id FROM items WHERE object_id >= ? AND object_id < ?", "SELECT object_id FROM items WHERE object_id >= ? AND object_id < ?", "SELECT charId FROM character_quests WHERE charId >= ? AND charId < ?", "SELECT char_id FROM character_friends WHERE char_id >= ? AND char_id < ?", "SELECT char_id FROM character_friends WHERE friend_id >= ? AND friend_id < ?", "SELECT char_obj_id FROM character_hennas WHERE char_obj_id >= ? AND char_obj_id < ?", "SELECT char_id FROM character_recipebook WHERE char_id >= ? AND char_id < ?",
			"SELECT char_obj_id FROM character_shortcuts WHERE char_obj_id >= ? AND char_obj_id < ?", "SELECT char_obj_id FROM character_macroses WHERE char_obj_id >= ? AND char_obj_id < ?", "SELECT char_obj_id FROM character_skills WHERE char_obj_id >= ? AND char_obj_id < ?", "SELECT char_obj_id FROM character_skills_save WHERE char_obj_id >= ? AND char_obj_id < ?", "SELECT char_obj_id FROM character_subclasses WHERE char_obj_id >= ? AND char_obj_id < ?", "SELECT obj_Id FROM characters WHERE obj_Id >= ? AND obj_Id < ?", "SELECT clanid FROM characters WHERE clanid >= ? AND clanid < ?", "SELECT clan_id FROM clan_data WHERE clan_id >= ? AND clan_id < ?",
			"SELECT clan_id FROM siege_clans WHERE clan_id >= ? AND clan_id < ?", "SELECT ally_id FROM clan_data WHERE ally_id >= ? AND ally_id < ?", "SELECT leader_id FROM clan_data WHERE leader_id >= ? AND leader_id < ?", "SELECT item_obj_id FROM pets WHERE item_obj_id >= ? AND item_obj_id < ?", "SELECT object_id FROM itemsonground WHERE object_id >= ? AND object_id < ?" };

	private static final String[] TIMESTAMPS_CLEAN = { "DELETE FROM character_skills_save WHERE restore_type = 1 AND systime <= ?" };

	protected boolean _initialized;

	public static final int FIRST_OID = 0x10000000;
	public static final int LAST_OID = 0x7FFFFFFF;
	public static final int FREE_OBJECT_ID_SIZE = LAST_OID - FIRST_OID;

	protected static IdFactory _instance = null;

	protected IdFactory()
	{
		setAllCharacterOffline();
		cleanUpDB();
		cleanUpTimeStamps();
	}

	static
	{
		switch (MainConfig.IDFACTORY_TYPE)
		{
			case BitSet:
				_instance = new BitSetIDFactory();
				break;
			case Stack:
				_instance = new StackIDFactory();
				break;
			default:
				_instance = new BitSetIDFactory();
				break;
		}
	}

	/**
	 * Sets all character offline
	 */
	private static void setAllCharacterOffline()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			Statement statement = con.createStatement();
			statement.executeUpdate("UPDATE characters SET online = 0");
			statement.close();

			_log.info("Updated characters online status.");
		}
		catch (SQLException e)
		{
		}
	}

	/**
	 * Cleans up Database
	 */
	private static void cleanUpDB()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			int cleanCount = 0;
			Statement stmt = con.createStatement();

			// Character related
			cleanCount += stmt.executeUpdate("DELETE FROM augmentations WHERE augmentations.item_id NOT IN (SELECT object_id FROM items);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_friends WHERE character_friends.char_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_friends WHERE character_friends.friend_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_hennas WHERE character_hennas.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_macroses WHERE character_macroses.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_quests WHERE character_quests.charId NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_quest_global_data WHERE character_quest_global_data.charId NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_raid_points WHERE character_raid_points.char_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_recipebook WHERE character_recipebook.char_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_shortcuts WHERE character_shortcuts.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_skills WHERE character_skills.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_skills_save WHERE character_skills_save.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_subclasses WHERE character_subclasses.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM cursed_weapons WHERE cursed_weapons.playerId NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM pets WHERE pets.item_obj_id NOT IN (SELECT object_id FROM items);");
			cleanCount += stmt.executeUpdate("DELETE FROM seven_signs WHERE seven_signs.char_obj_id NOT IN (SELECT obj_Id FROM characters);");

			// Olympiads & Heroes
			cleanCount += stmt.executeUpdate("DELETE FROM heroes WHERE heroes.char_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM olympiad_nobles WHERE olympiad_nobles.char_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM olympiad_nobles_eom WHERE olympiad_nobles_eom.char_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM olympiad_fights WHERE olympiad_fights.charOneId NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM olympiad_fights WHERE olympiad_fights.charTwoId NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM heroes_diary WHERE heroes_diary.char_id NOT IN (SELECT obj_Id FROM characters);");

			// Auction
			cleanCount += stmt.executeUpdate("DELETE FROM auction WHERE auction.id IN (SELECT id FROM clanhall WHERE ownerId <> 0);");
			cleanCount += stmt.executeUpdate("DELETE FROM auction_bid WHERE auctionId IN (SELECT id FROM clanhall WHERE ownerId <> 0)");

			// Clan related
			cleanCount += stmt.executeUpdate("DELETE FROM clan_data WHERE clan_data.leader_id NOT IN (SELECT obj_Id FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM auction_bid WHERE auction_bid.bidderId NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clanhall_functions WHERE clanhall_functions.hall_id NOT IN (SELECT id FROM clanhall WHERE ownerId <> 0);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_privs WHERE clan_privs.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_skills WHERE clan_skills.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_subpledges WHERE clan_subpledges.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_wars WHERE clan_wars.clan1 NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_wars WHERE clan_wars.clan2 NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM siege_clans WHERE siege_clans.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_notices WHERE clan_notices.clan_id NOT IN (SELECT clan_id FROM clan_data);");

			// Items
			cleanCount += stmt.executeUpdate("DELETE FROM items WHERE items.owner_id NOT IN (SELECT obj_Id FROM characters) AND items.owner_id NOT IN (SELECT clan_id FROM clan_data);");

			// Forum related
			cleanCount += stmt.executeUpdate("DELETE FROM forums WHERE forums.forum_owner_id NOT IN (SELECT clan_id FROM clan_data) AND forums.forum_parent=2;");
			cleanCount += stmt.executeUpdate("DELETE FROM topic WHERE topic.topic_forum_id NOT IN (SELECT forum_id FROM forums);");
			cleanCount += stmt.executeUpdate("DELETE FROM posts WHERE posts.post_forum_id NOT IN (SELECT forum_id FROM forums);");

			stmt.executeUpdate("UPDATE clan_data SET auction_bid_at = 0 WHERE auction_bid_at NOT IN (SELECT auctionId FROM auction_bid);");
			stmt.executeUpdate("UPDATE clan_subpledges SET leader_id=0 WHERE clan_subpledges.leader_id NOT IN (SELECT obj_Id FROM characters) AND leader_id > 0;");
			stmt.executeUpdate("UPDATE castle SET taxpercent=0 WHERE castle.id NOT IN (SELECT hasCastle FROM clan_data);");
			stmt.executeUpdate("UPDATE characters SET clanid=0 WHERE characters.clanid NOT IN (SELECT clan_id FROM clan_data);");
			stmt.executeUpdate("UPDATE clanhall SET ownerId=0, paidUntil=0, paid=0 WHERE clanhall.ownerId NOT IN (SELECT clan_id FROM clan_data);");

			stmt.close();
			_log.info("Cleaned " + cleanCount + " elements from database.");
		}
		catch (SQLException e)
		{
		}
	}

	private static void cleanUpTimeStamps()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement stmt = null;
			int cleanCount = 0;

			for (String line : TIMESTAMPS_CLEAN)
			{
				stmt = con.prepareStatement(line);
				stmt.setLong(1, System.currentTimeMillis());
				cleanCount += stmt.executeUpdate();
				stmt.close();
			}

			_log.info("Cleaned " + cleanCount + " expired timestamps from database.");
		}
		catch (SQLException e)
		{
		}
	}

	protected final static int[] extractUsedObjectIDTable() throws Exception
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final TIntArrayList temp = new TIntArrayList();

			Statement statement = con.createStatement();
			ResultSet rset = statement.executeQuery("SELECT COUNT(*) FROM characters");
			rset.next();

			temp.ensureCapacity(rset.getInt(1));

			rset = statement.executeQuery("SELECT obj_Id FROM characters");
			while (rset.next())
				temp.add(rset.getInt(1));

			rset = statement.executeQuery("SELECT COUNT(*) FROM items");
			rset.next();
			temp.ensureCapacity(temp.size() + rset.getInt(1));
			rset = statement.executeQuery("SELECT object_id FROM items");
			while (rset.next())
				temp.add(rset.getInt(1));

			rset = statement.executeQuery("SELECT COUNT(*) FROM clan_data");
			rset.next();
			temp.ensureCapacity(temp.size() + rset.getInt(1));
			rset = statement.executeQuery("SELECT clan_id FROM clan_data");
			while (rset.next())
				temp.add(rset.getInt(1));

			rset = statement.executeQuery("SELECT COUNT(*) FROM itemsonground");
			rset.next();
			temp.ensureCapacity(temp.size() + rset.getInt(1));
			rset = statement.executeQuery("SELECT object_id FROM itemsonground");
			while (rset.next())
				temp.add(rset.getInt(1));

			temp.sort();

			return temp.toArray();
		}
	}

	public boolean isInitialized()
	{
		return _initialized;
	}

	public static IdFactory getInstance()
	{
		return _instance;
	}

	public abstract int getNextId();

	public abstract void releaseId(int id);

	public abstract int size();
}
