package silentium.commons.utils;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Properties;

/**
 * This class is designed to simplify routine job with properties
 *
 * @author SoulKeeper
 */
public class PropertiesUtils {
	private static final Logger log = LoggerFactory.getLogger(PropertiesUtils.class);

	/**
	 * Loads properties by given file
	 *
	 * @param file filename
	 * @return loaded properties
	 * @throws java.io.IOException if can't load file
	 */
	public static Properties load(final String file) {
		try {
			return load(new File(file));
		} catch (IOException e) {
			log.error("Can't load file {}", file);
		}

		return null;
	}

	/**
	 * Loads properties by given file
	 *
	 * @param file filename
	 * @return loaded properties
	 * @throws java.io.IOException if can't load file
	 */
	public static Properties load(final File file) throws IOException {
		final Properties p = new Properties();

		try (InputStream fis = new FileInputStream(file); InputStream bis = new BufferedInputStream(fis)) {
			p.load(bis);
		}

		return p;
	}

	/**
	 * Loades properties from given files
	 *
	 * @param files list of string that represents files
	 * @return array of loaded properties
	 * @throws java.io.IOException if was unable to read properties
	 */
	public static Properties[] load(final String... files) {
		final Properties[] result = new Properties[files.length];
		final int resultLength = result.length;

		for (int i = 0; i < resultLength; i++)
			result[i] = load(files[i]);

		return result;
	}

	/**
	 * Loades properties from given files
	 *
	 * @param files list of files
	 * @return array of loaded properties
	 * @throws java.io.IOException if was unable to read properties
	 */
	public static Properties[] load(final File... files) throws IOException {
		final Properties[] result = new Properties[files.length];
		final int resultLength = result.length;

		for (int i = 0; i < resultLength; i++)
			result[i] = load(files[i]);

		return result;
	}

	/**
	 * Loads non-recursively all .property files form directory
	 *
	 * @param dir string that represents directory
	 * @return array of loaded properties
	 * @throws java.io.IOException if was unable to read properties
	 */
	public static Properties[] loadAllFromDirectory(final String dir) throws IOException {
		return loadAllFromDirectory(new File(dir), false);
	}

	/**
	 * Loads non-recursively all .property files form directory
	 *
	 * @param dir directory
	 * @return array of loaded properties
	 * @throws java.io.IOException if was unable to read properties
	 */
	public static Properties[] loadAllFromDirectory(final File dir) throws IOException {
		return loadAllFromDirectory(dir, false);
	}

	/**
	 * Loads all .property files form directory
	 *
	 * @param dir       string that represents directory
	 * @param recursive parse subdirectories or not
	 * @return array of loaded properties
	 * @throws java.io.IOException if was unable to read properties
	 */
	public static Properties[] loadAllFromDirectory(final String dir, final boolean recursive) throws IOException {
		return loadAllFromDirectory(new File(dir), recursive);
	}

	/**
	 * Loads all .property files form directory
	 *
	 * @param dir       directory
	 * @param recursive parse subdirectories or not
	 * @return array of loaded properties
	 * @throws java.io.IOException if was unable to read properties
	 */
	public static Properties[] loadAllFromDirectory(final File dir, final boolean recursive) throws IOException {
		final Collection<File> files = FileUtils.listFiles(dir, new String[] { "properties" }, recursive);

		return load(files.toArray(new File[files.size()]));
	}
}
