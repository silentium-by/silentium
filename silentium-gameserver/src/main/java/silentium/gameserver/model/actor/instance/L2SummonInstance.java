/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.network.serverpackets.SetSummonRemainTime;
import silentium.gameserver.skills.l2skills.L2SkillSummon;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;

import java.util.concurrent.Future;

public class L2SummonInstance extends L2Summon
{
	protected static final Logger log = LoggerFactory.getLogger(L2SummonInstance.class.getName());

	private float _expPenalty = 0; // exp decrease multiplier (i.e. 0.3 (= 30%) for shadow)
	private int _itemConsumeId;
	private int _itemConsumeCount;
	private int _itemConsumeSteps;
	private final int _totalLifeTime;
	private final int _timeLostIdle;
	private final int _timeLostActive;
	private int _timeRemaining;
	private int _nextItemConsumeTime;
	public int lastShowntimeRemaining; // Following FbiAgent's example to avoid sending useless packets

	private Future<?> _summonLifeTask;

	public L2SummonInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2Skill skill)
	{
		super(objectId, template, owner);
		setShowSummonAnimation(true);

		if (skill != null)
		{
			final L2SkillSummon summonSkill = (L2SkillSummon) skill;
			_itemConsumeId = summonSkill.getItemConsumeIdOT();
			_itemConsumeCount = summonSkill.getItemConsumeOT();
			_itemConsumeSteps = summonSkill.getItemConsumeSteps();
			_totalLifeTime = summonSkill.getTotalLifeTime();
			_timeLostIdle = summonSkill.getTimeLostIdle();
			_timeLostActive = summonSkill.getTimeLostActive();
		}
		else
		{
			// defaults
			_itemConsumeId = 0;
			_itemConsumeCount = 0;
			_itemConsumeSteps = 0;
			_totalLifeTime = 1200000; // 20 minutes
			_timeLostIdle = 1000;
			_timeLostActive = 1000;
		}
		_timeRemaining = _totalLifeTime;
		lastShowntimeRemaining = _totalLifeTime;

		if (_itemConsumeId == 0)
			_nextItemConsumeTime = -1; // do not consume
		else if (_itemConsumeSteps == 0)
			_nextItemConsumeTime = -1; // do not consume
		else
			_nextItemConsumeTime = _totalLifeTime - _totalLifeTime / (_itemConsumeSteps + 1);

		// When no item consume is defined task only need to check when summon life time has ended.
		// Otherwise have to destroy items from owner's inventory in order to let summon live.
		int delay = 1000;

		if (_itemConsumeCount != 0)
			_log.debug("L2SummonInstance: Item Consume ID: " + _itemConsumeId + ", Count: " + _itemConsumeCount + ", " +
					"Rate: " + _itemConsumeSteps + " times.");

		_log.debug("L2SummonInstance: Task Delay " + (delay / 1000) + " seconds.");

		_summonLifeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new SummonLifetime(getOwner(), this), delay, delay);
	}

	@Override
	public final int getLevel()
	{
		return (getTemplate() != null ? getTemplate().getLevel() : 0);
	}

	@Override
	public int getSummonType()
	{
		return 1;
	}

	public void setExpPenalty(float expPenalty)
	{
		_expPenalty = expPenalty;
	}

	public float getExpPenalty()
	{
		return _expPenalty;
	}

	public int getItemConsumeCount()
	{
		return _itemConsumeCount;
	}

	public int getItemConsumeId()
	{
		return _itemConsumeId;
	}

	public int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}

	public int getNextItemConsumeTime()
	{
		return _nextItemConsumeTime;
	}

	public int getTotalLifeTime()
	{
		return _totalLifeTime;
	}

	public int getTimeLostIdle()
	{
		return _timeLostIdle;
	}

	public int getTimeLostActive()
	{
		return _timeLostActive;
	}

	public int getTimeRemaining()
	{
		return _timeRemaining;
	}

	public void setNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime = value;
	}

	public void decNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime -= value;
	}

	public void decTimeRemaining(int value)
	{
		_timeRemaining -= value;
	}

	public void addExpAndSp(int addToExp, int addToSp)
	{
		getOwner().addExpAndSp(addToExp, addToSp);
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		_log.debug("L2SummonInstance: " + getTemplate().getName() + " (" + getOwner().getName() + ") has been killed.");

		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
			_summonLifeTask = null;
		}
		return true;

	}

	/**
	 * Servitors' skills automatically change their level based on the servitor's level. Until level 70, the servitor gets 1 lv of
	 * skill per 10 levels. After that, it is 1 skill level per 5 servitor levels. If the resulting skill level doesn't exist use
	 * the max that does exist!
	 *
	 * @see silentium.gameserver.model.actor.L2Character#doCast(silentium.gameserver.model.L2Skill)
	 */
	@Override
	public void doCast(L2Skill skill)
	{
		final int petLevel = getLevel();
		int skillLevel = petLevel / 10;
		if (petLevel >= 70)
			skillLevel += (petLevel - 65) / 10;

		// adjust the level for servitors less than lv 10
		if (skillLevel < 1)
			skillLevel = 1;

		final L2Skill skillToCast = SkillTable.getInstance().getInfo(skill.getId(), skillLevel);

		if (skillToCast != null)
			super.doCast(skillToCast);
		else
			super.doCast(skill);
	}

	static class SummonLifetime implements Runnable
	{
		private final L2PcInstance _activeChar;
		private final L2SummonInstance _summon;

		SummonLifetime(L2PcInstance activeChar, L2SummonInstance newpet)
		{
			_activeChar = activeChar;
			_summon = newpet;
		}

		@Override
		public void run()
		{
			log.debug("L2SummonInstance: " + _summon.getTemplate().getName() + " (" + _activeChar.getName() + ") run " +
					"task.");

			try
			{
				double oldTimeRemaining = _summon.getTimeRemaining();
				int maxTime = _summon.getTotalLifeTime();
				double newTimeRemaining;

				// if pet is attacking
				if (_summon.isAttackingNow())
					_summon.decTimeRemaining(_summon.getTimeLostActive());
				else
					_summon.decTimeRemaining(_summon.getTimeLostIdle());

				newTimeRemaining = _summon.getTimeRemaining();

				// check if the summon's lifetime has ran out
				if (newTimeRemaining < 0)
					_summon.unSummon(_activeChar);
				else if ((newTimeRemaining <= _summon.getNextItemConsumeTime()) && (oldTimeRemaining > _summon.getNextItemConsumeTime()))
				{
					_summon.decNextItemConsumeTime(maxTime / (_summon.getItemConsumeSteps() + 1));

					// check if owner has enought itemConsume, if requested
					if (_summon.getItemConsumeCount() > 0 && _summon.getItemConsumeId() != 0 && !_summon.isDead() && !_summon.destroyItemByItemId("Consume", _summon.getItemConsumeId(), _summon.getItemConsumeCount(), _activeChar, true))
						_summon.unSummon(_activeChar);
				}

				// prevent useless packet-sending when the difference isn't visible.
				if ((_summon.lastShowntimeRemaining - newTimeRemaining) > maxTime / 352)
				{
					_activeChar.sendPacket(new SetSummonRemainTime(maxTime, (int) newTimeRemaining));
					_summon.lastShowntimeRemaining = (int) newTimeRemaining;
					_summon.updateEffectIcons();
				}
			}
			catch (Exception e)
			{
				log.error("Error on player [" + _activeChar.getName() + "] summon item consume task.", e);
			}
		}
	}

	@Override
	public void unSummon(L2PcInstance owner)
	{
		_log.debug("L2SummonInstance: " + getTemplate().getName() + " (" + owner.getName() + ") unsummoned.");

		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
			_summonLifeTask = null;
		}

		super.unSummon(owner);
	}

	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
	}

	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		_log.debug("L2SummonInstance: " + getTemplate().getName() + " (" + getOwner().getName() + ") consume.");

		return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
	}
}
