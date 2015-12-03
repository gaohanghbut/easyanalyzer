package cn.yxffcode.easyanalyzer.analyzer;

import cn.yxffcode.easyanalyzer.lang.IntArrayStringBuilder;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.FST;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.SortedSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 最多数量匹配
 *
 * @author gaohang on 15/12/3.
 */
public class MaxCountAnalyzer extends Analyzer {

    /**
     * 从指定的classpath路径下读取词典，使用{@link Thread#getContextClassLoader()}
     * 加载类路径下的词典文件.词典文件名需要以.dic结尾，词典文件中一行为一个词条
     *
     * @param classpath    词典文件的类路径，支持目录
     * @param outputPrefix 如果输入不能完全匹配，只匹配了一部分，是否将匹配的一部分输出
     *
     * @return 基于FST的分词器
     *
     * @throws IOException 读取字典或创建FST出错
     * @see #create(String, ClassLoader, boolean)
     */
    public static MaxCountAnalyzer create(String classpath,
                                          boolean outputPrefix) throws IOException {
        return create(FSTFactory.create(classpath), outputPrefix);
    }

    /**
     * 从指定的classpath路径下读取词典， 词典文件名需要以.dic结尾，词典文件中一行为一个词条
     *
     * @param classpath    词典文件的类路径，支持目录
     * @param classLoader  用于加载词典文件的类加载器
     * @param outputPrefix 如果输入不能完全匹配，只匹配了一部分，是否将匹配的一部分输出
     *
     * @return 基于FST的分词器
     *
     * @throws IOException 读取字典或创建FST出错
     */
    public static MaxCountAnalyzer create(String classpath,
                                          ClassLoader classLoader,
                                          boolean outputPrefix) throws IOException {
        return create(FSTFactory.create(classpath, classLoader), outputPrefix);
    }

    /**
     * 指定词典文件创建分词器
     *
     * @param dictionaries 词典文件列表
     * @param outputPrefix 如果输入不能完全匹配，只匹配了一部分，是否将匹配的一部分输出
     */
    public static MaxCountAnalyzer create(@NotNull Iterable<File> dictionaries,
                                          boolean outputPrefix) throws IOException {
        return create(FSTFactory.create(dictionaries), outputPrefix);
    }

    /**
     * 指定词条创建分词器
     *
     * @param sortedWords  所有词条，因为FST的创建过程中需要词条排好序，所以使用SortedSet,
     *                     使用字符串的默认排序，不要使用字符串的自定义排序
     * @param outputPrefix 如果输入不能完全匹配，只匹配了一部分，是否将匹配的一部分输出
     */
    public static MaxCountAnalyzer create(@NotNull SortedSet<String> sortedWords,
                                          boolean outputPrefix) throws IOException {
        return create(FSTFactory.create(sortedWords), outputPrefix);
    }

    /**
     * 通过FST创建分词器，如果需要在多个分词器之间共享FST或者复用已有的FST，可以使用此方法创建分词器
     */
    public static MaxCountAnalyzer create(FST<CharsRef> fst,
                                          boolean outputPrefix) {
        checkNotNull(fst);
        return new MaxCountAnalyzer(fst, outputPrefix);
    }

    private final FST<CharsRef> fst;
    private final boolean       outputPrefix;

    private MaxCountAnalyzer(FST<CharsRef> fst,
                             boolean outputPrefix) {
        this.fst = fst;
        this.outputPrefix = outputPrefix;
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        return new TokenStreamComponents(new FSTTokenizer(fst, outputPrefix));
    }

    static final class FSTTokenizer extends PrefixWordFSTAnalyzer.FSTTokenizer {

        private IntArrayStringBuilder lastPushBack;

        FSTTokenizer(final FST<CharsRef> fst,
                     final boolean outputPrefix) {
            super(fst, outputPrefix);
        }

        @Override
        protected void onMatchFinished(IntArrayStringBuilder appender) {
            if (remains()) {
                int s = shortestWord();
                if (s != appender.length()) {//如果栈里的元素和当前元素不相同才压回
                    pushBack(appender, s);
                    lastPushBack = appender.slice(s, appender.length() - s);
                }
            } else {
                if (state == TokenState.FINISHED) {
                    lastPushBack = null;
                    return;
                }
                if (lastPushBack != null && ! lastPushBack.isEmpty() && lastPushBack.startWith(appender)) {
                    lastPushBack = lastPushBack.slice(appender.length());
                    return;
                }
                //压回所有
                pushBack(appender, 0);
                lastPushBack = appender;
            }
            super.onMatchFinished(appender);
        }

    }
}
