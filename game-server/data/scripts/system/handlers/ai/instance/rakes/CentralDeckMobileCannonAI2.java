package ai.instance.rakes;

import java.util.List;

import ai.ActionItemNpcAI2;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * @author xTz
 */
@AIName("centralcannon")
public class CentralDeckMobileCannonAI2 extends ActionItemNpcAI2 {

	@Override
	protected void handleUseItemFinish(Player player) {
		if (!player.getInventory().decreaseByItemId(185000052, 1)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1111302));
			return;
		}
		WorldPosition worldPosition = player.getPosition();

		if (worldPosition.isInstanceMap()) {
			if (worldPosition.getMapId() == 300100000) {
				WorldMapInstance worldMapInstance = worldPosition.getWorldMapInstance();
				// need check
				// getOwner().getController().useSkill(18572);

				killNpc(worldMapInstance.getNpcs(215402));
				killNpc(worldMapInstance.getNpcs(215403));
				killNpc(worldMapInstance.getNpcs(215404));
				killNpc(worldMapInstance.getNpcs(215405));
				killNpc(worldMapInstance.getNpcs(230727));
				killNpc(worldMapInstance.getNpcs(231460));
				killNpc(worldMapInstance.getNpcs(214968));
				spawn(215069, 473.178f, 508.966f, 1032.84f, (byte) 0); // Chief Mate Menekiki
			}
		}
	}

	private void killNpc(List<Npc> npcs) {
		for (Npc npc : npcs) {
			AI2Actions.kill(this, npc);
		}
	}

}
