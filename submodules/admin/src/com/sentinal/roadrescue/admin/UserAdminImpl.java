package com.sentinal.roadrescue.admin;

import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;
import com.sentinal.roadrescue.common.DateUtil;
import com.sentinal.roadrescue.roles.UserRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserAdminImpl implements UserAdminOperations
{
	@Override
	public List<Map<String, Object>> getUsers()
	{
		PersistenceQuery query = new PersistenceQuery("Users");

		PersistenceObject dataObject = PersistenceManager.get(query);

		List<Map<String, Object>> users = new ArrayList<>();

		for (Row row : dataObject.getRows("Users"))
		{
			int roleCode = ((Number) row.get("UserType")).intValue();

			UserRole role = UserRole.findByCode(roleCode);

			Map<String, Object> user = new LinkedHashMap<>();

			user.put("user_id", row.get("UserID"));

			user.put("name", row.get("Name"));

			user.put("email", row.get("Email"));

			user.put("phone", row.get("Phone"));

			user.put("role", role.getNodeRepresentation());

			user.put("role_formatted", role.getDisplayName());

			Object lastModifiedTime = row.get("LastModifiedTime");

			user.put("last_modified_time", lastModifiedTime);

			user.put("last_modified_time_formatted", DateUtil.formatEpochTime(lastModifiedTime));

			users.add(user);
		}

		return users;
	}
}