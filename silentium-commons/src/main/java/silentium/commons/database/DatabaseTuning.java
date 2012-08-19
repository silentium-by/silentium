/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.commons.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.ServerType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author ProGramMoS (created the source)
 * @author Aleff (fixed the source)
 * @author Demon (rework)
 * @author Tatanka (full rework)
 */
public class DatabaseTuning {
	private static final Logger log = LoggerFactory.getLogger(DatabaseTuning.class.getName());

	// TODO сделать так, чтобы все нужные таблицы сами находились.
	private static final String GAME_SERVER_TABLES = "auction, auction_bid, augmentations, castle, " + "castle_doorupgrade, castle_manor_procure, castle_manor_production, castle_siege_guards, " + "character_friends, character_hennas, character_macroses, character_mail, character_premium, " + "character_quest_global_data, character_quests, character_raid_points, character_recipebook, " + "character_recommends, character_shortcuts, character_skills, character_skills_save, " + "character_subclasses, characters, clan_data, clan_notices, clan_privs, clan_skills, clan_subpledges, "
			+ "clan_wars, clanhall, clanhall_functions, cursed_weapons, dimensional_rift, droplist, forums, games, " + "global_tasks, grandboss_data, grandboss_list, heroes, heroes_diary, items, itemsonground, " + "merchant_areas_list, merchant_buylists, merchant_shopids, merchants, minions, mods_wedding, npc, " + "npc_ai_data, npc_skills, olympiad_data, olympiad_fights, olympiad_nobles, olympiad_nobles_eom, pets, " + "posts, quest_global_data, raidboss_spawnlist, random_spawn, random_spawn_loc, seven_signs, " + "seven_signs_festival, seven_signs_status, siege_clans, skill_learn, spawnlist, spawnlist_4s, topic";
	private static final String AUTH_SERVER_TABLES = "accounts, gameservers";

	private static final String GAME_SERVER_TABLES_OPTIMIZE_QUERY = "OPTIMIZE TABLE " + GAME_SERVER_TABLES;
	private static final String GAME_SERVER_TABLES_CHECK_QUERY = "CHECK TABLE " + GAME_SERVER_TABLES;
	private static final String GAME_SERVER_TABLES_REPAIR_QUERY = "REPAIR TABLE " + GAME_SERVER_TABLES;

	private static final String AUTH_SERVER_TABLES_OPTIMIZE_QUERY = "OPTIMIZE TABLE " + AUTH_SERVER_TABLES;
	private static final String AUTH_SERVER_TABLES_CHECK_QUERY = "CHECK TABLE " + AUTH_SERVER_TABLES;
	private static final String AUTH_SERVER_TABLES_REPAIR_QUERY = "REPAIR TABLE " + AUTH_SERVER_TABLES;

	public static void start() {
		log.info("Start tuning database...");

		for (final DatabaseTuningType tuningType : DatabaseTuningType.values())
			tuningType.tune();
	}

	private enum DatabaseTuningType {
		OPTIMIZE(GAME_SERVER_TABLES_OPTIMIZE_QUERY, AUTH_SERVER_TABLES_OPTIMIZE_QUERY), CHECK(GAME_SERVER_TABLES_CHECK_QUERY, AUTH_SERVER_TABLES_CHECK_QUERY), REPAIR(GAME_SERVER_TABLES_REPAIR_QUERY, AUTH_SERVER_TABLES_REPAIR_QUERY);

		private final String gameServerQuery;
		private final String authServerQuery;

		DatabaseTuningType(final String gameServerQuery, final String authServerQuery) {
			this.gameServerQuery = gameServerQuery;
			this.authServerQuery = authServerQuery;
		}

		void tune() {
			try (Connection con = DatabaseFactory.getConnection()) {
				final PreparedStatement statement;

				if (ServerType.SERVER_TYPE == ServerType.GAMESERVER)
					statement = con.prepareStatement(gameServerQuery);
				else if (ServerType.SERVER_TYPE == ServerType.AUTHSERVER)
					statement = con.prepareStatement(authServerQuery);
				else
					return;

				statement.execute();
			} catch (SQLException e) {
				log.info("Tuning failed." + e.getLocalizedMessage());
			}
		}
	}
}
