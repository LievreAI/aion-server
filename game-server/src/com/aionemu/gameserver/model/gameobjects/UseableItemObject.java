package com.aionemu.gameserver.model.gameobjects;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;

import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingUseableItem;
import com.aionemu.gameserver.model.templates.housing.LimitType;
import com.aionemu.gameserver.model.templates.housing.UseItemAction;
import com.aionemu.gameserver.network.PacketWriteHelper;
import com.aionemu.gameserver.network.aion.serverpackets.SM_OBJECT_USE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_OBJECT;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * @author Rolandas
 */
public class UseableItemObject extends HouseObject<HousingUseableItem> {

	private volatile boolean mustGiveLastReward = false;
	private AtomicInteger usingPlayer = new AtomicInteger();
	private UseDataWriter entryWriter = null;

	public UseableItemObject(House owner, int objId, int templateId) {
		super(owner, objId, templateId);
		UseItemAction action = getObjectTemplate().getAction();
		if (action != null && action.getFinalRewardId() != null && getUseSecondsLeft() == 0)
			mustGiveLastReward = true;
		entryWriter = new UseDataWriter(this);
	}

	static class UseDataWriter extends PacketWriteHelper {

		UseableItemObject obj;

		public UseDataWriter(UseableItemObject obj) {
			this.obj = obj;
		}

		@Override
		protected void writeMe(ByteBuffer buffer) {
			writeD(buffer, obj.getObjectTemplate().getUseCount() == null ? 0 : obj.getOwnerUsedCount() + obj.getVisitorUsedCount());
			UseItemAction action = obj.getObjectTemplate().getAction();
			writeC(buffer, action == null || action.getCheckType() == null ? 0 : action.getCheckType());
		}
	}

	@Override
	public void onUse(final Player player) {
		UseItemAction action = getObjectTemplate().getAction();
		if (action == null) { // Some objects do not have actions; they are test items now
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_ALL_CANT_USE);
			return;
		}

		boolean isOwner = getOwnerHouse().getOwnerId() == player.getObjectId();
		if (!isOwner && getObjectTemplate().isOwnerOnly()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_IS_ONLY_FOR_OWNER_VALID);
			return;
		}

		if (!player.getHouseObjectCooldownList().isCanUseObject(getObjectId())) {
			if (getObjectTemplate().getCd() != null && getObjectTemplate().getCd() > 0)
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANNOT_USE_FLOWERPOT_COOLTIME);
			else
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_CANT_USE_PER_DAY);
			return;
		}

		final Integer useCount = getObjectTemplate().getUseCount();
		int currentUseCount = 0;
		if (useCount != null) {
			// Counter is for both, but could be made custom from configs
			currentUseCount = getOwnerUsedCount() + getVisitorUsedCount();
			if (currentUseCount >= useCount && !isOwner || currentUseCount > useCount && isOwner) {
				// if expiration is set then final reward has to be given for owner only due to inventory full. If inventory was not full, the object had to
				// be despawned, so we wouldn't reach this check.
				if (!mustGiveLastReward || !isOwner) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_ACHIEVE_USE_COUNT);
					return;
				}
			}
		}

		if (mustGiveLastReward && !isOwner) { // expired, wait for owner
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_DELETE_EXPIRE_TIME(getObjectTemplate().getNameId()));
			return;
		}

		if (!usingPlayer.compareAndSet(0, player.getObjectId())) {
			if (usingPlayer.get() != player.getObjectId()) // if it's the same player using, don't send the message (might be double-click)
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_OCCUPIED_BY_OTHER);
			return;
		}

		if (getObjectTemplate().getPlacementLimit() == LimitType.COOKING) {
			// Check if player already has an item
			if (player.getInventory().getItemCountByItemId(action.getRewardId()) > 0) {
				int nameId = DataManager.ITEM_DATA.getItemTemplate(action.getRewardId()).getNameId();
				warnAndRelease(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_USE_ALREADY_HAVE_REWARD_ITEM(nameId, getObjectTemplate().getNameId()));
				return;
			}
		}

		final Integer requiredItem = getObjectTemplate().getRequiredItem();
		if (requiredItem != null) {
			if (action.getCheckType() == 1) { // equip item needed
				if (player.getEquipment().getEquippedItemsByItemId(requiredItem).size() == 0) {
					int descId = DataManager.ITEM_DATA.getItemTemplate(requiredItem).getNameId();
					warnAndRelease(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_USE_HOUSE_OBJECT_ITEM_EQUIP(new DescriptionId(descId)));
					return;
				}
			} else if (player.getInventory().getItemCountByItemId(requiredItem) < action.getRemoveCount()) {
				int descId = DataManager.ITEM_DATA.getItemTemplate(requiredItem).getNameId();
				warnAndRelease(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_USE_HOUSE_OBJECT_ITEM_CHECK(new DescriptionId(descId)));
				return;
			}
		}

		if (player.getInventory().isFull()) {
			warnAndRelease(player, SM_SYSTEM_MESSAGE.STR_WAREHOUSE_TOO_MANY_ITEMS_INVENTORY);
			return;
		}

		final int delay = getObjectTemplate().getDelay();
		final int ownerId = getOwnerHouse().getOwnerId();
		final int usedCount = useCount == null ? 0 : currentUseCount + 1;
		final ItemUseObserver observer = new ItemUseObserver() {

			@Override
			public void abort() {
				PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), 0, 9));
				player.getObserveController().removeObserver(this);
				warnAndRelease(player, null);
			}

			@Override
			public void itemused(Item item) {
				abort();
			}
		};

		player.getObserveController().attach(observer);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_USE(getObjectTemplate().getNameId()));
		PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), delay, 8));
		player.getController().addTask(TaskId.HOUSE_OBJECT_USE, ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				try {
					PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getObjectId(), 0, 9));
					if (action.getRemoveCount() != null && action.getRemoveCount() > 0) {
						if (!player.getInventory().decreaseByItemId(requiredItem, action.getRemoveCount()))
							return;
					}

					int rewardId = 0;
					boolean delete = false;

					if (useCount != null) {
						if (action.getFinalRewardId() != null && useCount + 1 == usedCount) {
							// visitors do not get final rewards
							rewardId = action.getFinalRewardId();
							delete = true;
						} else if (action.getRewardId() != null) {
							rewardId = action.getRewardId();
							if (useCount == usedCount) {
								PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_FLOWERPOT_GOAL(getObjectTemplate().getNameId()));
								if (action.getFinalRewardId() == null) {
									delete = true;
								} else {
									setMustGiveLastReward(true);
									setExpireTime((int) (System.currentTimeMillis() / 1000));
									setPersistentState(PersistentState.UPDATE_REQUIRED);
								}
							}
						}
					} else if (action.getRewardId() != null) {
						rewardId = action.getRewardId();
					}
					if (usedCount > 0) {
						if (!delete)
							if (player.getObjectId() == ownerId)
								incrementOwnerUsedCount();
							else
								incrementVisitorUsedCount();
						PacketSendUtility.broadcastPacket(player, new SM_OBJECT_USE_UPDATE(player.getObjectId(), ownerId, usedCount, UseableItemObject.this), true);
					}
					if (rewardId > 0) {
						ItemService.addItem(player, rewardId, 1);
						int rewardNameId = DataManager.ITEM_DATA.getItemTemplate(rewardId).getNameId();
						PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_REWARD_ITEM(getObjectTemplate().getNameId(), rewardNameId));
					}
					if (delete)
						selfDestroy(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OBJECT_DELETE_USE_COUNT_FINAL(getObjectTemplate().getNameId()));
					else {
						Integer cd = getObjectTemplate().getCd();
						DateTime repeatDate;
						if (cd == null || cd == 0) {
							// use once per day
							DateTime tomorrow = DateTime.now().plusDays(1);
							repeatDate = new DateTime(tomorrow.getYear(), tomorrow.getMonthOfYear(), tomorrow.getDayOfMonth(), 0, 0, 0);
							cd = (int) (repeatDate.getMillis() - DateTime.now().getMillis()) / 1000;
						}
						player.getHouseObjectCooldownList().addHouseObjectCooldown(getObjectId(), cd);
					}
				} finally {
					player.getObserveController().removeObserver(observer);
					warnAndRelease(player, null);
				}
			}
		}, delay));
	}

	private void warnAndRelease(Player player, SM_SYSTEM_MESSAGE systemMessage) {
		if (systemMessage != null)
			PacketSendUtility.sendPacket(player, systemMessage);
		usingPlayer.set(0);
	}

	public void setMustGiveLastReward(boolean mustGiveLastReward) {
		this.mustGiveLastReward = mustGiveLastReward;
	}

	@Override
	public boolean canExpireNow() {
		return !mustGiveLastReward && usingPlayer.get() == 0;
	}

	public void writeUsageData(ByteBuffer buffer) {
		entryWriter.writeMe(buffer);
	}
}
