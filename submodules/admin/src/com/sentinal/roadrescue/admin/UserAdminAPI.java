package com.sentinal.roadrescue.admin;

import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class UserAdminAPI
{
	private static final class Holder
	{
		private static final UserAdminOperations INSTANCE = TransactionProxy.create(UserAdminOperations.class, new UserAdminImpl());
	}

	public static UserAdminOperations getUserAdminImpl()
	{
		return Holder.INSTANCE;
	}

	public List<Map<String, Object>> getUsers()
	{
		return getUserAdminImpl().getUsers();
	}
}
