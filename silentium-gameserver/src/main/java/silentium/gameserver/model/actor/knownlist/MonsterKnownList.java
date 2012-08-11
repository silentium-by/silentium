/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.knownlist;

import silentium.gameserver.ai.CharacterAI;
import silentium.gameserver.ai.CtrlEvent;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2MonsterInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;

public class MonsterKnownList extends AttackableKnownList
{
	public MonsterKnownList(L2MonsterInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean addKnownObject(L2Object object)
	{
		if (!super.addKnownObject(object))
			return false;

		final CharacterAI ai = getActiveChar().getAI(); // force AI creation

		// Set the L2MonsterInstance Intention to AI_INTENTION_ACTIVE if the state was AI_INTENTION_IDLE
		if (object instanceof L2PcInstance && ai != null && ai.getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			ai.setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);

		return true;
	}

	@Override
	protected boolean removeKnownObject(L2Object object, boolean forget)
	{
		if (!super.removeKnownObject(object, forget))
			return false;

		if (!(object instanceof L2Character))
			return true;

		// Notify the L2MonsterInstance AI with EVT_FORGET_OBJECT
		if (getActiveChar().hasAI())
			getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, object);

		// Clear the _aggroList of the L2MonsterInstance
		if (getActiveChar().isVisible() && getKnownPlayers().isEmpty() && getKnownSummons().isEmpty())
			getActiveChar().clearAggroList();

		return true;
	}

	@Override
	public final L2MonsterInstance getActiveChar()
	{
		return (L2MonsterInstance) super.getActiveChar();
	}
}