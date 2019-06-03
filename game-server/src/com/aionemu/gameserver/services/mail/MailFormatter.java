package com.aionemu.gameserver.services.mail;

import java.time.Duration;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.templates.mail.MailPart;
import com.aionemu.gameserver.model.templates.mail.MailTemplate;

/**
 * @author Rolandas
 */
public final class MailFormatter {

	public static void sendBlackCloudMail(String recipientName, int itemObjectId, int itemCount) {
		MailTemplate template = DataManager.SYSTEM_MAIL_TEMPLATES.getMailTemplate("$$CASH_ITEM_MAIL", "", Race.PC_ALL);

		MailPart formatter = new MailPart() {

			@Override
			public String getParamValue(String name) {
				if ("itemid".equals(name))
					return Integer.toString(itemObjectId);
				else if ("count".equals(name))
					return Integer.toString(itemCount);
				else if ("unk1".equals(name))
					return "0";
				else if ("purchasedate".equals(name))
					return Long.toString(System.currentTimeMillis() / 1000);
				return "";
			}

		};

		String title = template.getFormattedTitle(formatter);
		String body = template.getFormattedMessage(formatter);

		SystemMailService.sendMail("$$CASH_ITEM_MAIL", recipientName, title, body, itemObjectId, itemCount, 0, LetterType.BLACKCLOUD);
	}

	public static void sendHouseMaintenanceMail(House ownedHouse, long impoundTimeMillis, long kinah) {
		String templateName;
		long daysUntilImpoundment = Duration.ofMillis(impoundTimeMillis - System.currentTimeMillis()).toDays();
		if (daysUntilImpoundment <= 0)
			templateName = "$$HS_OVERDUE_3RD";
		else if (daysUntilImpoundment <= 7)
			templateName = "$$HS_OVERDUE_2ND";
		else if (daysUntilImpoundment <= 14)
			templateName = "$$HS_OVERDUE_1ST";
		else
			return;

		MailTemplate template = DataManager.SYSTEM_MAIL_TEMPLATES.getMailTemplate(templateName, "", ownedHouse.getPlayerRace());

		MailPart formatter = new MailPart() {

			@Override
			public String getParamValue(String name) {
				if ("address".equals(name))
					return Integer.toString(ownedHouse.getAddress().getId());
				else if ("datetime".equals(name))
					return Long.toString(impoundTimeMillis / 60000);
				return "";
			}

		};

		String title = template.getFormattedTitle(null);
		String message = template.getFormattedMessage(formatter);

		SystemMailService.sendMail(templateName, ownedHouse.getButler().getMasterName(), title, message, 0, 0, kinah, LetterType.NORMAL);
	}

	public static void sendHouseAuctionMail(House ownedHouse, PlayerCommonData playerData, AuctionResult result, long time, long returnKinah) {
		MailTemplate template = DataManager.SYSTEM_MAIL_TEMPLATES.getMailTemplate("$$HS_AUCTION_MAIL", "", playerData.getRace());
		if (ownedHouse == null || result == null)
			return;

		MailPart formatter = new MailPart() {

			@Override
			public String getParamValue(String name) {
				if ("address".equals(name))
					return Integer.toString(ownedHouse.getAddress().getId());
				else if ("datetime".equals(name))
					return Long.toString(time / 1000);
				else if ("resultid".equals(name))
					return Integer.toString(result.getId());
				else if ("raceid".equals(name))
					return Integer.toString(playerData.getRace().getRaceId());
				return "";
			}
		};

		String title = template.getFormattedTitle(formatter);
		String message = template.getFormattedMessage(formatter);

		SystemMailService.sendMail("$$HS_AUCTION_MAIL", playerData.getName(), title, message, 0, 0, returnKinah, LetterType.NORMAL);
	}

	public static void sendAbyssRewardMail(SiegeLocation siegeLocation, PlayerCommonData playerData, AbyssSiegeLevel level, SiegeResult result,
		long time, int attachedItemObjId, long attachedItemCount, long attachedKinahCount) {

		MailTemplate template = DataManager.SYSTEM_MAIL_TEMPLATES.getMailTemplate("$$ABYSS_REWARD_MAIL", "", playerData.getRace());

		MailPart formatter = new MailPart() {

			@Override
			public String getParamValue(String name) {
				if ("siegelocid".equals(name))
					return Integer.toString(siegeLocation.getTemplate().getId());
				else if ("datetime".equals(name))
					return Long.toString(time / 1000);
				else if ("rankid".equals(name))
					return Integer.toString(level.getId());
				else if ("raceid".equals(name))
					return Integer.toString(playerData.getRace().getRaceId());
				else if ("resultid".equals(name))
					return Integer.toString(result.getId());
				return "";
			}
		};

		String title = template.getFormattedTitle(formatter);
		String message = template.getFormattedMessage(formatter);

		SystemMailService.sendMail("$$ABYSS_REWARD_MAIL", playerData.getName(), title, message, attachedItemObjId, attachedItemCount, attachedKinahCount,
			LetterType.NORMAL);
	}
}
