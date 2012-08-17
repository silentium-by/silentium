/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.taskmanager.tasks;

import java.util.Calendar;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.instancemanager.RaidBossPointsManager;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.tables.ClanTable;
import silentium.gameserver.taskmanager.Task;
import silentium.gameserver.taskmanager.TaskManager;
import silentium.gameserver.taskmanager.TaskManager.ExecutedTask;
import silentium.gameserver.taskmanager.TaskTypes;

public class TaskRaidPointsReset extends Task
{
	private static final Logger _log = LoggerFactory.getLogger(TaskRaidPointsReset.class.getName());
	public static final String NAME = "raid_points_reset";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		Calendar cal = Calendar.getInstance();

		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
		{
			// reward clan reputation points
			Map<Integer, Integer> rankList = RaidBossPointsManager.getInstance().getRankList();
			for (L2Clan c : ClanTable.getInstance().getClans())
			{
				for (Map.Entry<Integer, Integer> entry : rankList.entrySet())
				{
					if (entry.getValue() <= 100 && c.isMember(entry.getKey()))
					{
						int reputation = 0;
						switch (entry.getValue())
						{
							case 1:
								reputation = 1250;
								break;
							case 2:
								reputation = 900;
								break;
							case 3:
								reputation = 700;
								break;
							case 4:
								reputation = 600;
								break;
							case 5:
								reputation = 450;
								break;
							case 6:
								reputation = 350;
								break;
							case 7:
								reputation = 300;
								break;
							case 8:
								reputation = 200;
								break;
							case 9:
								reputation = 150;
								break;
							case 10:
								reputation = 100;
								break;
							default:
								if (entry.getValue() <= 50)
									reputation = 25;
								else
									reputation = 12;
								break;
						}
						c.addReputationScore(reputation);
					}
				}
			}

			RaidBossPointsManager.getInstance().cleanUp();
			_log.info("Raid Points Reset Global Task: launched.");
		}
	}

	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "00:10:00", "");
	}
}
