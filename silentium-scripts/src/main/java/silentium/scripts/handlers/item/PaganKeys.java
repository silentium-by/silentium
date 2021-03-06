/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.item;

import silentium.gameserver.data.xml.DoorData;
import silentium.gameserver.handler.IItemHandler;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2DoorInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;

/**
 * @author chris
 */
public class PaganKeys implements IItemHandler {
	public static final int INTERACTION_DISTANCE = 100;

	@Override
	public void useItem(final L2Playable playable, final L2ItemInstance item, final boolean forceUse) {
		final int itemId = item.getItemId();
		if (!(playable instanceof L2PcInstance))
			return;

		final L2PcInstance activeChar = (L2PcInstance) playable;
		final L2Object target = activeChar.getTarget();

		if (!(target instanceof L2DoorInstance)) {
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		final L2DoorInstance door = (L2DoorInstance) target;

		if (!activeChar.isInsideRadius(door, INTERACTION_DISTANCE, false, false)) {
			activeChar.sendPacket(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (activeChar.getAbnormalEffect() > 0 || activeChar.isInCombat()) {
			activeChar.sendMessage("You cannot use the key now.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
			return;

		switch (itemId) {
			case 9698:
				if (door.getDoorId() == 24220020)
					door.openMe();
				else
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				break;
			case 9699:
				if (door.getDoorId() == 24220022)
					door.openMe();
				else
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				break;
			case 8056:
				if (door.getDoorId() == 23150004 || door.getDoorId() == 23150003) {
					DoorData.getInstance().getDoor(23150003).openMe();
					DoorData.getInstance().getDoor(23150003).onOpen();
					DoorData.getInstance().getDoor(23150004).openMe();
					DoorData.getInstance().getDoor(23150004).onOpen();
				} else
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				break;
		}
	}
}