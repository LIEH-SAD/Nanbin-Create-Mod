package com.Nanbin.client.ClientData;

import org.mtr.mapping.holder.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 路名标识内容的客户端剪贴数据。
 */
public class RoadNameClipboard {

	private static String[] texts;

	private static final Deque<State> UNDO_STACK = new ArrayDeque<>();

	private record State(BlockPos pos, String[] texts) {
	}

	public static void copy(String[] sourceTexts) {
		texts = sourceTexts == null ? null : sourceTexts.clone();
	}

	public static boolean canPaste() {
		return texts != null;
	}

	public static void paste(BlockPos pos, String[] targetTexts) {
		if (!canPaste() || targetTexts == null) {
			return;
		}
		pushUndo(pos, targetTexts);
		System.arraycopy(texts, 0, targetTexts, 0, Math.min(texts.length, targetTexts.length));
	}

	public static void clear(BlockPos pos, String[] targetTexts) {
		if (targetTexts == null) {
			return;
		}
		pushUndo(pos, targetTexts);
		for (int i = 0; i < targetTexts.length; i++) {
			targetTexts[i] = "";
		}
	}

	public static boolean canUndo(BlockPos pos) {
		final State state = UNDO_STACK.peek();
		return state != null && pos != null && pos.equals(state.pos());
	}

	public static void undo(BlockPos pos, String[] targetTexts) {
		if (!canUndo(pos) || targetTexts == null) {
			return;
		}
		final State state = UNDO_STACK.pop();
		System.arraycopy(state.texts(), 0, targetTexts, 0, Math.min(state.texts().length, targetTexts.length));
	}

	private static void pushUndo(BlockPos pos, String[] texts) {
		UNDO_STACK.push(new State(pos, texts == null ? null : texts.clone()));
	}
}
