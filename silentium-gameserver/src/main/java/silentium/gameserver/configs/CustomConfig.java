/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.configs;

import silentium.commons.configuration.annotations.PropertiesFile;
import silentium.commons.configuration.annotations.Property;

@PropertiesFile(propertiesPatch = "./config/custom.properties")
public final class CustomConfig
{
	@Property(key = "UnlimitedPetShots", defaultValue = "False")
	public static boolean UNLIM_SHOTS;
	
	@Property(key = "UnlimitedCharacterShots", defaultValue = "False")
	public static boolean UNLIM_SSHOTS;
	
	@Property(key = "StartSubclassLevel", defaultValue = "40")
	public static int START_SUBCLASS_LEVEL;
	
	@Property(key = "AnnounceBanChat", defaultValue = "False")
	public static boolean ANNOUNCE_BAN_CHAT;
	
	@Property(key = "AnnounceUnbanChat", defaultValue = "False")
	public static boolean ANNOUNCE_UNBAN_CHAT;
	
	@Property(key = "AnnounceBanAccount", defaultValue = "False")
	public static boolean ANNOUNCE_BAN_ACC;
	
	@Property(key = "AnnounceUnbanAccount", defaultValue = "False")
	public static boolean ANNOUNCE_UNBAN_ACC;
	
	@Property(key = "AnnounceJail", defaultValue = "False")
	public static boolean ANNOUNCE_JAIL;
	
	@Property(key = "AnnounceUnjail", defaultValue = "False")
	public static boolean ANNOUNCE_UNJAIL;
	
	@Property(key = "UsePremiumServices", defaultValue = "False")
	public static boolean USE_PREMIUMSERVICE;
	
	@Property(key = "PremiumRateXp", defaultValue = "2")
	public static float PREMIUM_RATE_XP;
	
	@Property(key = "PremiumRateSp", defaultValue = "2")
	public static float PREMIUM_RATE_SP;
	
	@Property(key = "PremiumRateDropSpoil", defaultValue = "2")
	public static float PREMIUM_RATE_DROP_SPOIL;
	
	@Property(key = "PremiumRateDropItems", defaultValue = "2")
	public static float PREMIUM_RATE_DROP_ITEMS;
	
	@Property(key = "PremiumRateDropQuest", defaultValue = "2")
	public static float PREMIUM_RATE_DROP_QUEST;
	
	@Property(key = "PremiumRateRaidDropItems", defaultValue = "2")
	public static float PREMIUM_RATE_DROP_ITEMS_BY_RAID;
	
	@Property(key = "PremiumRateDropAdena", defaultValue = "2")
	public static float PREMIUM_RATE_DROP_ADENA;
	
	@Property(key = "CustomSpawn", defaultValue = "False")
	public static boolean SPAWN_CHAR;
	
	@Property(key = "SpawnX", defaultValue = "")
	public static int SPAWN_X;
	
	@Property(key = "SpawnY", defaultValue = "")
	public static int SPAWN_Y;
	
	@Property(key = "SpawnZ", defaultValue = "")
	public static int SPAWN_Z;
	
	@Property(key = "UseChatFilter", defaultValue = "False")
	public static boolean USE_SAY_FILTER;
	
	@Property(key = "ChatFilterChars", defaultValue = "[censored]")
	public static String CHAT_FILTER_CHARS;
}