package cn.yxffcode.easyanalyzer.analyzer;

import cn.yxffcode.easyanalyzer.collection.IntStack;
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
 * @author gaohang on 15/11/18.
 */
public class PrefixWordFSTAnalyzer extends Analyzer {

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
    public static PrefixWordFSTAnalyzer create(String classpath,
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
    public static PrefixWordFSTAnalyzer create(String classpath,
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
    public static PrefixWordFSTAnalyzer create(@NotNull Iterable<File> dictionaries,
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
    public static PrefixWordFSTAnalyzer create(@NotNull SortedSet<String> sortedWords,
                                               boolean outputPrefix) throws IOException {
        return create(FSTFactory.create(sortedWords), outputPrefix);
    }

    /**
     * 通过FST创建分词器，如果需要在多个分词器之间共享FST或者复用已有的FST，可以使用此方法创建分词器
     */
    public static PrefixWordFSTAnalyzer create(FST<CharsRef> fst,
                                               boolean outputPrefix) {
        checkNotNull(fst);
        return new PrefixWordFSTAnalyzer(fst, outputPrefix);
    }

    private final FST<CharsRef> fst;
    private final boolean       outputPrefix;

    private PrefixWordFSTAnalyzer(FST<CharsRef> fst,
                                  boolean outputPrefix) {
        this.fst = fst;
        this.outputPrefix = outputPrefix;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        return new TokenStreamComponents(new FSTTokenizer(fst, outputPrefix));
    }

    static class FSTTokenizer extends BaseTokenizer {

        protected       IntStack              words;
        private         IntArrayStringBuilder appender;
        protected final boolean               outputPrefix;

        FSTTokenizer(FST<CharsRef> fst,
                     boolean outputPrefix) {
            super(fst);
            this.words = new IntStack();
            this.outputPrefix = outputPrefix;
        }

        @Override
        protected String nextWorld() throws IOException {
            while (words.isEmpty() && state != TokenState.FINISHED) {
                doToken();
            }
            if (words.isEmpty()) {
                return null;
            }
            return appender.toString(0, words.poll());
        }

        @Override
        protected void onUnmatched(IntArrayStringBuilder appender) {
            if (! words.isEmpty()) {
                pushBack(appender, words.peak());
                return;
            }
            if (outputPrefix) {
                onWordMatched(appender);
            }
        }

        protected void pushBack(final IntArrayStringBuilder appender,
                                final int begin) {
            for (int i = appender.length() - 1; i >= begin; -- i) {
                bufStack.push(appender.element(i));
            }
        }

        @Override
        protected void onMatchFinished(IntArrayStringBuilder appender) {
            if (words.isEmpty()) {
                onWordMatched(appender);
                return;
            }
            if (words.peak() != appender.length()) {
                onWordMatched(appender);
            }
        }

        @Override
        protected boolean onWordMatched(IntArrayStringBuilder appender) {
            words.push(appender.length());
            capture(appender);
            return true;
        }

        protected boolean remains() {
            return ! words.isEmpty();
        }

        protected int shortestWord() {
            return words.botton();
        }

        private void capture(IntArrayStringBuilder appender) {
            if (this.appender == null || this.appender != appender) {
                this.appender = appender;
            }
        }
    }

}
