/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.item;

import silentium.gameserver.data.xml.MapRegionData;
import silentium.gameserver.handler.IItemHandler;
import silentium.gameserver.instancemanager.CastleManorManager;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Manor;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2ChestInstance;
import silentium.gameserver.model.actor.instance.L2MonsterInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.SkillHolder;

/**
 * @author l3x
 */
public class Seed implements IItemHandler {
	@Override
	public void useItem(final L2Playable playable, final L2ItemInstance item, final boolean forceUse) {
		if (!(playable instanceof L2PcInstance))
			return;

		if (CastleManorManager.getInstance().isDisabled())
			return;

		final L2Object tgt = playable.getTarget();
		if (!(tgt instanceof L2Npc)) {
			playable.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET));
			playable.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (!(tgt instanceof L2MonsterInstance) || tgt instanceof L2ChestInstance || ((L2Character) tgt).isRaid()) {
			playable.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THE_TARGET_IS_UNAVAILABLE_FOR_SEEDING));
			playable.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		final L2MonsterInstance target = (L2MonsterInstance) tgt;
		if (target.isDead()) {
			playable.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET));
			playable.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (target.isSeeded()) {
			playable.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		final int seedId = item.getItemId();
		if (areaValid(seedId, MapRegionData.getInstance().getAreaCastle(playable))) {
			target.setSeeded(seedId, (L2PcInstance) playable);
			final SkillHolder[] skills = item.getEtcItem().getSkills();
			if (skills != null) {
				if (skills[0] == null)
					return;

				final L2Skill itemskill = skills[0].getSkill();
				playable.useMagic(itemskill, false, false);
			}

		} else
			playable.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THIS_SEED_MAY_NOT_BE_SOWN_HERE));
	}

	/**
	 * @param seedId
	 * @param castleId
	 * @return
	 */
	private static boolean areaValid(final int seedId, final int castleId) {
		return L2Manor.getInstance().getCastleIdForSeed(seedId) == castleId;
	}
}