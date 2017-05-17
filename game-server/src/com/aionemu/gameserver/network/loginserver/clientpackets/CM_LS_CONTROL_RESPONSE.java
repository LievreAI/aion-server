package com.aionemu.gameserver.network.loginserver.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.World;

/**
 * @author Aionchs-Wylovech
 */
public class CM_LS_CONTROL_RESPONSE extends LsClientPacket {

	public CM_LS_CONTROL_RESPONSE(int opCode) {
		super(opCode);
	}

	private int type;
	private boolean result;
	private String playerName;
	private byte param;
	private String adminName;
	private int accountId;

	@Override
	public void readImpl() {
		type = readUC();
		result = readUC() == 1;
		adminName = readS();
		playerName = readS();
		param = readC();
		accountId = readD();
	}

	@Override
	public void runImpl() {
		World world = World.getInstance();
		Player admin = world.findPlayer(Util.convertName(adminName));
		Player player = world.findPlayer(Util.convertName(playerName));
		LoginServer.getInstance().accountUpdate(accountId, param, type);
		switch (type) {
			case 1:
				if (!result) {
					if (admin != null)
						PacketSendUtility.sendMessage(admin, playerName + " has been promoted Administrator with role " + param);
					if (player != null) {
						PacketSendUtility.sendMessage(player, "You have been promoted Administrator with role " + param + " by " + adminName);
					}
				} else {
					if (admin != null)
						PacketSendUtility.sendMessage(admin, " Abnormal, the operation failed! ");
				}
				break;
			case 2:
				if (!result) {
					if (admin != null)
						PacketSendUtility.sendMessage(admin, playerName + " has been promoted membership with level " + param);
					if (player != null)
						PacketSendUtility.sendMessage(player, "You have been promoted to membership with level " + param + " by " + adminName);
				} else {
					if (admin != null)
						PacketSendUtility.sendMessage(admin, "The operation failed!");
				}
				break;
		}
	}
}
