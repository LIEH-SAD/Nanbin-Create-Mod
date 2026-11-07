package com.Nanbin.client.ClientData;

import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRT 铁路指示牌专用剪贴板与撤销栈。
 *
 * <p>与 MTR 原版 {@code RailwaySignScreen} 使用的 {@link SignClipboard} 完全独立：
 * 各自的静态存储、各自的撤销历史、各自的容量限制，二者互不可见、互不覆盖。
 * 原版导视牌上的复制／粘贴不会串到 CRT 指示牌，反之亦然。</p>
 *
 * <p>数据按「整块牌」保存：格数必须一致才能粘贴；撤销历史按方块坐标分桶，
 * 因此先编辑 A 牌、再编辑 B 牌，回到 A 牌时仍能继续撤回 A 牌自己的改动。
 * 每块牌最多保留 {@link #MAX_UNDO_PER_SIGN} 步，全局最多记住
 * {@link #MAX_SIGN_HISTORY} 块牌的历史；超出后丢弃最早的记录。</p>
 *
 * <p>本类只负责纯数据操作，不发送任何数据包；界面在调用后自行调用保存回调。</p>
 */
public final class CRTSignClipboard {

	/** 每块指示牌保留的撤销步数上限。 */
	private static final int MAX_UNDO_PER_SIGN = 8;
	/** 撤销历史最多覆盖多少块指示牌（按最近使用淘汰）。 */
	private static final int MAX_SIGN_HISTORY = 64;

	/** 剪贴板内容；-1 表示尚未复制过。 */
	private static int copiedLength = -1;
	private static String[] copiedSignIds;
	private static LongAVLTreeSet copiedSelectedIds;

	/** 方块坐标 -> 该牌的撤销栈（栈顶为最近一次操作前的快照）。 */
	private static final Map<Long, Deque<Snapshot>> UNDO_BY_POS = new UndoHistory();

	private CRTSignClipboard() {
	}

	/** 复制整块指示牌（格子内容 + 选中数据）。 */
	public static void copy(int length, String[] signIds, LongAVLTreeSet selectedIds) {
		copiedLength = length;
		copiedSignIds = signIds == null ? null : signIds.clone();
		copiedSelectedIds = selectedIds == null ? new LongAVLTreeSet() : new LongAVLTreeSet(selectedIds);
	}

	/** 只有格数一致的指示牌才能粘贴。 */
	public static boolean canPaste(int targetLength) {
		return copiedSignIds != null && copiedLength == targetLength && copiedSignIds.length == targetLength;
	}

	/**
	 * 把剪贴板内容粘贴到目标数据上。
	 *
	 * @return 是否真的发生了粘贴（剪贴板为空或格数不一致时返回 false，且不写入撤销栈）
	 */
	public static boolean paste(BlockPos pos, int length, String[] signIds, LongAVLTreeSet selectedIds) {
		if (!canPaste(length) || signIds == null) {
			return false;
		}
		pushUndo(pos, length, signIds, selectedIds);
		final int copyLength = Math.min(Math.min(length, signIds.length), copiedSignIds.length);
		System.arraycopy(copiedSignIds, 0, signIds, 0, copyLength);
		if (selectedIds != null) {
			selectedIds.clear();
			if (copiedSelectedIds != null) {
				selectedIds.addAll(copiedSelectedIds);
			}
		}
		return true;
	}

	/** 清空整块指示牌（格子内容 + 选中数据），并记录一步撤销。 */
	public static boolean clear(BlockPos pos, int length, String[] signIds, LongAVLTreeSet selectedIds) {
		if (signIds == null) {
			return false;
		}
		pushUndo(pos, length, signIds, selectedIds);
		final int clearLength = Math.min(length, signIds.length);
		for (int i = 0; i < clearLength; i++) {
			signIds[i] = null;
		}
		if (selectedIds != null) {
			selectedIds.clear();
		}
		return true;
	}

	/** 该坐标的指示牌是否有可撤销的操作。 */
	public static boolean canUndo(BlockPos pos) {
		if (pos == null) {
			return false;
		}
		final Deque<Snapshot> history = UNDO_BY_POS.get(pos.asLong());
		return history != null && !history.isEmpty();
	}

	/**
	 * 撤回该坐标指示牌的最近一次复制／粘贴／清空。
	 *
	 * @return 是否真的发生了撤回
	 */
	public static boolean undo(BlockPos pos, int length, String[] signIds, LongAVLTreeSet selectedIds) {
		if (signIds == null || pos == null) {
			return false;
		}
		final Deque<Snapshot> history = UNDO_BY_POS.get(pos.asLong());
		if (history == null || history.isEmpty()) {
			return false;
		}
		final Snapshot snapshot = history.pop();
		final int copyLength = Math.min(Math.min(length, signIds.length), snapshot.signIds.length);
		System.arraycopy(snapshot.signIds, 0, signIds, 0, copyLength);
		if (selectedIds != null) {
			selectedIds.clear();
			selectedIds.addAll(snapshot.selectedIds);
		}
		return true;
	}

	/** 丢弃该坐标指示牌的全部撤销记录（例如切换 JS 样式后旧布局快照已无意义）。 */
	public static void clearHistory(@Nullable BlockPos pos) {
		if (pos != null) {
			UNDO_BY_POS.remove(pos.asLong());
		}
	}

	/** 当前坐标的指示牌还有多少步可撤销（供界面/调试显示，0 表示不可撤销）。 */
	public static int undoDepth(BlockPos pos) {
		if (pos == null) {
			return 0;
		}
		final Deque<Snapshot> history = UNDO_BY_POS.get(pos.asLong());
		return history == null ? 0 : history.size();
	}

	private static void pushUndo(BlockPos pos, int length, String[] signIds, LongAVLTreeSet selectedIds) {
		if (pos == null || signIds == null) {
			return;
		}
		final int copyLength = Math.min(length, signIds.length);
		final String[] snapshotIds = new String[copyLength];
		System.arraycopy(signIds, 0, snapshotIds, 0, copyLength);
		final LongAVLTreeSet snapshotSelected = selectedIds == null ? new LongAVLTreeSet() : new LongAVLTreeSet(selectedIds);
		Deque<Snapshot> history = UNDO_BY_POS.get(pos.asLong());
		if (history == null) {
			history = new ArrayDeque<>();
			UNDO_BY_POS.put(pos.asLong(), history);
		}
		history.push(new Snapshot(snapshotIds, snapshotSelected));
		while (history.size() > MAX_UNDO_PER_SIGN) {
			history.removeLast();
		}
	}

	/** 一次操作前的数据快照。 */
	private static final class Snapshot {

		private final String[] signIds;
		private final LongAVLTreeSet selectedIds;

		private Snapshot(String[] signIds, LongAVLTreeSet selectedIds) {
			this.signIds = signIds;
			this.selectedIds = selectedIds;
		}
	}

	/** 按最近访问顺序淘汰方块历史的 LinkedHashMap（避免长时间游戏后无限增长）。 */
	private static final class UndoHistory extends LinkedHashMap<Long, Deque<Snapshot>> {

		private UndoHistory() {
			super(16, 0.75F, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, Deque<Snapshot>> eldest) {
			return size() > MAX_SIGN_HISTORY;
		}
	}
}
