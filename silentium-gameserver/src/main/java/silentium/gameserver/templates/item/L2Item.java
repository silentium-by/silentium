/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.templates.item;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastList;
import silentium.gameserver.configs.MainConfig;
import silentium.commons.utils.StringUtil;
import silentium.gameserver.model.L2Effect;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.SkillHolder;
import silentium.gameserver.skills.basefuncs.Func;
import silentium.gameserver.skills.basefuncs.FuncTemplate;
import silentium.gameserver.skills.conditions.Condition;
import silentium.gameserver.skills.effects.EffectTemplate;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.StatsSet;

/**
 * This class contains all informations concerning the item (weapon, armor, etc).<BR>
 * Mother class of : <LI>L2Armor</LI> <LI>L2EtcItem</LI> <LI>L2Weapon</LI>
 */
public abstract class L2Item
{
	public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
	public static final int TYPE1_SHIELD_ARMOR = 1;
	public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;

	public static final int TYPE2_WEAPON = 0;
	public static final int TYPE2_SHIELD_ARMOR = 1;
	public static final int TYPE2_ACCESSORY = 2;
	public static final int TYPE2_QUEST = 3;
	public static final int TYPE2_MONEY = 4;
	public static final int TYPE2_OTHER = 5;

	public static final int SLOT_NONE = 0x0000;
	public static final int SLOT_UNDERWEAR = 0x0001;
	public static final int SLOT_R_EAR = 0x0002;
	public static final int SLOT_L_EAR = 0x0004;
	public static final int SLOT_LR_EAR = 0x00006;
	public static final int SLOT_NECK = 0x0008;
	public static final int SLOT_R_FINGER = 0x0010;
	public static final int SLOT_L_FINGER = 0x0020;
	public static final int SLOT_LR_FINGER = 0x0030;
	public static final int SLOT_HEAD = 0x0040;
	public static final int SLOT_R_HAND = 0x0080;
	public static final int SLOT_L_HAND = 0x0100;
	public static final int SLOT_GLOVES = 0x0200;
	public static final int SLOT_CHEST = 0x0400;
	public static final int SLOT_LEGS = 0x0800;
	public static final int SLOT_FEET = 0x1000;
	public static final int SLOT_BACK = 0x2000;
	public static final int SLOT_LR_HAND = 0x4000;
	public static final int SLOT_FULL_ARMOR = 0x8000;
	public static final int SLOT_FACE = 0x010000;
	public static final int SLOT_ALLDRESS = 0x020000;
	public static final int SLOT_HAIR = 0x040000;
	public static final int SLOT_HAIRALL = 0x080000;

	public static final int SLOT_WOLF = -100;
	public static final int SLOT_HATCHLING = -101;
	public static final int SLOT_STRIDER = -102;
	public static final int SLOT_BABYPET = -103;

	public static final int SLOT_ALLWEAPON = SLOT_LR_HAND | SLOT_R_HAND;

	public static final int MATERIAL_STEEL = 0x00;
	public static final int MATERIAL_FINE_STEEL = 0x01;
	public static final int MATERIAL_BLOOD_STEEL = 0x02;
	public static final int MATERIAL_BRONZE = 0x03;
	public static final int MATERIAL_SILVER = 0x04;
	public static final int MATERIAL_GOLD = 0x05;
	public static final int MATERIAL_MITHRIL = 0x06;
	public static final int MATERIAL_ORIHARUKON = 0x07;
	public static final int MATERIAL_PAPER = 0x08;
	public static final int MATERIAL_WOOD = 0x09;
	public static final int MATERIAL_CLOTH = 0x0a;
	public static final int MATERIAL_LEATHER = 0x0b;
	public static final int MATERIAL_BONE = 0x0c;
	public static final int MATERIAL_HORN = 0x0d;
	public static final int MATERIAL_DAMASCUS = 0x0e;
	public static final int MATERIAL_ADAMANTAITE = 0x0f;
	public static final int MATERIAL_CHRYSOLITE = 0x10;
	public static final int MATERIAL_CRYSTAL = 0x11;
	public static final int MATERIAL_LIQUID = 0x12;
	public static final int MATERIAL_SCALE_OF_DRAGON = 0x13;
	public static final int MATERIAL_DYESTUFF = 0x14;
	public static final int MATERIAL_COBWEB = 0x15;
	public static final int MATERIAL_SEED = 0x16;

	public static final int CRYSTAL_NONE = 0x00;
	public static final int CRYSTAL_D = 0x01;
	public static final int CRYSTAL_C = 0x02;
	public static final int CRYSTAL_B = 0x03;
	public static final int CRYSTAL_A = 0x04;
	public static final int CRYSTAL_S = 0x05;

	private static final int[] crystalItemId = { 0, 1458, 1459, 1460, 1461, 1462 };
	private static final int[] crystalEnchantBonusArmor = { 0, 11, 6, 11, 19, 25 };
	private static final int[] crystalEnchantBonusWeapon = { 0, 90, 45, 67, 144, 250 };

	private final int _itemId;
	private final String _name;
	protected int _type1; // needed for item list (inventory)
	protected int _type2; // different lists for armor, weapon, etc
	private final int _weight;
	private final boolean _stackable;
	private final int _materialType;
	private final int _crystalType; // default to none-grade
	private final int _duration;
	private final int _bodyPart;
	private final int _referencePrice;
	private final int _crystalCount;

	private final boolean _sellable;
	private final boolean _dropable;
	private final boolean _destroyable;
	private final boolean _tradable;
	private final boolean _depositable;

	private final boolean _heroItem;
	private final boolean _isOlyRestricted;

	private final L2ActionType _defaultAction;

	protected FuncTemplate[] _funcTemplates;
	protected EffectTemplate[] _effectTemplates;
	protected List<Condition> _preConditions;
	private SkillHolder[] _skillHolder;

	protected static final Func[] _emptyFunctionSet = new Func[0];
	protected static final L2Effect[] _emptyEffectSet = new L2Effect[0];

	protected static final Logger _log = LoggerFactory.getLogger(L2Item.class.getName());

	/**
	 * Constructor of the L2Item that fill class variables.<BR>
	 * <BR>
	 *
	 * @param set
	 *            : StatsSet corresponding to a set of couples (key,value) for description of the item
	 */
	protected L2Item(StatsSet set)
	{
		_itemId = set.getInteger("item_id");
		_name = set.getString("name");
		_weight = set.getInteger("weight", 0);
		_materialType = ItemTable._materials.get(set.getString("material", "steel")); // default is steel, yeah and what?
		_duration = set.getInteger("duration", -1);
		_bodyPart = ItemTable._slots.get(set.getString("bodypart", "none"));
		_referencePrice = set.getInteger("price", 0);
		_crystalType = ItemTable._crystalTypes.get(set.getString("crystal_type", "none")); // default to none-grade
		_crystalCount = set.getInteger("crystal_count", 0);

		_stackable = set.getBool("is_stackable", false);
		_sellable = set.getBool("is_sellable", true);
		_dropable = set.getBool("is_dropable", true);
		_destroyable = set.getBool("is_destroyable", true);
		_tradable = set.getBool("is_tradable", true);
		_depositable = set.getBool("is_depositable", true);

		_heroItem = (_itemId >= 6611 && _itemId <= 6621) || _itemId == 6842;
		_isOlyRestricted = set.getBool("is_oly_restricted", false);

		_defaultAction = set.getEnum("default_action", L2ActionType.class, L2ActionType.none);

		String skills = set.getString("item_skill", null);
		if (skills != null)
		{
			String[] skillsSplit = skills.split(";");
			_skillHolder = new SkillHolder[skillsSplit.length];
			int used = 0;

			for (String element : skillsSplit)
			{
				try
				{
					String[] skillSplit = element.split("-");
					int id = Integer.parseInt(skillSplit[0]);
					int level = Integer.parseInt(skillSplit[1]);

					if (id == 0)
					{
						_log.info(StringUtil.concat("Ignoring item_skill(", element, ") for item ", toString(), ". Skill id is 0."));
						continue;
					}

					if (level == 0)
					{
						_log.info(StringUtil.concat("Ignoring item_skill(", element, ") for item ", toString(), ". Skill level is 0."));
						continue;
					}

					_skillHolder[used] = new SkillHolder(id, level);
					++used;
				}
				catch (Exception e)
				{
					_log.warn(StringUtil.concat("Failed to parse item_skill(", element, ") for item ", toString(), ". The used format is wrong."));
				}
			}

			// this is only loading? just don't leave a null or use a collection?
			if (used != _skillHolder.length)
			{
				SkillHolder[] skillHolder = new SkillHolder[used];
				System.arraycopy(_skillHolder, 0, skillHolder, 0, used);
				_skillHolder = skillHolder;
			}
		}
	}

	/**
	 * @return Enum the itemType.
	 */
	public abstract L2ItemType getItemType();

	/**
	 * @return int the duration of the item
	 */
	public final int getDuration()
	{
		return _duration;
	}

	/**
	 * @return int the ID of the item
	 */
	public final int getItemId()
	{
		return _itemId;
	}

	public abstract int getItemMask();

	/**
	 * @return int the type of material of the item
	 */
	public final int getMaterialType()
	{
		return _materialType;
	}

	/**
	 * @return int the type 2 of the item
	 */
	public final int getType2()
	{
		return _type2;
	}

	/**
	 * @return int the weight of the item
	 */
	public final int getWeight()
	{
		return _weight;
	}

	/**
	 * @return boolean if the item is crystallizable
	 */
	public final boolean isCrystallizable()
	{
		return _crystalType != L2Item.CRYSTAL_NONE && _crystalCount > 0;
	}

	/**
	 * @return int the type of crystal if item is crystallizable
	 */
	public final int getCrystalType()
	{
		return _crystalType;
	}

	/**
	 * @return int the type of crystal if item is crystallizable
	 */
	public final int getCrystalItemId()
	{
		return crystalItemId[_crystalType];
	}

	/**
	 * @return int the quantity of crystals for crystallization
	 */
	public final int getCrystalCount()
	{
		return _crystalCount;
	}

	/**
	 * @param enchantLevel
	 * @return int the quantity of crystals for crystallization on specific enchant level
	 */
	public final int getCrystalCount(int enchantLevel)
	{
		if (enchantLevel > 3)
		{
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
					return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * (3 * enchantLevel - 6);
				case TYPE2_WEAPON:
					return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * (2 * enchantLevel - 3);
				default:
					return _crystalCount;
			}
		}
		else if (enchantLevel > 0)
		{
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
					return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * enchantLevel;
				case TYPE2_WEAPON:
					return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * enchantLevel;
				default:
					return _crystalCount;
			}
		}
		else
			return _crystalCount;
	}

	/**
	 * @return String the name of the item
	 */
	public final String getName()
	{
		return _name;
	}

	/**
	 * @return int the part of the body used with the item.
	 */
	public final int getBodyPart()
	{
		return _bodyPart;
	}

	/**
	 * @return int the type 1 of the item
	 */
	public final int getType1()
	{
		return _type1;
	}

	/**
	 * @return boolean if the item is stackable
	 */
	public final boolean isStackable()
	{
		return _stackable;
	}

	/**
	 * @return boolean if the item is consumable
	 */
	public boolean isConsumable()
	{
		return false;
	}

	public boolean isEquipable()
	{
		return getBodyPart() != 0 && !(getItemType() instanceof L2EtcItemType);
	}

	/**
	 * @return int the price of reference of the item
	 */
	public final int getReferencePrice()
	{
		return (isConsumable() ? (int) (_referencePrice * MainConfig.RATE_CONSUMABLE_COST) : _referencePrice);
	}

	/**
	 * Returns if the item can be sold
	 *
	 * @return boolean
	 */
	public final boolean isSellable()
	{
		return _sellable;
	}

	/**
	 * Returns if the item can dropped
	 *
	 * @return boolean
	 */
	public final boolean isDropable()
	{
		return _dropable;
	}

	/**
	 * Returns if the item can destroy
	 *
	 * @return boolean
	 */
	public final boolean isDestroyable()
	{
		return _destroyable;
	}

	/**
	 * Returns if the item can add to trade
	 *
	 * @return boolean
	 */
	public final boolean isTradable()
	{
		return _tradable;
	}

	/**
	 * Returns if the item can be put into warehouse
	 *
	 * @return boolean
	 */
	public final boolean isDepositable()
	{
		return _depositable;
	}

	/**
	 * Returns array of Func objects containing the list of functions used by the item
	 *
	 * @param instance
	 *            : L2ItemInstance pointing out the item
	 * @param player
	 *            : L2Character pointing out the player
	 * @return Func[] : array of functions
	 */
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
	{
		if (_funcTemplates == null || _funcTemplates.length == 0)
			return _emptyFunctionSet;

		ArrayList<Func> funcs = new ArrayList<>(_funcTemplates.length);

		Env env = new Env();
		env.player = player;
		env.target = player;
		env.item = instance;

		Func f;

		for (FuncTemplate t : _funcTemplates)
		{
			f = t.getFunc(env, this); // skill is owner
			if (f != null)
				funcs.add(f);
		}

		if (funcs.isEmpty())
			return _emptyFunctionSet;

		return funcs.toArray(new Func[funcs.size()]);
	}

	/**
	 * Returns the effects associated with the item.
	 *
	 * @param instance
	 *            : L2ItemInstance pointing out the item
	 * @param player
	 *            : L2Character pointing out the player
	 * @return L2Effect[] : array of effects generated by the item
	 */
	public L2Effect[] getEffects(L2ItemInstance instance, L2Character player)
	{
		if (_effectTemplates == null || _effectTemplates.length == 0)
			return _emptyEffectSet;

		FastList<L2Effect> effects = FastList.newInstance();

		Env env = new Env();
		env.player = player;
		env.target = player;
		env.item = instance;

		L2Effect e;

		for (EffectTemplate et : _effectTemplates)
		{

			e = et.getEffect(env);
			if (e != null)
			{
				e.scheduleEffect();
				effects.add(e);
			}
		}

		if (effects.isEmpty())
			return _emptyEffectSet;

		L2Effect[] result = effects.toArray(new L2Effect[effects.size()]);
		FastList.recycle(effects);
		return result;
	}

	/**
	 * Add the FuncTemplate f to the list of functions used with the item
	 *
	 * @param f
	 *            : FuncTemplate to add
	 */
	public void attach(FuncTemplate f)
	{
		switch (f.stat)
		{/*
		 * FIXME elementals case FIRE_RES: case FIRE_POWER: setElementals(new Elementals(Elementals.FIRE, (int)
		 * f.lambda.calc(null))); break; case WATER_RES: case WATER_POWER: setElementals(new Elementals(Elementals.WATER, (int)
		 * f.lambda.calc(null))); break; case WIND_RES: case WIND_POWER: setElementals(new Elementals(Elementals.WIND, (int)
		 * f.lambda.calc(null))); break; case EARTH_RES: case EARTH_POWER: setElementals(new Elementals(Elementals.EARTH, (int)
		 * f.lambda.calc(null))); break; case HOLY_RES: case HOLY_POWER: setElementals(new Elementals(Elementals.HOLY, (int)
		 * f.lambda.calc(null))); break; case DARK_RES: case DARK_POWER: setElementals(new Elementals(Elementals.DARK, (int)
		 * f.lambda.calc(null))); break;
		 */
		}

		// If _functTemplates is empty, create it and add the FuncTemplate f in it
		if (_funcTemplates == null)
			_funcTemplates = new FuncTemplate[] { f };
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}

	/**
	 * Add the EffectTemplate effect to the list of effects generated by the item
	 *
	 * @param effect
	 *            : EffectTemplate
	 */
	public void attach(EffectTemplate effect)
	{
		if (_effectTemplates == null)
			_effectTemplates = new EffectTemplate[] { effect };
		else
		{
			int len = _effectTemplates.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}
	}

	public final void attach(Condition c)
	{
		if (_preConditions == null)
			_preConditions = new FastList<>();

		if (!_preConditions.contains(c))
			_preConditions.add(c);
	}

	/**
	 * Method to retrieve skills linked to this item
	 *
	 * @return Skills linked to this item as SkillHolder[]
	 */
	public final SkillHolder[] getSkills()
	{
		return _skillHolder;
	}

	public boolean checkCondition(L2Character activeChar, L2Object target, boolean sendMessage)
	{
		// Don't allow hero equipment and restricted items during Olympiad
		if ((isOlyRestrictedItem() || isHeroItem()) && ((activeChar instanceof L2PcInstance) && activeChar.getActingPlayer().isInOlympiadMode()))
		{
			if (isEquipable())
				activeChar.getActingPlayer().sendPacket(SystemMessageId.THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT);
			else
				activeChar.getActingPlayer().sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);

			return false;
		}

		if (_preConditions == null)
			return true;

		Env env = new Env();
		env.player = activeChar;
		if (target instanceof L2Character)
			env.target = (L2Character) target;

		for (Condition preCondition : _preConditions)
		{
			if (preCondition == null)
				continue;

			if (!preCondition.test(env))
			{
				if (activeChar instanceof L2Summon)
				{
					activeChar.getActingPlayer().sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
					return false;
				}

				if (sendMessage)
				{
					String msg = preCondition.getMessage();
					int msgId = preCondition.getMessageId();
					if (msg != null)
					{
						activeChar.sendMessage(msg);
					}
					else if (msgId != 0)
					{
						SystemMessage sm = SystemMessage.getSystemMessage(msgId);
						if (preCondition.isAddName())
							sm.addItemName(_itemId);
						activeChar.sendPacket(sm);
					}
				}
				return false;
			}
		}
		return true;
	}

	public boolean isConditionAttached()
	{
		return _preConditions != null && !_preConditions.isEmpty();
	}

	public boolean isQuestItem()
	{
		return (getItemType() == L2EtcItemType.QUEST);
	}

	public final boolean isHeroItem()
	{
		return _heroItem;
	}

	public boolean isOlyRestrictedItem()
	{
		return _isOlyRestricted;
	}

	public boolean isPetItem()
	{
		return (getItemType() == L2ArmorType.PET || getItemType() == L2WeaponType.PET);
	}

	public boolean isPotion()
	{
		return (getItemType() == L2EtcItemType.POTION);
	}

	public boolean isElixir()
	{
		return (getItemType() == L2EtcItemType.ELIXIR);
	}

	public L2ActionType getDefaultAction()
	{
		return _defaultAction;
	}

	/**
	 * Returns the name of the item
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return _name + " (" + _itemId + ")";
	}
}
