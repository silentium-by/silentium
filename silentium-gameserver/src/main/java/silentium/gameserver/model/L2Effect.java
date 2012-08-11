/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.GameTimeController;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2SummonInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.AbnormalStatusUpdate;
import silentium.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import silentium.gameserver.network.serverpackets.PartySpelled;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.AbnormalEffect;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.basefuncs.Func;
import silentium.gameserver.skills.basefuncs.FuncTemplate;
import silentium.gameserver.skills.basefuncs.Lambda;
import silentium.gameserver.skills.effects.EffectTemplate;
import silentium.gameserver.templates.skills.L2EffectType;
import silentium.gameserver.templates.skills.L2SkillType;

public abstract class L2Effect
{
	protected static final Logger _log = LoggerFactory.getLogger(L2Effect.class.getName());

	public static enum EffectState
	{
		CREATED, ACTING, FINISHING
	}

	private static final Func[] _emptyFunctionSet = new Func[0];

	private final L2Character _effector;
	private final L2Character _effected;

	private final L2Skill _skill; // the skill that was used.

	private final boolean _isHerbEffect;

	private final Lambda _lambda; // the value of an update
	private EffectState _state; // the current state

	private final int _period; // period, seconds
	protected int _periodStartTicks;
	protected int _periodFirstTime;

	private final EffectTemplate _template;

	private final FuncTemplate[] _funcTemplates; // function templates

	private final int _totalCount; // initial count
	private int _count; // counter

	private final AbnormalEffect _abnormalEffect; // abnormal effect mask
	private final boolean _icon; // show icon
	private boolean _isSelfEffect = false; // is selfeffect ?

	public boolean preventExitUpdate;

	protected final class EffectTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				_periodFirstTime = 0;
				_periodStartTicks = GameTimeController.getGameTicks();
				scheduleEffect();
			}
			catch (Exception e)
			{
				_log.error("", e);
			}
		}
	}

	private ScheduledFuture<?> _currentFuture;

	/** The Identifier of the stack group */
	private final String _stackType;

	/** The position of the effect in the stack group */
	private final float _stackOrder;

	private boolean _inUse = false;
	private boolean _startConditionsCorrect = true;

	private final double _effectPower;
	private final L2SkillType _effectSkillType;

	/**
	 * <font color="FF0000"><b>WARNING: scheduleEffect no longer inside constructor ; you must call it explicitly.</b></font>
	 *
	 * @param env
	 * @param template
	 */
	protected L2Effect(Env env, EffectTemplate template)
	{
		_state = EffectState.CREATED;
		_skill = env.skill;
		_template = template;
		_effected = env.target;
		_effector = env.player;
		_lambda = template.lambda;
		_funcTemplates = template.funcTemplates;
		_count = template.counter;
		_totalCount = _count;

		// Support for retail herbs duration when _effected has a Summon
		int temp = template.period;

		if ((_skill.getId() > 2277 && _skill.getId() < 2286) || (_skill.getId() >= 2512 && _skill.getId() <= 2514))
		{
			if (_effected instanceof L2SummonInstance || (_effected instanceof L2PcInstance && ((L2PcInstance) _effected).getPet() != null))
				temp /= 2;
		}

		if (env.skillMastery)
			temp *= 2;

		_period = temp;
		_abnormalEffect = template.abnormalEffect;
		_stackType = template.stackType;
		_stackOrder = template.stackOrder;
		_periodStartTicks = GameTimeController.getGameTicks();
		_periodFirstTime = 0;
		_icon = template.icon;
		_effectPower = template.effectPower;
		_effectSkillType = template.effectType;

		_isHerbEffect = _skill.getName().contains("Herb");
	}

	public int getCount()
	{
		return _count;
	}

	public int getTotalCount()
	{
		return _totalCount;
	}

	public void setCount(int newcount)
	{
		_count = Math.min(newcount, _totalCount); // sanity check
	}

	public void setFirstTime(int newFirstTime)
	{
		_periodFirstTime = Math.min(newFirstTime, _period);
		_periodStartTicks -= _periodFirstTime * GameTimeController.TICKS_PER_SECOND;
	}

	public boolean getShowIcon()
	{
		return _icon;
	}

	public int getPeriod()
	{
		return _period;
	}

	public int getTime()
	{
		return (GameTimeController.getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND;
	}

	/**
	 * Returns the elapsed time of the task.
	 *
	 * @return Time in seconds.
	 */
	public int getTaskTime()
	{
		if (_count == _totalCount)
			return 0;
		return (Math.abs(_count - _totalCount + 1) * _period) + getTime() + 1;
	}

	public boolean getInUse()
	{
		return _inUse;
	}

	public boolean setInUse(boolean inUse)
	{
		_inUse = inUse;
		if (_inUse)
			_startConditionsCorrect = onStart();
		else
			onExit();

		return _startConditionsCorrect;
	}

	public String getStackType()
	{
		return _stackType;
	}

	public float getStackOrder()
	{
		return _stackOrder;
	}

	public final L2Skill getSkill()
	{
		return _skill;
	}

	public final L2Character getEffector()
	{
		return _effector;
	}

	public final L2Character getEffected()
	{
		return _effected;
	}

	public boolean isSelfEffect()
	{
		return _isSelfEffect;
	}

	public void setSelfEffect()
	{
		_isSelfEffect = true;
	}

	public boolean isHerbEffect()
	{
		return _isHerbEffect;
	}

	public final double calc()
	{
		Env env = new Env();
		env.player = _effector;
		env.target = _effected;
		env.skill = _skill;
		return _lambda.calc(env);
	}

	private final synchronized void startEffectTask()
	{
		if (_period > 0)
		{
			stopEffectTask();
			final int initialDelay = Math.max((_period - _periodFirstTime) * 1000, 5);
			if (_count > 1)
				_currentFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new EffectTask(), initialDelay, _period * 1000);
			else
				_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(new EffectTask(), initialDelay);
		}
		if (_state == EffectState.ACTING)
		{
			if (isSelfEffectType())
				_effector.addEffect(this);
			else
				_effected.addEffect(this);
		}
	}

	/**
	 * Stop the L2Effect task and send Server->Client update packet.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Cancel the effect in the the abnormal effect map of the L2Character</li> <li>Stop the task of the L2Effect, remove it
	 * and update client magic icon</li><BR>
	 * <BR>
	 */
	public final void exit()
	{
		this.exit(false);
	}

	public final void exit(boolean preventUpdate)
	{
		preventExitUpdate = preventUpdate;
		_state = EffectState.FINISHING;
		scheduleEffect();
	}

	/**
	 * Stop the task of the L2Effect, remove it and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Cancel the task</li> <li>Stop and remove L2Effect from L2Character and update client magic icon</li><BR>
	 * <BR>
	 */
	public final synchronized void stopEffectTask()
	{
		if (_currentFuture != null)
		{
			// Cancel the task
			_currentFuture.cancel(false);

			_currentFuture = null;

			if (isSelfEffectType() && getEffector() != null)
				getEffector().removeEffect(this);
			else if (getEffected() != null)
				getEffected().removeEffect(this);
		}
	}

	/**
	 * @return effect type
	 */
	public abstract L2EffectType getEffectType();

	/**
	 * Notify started
	 *
	 * @return always true, overidden in each effect.
	 */
	public boolean onStart()
	{
		if (_abnormalEffect != AbnormalEffect.NULL)
			getEffected().startAbnormalEffect(_abnormalEffect);

		return true;
	}

	/**
	 * Cancel the effect in the the abnormal effect map of the effected L2Character.
	 */
	public void onExit()
	{
		if (_abnormalEffect != AbnormalEffect.NULL)
			getEffected().stopAbnormalEffect(_abnormalEffect);
	}

	/**
	 * @return true for continuation of this effect
	 */
	public abstract boolean onActionTime();

	public final void rescheduleEffect()
	{
		if (_state != EffectState.ACTING)
			scheduleEffect();
		else
		{
			if (_period != 0)
			{
				startEffectTask();
				return;
			}
		}
	}

	public final void scheduleEffect()
	{
		switch (_state)
		{
			case CREATED:
			{
				_state = EffectState.ACTING;

				if (_skill.isPvpSkill() && _icon && getEffected() instanceof L2PcInstance)
				{
					SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					smsg.addSkillName(_skill);
					getEffected().sendPacket(smsg);
				}

				if (_period != 0)
				{
					startEffectTask();
					return;
				}
				// effects not having count or period should start
				_startConditionsCorrect = onStart();
			}
			case ACTING:
			{
				if (_count > 0)
				{
					_count--;
					if (getInUse())
					{ // effect has to be in use
						if (onActionTime() && _startConditionsCorrect && _count > 0)
							return; // false causes effect to finish right away
					}
					else if (_count > 0)
					{ // do not finish it yet, in case reactivated
						return;
					}
				}
				_state = EffectState.FINISHING;
			}
			case FINISHING:
			{
				// If the time left is equal to zero, send the message
				if (_count == 0 && _icon && getEffected() instanceof L2PcInstance)
					getEffected().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WORN_OFF).addSkillName(_skill));

				// if task is null - stopEffectTask does not remove effect
				if (_currentFuture == null && getEffected() != null)
					getEffected().removeEffect(this);

				// Stop the task of the L2Effect, remove it and update client magic icon
				stopEffectTask();

				// Cancel the effect in the the abnormal effect map of the L2Character
				if (getInUse() || !(_count > 1 || _period > 0))
					if (_startConditionsCorrect)
						onExit();
			}
		}
	}

	public Func[] getStatFuncs()
	{
		if (_funcTemplates == null)
			return _emptyFunctionSet;
		ArrayList<Func> funcs = new ArrayList<>(_funcTemplates.length);

		Env env = new Env();
		env.player = getEffector();
		env.target = getEffected();
		env.skill = getSkill();
		Func f;

		for (FuncTemplate t : _funcTemplates)
		{
			f = t.getFunc(env, this); // effect is owner
			if (f != null)
				funcs.add(f);
		}
		if (funcs.isEmpty())
			return _emptyFunctionSet;

		return funcs.toArray(new Func[funcs.size()]);
	}

	public final void addIcon(AbnormalStatusUpdate mi)
	{
		if (_state != EffectState.ACTING)
			return;

		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (_totalCount > 1)
		{
			if (sk.isPotion())
				mi.addEffect(sk.getId(), getLevel(), sk.getBuffDuration() - (getTaskTime() * 1000));
			else
				mi.addEffect(sk.getId(), getLevel(), -1);
		}
		else if (future != null)
			mi.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_period == -1)
			mi.addEffect(sk.getId(), getLevel(), _period);
	}

	public final void addPartySpelledIcon(PartySpelled ps)
	{
		if (_state != EffectState.ACTING)
			return;

		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (future != null)
			ps.addPartySpelledEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_period == -1)
			ps.addPartySpelledEffect(sk.getId(), getLevel(), _period);
	}

	public final void addOlympiadSpelledIcon(ExOlympiadSpelledInfo os)
	{
		if (_state != EffectState.ACTING)
			return;

		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (future != null)
			os.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_period == -1)
			os.addEffect(sk.getId(), getLevel(), _period);
	}

	public int getLevel()
	{
		return getSkill().getLevel();
	}

	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}

	public EffectTemplate getEffectTemplate()
	{
		return _template;
	}

	public double getEffectPower()
	{
		return _effectPower;
	}

	public L2SkillType getSkillType()
	{
		return _effectSkillType;
	}

	/**
	 * Return bit flag for current effect
	 *
	 * @return int flag
	 */
	public int getEffectFlags()
	{
		return 0;
	}

	@Override
	public String toString()
	{
		return "L2Effect [_skill=" + _skill + ", _state=" + _state + ", _period=" + _period + "]";
	}

	public boolean isSelfEffectType()
	{
		return false;
	}
}
