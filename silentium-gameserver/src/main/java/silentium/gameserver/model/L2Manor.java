/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model;

import java.io.File;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.parsers.XMLDocumentFactory;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.item.L2Item;

/**
 * Service class for manor
 * 
 * @author l3x
 */
public class L2Manor
{
	private static Logger _log = LoggerFactory.getLogger(L2Manor.class.getName());

	private static FastMap<Integer, SeedData> _seeds;

	public L2Manor()
	{
		_seeds = new FastMap<Integer, SeedData>().shared();
		parseData();
	}

	public static L2Manor getInstance()
	{
		return SingletonHolder._instance;
	}

	public FastList<Integer> getAllCrops()
	{
		FastList<Integer> crops = new FastList<>();

		for (SeedData seed : _seeds.values())
		{
			if (!crops.contains(seed.getCrop()) && seed.getCrop() != 0 && !crops.contains(seed.getCrop()))
				crops.add(seed.getCrop());
		}

		return crops;
	}

	public int getSeedBasicPrice(int seedId)
	{
		L2Item seedItem = ItemTable.getInstance().getTemplate(seedId);

		if (seedItem != null)
			return seedItem.getReferencePrice();

		return 0;
	}

	public int getSeedBasicPriceByCrop(int cropId)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getCrop() == cropId)
				return getSeedBasicPrice(seed.getId());
		}
		return 0;
	}

	public int getCropBasicPrice(int cropId)
	{
		L2Item cropItem = ItemTable.getInstance().getTemplate(cropId);

		if (cropItem != null)
			return cropItem.getReferencePrice();

		return 0;
	}

	public int getMatureCrop(int cropId)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getCrop() == cropId)
				return seed.getMature();
		}
		return 0;
	}

	/**
	 * Returns price which lord pays to buy one seed
	 * 
	 * @param seedId
	 * @return seed price
	 */
	public int getSeedBuyPrice(int seedId)
	{
		int buyPrice = getSeedBasicPrice(seedId) / 10;
		return (buyPrice > 0 ? buyPrice : 1);
	}

	public int getSeedMinLevel(int seedId)
	{
		SeedData seed = _seeds.get(seedId);
		if (seed != null)
			return seed.getLevel() - 5;

		return -1;
	}

	public int getSeedMaxLevel(int seedId)
	{
		SeedData seed = _seeds.get(seedId);
		if (seed != null)
			return seed.getLevel() + 5;

		return -1;
	}

	public int getSeedLevelByCrop(int cropId)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getCrop() == cropId)
				return seed.getLevel();
		}
		return 0;
	}

	public int getSeedLevel(int seedId)
	{
		SeedData seed = _seeds.get(seedId);
		if (seed != null)
			return seed.getLevel();

		return -1;
	}

	public boolean isAlternative(int seedId)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getId() == seedId)
				return seed.isAlternative();
		}
		return false;
	}

	public int getCropType(int seedId)
	{
		SeedData seed = _seeds.get(seedId);
		if (seed != null)
			return seed.getCrop();

		return -1;
	}

	public synchronized int getRewardItem(int cropId, int type)
	{
		for (SeedData seed : _seeds.values())
		{
			// there can be several seeds with same crop, but reward should be the same for all
			if (seed.getCrop() == cropId)
				return seed.getReward(type);
		}
		return -1;
	}

	public synchronized int getRewardItemBySeed(int seedId, int type)
	{
		SeedData seed = _seeds.get(seedId);
		if (seed != null)
			return seed.getReward(type);

		return 0;
	}

	/**
	 * Return all crops which can be purchased by given castle
	 * 
	 * @param castleId
	 * @return
	 */
	public FastList<Integer> getCropsForCastle(int castleId)
	{
		FastList<Integer> crops = new FastList<>();

		for (SeedData seed : _seeds.values())
		{
			if (seed.getManorId() == castleId && !crops.contains(seed.getCrop()))
				crops.add(seed.getCrop());
		}
		return crops;
	}

	/**
	 * Return list of seed ids, which belongs to castle with given id
	 * 
	 * @param castleId
	 *            - id of the castle
	 * @return seedIds - list of seed ids
	 */
	public FastList<Integer> getSeedsForCastle(int castleId)
	{
		FastList<Integer> seedsID = new FastList<>();

		for (SeedData seed : _seeds.values())
		{
			if (seed.getManorId() == castleId && !seedsID.contains(seed.getId()))
				seedsID.add(seed.getId());
		}
		return seedsID;
	}

	/**
	 * Returns castle id where seed can be sowned<br>
	 * 
	 * @param seedId
	 * @return castleId
	 */
	public int getCastleIdForSeed(int seedId)
	{
		SeedData seed = _seeds.get(seedId);
		if (seed != null)
			return seed.getManorId();

		return 0;
	}

	public int getSeedSaleLimit(int seedId)
	{
		SeedData seed = _seeds.get(seedId);
		if (seed != null)
			return seed.getSeedLimit();

		return 0;
	}

	public int getCropPuchaseLimit(int cropId)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getCrop() == cropId)
				return seed.getCropLimit();
		}
		return 0;
	}

	private class SeedData
	{
		private int _id;
		private final int _level; // seed level
		private final int _crop; // crop type
		private final int _mature; // mature crop type
		private int _type1;
		private int _type2;
		private int _manorId; // id of manor (castle id) where seed can be farmed
		private int _isAlternative;
		private int _limitSeeds;
		private int _limitCrops;

		public SeedData(int level, int crop, int mature)
		{
			_level = level;
			_crop = crop;
			_mature = mature;
		}

		public void setData(int id, int t1, int t2, int manorId, int isAlt, int lim1, int lim2)
		{
			_id = id;
			_type1 = t1;
			_type2 = t2;
			_manorId = manorId;
			_isAlternative = isAlt;
			_limitSeeds = lim1;
			_limitCrops = lim2;
		}

		public int getManorId()
		{
			return _manorId;
		}

		public int getId()
		{
			return _id;
		}

		public int getCrop()
		{
			return _crop;
		}

		public int getMature()
		{
			return _mature;
		}

		public int getReward(int type)
		{
			return (type == 1 ? _type1 : _type2);
		}

		public int getLevel()
		{
			return _level;
		}

		public boolean isAlternative()
		{
			return (_isAlternative == 1);
		}

		public int getSeedLimit()
		{
			return _limitSeeds * MainConfig.RATE_DROP_MANOR;
		}

		public int getCropLimit()
		{
			return _limitCrops * MainConfig.RATE_DROP_MANOR;
		}
	}

	private void parseData()
	{
		try
		{
			File f = new File(MainConfig.DATAPACK_ROOT + "/data/xml/seeds.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("seed"))
				{
					int seedId = Integer.valueOf(d.getAttributes().getNamedItem("id").getNodeValue()); // seed id
					int level = Integer.valueOf(d.getAttributes().getNamedItem("level").getNodeValue()); // seed level
					int cropId = Integer.valueOf(d.getAttributes().getNamedItem("cropId").getNodeValue()); // crop id
					int matureId = Integer.valueOf(d.getAttributes().getNamedItem("matureId").getNodeValue()); // mature crop id
					int type1R = Integer.valueOf(d.getAttributes().getNamedItem("r1").getNodeValue()); // type I reward
					int type2R = Integer.valueOf(d.getAttributes().getNamedItem("r2").getNodeValue()); // type II reward
					int manorId = Integer.valueOf(d.getAttributes().getNamedItem("manor").getNodeValue()); // id of manor, where
																											// seed can be farmed
					int isAlt = Integer.valueOf(d.getAttributes().getNamedItem("isAlternative").getNodeValue()); // alternative
																													// seed
					int limitSeeds = Integer.valueOf(d.getAttributes().getNamedItem("seedsLimit").getNodeValue()); // limit for
																													// seeds
					int limitCrops = Integer.valueOf(d.getAttributes().getNamedItem("cropsLimit").getNodeValue()); // limit for
																													// crops

					SeedData seed = new SeedData(level, cropId, matureId);
					seed.setData(seedId, type1R, type2R, manorId, isAlt, limitSeeds, limitCrops);
					_seeds.put(seed.getId(), seed);
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("ManorManager: Error while creating table: " + e);
		}
		_log.info("ManorManager: Loaded " + _seeds.size() + " seeds.");
	}

	private static class SingletonHolder
	{
		protected static final L2Manor _instance = new L2Manor();
	}
}
