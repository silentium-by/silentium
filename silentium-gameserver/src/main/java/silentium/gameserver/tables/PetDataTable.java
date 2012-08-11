/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.parsers.XMLDocumentFactory;
import silentium.gameserver.model.L2PetData;
import silentium.gameserver.model.L2PetData.L2PetLevelData;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.templates.item.L2EtcItemType;
import silentium.gameserver.templates.item.L2Item;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PetDataTable
{
	private static Logger _log = LoggerFactory.getLogger(L2PetInstance.class.getName());

	private static TIntObjectHashMap<L2PetData> _petTable;

	public static PetDataTable getInstance()
	{
		return SingletonHolder._instance;
	}

	protected PetDataTable()
	{
		_petTable = new TIntObjectHashMap<>();
		load();
	}

	public void reload()
	{
		_petTable.clear();
		load();
	}

	public void load()
	{
		try
		{
			File f = new File(MainConfig.DATAPACK_ROOT + "/data/xml/pets_stats.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				// General behavior of the pet (currently, petId / foodId)
				if (d.getNodeName().equalsIgnoreCase("pet"))
				{
					int petId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
					L2PetData petData = new L2PetData();

					String[] values = d.getAttributes().getNamedItem("food").getNodeValue().split(";");
					int[] food = new int[values.length];
					for (int i = 0; i < values.length; i++)
						food[i] = Integer.parseInt(values[i]);
					petData.setFood(food);

					// Then check particular stats (each line equals one different level)
					for (Node p = d.getFirstChild(); p != null; p = p.getNextSibling())
					{
						if (p.getNodeName().equals("stat"))
						{
							int petLevel = Integer.parseInt(p.getAttributes().getNamedItem("level").getNodeValue());
							L2PetLevelData stat = new L2PetLevelData();

							stat.setPetMaxExp(Integer.parseInt(p.getAttributes().getNamedItem("expMax").getNodeValue()));
							stat.setPetMaxHP(Integer.parseInt(p.getAttributes().getNamedItem("hpMax").getNodeValue()));
							stat.setPetMaxMP(Integer.parseInt(p.getAttributes().getNamedItem("mpMax").getNodeValue()));
							stat.setPetPAtk(Integer.parseInt(p.getAttributes().getNamedItem("patk").getNodeValue()));
							stat.setPetPDef(Integer.parseInt(p.getAttributes().getNamedItem("pdef").getNodeValue()));
							stat.setPetMAtk(Integer.parseInt(p.getAttributes().getNamedItem("matk").getNodeValue()));
							stat.setPetMDef(Integer.parseInt(p.getAttributes().getNamedItem("mdef").getNodeValue()));
							stat.setPetMaxFeed(Integer.parseInt(p.getAttributes().getNamedItem("feedMax").getNodeValue()));
							stat.setPetFeedNormal(Integer.parseInt(p.getAttributes().getNamedItem("feednormal").getNodeValue()));
							stat.setPetFeedBattle(Integer.parseInt(p.getAttributes().getNamedItem("feedbattle").getNodeValue()));
							stat.setPetRegenHP(Integer.parseInt(p.getAttributes().getNamedItem("hpregen").getNodeValue()));
							stat.setPetRegenMP(Integer.parseInt(p.getAttributes().getNamedItem("mpregen").getNodeValue()));
							stat.setOwnerExpTaken(Float.valueOf(p.getAttributes().getNamedItem("owner_exp_taken").getNodeValue()));

							// Create a line with pet level as "cursor"
							petData.addNewStat(petLevel, stat);
						}
					}
					// Attach this stat line to the pet
					_petTable.put(petId, petData);
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("L2PetDataTable: Error while creating table" + e);
		}
		_log.info("PetDataTable: Loaded " + _petTable.size() + " pets.");
	}

	public L2PetLevelData getPetLevelData(int petID, int petLevel)
	{
		return _petTable.get(petID).getPetLevelData(petLevel);
	}

	public L2PetData getPetData(int petID)
	{
		if (!_petTable.contains(petID))
			_log.info("Missing pet data for npcid: " + petID);

		return _petTable.get(petID);
	}

	/*
	 * Pets stuffs
	 */
	public static boolean isWolf(int npcId)
	{
		return npcId == 12077;
	}

	public static boolean isSinEater(int npcId)
	{
		return npcId == 12564;
	}

	public static boolean isHatchling(int npcId)
	{
		return npcId > 12310 && npcId < 12314;
	}

	public static boolean isStrider(int npcId)
	{
		return npcId > 12525 && npcId < 12529;
	}

	public static boolean isWyvern(int npcId)
	{
		return npcId == 12621;
	}

	public static boolean isBaby(int npcId)
	{
		return npcId > 12779 && npcId < 12783;
	}

	public static boolean isPetFood(int itemId)
	{
		switch (itemId)
		{
			case 2515:
			case 4038:
			case 5168:
			case 5169:
			case 6316:
			case 7582:
				return true;
			default:
				return false;
		}
	}

	public static boolean isPetCollar(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		if (item != null && item.getItemType() == L2EtcItemType.PET_COLLAR)
			return true;

		return false;
	}

	public static int[] getPetItemsAsNpc(int npcId)
	{
		switch (npcId)
		{
			case 12077:// wolf pet a
				return new int[] { 2375 };
			case 12564:// Sin Eater
				return new int[] { 4425 };

			case 12311:// hatchling of wind
			case 12312:// hatchling of star
			case 12313:// hatchling of twilight
				return new int[] { 3500, 3501, 3502 };

			case 12526:// wind strider
			case 12527:// Star strider
			case 12528:// Twilight strider
				return new int[] { 4422, 4423, 4424 };

			case 12621:// Wyvern
				return new int[] { 8663 };

			case 12780:// Baby Buffalo
			case 12782:// Baby Cougar
			case 12781:// Baby Kookaburra
				return new int[] { 6648, 6649, 6650 };

				// unknown item id.. should never happen
			default:
				return new int[] { 0 };
		}
	}

	public static boolean isMountable(int npcId)
	{
		return npcId == 12526 // wind strider
				|| npcId == 12527 // star strider
				|| npcId == 12528 // twilight strider
				|| npcId == 12621; // wyvern
	}

	public boolean doesPetNameExist(String name, int petNpcId)
	{
		boolean result = true;
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT name FROM pets p, items i WHERE p.item_obj_id = i.object_id AND name=? AND i.item_id IN (?)");
			statement.setString(1, name);

			String cond = "";
			for (int it : PetDataTable.getPetItemsAsNpc(petNpcId))
			{
				if (!cond.isEmpty())
					cond += ", ";
				cond += it;
			}
			statement.setString(2, cond);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn("could not check existing petname:" + e.getMessage());
		}
		return result;
	}

	private static class SingletonHolder
	{
		protected static final PetDataTable _instance = new PetDataTable();
	}
}
