package com.fpoly.java5demo.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Utils {
	private static final Pattern SIZE_PATTERN = Pattern.compile("^([\\d.]+)\\s*([a-zA-Z]+)$");
	public static final String COOKIE_KEY_USER_ID = "user_id";
	public static final String COOKIE_KEY_ROLE = "role";
	public static final String COOKIE_KEY_AUTH_TOKEN = "remember_token";
	private static final String SECRET_SALT = "PolyShop_Enterprise_Secret_Salt_2026";

	public static long parseToBytes(String input) {
		if (input == null || input.trim().isEmpty()) {
			return 0;
		}

		String normalizedInput = input.trim().toUpperCase();

		Matcher matcher = SIZE_PATTERN.matcher(normalizedInput);

		if (!matcher.find()) {
			throw new IllegalArgumentException(String.format("Định dạng kích thước không hợp lệ: %s", input));
		}
		double value = Double.parseDouble(matcher.group(1));
		String unit = matcher.group(2);

		long multiplier = 1;

		switch (unit) {
		case "KB":
		case "K":
			multiplier = 1024L;
			break;
		case "MB":
		case "M":
			multiplier = 1024L * 1024;
			break;
		case "GB":
		case "G":
			multiplier = 1024L * 1024 * 1024;
			break;
		case "TB":
		case "T":
			multiplier = 1024L * 1024 * 1024 * 1024;
			break;
		case "B":
		case "BYTE":
		case "BYTES":
			multiplier = 1;
			break;
		default:
			throw new IllegalArgumentException(String.format("Đơn vị không được hỗ trợ: %s", unit));
		}

		return (long) (value * multiplier);
	}

	public static String signAuthToken(int userId, int role, String password) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
			String data = String.format("%d:%d:%s:%s", userId, role, (password != null ? password : ""), SECRET_SALT);
			byte[] hash = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (Exception e) {
			return "";
		}
	}

	public static String getCookieValue(String key, HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(key)) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public static void clearCookie(HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				Cookie deleteCookie = new Cookie(cookie.getName(), "");
				deleteCookie.setMaxAge(0);
				deleteCookie.setPath("/");
				deleteCookie.setHttpOnly(true);
				response.addCookie(deleteCookie);
			}
		}
		// Đảm bảo xóa triệt để các cookie phiên và token
		Cookie deleteUser = new Cookie(COOKIE_KEY_USER_ID, "");
		deleteUser.setMaxAge(0);
		deleteUser.setPath("/");
		deleteUser.setHttpOnly(true);
		response.addCookie(deleteUser);

		Cookie deleteRole = new Cookie(COOKIE_KEY_ROLE, "");
		deleteRole.setMaxAge(0);
		deleteRole.setPath("/");
		deleteRole.setHttpOnly(true);
		response.addCookie(deleteRole);

		Cookie deleteToken = new Cookie(COOKIE_KEY_AUTH_TOKEN, "");
		deleteToken.setMaxAge(0);
		deleteToken.setPath("/");
		deleteToken.setHttpOnly(true);
		response.addCookie(deleteToken);
	}

	public static void setCookie(String key, String value, HttpServletResponse response) {
		int day = 60 * 60 * 24 * 7;

		Cookie cookie = new Cookie(key, value);
		cookie.setMaxAge(day);
		cookie.setPath("/");
		cookie.setHttpOnly(true); // Ngăn chặn tấn công XSS đánh cắp Cookie
		response.addCookie(cookie);
	}
}
