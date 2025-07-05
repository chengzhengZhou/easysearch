package com.ppwx.easysearch.core.data.element;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

public final class ColumnCast {

	public static Date string2Date(final StringColumn column)
			throws ParseException {
		return StringCast.asDate(column);
	}
	
	public static Date string2Date(final StringColumn column, String dateFormat)
			throws ParseException {
		return StringCast.asDate(column, dateFormat);
	}

	public static byte[] string2Bytes(final StringColumn column)
			throws UnsupportedEncodingException {
		return StringCast.asBytes(column);
	}

	public static String date2String(final DateColumn column) {
		return DateCast.asString(column);
	}

	public static String bytes2String(final BytesColumn column)
			throws UnsupportedEncodingException {
		return BytesCast.asString(column);
	}
}

class StringCast {
	static String datetimeFormat = "yyyy-MM-dd HH:mm:ss";

	static String dateFormat = "yyyy-MM-dd";

	static String timeFormat = "HH:mm:ss";

	static String timeZone = "GMT+8";

	static FastDateFormat dateFormatter;

	static FastDateFormat timeFormatter;

	static FastDateFormat datetimeFormatter;

	static TimeZone timeZoner;

	static String encoding = "UTF-8";

	static void init() {
		StringCast.datetimeFormat = System.getProperty(
				"common.column.datetimeFormat", StringCast.datetimeFormat);
		StringCast.dateFormat = System.getProperty(
				"common.column.dateFormat", StringCast.dateFormat);
		StringCast.timeFormat = System.getProperty(
				"common.column.timeFormat", StringCast.timeFormat);

		StringCast.timeZone = System.getProperty("common.column.timeZone",
				StringCast.timeZone);
		StringCast.timeZoner = TimeZone.getTimeZone(StringCast.timeZone);

		StringCast.datetimeFormatter = FastDateFormat.getInstance(
				StringCast.datetimeFormat, StringCast.timeZoner);
		StringCast.dateFormatter = FastDateFormat.getInstance(
				StringCast.dateFormat, StringCast.timeZoner);
		StringCast.timeFormatter = FastDateFormat.getInstance(
				StringCast.timeFormat, StringCast.timeZoner);

		StringCast.encoding = System.getProperty("common.column.encoding",
				StringCast.encoding);
	}

	static Date asDate(final StringColumn column) throws ParseException {
		if (null == column.asString()) {
			return null;
		}

		try {
			return StringCast.datetimeFormatter.parse(column.asString());
		} catch (ParseException ignored) {
		}

		try {
			return StringCast.dateFormatter.parse(column.asString());
		} catch (ParseException ignored) {
		}

		ParseException e;
		try {
			return StringCast.timeFormatter.parse(column.asString());
		} catch (ParseException ignored) {
			e = ignored;
		}
		throw e;
	}
	
	static Date asDate(final StringColumn column, String dateFormat) throws ParseException {
		ParseException e;
		try {
			return FastDateFormat.getInstance(dateFormat, StringCast.timeZoner).parse(column.asString());
		} catch (ParseException ignored) {
			e = ignored;
		}
		throw e;
	}

	static byte[] asBytes(final StringColumn column)
			throws UnsupportedEncodingException {
		if (null == column.asString()) {
			return null;
		}

		return column.asString().getBytes(StringCast.encoding);
	}
}

/**
 * 后续为了可维护性，可以考虑直接使用 apache 的DateFormatUtils.
 * 
 * 迟南已经修复了该问题，但是为了维护性，还是直接使用apache的内置函数
 */
class DateCast {

	static String datetimeFormat = "yyyy-MM-dd HH:mm:ss";

	static String dateFormat = "yyyy-MM-dd";

	static String timeFormat = "HH:mm:ss";

	static String timeZone = "GMT+8";

	static TimeZone timeZoner = TimeZone.getTimeZone(DateCast.timeZone);

	static void init() {
		DateCast.datetimeFormat = System.getProperty(
				"common.column.datetimeFormat", datetimeFormat);
		DateCast.timeFormat = System.getProperty(
				"common.column.timeFormat", timeFormat);
		DateCast.dateFormat = System.getProperty(
				"common.column.dateFormat", dateFormat);
		DateCast.timeZone = System.getProperty("common.column.timeZone",
				DateCast.timeZone);
		DateCast.timeZoner = TimeZone.getTimeZone(DateCast.timeZone);
		return;
	}

	static String asString(final DateColumn column) {
		if (null == column.asDate()) {
			return null;
		}

		switch (column.getSubType()) {
		case DATE:
			return DateFormatUtils.format(column.asDate(), DateCast.dateFormat,
					DateCast.timeZoner);
		case TIME:
			return DateFormatUtils.format(column.asDate(), DateCast.timeFormat,
					DateCast.timeZoner);
		case DATETIME:
			return DateFormatUtils.format(column.asDate(),
					DateCast.datetimeFormat, DateCast.timeZoner);
		default:
			throw new IllegalArgumentException("时间类型出现不支持类型，目前仅支持DATE/TIME/DATETIME。");
		}
	}
}

class BytesCast {
	static String encoding = "utf-8";

	static void init() {
		BytesCast.encoding = System.getProperty("common.column.encoding",
				BytesCast.encoding);
		return;
	}

	static String asString(final BytesColumn column)
			throws UnsupportedEncodingException {
		if (null == column.asBytes()) {
			return null;
		}

		return new String(column.asBytes(), encoding);
	}
}