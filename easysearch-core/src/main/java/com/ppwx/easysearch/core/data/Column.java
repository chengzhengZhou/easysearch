package com.ppwx.easysearch.core.data;

import com.alibaba.fastjson.JSON;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 *
 * 字段
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/12/07 10:16
 * @since 1.0.0
 */
public abstract class Column {

	private Type type;

	private Object rawData;

	private int byteSize;

	public Column(final Object object, final Type type, int byteSize) {
		this.rawData = object;
		this.type = type;
		this.byteSize = byteSize;
	}

	public Object getRawData() {
		return this.rawData;
	}

	public Type getType() {
		return this.type;
	}

	public int getByteSize() {
		return this.byteSize;
	}

	protected void setType(Type type) {
		this.type = type;
	}

	protected void setRawData(Object rawData) {
		this.rawData = rawData;
	}

	protected void setByteSize(int byteSize) {
		this.byteSize = byteSize;
	}

	public abstract Long asLong();

	public abstract Double asDouble();

	public abstract String asString();

	public abstract Date asDate();

	public abstract Date asDate(String dateFormat);

	public abstract byte[] asBytes();

	public abstract Boolean asBoolean();

	public abstract BigDecimal asBigDecimal();

	public abstract BigInteger asBigInteger();

	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}

	public enum Type {
		DYNAMIC, BAD, NULL, INT, LONG, DOUBLE, STRING, BOOL, DATE, BYTES
	}
}
