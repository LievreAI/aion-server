package quest.inggison;

import static com.aionemu.gameserver.model.DialogAction.*;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author dta3000
 */
public class _11026SolidEvidence extends AbstractQuestHandler {

	public _11026SolidEvidence() {
		super(11026);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798950).addOnQuestStart(questId);
		qe.registerQuestNpc(798950).addOnTalkEvent(questId);
		qe.registerQuestNpc(798941).addOnTalkEvent(questId);
		qe.registerQuestNpc(203384).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.isStartable()) {
			if (targetId == 798950) {
				if (env.getDialogActionId() == QUEST_SELECT)
					return sendQuestDialog(env, 1011);
				else if (env.getDialogActionId() == QUEST_ACCEPT_1) {
					if (giveQuestItem(env, 182206719, 1))
						return sendQuestStartDialog(env);
					else
						return true;
				} else
					return sendQuestStartDialog(env);
			}
		} else if (targetId == 798941) {
			if (qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
				if (env.getDialogActionId() == QUEST_SELECT)
					return sendQuestDialog(env, 1352);
				else if (env.getDialogActionId() == SETPRO1) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
					return true;
				} else
					return sendQuestStartDialog(env);
			}
		} else if (targetId == 203384) {
			if (qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 1) {
				if (env.getDialogActionId() == QUEST_SELECT)
					return sendQuestDialog(env, 2375);
				else if (env.getDialogActionId() == SELECT_QUEST_REWARD) {
					removeQuestItem(env, 182206719, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestDialog(env, 2375);
				} else
					return sendQuestStartDialog(env);
			} else if (qs.getStatus() == QuestStatus.REWARD)
				return sendQuestEndDialog(env);
		}

		return false;
	}
}
