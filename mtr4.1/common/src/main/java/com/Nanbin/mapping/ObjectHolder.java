package com.Nanbin.mapping;

import java.util.function.Supplier;

public final class ObjectHolder<T> {

	private final Supplier<T> supplier;
	private T object;

	public ObjectHolder(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	public void create() {
		if (object == null) {
			object = supplier.get();
		}
	}

	public boolean exists() {
		return object != null;
	}

	public T get() {
		create();
		return object;
	}

	/**
	 * 取对象，拿不到就返回 null（不抛异常）。
	 * <p>
	 * 为什么需要：NeoForge 的注册是<b>延迟</b>的（{@code DeferredRegister} 在对应的
	 * register 事件里才绑定），在绑定之前 {@code DeferredHolder#get()} 会直接抛
	 * {@code NullPointerException: Trying to access unbound value}；
	 * Fabric 则是立即注册，构造期就能取到。需要「现在能取就取、取不到稍后再试」的地方
	 * （例如创作标签页注册之后的 filters 集成）一律用这个方法。
	 */
	public T tryGet() {
		try {
			return get();
		} catch (Exception e) {
			return null;
		}
	}
}
