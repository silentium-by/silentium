/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.utils.StringUtil;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.instancemanager.DayNightSpawnManager;
import silentium.gameserver.instancemanager.RaidBossSpawnManager;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Spawn;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.sevensigns.SevenSigns;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.GmListTable;
import silentium.gameserver.tables.NpcTable;
import silentium.gameserver.tables.SpawnTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;
import silentium.gameserver.utils.Broadcast;

/**
 * This class handles following admin commands:<br>
 * - show_spawns = shows menu<br>
 * - spawn_index lvl = shows menu for monsters with respective level<br>
 * - spawn id = spawns monster id on target
 */
public class AdminSpawn implements IAdminCommandHandler
{
	public static Logger _log = LoggerFactory.getLogger(AdminSpawn.class.getName());
	private static final String[] ADMIN_COMMANDS = { "admin_show_spawns", "admin_spawn", "admin_spawn_index", "admin_unspawnall", "admin_respawnall", "admin_spawn_reload", "admin_npc_index", "admin_spawn_once", "admin_show_npcs", "admin_spawnnight", "admin_spawnday" };

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_show_spawns"))
			AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
		else if (command.startsWith("admin_spawn_index"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				int level = Integer.parseInt(st.nextToken());
				int from = 0;
				try
				{
					from = Integer.parseInt(st.nextToken());
				}
				catch (NoSuchElementException nsee)
				{
				}
				showMonsters(activeChar, level, from);
			}
			catch (Exception e)
			{
				AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
			}
		}
		else if (command.equals("admin_show_npcs"))
			AdminHelpPage.showHelpPage(activeChar, "npcs.htm");
		else if (command.startsWith("admin_npc_index"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				String letter = st.nextToken();
				int from = 0;
				try
				{
					from = Integer.parseInt(st.nextToken());
				}
				catch (NoSuchElementException nsee)
				{
				}
				showNpcs(activeChar, letter, from);
			}
			catch (Exception e)
			{
				AdminHelpPage.showHelpPage(activeChar, "npcs.htm");
			}
		}
		else if (command.startsWith("admin_spawn"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				String cmd = st.nextToken();
				String id = st.nextToken();
				int respawnTime = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;

				if (cmd.equalsIgnoreCase("admin_spawn_once"))
					spawn(activeChar, id, respawnTime, false);
				else
					spawn(activeChar, id, respawnTime, true);
			}
			catch (Exception e)
			{
				AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
			}
		}
		else if (command.startsWith("admin_unspawnall"))
		{
			Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.NPC_SERVER_NOT_OPERATING));
			RaidBossSpawnManager.getInstance().cleanUp();
			DayNightSpawnManager.getInstance().cleanUp();
			L2World.getInstance().deleteVisibleNpcSpawns();
			GmListTable.broadcastMessageToGMs("NPCs' unspawn is now complete.");
		}
		else if (command.startsWith("admin_spawnday"))
			DayNightSpawnManager.getInstance().spawnDayCreatures();
		else if (command.startsWith("admin_spawnnight"))
			DayNightSpawnManager.getInstance().spawnNightCreatures();
		else if (command.startsWith("admin_respawnall") || command.startsWith("admin_spawn_reload"))
		{
			// make sure all spawns are deleted
			RaidBossSpawnManager.getInstance().cleanUp();
			DayNightSpawnManager.getInstance().cleanUp();
			L2World.getInstance().deleteVisibleNpcSpawns();
			// now respawn all
			NpcTable.getInstance().reloadAllNpc();
			SpawnTable.getInstance().reloadAll();
			RaidBossSpawnManager.getInstance().reloadBosses();
			SevenSigns.getInstance().spawnSevenSignsNPC();
			GmListTable.broadcastMessageToGMs("NPCs' respawn is now complete.");
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private static void spawn(L2PcInstance activeChar, String monsterId, int respawnTime, boolean permanent)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;

		L2NpcTemplate template;

		if (monsterId.matches("[0-9]*")) // First parameter was an ID number
			template = NpcTable.getInstance().getTemplate(Integer.parseInt(monsterId));
		else
		// First parameter wasn't just numbers, so go by name not ID
		{
			monsterId = monsterId.replace('_', ' ');
			template = NpcTable.getInstance().getTemplateByName(monsterId);
		}

		try
		{
			L2Spawn spawn = new L2Spawn(template);
			spawn.setLocx(target.getX());
			spawn.setLocy(target.getY());
			spawn.setLocz(target.getZ());
			spawn.setHeading(activeChar.getHeading());
			spawn.setRespawnDelay(respawnTime);

			if (RaidBossSpawnManager.getInstance().getValidTemplate(spawn.getNpcId()) != null)
			{
				if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcId()))
				{
					activeChar.sendMessage("You cannot spawn another instance of " + template.getName() + ".");
					return;
				}

				spawn.setRespawnMinDelay(43200);
				spawn.setRespawnMaxDelay(129600);
				RaidBossSpawnManager.getInstance().addNewSpawn(spawn, 0, template.getBaseHpMax(), template.getBaseMpMax(), permanent);
			}
			else
			{
				SpawnTable.getInstance().addNewSpawn(spawn, permanent);
				spawn.init();
			}

			if (!permanent)
				spawn.stopRespawn();

			activeChar.sendMessage("Spawned " + template.getName() + ".");

		}
		catch (Exception e)
		{
			activeChar.sendPacket(SystemMessageId.APPLICANT_INFORMATION_INCORRECT);
		}
	}

	private static void showMonsters(L2PcInstance activeChar, int level, int from)
	{
		final List<L2NpcTemplate> mobs = NpcTable.getInstance().getAllMonstersOfLevel(level);
		final int mobsCount = mobs.size();
		final StringBuilder tb = StringUtil.startAppend(500 + mobsCount * 80, "<html><title>Spawn Monster:</title><body><p> Level : ", Integer.toString(level), "<br>Total Npc's : ", Integer.toString(mobsCount), "<br>");

		// Loop
		int i = from;
		for (int j = 0; i < mobsCount && j < 50; i++, j++)
			StringUtil.append(tb, "<a action=\"bypass -h admin_spawn ", Integer.toString(mobs.get(i).getNpcId()), "\">", mobs.get(i).getName(), "</a><br1>");

		if (i == mobsCount)
			tb.append("<br><center><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		else
			StringUtil.append(tb, "<br><center><button value=\"Next\" action=\"bypass -h admin_spawn_index ", Integer.toString(level), " ", Integer.toString(i), "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");

		activeChar.sendPacket(new NpcHtmlMessage(5, tb.toString()));
	}

	private static void showNpcs(L2PcInstance activeChar, String starting, int from)
	{
		final List<L2NpcTemplate> mobs = NpcTable.getInstance().getAllNpcStartingWith(starting);
		final int mobsCount = mobs.size();
		final StringBuilder tb = StringUtil.startAppend(500 + mobsCount * 80, "<html><title>Spawn Monster:</title><body><p> There are ", Integer.toString(mobsCount), " Npcs whose name starts with ", starting, ":<br>");

		// Loop
		int i = from;
		for (int j = 0; i < mobsCount && j < 50; i++, j++)
			StringUtil.append(tb, "<a action=\"bypass -h admin_spawn ", Integer.toString(mobs.get(i).getNpcId()), "\">", mobs.get(i).getName(), "</a><br1>");

		if (i == mobsCount)
			tb.append("<br><center><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		else
			StringUtil.append(tb, "<br><center><button value=\"Next\" action=\"bypass -h admin_npc_index ", starting, " ", Integer.toString(i), "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");

		activeChar.sendPacket(new NpcHtmlMessage(5, tb.toString()));
	}
}
