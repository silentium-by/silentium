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

@PropertiesFile(propertiesPatch = "./config/clans.properties")
public final class ClansConfig extends ConfigEngine
{
	@Property(key = "AltManorSaveAllActions", defaultValue = "False")
	public static boolean ALT_MANOR_SAVE_ALL_ACTIONS;
		
	@Property(key = "AltMembersCanWithdrawFromClanWH", defaultValue = "False")
	public static boolean ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
		
	@Property(key = "RemoveCastleCirclets", defaultValue = "True")
	public static boolean REMOVE_CASTLE_CIRCLETS;
		
	@Property(key = "DaysBeforeAcceptNewClanWhenDismissed", defaultValue = "1")
	public static int ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
		
	@Property(key = "DaysBeforeJoinAllyWhenDismissed", defaultValue = "1")
	public static int ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
		
	@Property(key = "DaysBeforeJoinAllyWhenLeaved", defaultValue = "1")
	public static int ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
		
	@Property(key = "DaysBeforeCreateAClan", defaultValue = "10")
	public static int ALT_CLAN_CREATE_DAYS;
		
	@Property(key = "DaysToPassToDissolveAClan", defaultValue = "7")
	public static int ALT_CLAN_DISSOLVE_DAYS;
		
	@Property(key = "DaysBeforeJoinAClan", defaultValue = "5")
	public static int ALT_CLAN_JOIN_DAYS;
		
	@Property(key = "AltClanMembersForWar", defaultValue = "15")
	public static int ALT_CLAN_MEMBERS_FOR_WAR;
		
	@Property(key = "DaysBeforeCreateNewAllyWhenDissolved", defaultValue = "10")
	public static int ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
		
	@Property(key = "AltManorApproveMin", defaultValue = "00")
	public static int ALT_MANOR_APPROVE_MIN;
		
	@Property(key = "AltManorApproveTime", defaultValue = "6")
	public static int ALT_MANOR_APPROVE_TIME;
		
	@Property(key = "AltManorMaintenancePeriod", defaultValue = "360000")
	public static int ALT_MANOR_MAINTENANCE_PERIOD;
		
	@Property(key = "AltManorRefreshMin", defaultValue = "00")
	public static int ALT_MANOR_REFRESH_MIN;
		
	@Property(key = "AltManorRefreshTime", defaultValue = "20")
	public static int ALT_MANOR_REFRESH_TIME;
		
	@Property(key = "AltManorSavePeriodRate", defaultValue = "2")
	public static int ALT_MANOR_SAVE_PERIOD_RATE;
		
	@Property(key = "AltMaxNumOfClansInAlly", defaultValue = "3")
	public static int ALT_MAX_NUM_OF_CLANS_IN_ALLY;
		
	@Property(key = "ClanHallCurtainFunctionFeeLvl1", defaultValue = "86400000")
	public static int CH_CURTAIN1_FEE;
		
	@Property(key = "ClanHallCurtainFunctionFeeLvl2", defaultValue = "86400000")
	public static int CH_CURTAIN2_FEE;
		
	@Property(key = "ClanHallExpRegenerationFeeLvl1", defaultValue = "86400000")
	public static int CH_EXPREG1_FEE;
		
	@Property(key = "ClanHallExpRegenerationFeeLvl2", defaultValue = "86400000")
	public static int CH_EXPREG2_FEE;
		
	@Property(key = "ClanHallExpRegenerationFeeLvl3", defaultValue = "86400000")
	public static int CH_EXPREG3_FEE;
		
	@Property(key = "ClanHallExpRegenerationFeeLvl4", defaultValue = "86400000")
	public static int CH_EXPREG4_FEE;
		
	@Property(key = "ClanHallExpRegenerationFeeLvl5", defaultValue = "86400000")
	public static int CH_EXPREG5_FEE;
		
	@Property(key = "ClanHallExpRegenerationFeeLvl6", defaultValue = "86400000")
	public static int CH_EXPREG6_FEE;
		
	@Property(key = "ClanHallExpRegenerationFeeLvl7", defaultValue = "86400000")
	public static int CH_EXPREG7_FEE;
		
	@Property(key = "ClanHallFrontPlatformFunctionFeeLvl1", defaultValue = "86400000")
	public static int CH_FRONT1_FEE;
		
	@Property(key = "ClanHallFrontPlatformFunctionFeeLvl2", defaultValue = "86400000")
	public static int CH_FRONT2_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl1", defaultValue = "86400000")
	public static int CH_HPREG1_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl10", defaultValue = "86400000")
	public static int CH_HPREG10_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl11", defaultValue = "86400000")
	public static int CH_HPREG11_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl12", defaultValue = "86400000")
	public static int CH_HPREG12_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl13", defaultValue = "86400000")
	public static int CH_HPREG13_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl2", defaultValue = "86400000")
	public static int CH_HPREG2_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl3", defaultValue = "86400000")
	public static int CH_HPREG3_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl4", defaultValue = "86400000")
	public static int CH_HPREG4_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl5", defaultValue = "86400000")
	public static int CH_HPREG5_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl6", defaultValue = "86400000")
	public static int CH_HPREG6_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl7", defaultValue = "86400000")
	public static int CH_HPREG7_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl8", defaultValue = "86400000")
	public static int CH_HPREG8_FEE;
		
	@Property(key = "ClanHallHpRegenerationFeeLvl9", defaultValue = "86400000")
	public static int CH_HPREG9_FEE;
		
	@Property(key = "ClanHallItemCreationFunctionFeeLvl1", defaultValue = "86400000")
	public static int CH_ITEM1_FEE;
		
	@Property(key = "ClanHallItemCreationFunctionFeeLvl2", defaultValue = "86400000")
	public static int CH_ITEM2_FEE;
		
	@Property(key = "ClanHallItemCreationFunctionFeeLvl3", defaultValue = "86400000")
	public static int CH_ITEM3_FEE;
		
	@Property(key = "ClanHallMpRegenerationFeeLvl1", defaultValue = "86400000")
	public static int CH_MPREG1_FEE;
		
	@Property(key = "ClanHallMpRegenerationFeeLvl2", defaultValue = "86400000")
	public static int CH_MPREG2_FEE;
		
	@Property(key = "ClanHallMpRegenerationFeeLvl3", defaultValue = "86400000")
	public static int CH_MPREG3_FEE;
		
	@Property(key = "ClanHallMpRegenerationFeeLvl4", defaultValue = "86400000")
	public static int CH_MPREG4_FEE;
		
	@Property(key = "ClanHallMpRegenerationFeeLvl5", defaultValue = "86400000")
	public static int CH_MPREG5_FEE;
		
	@Property(key = "ClanHallSupportFeeLvl1", defaultValue = "86400000")
	public static int CH_SUPPORT1_FEE;
		
	@Property(key = "ClanHallSupportFeeLvl2", defaultValue = "86400000")
	public static int CH_SUPPORT2_FEE;
		
	@Property(key = "ClanHallSupportFeeLvl3", defaultValue = "86400000")
	public static int CH_SUPPORT3_FEE;
		
	@Property(key = "ClanHallSupportFeeLvl4", defaultValue = "86400000")
	public static int CH_SUPPORT4_FEE;
		
	@Property(key = "ClanHallSupportFeeLvl5", defaultValue = "86400000")
	public static int CH_SUPPORT5_FEE;
		
	@Property(key = "ClanHallSupportFeeLvl6", defaultValue = "86400000")
	public static int CH_SUPPORT6_FEE;
		
	@Property(key = "ClanHallSupportFeeLvl7", defaultValue = "86400000")
	public static int CH_SUPPORT7_FEE;
		
	@Property(key = "ClanHallSupportFeeLvl8", defaultValue = "86400000")
	public static int CH_SUPPORT8_FEE;
		
	@Property(key = "ClanHallTeleportFunctionFeeLvl1", defaultValue = "86400000")
	public static int CH_TELE1_FEE;
		
	@Property(key = "ClanHallTeleportFunctionFeeLvl2", defaultValue = "86400000")
	public static int CH_TELE2_FEE;
		
	@Property(key = "ClanHallCurtainFunctionFeeRation", defaultValue = "86400000")
	public static long CH_CURTAIN_FEE_RATIO;
		
	@Property(key = "ClanHallExpRegenerationFunctionFeeRation", defaultValue = "86400000")
	public static long CH_EXPREG_FEE_RATIO;
		
	@Property(key = "ClanHallFrontPlatformFunctionFeeRation", defaultValue = "86400000")
	public static long CH_FRONT_FEE_RATIO;
		
	@Property(key = "ClanHallHpRegenerationFunctionFeeRation", defaultValue = "86400000")
	public static long CH_HPREG_FEE_RATIO;
		
	@Property(key = "ClanHallItemCreationFunctionFeeRation", defaultValue = "86400000")
	public static long CH_ITEM_FEE_RATIO;
		
	@Property(key = "ClanHallMpRegenerationFunctionFeeRation", defaultValue = "86400000")
	public static long CH_MPREG_FEE_RATIO;
		
	@Property(key = "ClanHallSupportFunctionFeeRation", defaultValue = "86400000")
	public static long CH_SUPPORT_FEE_RATIO;
		
	@Property(key = "ClanHallTeleportFunctionFeeRation", defaultValue = "86400000")
	public static long CH_TELE_FEE_RATIO;
}