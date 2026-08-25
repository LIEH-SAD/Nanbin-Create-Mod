package com.Nanbin.client.ClientData;

import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 指示牌内容的客户端剪贴板。复制时保存行数、格子数与整体排版（signIds）以及选中的线路（selectedIds），
 * 只有行数和格数都相同的指示牌才能粘贴。同时维护按方块位置区分的撤回历史。
 * 支持单行（String[]）与多行（String[][]）两种数据。
 */
public class SignClipboard {

	private static int lines = -1;
	private static int length = -1;
	private static String[][] signIds;
	private static List<LongAVLTreeSet> selectedIds;

	private static final Deque<State> UNDO_STACK = new ArrayDeque<>();

	private record State(BlockPos pos, int lines, int length, String[][] signIds, List<LongAVLTreeSet> selectedIds) {
	}

	/** 单行指示牌复制（兼容旧调用）。 */
	public static void copy(int length, String[] signIds, LongAVLTreeSet selectedIds) {
		copy(1, length, new String[][]{signIds}, single(selectedIds));
	}

	/** 多行指示牌复制。 */
	public static void copy(int lines, int length, String[][] signIds, List<LongAVLTreeSet> selectedIds) {
		SignClipboard.lines = lines;
		SignClipboard.length = length;
		SignClipboard.signIds = cloneLines(signIds);
		SignClipboard.selectedIds = cloneList(selectedIds);
	}

	//只有格数完全相同的指示牌才允许粘贴。
	public static boolean canPaste(int targetLength) {
		return canPaste(1, targetLength);
	}

	//只有行数和格数都完全相同的指示牌才允许粘贴。
	public static boolean canPaste(int targetLines, int targetLength) {
		return signIds != null && selectedIds != null && signIds.length > 0 && signIds[0] != null
				&& lines == targetLines && length == targetLength
				&& signIds.length >= targetLines && signIds[0].length >= targetLength;
	}

	public static void paste(BlockPos pos, int targetLength, String[] targetSignIds, LongAVLTreeSet targetSelectedIds) {
		paste(pos, 1, targetLength, new String[][]{targetSignIds}, single(targetSelectedIds));
	}

	public static void paste(BlockPos pos, int targetLines, int targetLength, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds) {
		if (!canPaste(targetLines, targetLength) || targetSignIds == null || targetSelectedIds == null) {
			return;
		}
		pushUndo(pos, targetLines, targetLength, targetSignIds, targetSelectedIds);
		final int copyLines = Math.min(targetLines, Math.min(signIds.length, targetSignIds.length));
		for (int i = 0; i < copyLines; i++) {
			final String[] srcLine = signIds[i];
			final String[] dstLine = targetSignIds[i];
			if (srcLine == null || dstLine == null) {
				continue;
			}
			final int copyLength = Math.min(targetLength, Math.min(srcLine.length, dstLine.length));
			System.arraycopy(srcLine, 0, dstLine, 0, copyLength);
			copySelected(targetSelectedIds, i, selectedIds);
		}
	}

	public static void clear(BlockPos pos, int targetLength, String[] targetSignIds, LongAVLTreeSet targetSelectedIds) {
		clear(pos, 1, targetLength, new String[][]{targetSignIds}, single(targetSelectedIds));
	}

	public static void clear(BlockPos pos, int targetLines, int targetLength, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds) {
		if (targetSignIds == null) {
			return;
		}
		pushUndo(pos, targetLines, targetLength, targetSignIds, targetSelectedIds);
		final int clearLines = Math.min(targetLines, targetSignIds.length);
		for (int i = 0; i < clearLines; i++) {
			final String[] line = targetSignIds[i];
			if (line == null) {
				continue;
			}
			for (int j = 0; j < Math.min(targetLength, line.length); j++) {
				line[j] = null;
			}
			if (targetSelectedIds != null && i < targetSelectedIds.size() && targetSelectedIds.get(i) != null) {
				targetSelectedIds.get(i).clear();
			}
		}
	}

	public static boolean canUndo(BlockPos pos) {
		final State state = UNDO_STACK.peek();
		return state != null && pos != null && pos.equals(state.pos());
	}

	public static void undo(BlockPos pos, int targetLength, String[] targetSignIds, LongAVLTreeSet targetSelectedIds) {
		undo(pos, 1, targetLength, new String[][]{targetSignIds}, single(targetSelectedIds));
	}

	public static void undo(BlockPos pos, int targetLines, int targetLength, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds) {
		if (!canUndo(pos) || targetSignIds == null || targetSelectedIds == null) {
			return;
		}
		final State state = UNDO_STACK.pop();
		final int undoLines = Math.min(Math.min(targetLines, state.signIds().length), targetSignIds.length);
		for (int i = 0; i < undoLines; i++) {
			final String[] srcLine = state.signIds()[i];
			final String[] dstLine = targetSignIds[i];
			if (srcLine == null || dstLine == null) {
				continue;
			}
			final int copyLength = Math.min(Math.min(targetLength, state.length()), Math.min(srcLine.length, dstLine.length));
			System.arraycopy(srcLine, 0, dstLine, 0, copyLength);
			copySelected(targetSelectedIds, i, state.selectedIds());
		}
	}

	private static void copySelected(List<LongAVLTreeSet> target, int index, List<LongAVLTreeSet> source) {
		if (source == null || index >= source.size() || source.get(index) == null) {
			return;
		}
		final LongAVLTreeSet srcSet = source.get(index);
		final LongAVLTreeSet dstSet = target.get(index);
		if (dstSet != null) {
			dstSet.clear();
			dstSet.addAll(srcSet);
		}
	}

	private static void pushUndo(BlockPos pos, int lines, int length, String[][] signIds, List<LongAVLTreeSet> selectedIds) {
		UNDO_STACK.push(new State(pos, lines, length, cloneLines(signIds), cloneList(selectedIds)));
	}

	private static List<LongAVLTreeSet> single(LongAVLTreeSet selectedIds) {
		final List<LongAVLTreeSet> list = new ArrayList<>(1);
		list.add(selectedIds);
		return list;
	}

	private static String[][] cloneLines(String[][] lines) {
		if (lines == null) {
			return null;
		}
		final String[][] result = new String[lines.length][];
		for (int i = 0; i < lines.length; i++) {
			result[i] = lines[i] == null ? null : lines[i].clone();
		}
		return result;
	}

	private static List<LongAVLTreeSet> cloneList(List<LongAVLTreeSet> list) {
		if (list == null) {
			return null;
		}
		final List<LongAVLTreeSet> result = new ArrayList<>(list.size());
		for (final LongAVLTreeSet set : list) {
			result.add(set == null ? null : new LongAVLTreeSet(set));
		}
		return result;
	}
}
