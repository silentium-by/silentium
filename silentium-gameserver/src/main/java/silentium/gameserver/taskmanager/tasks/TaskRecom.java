/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.taskmanager.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.UserInfo;
import silentium.gameserver.taskmanager.Task;
import silentium.gameserver.taskmanager.TaskManager;
import silentium.gameserver.taskmanager.TaskManager.ExecutedTask;
import silentium.gameserver.taskmanager.TaskTypes;

import java.util.Collection;

/**
 * @author Layane
 */
public class TaskRecom extends Task
{
	private static final Logger _log = LoggerFactory.getLogger(TaskRecom.class.getName());
	private static final String NAME = "sp_recommendations";

	/*
	 * (non-Javadoc)
	 * @see silentium.gameserver.taskmanager.Task#getName()
	 */
	@Override
	public String getName()
	{
		return NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see silentium.gameserver.taskmanager.Task#onTimeElapsed(silentium.gameserver.taskmanager.TaskManager.ExecutedTask)
	 */
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();

		for (L2PcInstance player : pls)
		{
			player.restartRecom();
			player.sendPacket(new UserInfo(player));
		}
		_log.info("Recommendation Global Task: launched.");
	}

	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "06:30:00", "");
	}
}
