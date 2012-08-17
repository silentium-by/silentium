/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.concurrent.ScheduledFuture;

import silentium.commons.utils.Rnd;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.knownlist.MonsterKnownList;
import silentium.gameserver.templates.chars.L2NpcTemplate;
import silentium.gameserver.utils.MinionList;

/**
 * This class manages all Monsters. L2MonsterInstance :<BR>
 * <BR>
 * <li>L2RaidBossInstance</li> <li>L2GrandBossInstance</li>
 */
public class L2MonsterInstance extends L2Attackable
{
	protected boolean _enableMinions = true;

	private L2MonsterInstance _master = null;
	private MinionList _minionList = null;

	protected ScheduledFuture<?> _maintenanceTask = null;
	private static final int MONSTER_MAINTENANCE_INTERVAL = 1000;

	/**
	 * Constructor of L2MonsterInstance (use L2Character and L2NpcInstance constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the L2MonsterInstance (copy skills from template to object and link
	 * _calculators to NPC_STD_CALCULATOR)</li> <li>Set the name of the L2MonsterInstance</li> <li>Create a RandomAnimation Task that will be
	 * launched after the calculated delay if the server allow it</li><BR>
	 * <BR>
	 * 
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param template
	 *            L2NpcTemplate to apply to the NPC
	 */
	public L2MonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void initKnownList()
	{
		setKnownList(new MonsterKnownList(this));
	}

	@Override
	public final MonsterKnownList getKnownList()
	{
		return (MonsterKnownList) super.getKnownList();
	}

	/**
	 * Return True if the attacker is not another L2MonsterInstance.<BR>
	 * <BR>
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		// FIXME: to test to allow monsters hit others monsters
		if (attacker instanceof L2MonsterInstance)
			return false;

		return true;
	}

	/**
	 * Return True if the L2MonsterInstance is Agressive (aggroRange > 0).<BR>
	 * <BR>
	 */
	@Override
	public boolean isAggressive()
	{
		return (getAggroRange() > 0);
	}

	@Override
	public void onSpawn()
	{
		if (!isTeleporting())
		{
			if (getLeader() != null)
			{
				setIsNoRndWalk(true);
				setIsRaidMinion(getLeader().isRaid());
				getLeader().getMinionList().onMinionSpawn(this);
			}

			// delete spawned minions before dynamic minions spawned by script
			if (hasMinions())
				getMinionList().onMasterSpawn();

			startMaintenanceTask();
		}

		// dynamic script-based minions spawned here, after all preparations.
		super.onSpawn();
	}

	@Override
	public void onTeleported()
	{
		super.onTeleported();

		if (hasMinions())
			getMinionList().onMasterTeleported();
	}

	protected int getMaintenanceInterval()
	{
		return MONSTER_MAINTENANCE_INTERVAL;
	}

	/**
	 * Spawn all minions at a regular interval
	 */
	protected void startMaintenanceTask()
	{
		// maintenance task now used only for minions spawn
		if (getTemplate().getMinionData() == null)
			return;

		if (_maintenanceTask == null)
		{
			_maintenanceTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					if (_enableMinions)
						getMinionList().spawnMinions();
				}
			}, getMaintenanceInterval() + Rnd.get(1000));
		}
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		if (_maintenanceTask != null)
		{
			_maintenanceTask.cancel(false); // doesn't do it?
			_maintenanceTask = null;
		}

		return true;
	}

	@Override
	public void deleteMe()
	{
		if (_maintenanceTask != null)
		{
			_maintenanceTask.cancel(false);
			_maintenanceTask = null;
		}

		if (hasMinions())
			getMinionList().onMasterDie(true);

		if (getLeader() != null)
			getLeader().getMinionList().onMinionDie(this, 0);

		super.deleteMe();
	}

	@Override
	public L2MonsterInstance getLeader()
	{
		return _master;
	}

	public void setLeader(L2MonsterInstance leader)
	{
		_master = leader;
	}

	public void enableMinions(boolean b)
	{
		_enableMinions = b;
	}

	public boolean hasMinions()
	{
		return _minionList != null;
	}

	public MinionList getMinionList()
	{
		if (_minionList == null)
			_minionList = new MinionList(this);

		return _minionList;
	}
}