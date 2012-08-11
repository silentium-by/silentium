package silentium.gameserver.skills.effects;

import silentium.gameserver.model.L2Effect;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.skills.AbnormalEffect;
import silentium.gameserver.skills.Env;
import silentium.gameserver.templates.skills.L2EffectType;

public class EffectGrow extends L2Effect
{
	public EffectGrow(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BUFF;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof L2Npc)
		{
			L2Npc npc = (L2Npc) getEffected();
			npc.setCollisionRadius((int) (npc.getCollisionRadius() * 1.19));

			getEffected().startAbnormalEffect(AbnormalEffect.GROW);
			return true;
		}
		return false;
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public void onExit()
	{
		if (getEffected() instanceof L2Npc)
		{
			L2Npc npc = (L2Npc) getEffected();
			npc.setCollisionRadius(npc.getTemplate().getCollisionRadius());

			getEffected().stopAbnormalEffect(AbnormalEffect.GROW);
		}
	}
}