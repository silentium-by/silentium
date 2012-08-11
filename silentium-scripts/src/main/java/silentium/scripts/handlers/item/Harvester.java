/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.item;

import silentium.gameserver.handler.IItemHandler;
import silentium.gameserver.instancemanager.CastleManorManager;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2MonsterInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.tables.SkillTable;

/**
 * @author l3x
 */
public class Harvester implements IItemHandler
{
	L2PcInstance _activeChar;
	L2MonsterInstance _target;

	@Override
	public void useItem(L2Playable playable, L2ItemInstance _item, boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		if (CastleManorManager.getInstance().isDisabled())
			return;

		_activeChar = (L2PcInstance) playable;

		if (!(_activeChar.getTarget() instanceof L2MonsterInstance))
		{
			_activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		_target = (L2MonsterInstance) _activeChar.getTarget();

		if (_target == null || !_target.isDead())
		{
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(2098, 1); // harvesting skill
		if (skill != null)
			_activeChar.useMagic(skill, false, false);
	}
}