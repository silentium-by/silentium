/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.data.xml;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.parsers.XMLDocumentFactory;
import silentium.gameserver.geo.pathfinding.AbstractNodeLoc;
import silentium.gameserver.idfactory.IdFactory;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.ClanHallManager;
import silentium.gameserver.model.actor.instance.L2DoorInstance;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.model.entity.ClanHall;
import silentium.gameserver.templates.StatsSet;
import silentium.gameserver.templates.chars.L2CharTemplate;

import java.io.File;
import java.util.ArrayList;

public class DoorData
{
	private static final Logger _log = LoggerFactory.getLogger(DoorData.class.getName());

	private final TIntObjectHashMap<L2DoorInstance> _staticItems;
	private final TIntObjectHashMap<ArrayList<L2DoorInstance>> _regions;

	public static DoorData getInstance()
	{
		return SingletonHolder._instance;
	}

	protected DoorData()
	{
		_staticItems = new TIntObjectHashMap<>();
		_regions = new TIntObjectHashMap<>();

		parseData();
		onStart();
	}

	public void reload()
	{
		_staticItems.clear();
		_regions.clear();

		parseData();
	}

	public void parseData()
	{
		try
		{
			File f = new File(MainConfig.DATAPACK_ROOT + "/data/xml/doors.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

			// Initialize variables
			L2DoorInstance door = null;
			String name;
			int id;
			NamedNodeMap attrs;
			Node att;

			int castleId = 0/* , sChId = 0 */;
			int x = 0, y = 0, z = 0;
			int rangeXMin = 0, rangeYMin = 0, rangeZMin = 0;
			int rangeXMax = 0, rangeYMax = 0, rangeZMax = 0;
			int hp = 0, pdef = 0, mdef = 0;
			boolean unlockable = false;
			int collisionRadius = 0;

			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if (d.getNodeName().equalsIgnoreCase("door"))
						{
							attrs = d.getAttributes();

							// Verify if the door got an id, else skip it
							att = attrs.getNamedItem("id");
							if (att == null)
							{
								_log.error("DoorData: Missing id for door, skipping.");
								continue;
							}
							id = Integer.valueOf(att.getNodeValue());

							// Verify if the door got a name, else skip it
							att = attrs.getNamedItem("name");
							if (att == null)
							{
								_log.error("DoorData: Missing name for door id: " + id + ", skipping.");
								continue;
							}
							name = String.valueOf(att.getNodeValue());

							for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
							{
								attrs = c.getAttributes();
								if ("castle".equalsIgnoreCase(c.getNodeName()))
								{
									castleId = Integer.valueOf(attrs.getNamedItem("id").getNodeValue());
								}
								else if ("siegableclanhall".equalsIgnoreCase(c.getNodeName()))
								{
									// FIXME sChId = Integer.valueOf(attrs.getNamedItem("id").getNodeValue());
								}
								else if ("position".equalsIgnoreCase(c.getNodeName()))
								{
									x = Integer.valueOf(attrs.getNamedItem("x").getNodeValue());
									y = Integer.valueOf(attrs.getNamedItem("y").getNodeValue());
									z = Integer.valueOf(attrs.getNamedItem("z").getNodeValue());
								}
								else if ("minpos".equalsIgnoreCase(c.getNodeName()))
								{
									rangeXMin = Integer.valueOf(attrs.getNamedItem("x").getNodeValue());
									rangeYMin = Integer.valueOf(attrs.getNamedItem("y").getNodeValue());
									rangeZMin = Integer.valueOf(attrs.getNamedItem("z").getNodeValue());
								}
								else if ("maxpos".equalsIgnoreCase(c.getNodeName()))
								{
									rangeXMax = Integer.valueOf(attrs.getNamedItem("x").getNodeValue());
									rangeYMax = Integer.valueOf(attrs.getNamedItem("y").getNodeValue());
									rangeZMax = Integer.valueOf(attrs.getNamedItem("z").getNodeValue());
								}
								else if ("stats".equalsIgnoreCase(c.getNodeName()))
								{
									hp = Integer.valueOf(attrs.getNamedItem("hp").getNodeValue());
									pdef = Integer.valueOf(attrs.getNamedItem("pdef").getNodeValue());
									mdef = Integer.valueOf(attrs.getNamedItem("mdef").getNodeValue());
								}
								else if ("unlockable".equalsIgnoreCase(c.getNodeName()))
									unlockable = Boolean.valueOf(attrs.getNamedItem("val").getNodeValue());
							}

							if (rangeXMin > rangeXMax)
								_log.error("DoorData: Error on rangeX min/max, ID:" + id);
							if (rangeYMin > rangeYMax)
								_log.error("DoorData: Error on rangeY min/max, ID:" + id);
							if (rangeZMin > rangeZMax)
								_log.error("DoorData: Error on rangeZ min/max, ID:" + id);

							if ((rangeXMax - rangeXMin) > (rangeYMax - rangeYMin))
								collisionRadius = rangeYMax - rangeYMin;
							else
								collisionRadius = rangeXMax - rangeXMin;

							// Template initialization
							StatsSet npcDat = new StatsSet();
							npcDat.set("npcId", id);
							npcDat.set("level", 0);
							npcDat.set("jClass", "door");
							npcDat.set("baseSTR", 0);
							npcDat.set("baseCON", 0);
							npcDat.set("baseDEX", 0);
							npcDat.set("baseINT", 0);
							npcDat.set("baseWIT", 0);
							npcDat.set("baseMEN", 0);
							npcDat.set("baseShldDef", 0);
							npcDat.set("baseShldRate", 0);
							npcDat.set("baseAccCombat", 38);
							npcDat.set("baseEvasRate", 38);
							npcDat.set("baseCritRate", 38);

							npcDat.set("collision_radius", collisionRadius);
							npcDat.set("collision_height", rangeZMax - rangeZMin);
							npcDat.set("sex", "male");
							npcDat.set("type", "");
							npcDat.set("baseAtkRange", 0);
							npcDat.set("baseMpMax", 0);
							npcDat.set("baseCpMax", 0);
							npcDat.set("rewardExp", 0);
							npcDat.set("rewardSp", 0);
							npcDat.set("basePAtk", 0);
							npcDat.set("baseMAtk", 0);
							npcDat.set("basePAtkSpd", 0);
							npcDat.set("aggroRange", 0);
							npcDat.set("baseMAtkSpd", 0);
							npcDat.set("rhand", 0);
							npcDat.set("lhand", 0);
							npcDat.set("armor", 0);
							npcDat.set("baseWalkSpd", 0);
							npcDat.set("baseRunSpd", 0);
							npcDat.set("name", name);
							npcDat.set("baseHpMax", hp);
							npcDat.set("baseHpReg", 3.e-3f);
							npcDat.set("baseMpReg", 3.e-3f);
							npcDat.set("basePDef", pdef);
							npcDat.set("baseMDef", mdef);

							L2CharTemplate template = new L2CharTemplate(npcDat);
							door = new L2DoorInstance(IdFactory.getInstance().getNextId(), template, id, name, unlockable);

							door.setRange(rangeXMin, rangeYMin, rangeZMin, rangeXMax, rangeYMax, rangeZMax);
							door.setCurrentHpMp(door.getMaxHp(), door.getMaxMp());
							door.setXYZInvisible(x, y, z);
							door.setMapRegion(MapRegionData.getInstance().getMapRegion(x, y));
							door.setOpen(false);

							// Attach door to a castle if a castleId is found
							if (castleId > 0)
							{
								Castle castle = CastleManager.getInstance().getCastleById(castleId);
								if (castle != null)
								{
									// Set the door as a wall if door name contains "wall".
									if (name.contains("wall"))
										door.setIsWall(true);

									castle.getDoors().add(door); // Add the door to castle doors list.

									_log.debug("DoorData: Door " + door.getDoorId() + " is now attached to " + castle
											.getName() + " castle.");
								}
							}

							// Test door, and attach it to a CH if a CH is found near
							ClanHall clanhall = ClanHallManager.getInstance().getNearbyClanHall(door.getX(), door.getY(), 500);
							if (clanhall != null)
							{
								clanhall.getDoors().add(door); // Add the door to CH doors list.
								door.setClanHall(clanhall);

								_log.debug("DoorData: Door " + door.getDoorId() + " is now attached to " + clanhall
											.getName() + " clanhall.");
							}

							putDoor(door);
							door.spawnMe(door.getX(), door.getY(), door.getZ());
						}
					}
				}
			}

			_log.info("DoorData: Loaded " + _staticItems.size() + " doors templates for " + _regions.size() + " regions.");
		}
		catch (Exception e)
		{
			_log.warn("DoorData: Error while creating table: " + e);
		}
	}

	public L2DoorInstance getDoor(Integer id)
	{
		return _staticItems.get(id);
	}

	public void putDoor(L2DoorInstance door)
	{
		_staticItems.put(door.getDoorId(), door);

		if (_regions.contains(door.getMapRegion()))
			_regions.get(door.getMapRegion()).add(door);
		else
		{
			final ArrayList<L2DoorInstance> region = new ArrayList<>();
			region.add(door);
			_regions.put(door.getMapRegion(), region);
		}
	}

	public L2DoorInstance[] getDoors()
	{
		return _staticItems.values(new L2DoorInstance[0]);
	}

	/**
	 * Performs a check and sets up a scheduled task for those doors that require auto opening/closing.
	 */
	public void checkAutoOpen()
	{
		for (L2DoorInstance doorInst : getDoors())
			// Garden of Eva (every 7 minutes)
			if (doorInst.getDoorName().startsWith("Eva"))
				doorInst.setAutoActionDelay(420000);

			// Tower of Insolence (every 5 minutes)
			else if (doorInst.getDoorName().startsWith("hubris"))
				doorInst.setAutoActionDelay(300000);
	}

	public boolean checkIfDoorsBetween(AbstractNodeLoc start, AbstractNodeLoc end)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
	}

	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz)
	{
		ArrayList<L2DoorInstance> allDoors;

		allDoors = _regions.get(MapRegionData.getInstance().getMapRegion(x, y));
		if (allDoors == null)
			return false;

		for (L2DoorInstance doorInst : allDoors)
		{
			if (doorInst.getXMax() == 0)
				continue;

			// line segment goes through box
			// first basic checks to stop most calculations short
			// phase 1, x
			if ((x <= doorInst.getXMax() && tx >= doorInst.getXMin()) || (tx <= doorInst.getXMax() && x >= doorInst.getXMin()))
			{
				// phase 2, y
				if ((y <= doorInst.getYMax() && ty >= doorInst.getYMin()) || (ty <= doorInst.getYMax() && y >= doorInst.getYMin()))
				{
					// phase 3, basically only z remains but now we calculate it with another formula (by rage)
					// in some cases the direct line check (only) in the beginning isn't sufficient,
					// when char z changes a lot along the path
					if (doorInst.getCurrentHp() > 0 && !doorInst.getOpen())
					{
						int px1 = doorInst.getXMin();
						int py1 = doorInst.getYMin();
						int pz1 = doorInst.getZMin();
						int px2 = doorInst.getXMax();
						int py2 = doorInst.getYMax();
						int pz2 = doorInst.getZMax();

						int l = tx - x;
						int m = ty - y;
						int n = tz - z;

						int dk;

						if ((dk = (doorInst.getA() * l + doorInst.getB() * m + doorInst.getC() * n)) == 0)
							continue; // Parallel

						float p = (float) (doorInst.getA() * x + doorInst.getB() * y + doorInst.getC() * z + doorInst.getD()) / (float) dk;

						int fx = (int) (x - l * p);
						int fy = (int) (y - m * p);
						int fz = (int) (z - n * p);

						if ((Math.min(x, tx) <= fx && fx <= Math.max(x, tx)) && (Math.min(y, ty) <= fy && fy <= Math.max(y, ty)) && (Math.min(z, tz) <= fz && fz <= Math.max(z, tz)))
						{
							if (((fx >= px1 && fx <= px2) || (fx >= px2 && fx <= px1)) && ((fy >= py1 && fy <= py2) || (fy >= py2 && fy <= py1)) && ((fz >= pz1 && fz <= pz2) || (fz >= pz2 && fz <= pz1)))
								return true; // Door between
						}
					}
				}
			}
		}
		return false;
	}

	public void onStart()
	{
		try
		{
			// Open following doors at server start: coliseums, ToI - RB Area Doors
			getDoor(24190001).openMe();
			getDoor(24190002).openMe();
			getDoor(24190003).openMe();
			getDoor(24190004).openMe();
			getDoor(23180001).openMe();
			getDoor(23180002).openMe();
			getDoor(23180003).openMe();
			getDoor(23180004).openMe();
			getDoor(23180005).openMe();
			getDoor(23180006).openMe();

			// Schedules a task to automatically open/close doors
			checkAutoOpen();
		}
		catch (NullPointerException e)
		{
			_log.warn("There are errors in your Door.csv file. Please update door.csv", e);
		}
	}

	private static class SingletonHolder
	{
		protected static final DoorData _instance = new DoorData();
	}
}
