/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.data.html;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.io.filters.HtmFilter;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.utils.Util;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Layane
 * @author Java-man
 * @author Tatanka
 */
public final class HtmCache {
	private static final Logger log = LoggerFactory.getLogger(HtmCache.class);

	private static final Pattern LFCR_PATTERN = Pattern.compile("\r\n");
	private static final int MAX_HTML_LENGTH = 8096;

	private final Cache<Integer, String> cache = CacheBuilder.newBuilder()
			.maximumSize(5000L)
			.expireAfterAccess(1L, TimeUnit.HOURS)
			.build();

	public static HtmCache getInstance() {
		return SingletonHolder.INSTANCE;
	}

	HtmCache() {
		reload();
	}

	public void reload() {
		cache.invalidateAll();

		log.info("Cache[HTML]: Running lazy cache");
	}

	public void reloadPath(final File file) {
		parseDir(file);

		log.info("Cache[HTML]: Reloaded specified path.");
	}

	private void parseDir(final File dir) {
		final File[] files = dir.listFiles(HtmFilter.INSTANCE);

		for (final File file : files) {
			if (file.isDirectory())
				parseDir(file);
			else
				loadFile(file);
		}
	}

	public String loadFile(final File file) {
		final String relpath = Util.getRelativePath(MainConfig.DATAPACK_ROOT, file);
		final int hashcode = relpath.hashCode();
		final StringBuilder stringBuilder = new StringBuilder(MAX_HTML_LENGTH);

		try {
			final List<String> lines = Files.readLines(file, Charsets.UTF_8);

			for (final String line : lines) {
				stringBuilder.append(line);
				stringBuilder.append('\n');
			}

			String content = stringBuilder.toString();
			content = LFCR_PATTERN.matcher(content).replaceAll("\n");

			cache.put(hashcode, content);

			return content;
		} catch (Exception e) {
			log.warn("Problem with htm file '{}':{}", file.getName(), e.getLocalizedMessage());
		} finally {
			stringBuilder.setLength(0);
		}

		return null;
	}

	public String getHtmForce(final String path) {
		String content = getHtm(path);

		if (Strings.isNullOrEmpty(content)) {
			content = "<html><body>My text is missing:<br>" + path + "</body></html>";
			log.warn("Cache[HTML]: Missing HTML page: " + path);
		}

		return content;
	}

	public String getHtm(final String path) {
		if (Strings.isNullOrEmpty(path))
			return ""; // avoid possible NPE

		final int hashCode = path.hashCode();
		String content = "";
		try {
			content = cache.get(hashCode, new LoadFileCallable(path));
		} catch (ExecutionException e) {
			log.warn(e.getLocalizedMessage(), e);
		}

		return content;
	}

	public boolean contains(final String path) {
		return cache.getIfPresent(path.hashCode()) != null;
	}

	/**
	 * Check if an HTM exists and can be loaded
	 *
	 * @param path The path to the HTM
	 * @return true if the HTM can be loaded.
	 */
	public static boolean isLoadable(final String path) {
		final File file = new File(path);

		return HtmFilter.INSTANCE.accept(file, file.getName());
	}

	private static class SingletonHolder {
		static final HtmCache INSTANCE = new HtmCache();
	}

	private class LoadFileCallable implements Callable<String> {
		private final String path;

		LoadFileCallable(final String path) {
			this.path = path;
		}

		@Override
		public String call() throws Exception {
			return loadFile(new File(MainConfig.DATAPACK_ROOT, path));
		}
	}
}
