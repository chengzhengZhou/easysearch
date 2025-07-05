package com.ppwx.easysearch.core.function.normalize;

import com.ppwx.easysearch.core.data.Column;
import com.ppwx.easysearch.core.function.NumberScoreFunction;

/**
 * 归一化，根据不同的算分将数值归一化至[0,1]
 * 场景概述相关性计算过程中，一篇doc的好坏需要从不同的维度衡量。而各个维度的分数值域可能不同，
 * 比如网页点击数可能是成百上千万，网页的文本相关性分数在[0, 1]之间，它们之间没有可比性。
 * 为了在公式中使用这些元素，需要将不同的分数归一化至同一个值域区间，而normalize为这种归一化提供了一种简便的方法。
 * normlize支持三种归一化方法：线性函数转化、对数函数转化、反正切函数转化。根据传入参数的不同，normalize自动选择不同的归一化方法。
 * 1.如果只指定value参数，normalize使用反正切函数转化，
 * 2.如果指定了value和max参数，normalize使用对数函数转化，
 * 3.如果指定了value、max和min，normalize使用线性函数转化
 *
 * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
 * @date 2023/10/13 13:59
 * @since 1.0.0
 */
public class FiniteNormalizeFunc implements NumberScoreFunction<Double> {

    private Number max;

    private Number min;

    /**
     * 有限域归一化
     *
     * @param max value的最大值，可选，支持double类型的浮点数
     * @param min value的最小值，可选，支持double类型的浮点数
     * @return
     */
    public FiniteNormalizeFunc(Number max, Number min) {
        this.max = max;
        this.min = min;
    }

    /**
     * 需要做归一化的值，支持double类型的浮点数，该值可以来自文档中的字段或者其他表达式
     * 适用场景1：对price字段做归一化，但是不知道price的值域，可以使用如下公式进行归一化normalize(price)
     * 场景2：对price字段做归一化，但是只知道price的最大值为100，可以使用如下公式进行归一化normalize(price, 100)
     * 场景3：对price字段做归一化，并且知道price的最大值为100，最小值为1，可以使用如下公式进行归一化normalize(price, 100, 1)
     * 注意事项：
     * 使用反正切函数进行归一化时，如果value小于0，归一化后的值为0
     * 使用对数函数进行归一化时，样本及max的值要大于1
     * 使用线性函数进行归一化时，max要大于min
     *
     * @author Mikey(ext.ahs.zhouchzh1 @ jd.com)
     * @param value 空值时被默认为0值
     * @date 2023/10/13 14:08
     * @return double
     */
    @Override
    public double score(Number value) {
        double normalizedValue = (value == null ? 0.0 : value.doubleValue());
        double maxValue;
        double minValue;
        double result;

        if (max != null && min != null) {
            minValue = min.doubleValue();
            maxValue = max.doubleValue();
            result = (normalizedValue - minValue) / (maxValue - minValue);
        } else if (max != null) {
            maxValue = max.intValue() > 1 ? max.doubleValue() : 10;
            result = Math.log10(normalizedValue) / Math.log10(maxValue);
        } else {
            result = Math.atan(normalizedValue) / (Math.PI / 2);
        }
        return Double.isNaN(result) ? 0.0 : Math.max(result, 0.0);
    }

    @Override
    public Double apply(Column column) {
        return score(column.asDouble());
    }

}
