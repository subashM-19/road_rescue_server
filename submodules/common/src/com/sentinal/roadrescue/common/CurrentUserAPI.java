package com.sentinal.roadrescue.common;

import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class CurrentUserAPI
{
	private static final class Holder
	{
		private static final CurrentUserOperations INSTANCE = TransactionProxy.create(CurrentUserOperations.class, new CurrentUserImpl());
	}

	public static CurrentUserOperations getCurrentUserImpl()
	{
		return Holder.INSTANCE;
	}

	public Map<String, Object> getCurrentUser()
	{
		return getCurrentUserImpl().getCurrentUser();
	}
}
