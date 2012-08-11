/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.utils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.geo.GeoData;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2PcInstance;

/**
 * General Utility functions related to Gameserver
 */
public final class Util
{
	public static void handleIllegalPlayerAction(L2PcInstance actor, String message, int punishment)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new IllegalPlayerAction(actor, message, punishment), 5000);
	}

	public static String getRelativePath(File base, File file)
	{
		return file.toURI().getPath().substring(base.toURI().getPath().length());
	}

	/**
	 * @param obj1
	 * @param obj2
	 * @return degree value of object 2 to the horizontal line with object 1 being the origin
	 */
	public static double calculateAngleFrom(L2Object obj1, L2Object obj2)
	{
		return calculateAngleFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}

	/**
	 * @param obj1X
	 * @param obj1Y
	 * @param obj2X
	 * @param obj2Y
	 * @return degree value of object 2 to the horizontal line with object 1 being the origin
	 */
	public static final double calculateAngleFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0)
			angleTarget = 360 + angleTarget;

		return angleTarget;
	}

	public static final double convertHeadingToDegree(int clientHeading)
	{
		return clientHeading / 182.044444444;
	}

	public static final int convertDegreeToClientHeading(double degree)
	{
		if (degree < 0)
			degree = 360 + degree;

		return (int) (degree * 182.044444444);
	}

	public static final int calculateHeadingFrom(L2Object obj1, L2Object obj2)
	{
		return calculateHeadingFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}

	public static final int calculateHeadingFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0)
			angleTarget = 360 + angleTarget;

		return (int) (angleTarget * 182.044444444);
	}

	public static final int calculateHeadingFrom(double dx, double dy)
	{
		double angleTarget = Math.toDegrees(Math.atan2(dy, dx));
		if (angleTarget < 0)
			angleTarget = 360 + angleTarget;

		return (int) (angleTarget * 182.044444444);
	}

	public static double calculateDistance(int x1, int y1, int z1, int x2, int y2)
	{
		return calculateDistance(x1, y1, 0, x2, y2, 0, false);
	}

	public static double calculateDistance(int x1, int y1, int z1, int x2, int y2, int z2, boolean includeZAxis)
	{
		double dx = (double) x1 - x2;
		double dy = (double) y1 - y2;

		if (includeZAxis)
		{
			double dz = z1 - z2;
			return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
		}

		return Math.sqrt((dx * dx) + (dy * dy));
	}

	public static double calculateDistance(L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return 1000000;

		return calculateDistance(obj1.getPosition().getX(), obj1.getPosition().getY(), obj1.getPosition().getZ(), obj2.getPosition().getX(), obj2.getPosition().getY(), obj2.getPosition().getZ(), includeZAxis);
	}

	/**
	 * Capitalizes the first letter of a string, and returns the result.<BR>
	 * (Based on ucfirst() function of PHP)
	 *
	 * @param str
	 * @return String containing the modified string.
	 */
	public static String capitalizeFirst(String str)
	{
		str = str.trim();

		if (str.length() > 0 && Character.isLetter(str.charAt(0)))
			return str.substring(0, 1).toUpperCase() + str.substring(1);

		return str;
	}

	/**
	 * Capitalizes the first letter of every "word" in a string.<BR>
	 * (Based on ucwords() function of PHP)
	 *
	 * @param str
	 * @return String containing the modified string.
	 */
	public static String capitalizeWords(String str)
	{
		char[] charArray = str.toCharArray();
		String result = "";

		// Capitalize the first letter in the given string!
		charArray[0] = Character.toUpperCase(charArray[0]);

		for (int i = 0; i < charArray.length; i++)
		{
			if (Character.isWhitespace(charArray[i]))
				charArray[i + 1] = Character.toUpperCase(charArray[i + 1]);

			result += Character.toString(charArray[i]);
		}

		return result;
	}

	/**
	 * Format the given date on the given format
	 *
	 * @param date
	 *            : the date to format.
	 * @param format
	 *            : the format to correct by.
	 * @return a string representation of the formatted date.
	 */
	public static String formatDate(Date date, String format)
	{
		final DateFormat dateFormat = new SimpleDateFormat(format);
		if (date != null)
			return dateFormat.format(date);

		return null;
	}

	public static String formatDate(long date, String format)
	{
		final DateFormat dateFormat = new SimpleDateFormat(format);
		if (date > 0)
			return dateFormat.format(date);

		return null;
	}

	/**
	 * Faster calculation than checkIfInRange if distance is short and collisionRadius isn't needed. Not for long distance checks
	 * (potential teleports, far away castles, etc)
	 *
	 * @param radius
	 *            The radius to use as check.
	 * @param obj1
	 *            The position 1 to make check on.
	 * @param obj2
	 *            The postion 2 to make check on.
	 * @param includeZAxis
	 *            Include Z check or not.
	 * @return true if both objects are in the given radius.
	 */
	public static boolean checkIfInShortRadius(int radius, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return false;

		if (radius == -1)
			return true; // not limited

		int dx = obj1.getX() - obj2.getX();
		int dy = obj1.getY() - obj2.getY();

		if (includeZAxis)
		{
			int dz = obj1.getZ() - obj2.getZ();
			return dx * dx + dy * dy + dz * dz <= radius * radius;
		}

		return dx * dx + dy * dy <= radius * radius;
	}

	/**
	 * This check includes collision radius of both characters.<br>
	 * Used for accurate checks (skill casts, knownlist, etc).
	 *
	 * @param range
	 *            The range to use as check.
	 * @param obj1
	 *            The position 1 to make check on.
	 * @param obj2
	 *            The postion 2 to make check on.
	 * @param includeZAxis
	 *            Include Z check or not.
	 * @return true if both objects are in the given radius.
	 */
	public static boolean checkIfInRange(int range, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return false;

		if (range == -1)
			return true; // not limited

		int rad = 0;
		if (obj1 instanceof L2Character)
			rad += ((L2Character) obj1).getTemplate().getCollisionRadius();

		if (obj2 instanceof L2Character)
			rad += ((L2Character) obj2).getTemplate().getCollisionRadius();

		double dx = obj1.getX() - obj2.getX();
		double dy = obj1.getY() - obj2.getY();

		if (includeZAxis)
		{
			double dz = obj1.getZ() - obj2.getZ();
			double d = dx * dx + dy * dy + dz * dz;

			return d <= range * range + 2 * range * rad + rad * rad;
		}

		double d = dx * dx + dy * dy;
		return d <= range * range + 2 * range * rad + rad * rad;
	}

	/**
	 * Return the number of players in a defined radius.<br>
	 *
	 * @param range
	 *            : the radius.
	 * @param npc
	 *            : the object to make the test on.
	 * @param playable
	 *            : true counts summons and pets.
	 * @param invisible
	 *            : true counts invisible characters.
	 * @return the number of targets found.
	 */
	public static int getPlayersCountInRadius(int range, L2Object npc, boolean playable, boolean invisible)
	{
		int count = 0;
		final Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
		for (L2Object obj : objs)
		{
			if ((obj instanceof L2Playable && playable) || obj instanceof L2PcInstance)
			{
				if (obj instanceof L2PcInstance && !invisible)
				{
					if (((L2PcInstance) obj).getAppearance().getInvisible())
						continue;
				}

				final L2Character cha = (L2Character) obj;
				if (cha.getZ() < (npc.getZ() - 100) && cha.getZ() > (npc.getZ() + 100) || !(GeoData.getInstance().canSeeTarget(cha.getX(), cha.getY(), cha.getZ(), npc.getX(), npc.getY(), npc.getZ())))
					continue;

				if (Util.checkIfInRange(range, npc, obj, true) && !cha.isDead())
					count++;
			}
		}
		return count;
	}

	/**
	 * Verify if the given text matches with the regex pattern.
	 *
	 * @param text
	 *            : the text to test.
	 * @param regex
	 *            : the regex pattern to make test with.
	 * @return true if matching.
	 */
	public static boolean isValidName(String text, String regex)
	{
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(regex);
		}
		catch (PatternSyntaxException e) // case of illegal pattern
		{
			pattern = Pattern.compile(".*");
		}

		Matcher regexp = pattern.matcher(text);

		return regexp.matches();
	}

	/**
	 * Child of isValidName, with regular pattern for players' name.
	 *
	 * @param text
	 *            : the text to test.
	 * @return true if matching.
	 */
	public static boolean isValidPlayerName(String text)
	{
		return isValidName(text, "^[A-Za-z0-9]{1,16}$");
	}

	/**
	 * Returns the number of "words" in a given string.
	 *
	 * @param str
	 * @return int numWords
	 */
	public static int countWords(String str)
	{
		return str.trim().split(" ").length;
	}

	/**
	 * Returns a delimited string for an given array of string elements.<BR>
	 * (Based on implode() in PHP)
	 *
	 * @param strArray
	 * @param strDelim
	 * @return String implodedString
	 */
	public static String implodeString(String[] strArray, String strDelim)
	{
		String result = "";

		for (String strValue : strArray)
			result += strValue + strDelim;

		return result;
	}

	/**
	 * Returns a delimited string for an given collection of string elements.<BR>
	 * (Based on implode() in PHP)
	 *
	 * @param strCollection
	 * @param strDelim
	 * @return String implodedString
	 */
	public static String implodeString(Collection<String> strCollection, String strDelim)
	{
		return implodeString(strCollection.toArray(new String[strCollection.size()]), strDelim);
	}

	/**
	 * Returns the rounded value of val to specified number of digits after the decimal point.<BR>
	 * (Based on round() in PHP)
	 *
	 * @param val
	 * @param numPlaces
	 * @return float roundedVal
	 */
	public static float roundTo(float val, int numPlaces)
	{
		if (numPlaces <= 1)
			return Math.round(val);

		float exponent = (float) Math.pow(10, numPlaces);

		return (Math.round(val * exponent) / exponent);
	}

	/**
	 * @param text
	 *            - the text to check
	 * @return {@code true} if {@code text} contains only numbers, {@code false} otherwise
	 */
	public static boolean isDigit(String text)
	{
		if (text == null)
			return false;

		return text.matches("[0-9]+");
	}

	public static boolean isAlphaNumeric(String text)
	{
		if (text == null)
			return false;

		boolean result = true;
		char[] chars = text.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			if (!Character.isLetterOrDigit(chars[i]))
			{
				result = false;
				break;
			}
		}
		return result;
	}

	/**
	 * Return amount of adena formatted with "," delimiter
	 *
	 * @param amount
	 * @return String formatted adena amount
	 */
	public static String formatAdena(long amount)
	{
		String s = "";
		long rem = amount % 1000;
		s = Long.toString(rem);
		amount = (amount - rem) / 1000;
		while (amount > 0)
		{
			if (rem < 99)
				s = '0' + s;
			if (rem < 9)
				s = '0' + s;
			rem = amount % 1000;
			s = Long.toString(rem) + "," + s;
			amount = (amount - rem) / 1000;
		}
		return s;
	}

	/**
	 * @param <T>
	 *            The Object type.
	 * @param array
	 *            - the array to look into
	 * @param obj
	 *            - the object to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise
	 */
	public static <T> boolean contains(T[] array, T obj)
	{
		for (T element : array)
			if (element == obj)
				return true;

		return false;
	}

	/**
	 * @param array
	 *            - the array to look into
	 * @param obj
	 *            - the integer to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise
	 */
	public static boolean contains(int[] array, int obj)
	{
		for (int element : array)
			if (element == obj)
				return true;

		return false;
	}
}