package com.github.thbrown.softballsim.util;

/**
 * Using custom stringUtils to avoid using Apache commons to keep jar size down.
 */
public class StringUtils {
	public static String trim(final String str) {
		return str == null ? null : str.trim();
	}
}
