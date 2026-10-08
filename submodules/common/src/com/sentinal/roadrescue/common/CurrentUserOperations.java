package com.sentinal.roadrescue.common;

import java.util.Map;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface CurrentUserOperations
{
	Map<String, Object> getCurrentUser();
}
