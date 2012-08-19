/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.data.crest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.database.DatabaseFactory;
import silentium.commons.io.filters.BmpFilter;
import silentium.commons.io.filters.OldPledgeFilter;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.idfactory.IdFactory;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.tables.ClanTable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static silentium.gameserver.data.crest.CrestCache.CrestType.PLEDGE_OLD;

/**
 * @author Layane, reworked by Java-man
 */
public class CrestCache
{
	private static final Logger log = LoggerFactory.getLogger(CrestCache.class);

	private static final List<CrestData> cache = new ArrayList<>();

	private static final String CRESTS_DIR = "data/crests/";

	private static final File[] EMPTY_FILE_ARRAY = new File[0];

	public enum CrestType
	{
		PLEDGE("Crest_"),
		PLEDGE_LARGE("Crest_Large_"),
		PLEDGE_OLD("Pledge_"),
		ALLY("AllyCrest_");

		private final String dirPrefix;

		CrestType(final String dirPrefix)
		{
			this.dirPrefix = dirPrefix;
		}

		public String getDirPrefix()
		{
			return dirPrefix;
		}
	}

	static
	{
		if (!new File(CRESTS_DIR).mkdirs())
			convertOldPledgeFiles();
	}

	public static void load()
	{
		cache.clear();

		File[] files = new File(MainConfig.DATAPACK_ROOT, CRESTS_DIR).listFiles(BmpFilter.INSTANCE);
		if (files == null)
			files = EMPTY_FILE_ARRAY;

		String fileName;
		byte[] content;

		CrestType crestType = null;
		int crestId = 0;

		for (final File file : files)
		{
			fileName = file.getName();

			try (RandomAccessFile f = new RandomAccessFile(file, "r"))
			{
				content = new byte[(int) f.length()];
				f.readFully(content);

				for (final CrestType type : CrestType.values())
					if (fileName.startsWith(type.getDirPrefix()))
					{
						crestType = type;
						crestId = getCrestIdFromFileName(fileName, type.getDirPrefix());
					}

				cache.add(new CrestData(crestType, crestId, content));
			}
			catch (Exception e)
			{
				log.warn("Problem with loading crest bmp file: " + file, e);
			}
		}

		log.info("Cache[Crest]: " + cache.size() + " files loaded.");
	}

	public static void convertOldPledgeFiles()
	{
		int clanId, newId;
		L2Clan clan;

		final File[] files = new File(MainConfig.DATAPACK_ROOT, CRESTS_DIR).listFiles(OldPledgeFilter.INSTANCE);
		for (final File file : files)
		{
			clanId = getCrestIdFromFileName(file.getName(), PLEDGE_OLD.getDirPrefix());
			newId = IdFactory.getInstance().getNextId();
			clan = ClanTable.getInstance().getClan(clanId);

			log.info("Found old crest file \"" + file.getName() + "\" for clanId " + clanId);

			if (clan != null)
			{
				removeCrest(CrestType.PLEDGE_LARGE, clan.getCrestId());

				file.renameTo(new File(MainConfig.DATAPACK_ROOT, CRESTS_DIR + "Crest_" + newId + ".bmp"));
				log.info("Renamed Clan crest to new format: Crest_" + newId + ".bmp");

				try (Connection con = DatabaseFactory.getConnection();
				     PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?"))
				{
					statement.setInt(1, newId);
					statement.setInt(2, clan.getClanId());
					statement.executeUpdate();
				}
				catch (SQLException e)
				{
					log.warn("Could not update the crest id:", e);
				}

				clan.setCrestId(newId);
			}
			else
			{
				log.info("Clan Id: " + clanId + " does not exist in table.. deleting.");
				file.delete();
			}
		}
	}

	public static byte[] getCrestHash(final CrestType crestType, final int id)
	{
		for (final CrestData crest : cache)
			if (crest.getCrestType() == crestType && crest.getCrestId() == id)
				return crest.getHash();

		return null;
	}

	public static void removeCrest(final CrestType crestType, final int id)
	{
		final String crestDirPrefix = crestType.getDirPrefix();

		if (!"Pledge_".equals(crestDirPrefix))
			for (final CrestData crestData : cache)
				if (crestData.getCrestType() == crestType && crestData.getCrestId() == id)
				{
					cache.remove(crestData);
					break;
				}

		final File crestFile = new File(MainConfig.DATAPACK_ROOT, CRESTS_DIR + crestDirPrefix + id + ".bmp");
		try
		{
			crestFile.delete();
		}
		catch (Exception e)
		{
			log.warn("", e);
		}
	}

	public static boolean saveCrest(final CrestType crestType, final int newId, final byte[] data)
	{
		final File crestFile = new File(MainConfig.DATAPACK_ROOT, CRESTS_DIR + crestType.getDirPrefix() + newId + ".bmp");
		try (FileOutputStream out = new FileOutputStream(crestFile))
		{
			out.write(data);

			cache.add(new CrestData(crestType, newId, data));

			return true;
		}
		catch (IOException e)
		{
			log.info("Error saving pledge crest" + crestFile + ':', e);
			return false;
		}
	}

	public static int getCrestIdFromFileName(final String fileName, final String dirPrefix)
	{
		return Integer.valueOf(fileName.substring(dirPrefix.length(), fileName.length() - 4));
	}

	private static class CrestData
	{
		private final CrestType crestType;
		private final int crestId;
		private final byte[] hash;

		CrestData(final CrestType crestType, final int crestId, final byte[] hash)
		{
			this.crestType = crestType;
			this.crestId = crestId;
			this.hash = hash;
		}

		public CrestType getCrestType()
		{
			return crestType;
		}

		public int getCrestId()
		{
			return crestId;
		}

		public byte[] getHash()
		{
			return hash;
		}
	}
}
