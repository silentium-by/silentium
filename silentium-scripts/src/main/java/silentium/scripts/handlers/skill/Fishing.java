/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.gameserver.configs.MainConfig;
import silentium.commons.utils.Rnd;
import silentium.gameserver.geo.GeoData;
import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.instancemanager.ZoneManager;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.itemcontainer.Inventory;
import silentium.gameserver.model.zone.L2ZoneType;
import silentium.gameserver.model.zone.type.L2FishingZone;
import silentium.gameserver.model.zone.type.L2WaterZone;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.InventoryUpdate;
import silentium.gameserver.templates.item.L2Weapon;
import silentium.gameserver.templates.item.L2WeaponType;
import silentium.gameserver.templates.skills.L2SkillType;
import silentium.gameserver.utils.Util;

public class Fishing implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.FISHING };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2PcInstance player = (L2PcInstance) activeChar;

		/*
		 * If fishing is disabled, there isn't much point in doing anything else, unless you are GM. so this got moved up here,
		 * before anything else.
		 */
		if (!MainConfig.ALLOWFISHING)
		{
			player.sendMessage("Fishing feature is disabled on this server.");
			return;
		}

		if (player.isFishing())
		{
			if (player.getFishCombat() != null)
				player.getFishCombat().doDie(false);
			else
				player.endFishing(false);
			// Cancels fishing
			player.sendPacket(SystemMessageId.FISHING_ATTEMPT_CANCELLED);
			return;
		}

		// Fishing poles arent installed
		L2Weapon weaponItem = player.getActiveWeaponItem();
		if ((weaponItem == null || weaponItem.getItemType() != L2WeaponType.FISHINGROD))
		{
			player.sendPacket(SystemMessageId.FISHING_POLE_NOT_EQUIPPED);
			return;
		}

		// Baits arent equipped
		L2ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (lure == null)
		{
			player.sendPacket(SystemMessageId.BAIT_ON_HOOK_BEFORE_FISHING);
			return;
		}

		player.setLure(lure);
		L2ItemInstance lure2 = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		// Not enough baits
		if (lure2 == null || lure2.getCount() < 1)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_BAIT);
			return;
		}

		// You can't fish while you are on boat
		if (player.isInBoat())
		{
			player.sendPacket(SystemMessageId.CANNOT_FISH_ON_BOAT);
			return;
		}

		if (player.isInCraftMode() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.CANNOT_FISH_WHILE_USING_RECIPE_BOOK);
			return;
		}

		// You can't fish in water
		if (player.isInsideZone(L2Character.ZONE_WATER))
		{
			player.sendPacket(SystemMessageId.CANNOT_FISH_UNDER_WATER);
			return;
		}

		/*
		 * If fishing is enabled, decide where will the hook be cast...
		 */
		int rnd = Rnd.get(150) + 50;
		double angle = Util.convertHeadingToDegree(player.getHeading());
		double radian = Math.toRadians(angle);
		double sin = Math.sin(radian);
		double cos = Math.cos(radian);
		int x = player.getX() + (int) (cos * rnd);
		int y = player.getY() + (int) (sin * rnd);
		int z = player.getZ() + 50;
		/*
		 * ...and if the spot is in a fishing zone. If it is, it will position the hook on the water surface. If not, you have to
		 * be GM to proceed past here... in that case, the hook will be positioned using the old Z lookup method.
		 */
		L2FishingZone aimingTo = null;
		L2WaterZone water = null;
		boolean canFish = false;
		for (L2ZoneType zone : ZoneManager.getInstance().getZones(x, y))
		{
			if (zone instanceof L2FishingZone)
			{
				aimingTo = (L2FishingZone) zone;
				continue;
			}

			if (zone instanceof L2WaterZone)
				water = (L2WaterZone) zone;
		}

		if (aimingTo != null)
		{
			// fishing zone found, we can fish here
			if (MainConfig.GEODATA > 0)
			{
				// geodata enabled, checking if we can see end of the pole
				if (GeoData.getInstance().canSeeTarget(player.getX(), player.getY(), z, x, y, z))
				{
					// finding z level for hook
					if (water != null)
					{
						// water zone exist
						if (GeoData.getInstance().getHeight(x, y, z) < water.getWaterZ())
						{
							// water Z is higher than geo Z
							z = water.getWaterZ() + 10;
							canFish = true;
						}
					}
					else
					{
						// no water zone, using fishing zone
						if (GeoData.getInstance().getHeight(x, y, z) < aimingTo.getWaterZ())
						{
							// fishing Z is higher than geo Z
							z = aimingTo.getWaterZ() + 10;
							canFish = true;
						}
					}
				}
			}
			// geodata disabled
			else
			{
				// if water zone exist using it, if not - using fishing zone
				if (water != null)
					z = water.getWaterZ() + 10;
				else
					z = aimingTo.getWaterZ() + 10;
				canFish = true;
			}
		}

		if (!canFish)
		{
			// You can't fish here
			player.sendPacket(SystemMessageId.CANNOT_FISH_HERE);
			return;
		}

		// Has enough bait, consume 1 and update inventory. Start fishing follows.
		lure2 = player.getInventory().destroyItem("Consume", player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, player, null);
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(lure2);
		player.sendPacket(iu);

		// If everything else checks out, actually cast the hook and start fishing... :P
		player.startFishing(x, y, z);
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}