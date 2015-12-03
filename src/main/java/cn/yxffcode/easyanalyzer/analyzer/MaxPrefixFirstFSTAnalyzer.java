package cn.yxffcode.easyanalyzer.analyzer;

import org.apache.lucene.analysis.Analyzer;

/**
 * 最长前缀优先
 *
 * @author gaohang on 15/12/3.
 */
public class MaxPrefixFirstFSTAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        return null;
    }
}
