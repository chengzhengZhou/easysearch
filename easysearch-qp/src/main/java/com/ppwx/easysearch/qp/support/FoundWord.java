package com.ppwx.easysearch.qp.support;

import cn.hutool.core.lang.DefaultSegment;

import java.util.Objects;

/**
 * <p>
 * 匹配到的单词，包含单词，text中匹配单词的内容，以及匹配内容在text中的下标，
 * 下标可以用来做单词的进一步处理，如果替换成**
 */
public class FoundWord extends DefaultSegment<Integer> {
	/**
	 * 关键词
	 */
	private final String keyWord;
	/**
	 * 单词匹配到的内容，即文中的单词
	 */
	private final String foundWord;

	/**
	 * 构造
	 *
	 * @param keyWord 关键词
	 * @param foundWord 单词匹配到的内容，即文中的单词
	 * @param startIndex 起始位置（包含）
	 * @param endIndex 结束位置（包含）
	 */
	public FoundWord(String keyWord, String foundWord, int startIndex, int endIndex) {
		super(startIndex, endIndex);
		this.keyWord = keyWord;
		this.foundWord = foundWord;
	}

	/**
	 * 获取单词匹配到的内容，即文中的单词
	 * @return 单词匹配到的内容
	 */
	public String getFoundWord() {
		return foundWord;
	}

	public String getKeyWord() {
		return keyWord;
	}

	/**
	 * 默认的，只输出匹配到的关键字
	 * @return 匹配到的关键字
	 */
	@Override
	public String toString() {
		return this.foundWord;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FoundWord foundWord1 = (FoundWord) o;
		return Objects.equals(foundWord, foundWord1.foundWord);
	}

	@Override
	public int hashCode() {
		return Objects.hash(foundWord);
	}
}
