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
 * 最短匹配分词
 *
 * @author gaohang on 15/11/18.
 */
public class ShortestFSTAnalyzer extends Analyzer {

  private final FST<CharsRef> fst;
  private final boolean outputPrefix;

  private ShortestFSTAnalyzer(FST<CharsRef> fst, boolean outputPrefix) {
    this.fst = fst;
    this.outputPrefix = outputPrefix;
  }

  /**
   * 从指定的classpath路径下读取词典，使用{@link Thread#getContextClassLoader()}
   * 加载类路径下的词典文件.词典文件名需要以.dic结尾，词典文件中一行为一个词条
   *
   * @param classpath    词典文件的类路径，支持目录
   * @param outputPrefix 如果输入不能完全匹配，只匹配了一部分，是否将匹配的一部分输出
   * @return 基于FST的分词器
   * @throws IOException 读取字典或创建FST出错
   * @see #create(String, ClassLoader, boolean)
   */
  public static ShortestFSTAnalyzer create(String classpath, boolean outputPrefix) throws IOException {
    return create(FSTFactory.create(classpath), outputPrefix);
  }

  /**
   * 通过FST创建分词器，如果需要在多个分词器之间共享FST或者复用已有的FST，可以使用此方法创建分词器
   */
  public static ShortestFSTAnalyzer create(FST<CharsRef> fst, boolean outputPrefix) {
    checkNotNull(fst);
    return new ShortestFSTAnalyzer(fst, outputPrefix);
  }

  /**
   * 从指定的classpath路径下读取词典， 词典文件名需要以.dic结尾，词典文件中一行为一个词条
   *
   * @param classpath    词典文件的类路径，支持目录
   * @param classLoader  用于加载词典文件的类加载器
   * @param outputPrefix 如果输入不能完全匹配，只匹配了一部分，是否将匹配的一部分输出
   * @return 基于FST的分词器
   * @throws IOException 读取字典或创建FST出错
   */
  public static ShortestFSTAnalyzer create(String classpath,
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
  public static ShortestFSTAnalyzer create(@NotNull Iterable<File> dictionaries,
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
  public static ShortestFSTAnalyzer create(@NotNull SortedSet<String> sortedWords,
                                           boolean outputPrefix) throws IOException {
    return create(FSTFactory.create(sortedWords), outputPrefix);
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    return new TokenStreamComponents(new FSTTokenizer(fst, outputPrefix));
  }

  static final class FSTTokenizer extends BaseTokenizer {

    private final boolean outputPrefix;
    private int word;
    private IntArrayStringBuilder appender;

    FSTTokenizer(FST<CharsRef> fst, boolean outputPrefix) {
      super(fst);
      this.outputPrefix = outputPrefix;
    }

    @Override
    protected String nextWorld() throws IOException {
      while (word == 0 && state != TokenState.FINISHED) {
        doToken();
      }
      if (word == 0) {
        return null;
      }
      String s = appender.toString(0, word);
      word = 0;
      return s;
    }

    @Override
    protected void onMatchFinished(IntArrayStringBuilder appender) {
      if (word != appender.length()) {
        onWordMatched(appender);
      }
    }

    @Override
    protected boolean onWordMatched(IntArrayStringBuilder appender) {
      word = appender.length();
      capture(appender);
      return false;
    }

    @Override
    protected void onUnmatched(IntArrayStringBuilder appender) {
      if (word > 0) {
        for (int i = appender.length() - 1; i >= word; --i) {
          bufStack.push(appender.element(i));
        }
        return;
      } else {
        for (int i = appender.length() - 1; i >= 1; --i) {
          bufStack.push(appender.element(i));
        }
      }
      if (outputPrefix) {
        onWordMatched(appender);
      }
    }

    private void capture(IntArrayStringBuilder appender) {
      if (this.appender == null || this.appender != appender) {
        this.appender = appender;
      }
    }
  }

}
