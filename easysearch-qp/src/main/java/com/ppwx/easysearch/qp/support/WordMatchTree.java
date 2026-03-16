package com.ppwx.easysearch.qp.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.StrUtil;

import java.util.*;

/**
 * DFA（Deterministic Finite Automaton 确定有穷自动机）
 * DFA单词树（以下简称单词树），常用于在某大段文字中快速查找某几个关键词是否存在。<br>
 * 单词树使用group区分不同的关键字集合，不同的分组可以共享树枝，避免重复建树。<br>
 * 单词树使用树状结构表示一组单词。<br>
 * 例如：红领巾，红河构建树后为：<br>
 * 红                    <br>
 * /      \                 <br>
 * 领         河             <br>
 * /                            <br>
 * 巾                            <br>
 * 其中每个节点都是一个WordTree对象，查找时从上向下查找。<br>
 *
 * @author Looly
 */
public class WordMatchTree extends HashMap<Character, WordMatchTree> {
	private static final long serialVersionUID = -4646423269465809276L;

	/**
	 * 单词字符末尾标识，用于标识单词末尾字符
	 */
	private final Set<Character> endCharacterSet = new HashSet<>();
	/**
	 * 字符过滤规则，通过定义字符串过滤规则，过滤不需要的字符，当accept为false时，此字符不参与匹配
	 */
	private Filter<Character> charFilter = CustomStopChar::isNotStopChar;

	//--------------------------------------------------------------------------------------- Constructor start

	/**
	 * 默认构造
	 */
	public WordMatchTree() {
	}
	//--------------------------------------------------------------------------------------- Constructor start

	/**
	 * 设置字符过滤规则，通过定义字符串过滤规则，过滤不需要的字符<br>
	 * 当accept为false时，此字符不参与匹配
	 *
	 * @param charFilter 过滤函数
	 * @return this
	 * @since 5.2.0
	 */
	public WordMatchTree setCharFilter(Filter<Character> charFilter) {
		this.charFilter = charFilter;
		return this;
	}

	//------------------------------------------------------------------------------- add word

	/**
	 * 增加一组单词
	 *
	 * @param words 单词集合
	 * @return this
	 */
	public WordMatchTree addWords(Collection<String> words) {
		if (false == (words instanceof Set)) {
			words = new HashSet<>(words);
		}
		for (String word : words) {
			addWord(word);
		}
		return this;
	}

	/**
	 * 增加一组单词
	 *
	 * @param words 单词数组
	 *              @return this
	 */
	public WordMatchTree addWords(String... words) {
		for (String word : CollUtil.newHashSet(words)) {
			addWord(word);
		}
		return this;
	}

	/**
	 * 添加单词，使用默认类型
	 *
	 * @param word 单词
	 * @return this
	 */
	public WordMatchTree addWord(String word) {
		final Filter<Character> charFilter = this.charFilter;
		WordMatchTree parent = null;
		WordMatchTree current = this;
		WordMatchTree child;
		char currentChar = 0;
		final int length = word.length();
		for (int i = 0; i < length; i++) {
			currentChar = word.charAt(i);
			if (charFilter.accept(currentChar)) {//只处理合法字符
				child = current.get(currentChar);
				if (child == null) {
					//无子类，新建一个子节点后存放下一个字符
					child = new WordMatchTree();
					current.put(currentChar, child);
				}
				parent = current;
				current = child;
			}
		}
		if (null != parent) {
			parent.setEnd(currentChar);
		}
		return this;
	}

	//------------------------------------------------------------------------------- match all


	/**
	 * 找出所有匹配的关键字
	 *
	 * @param text  被检查的文本
	 * @return 匹配的词列表
	 */
	public List<String> matchAll(String text) {
		final List<FoundWord> matchAllWords = matchAllWords(text);
		return CollUtil.map(matchAllWords, FoundWord::toString, true);
	}

	/**
	 * 找出所有匹配的关键字<br>
	 * 贪婪匹配（最长匹配）原则：匹配符合的最长序列
	 *
	 * @param text           被检查的文本
	 * @return 匹配的词列表
	 * @since 5.5.3
	 */
	public List<FoundWord> matchAllWords(String text) {
		if (null == text) {
			return null;
		}

		List<FoundWord> foundWords = new ArrayList<>();
		WordMatchTree current = this;
		final int length = text.length();
		final Filter<Character> charFilter = this.charFilter;
		//存放查找到的字符缓存。完整出现一个词时加到findedWords中，否则清空
		final StringBuilder wordBuffer = StrUtil.builder();
		final StringBuilder keyBuffer = StrUtil.builder();
		char currentChar;
		for (int i = 0; i < length; i++) {
			wordBuffer.setLength(0);
			keyBuffer.setLength(0);
			int foundIdx = -1;
			for (int j = i; j < length; j++) {
				currentChar = text.charAt(j);
				if (false == charFilter.accept(currentChar)) {
					if (wordBuffer.length() > 0) {
						//做为关键词中间的停顿词被当作关键词的一部分被返回
						wordBuffer.append(currentChar);
					} else {
						//停顿词做为关键词的第一个字符时需要跳过
						i++;
					}
					continue;
				} else if (false == current.containsKey(currentChar)) {
					//非关键字符被整体略过，重新以下个字符开始检查
					break;
				}
				wordBuffer.append(currentChar);
				keyBuffer.append(currentChar);
				if (current.isEnd(currentChar)) {
					//到达单词末尾，关键词成立，从此词的下一个位置开始查找
					foundIdx = j;
					// 整词跟的是停顿词的话就停止
					if (j + 1 < length && !charFilter.accept(text.charAt(j + 1))) {
						break;
					}
					//foundWords.add(new FoundWord(keyBuffer.toString(), wordBuffer.toString(), i, j));

					//如果非密度匹配，跳过匹配到的词
					//i = j;
				}
				current = current.get(currentChar);
				if (null == current) {
					break;
				}
			}
			if (foundIdx > 0) {
				if (wordBuffer.length() > (foundIdx - i + 1)) {
					foundWords.add(new FoundWord(keyBuffer.toString(),
							wordBuffer.substring(0, (foundIdx - i + 1)), i, foundIdx));
				} else {
					foundWords.add(new FoundWord(keyBuffer.toString(), wordBuffer.toString(), i, foundIdx));
				}

				// jump
				i = foundIdx;
			}
			current = this;
		}
		return foundWords;
	}
	//--------------------------------------------------------------------------------------- Private method start

	/**
	 * 是否末尾
	 *
	 * @param c 检查的字符
	 * @return 是否末尾
	 */
	private boolean isEnd(Character c) {
		return this.endCharacterSet.contains(c);
	}

	/**
	 * 设置是否到达末尾
	 *
	 * @param c 设置结尾的字符
	 */
	private void setEnd(Character c) {
		if (null != c) {
			this.endCharacterSet.add(c);
		}
	}

	/**
	 * 清除所有的词,
	 * 此方法调用后, wordTree 将被清空
	 * endCharacterSet 也将清空
	 */
	@Override
	public void clear() {
		super.clear();
		this.endCharacterSet.clear();
	}
	//--------------------------------------------------------------------------------------- Private method end
}
