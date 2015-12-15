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
 * 基于lucene的{@link FST}实现的分词器，使用最大匹配。需要指定词典，
 * 同时支持通过代码指定或通过词典文件指定，可以自定义词典格式。
 * mmseg会把汉字，英文和数字分开，即使词典里有"宝马X5"这样的词，
 * mmseg也会把它拆分成三个词(分别是：宝马，X，5).
 * <p/>
 * FSTAnalyzer完全基于字典，暂不支持英文单词以空白字符拆分
 *
 * @author gaohang on 15/11/15.
 */
public class CompleteFSTAnalyzer extends Analyzer {

  private final FST<CharsRef> fst;
  private final boolean       outputPrefix;

  /**
   * 私有化构造器，使用create方法创建分词器对象
   *
   * @see #create(FST, boolean)
   * @see #create(Iterable, boolean)
   * @see #create(SortedSet, boolean)
   * @see #create(String, boolean)
   * @see #create(String, ClassLoader, boolean)
   */
  private CompleteFSTAnalyzer(FST<CharsRef> fst, boolean outputPrefix) {
    this.fst = fst;
    this.outputPrefix = outputPrefix;
  }

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
  public static CompleteFSTAnalyzer create(String classpath, boolean outputPrefix) throws IOException {
    return create(FSTFactory.create(classpath), outputPrefix);
  }

  /**
   * 通过FST创建分词器，如果需要在多个分词器之间共享FST或者复用已有的FST，可以使用此方法创建分词器
   */
  public static CompleteFSTAnalyzer create(FST<CharsRef> fst, boolean outputPrefix) {
    checkNotNull(fst);
    return new CompleteFSTAnalyzer(fst, outputPrefix);
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
  public static CompleteFSTAnalyzer create(String classpath, ClassLoader classLoader, boolean outputPrefix)
          throws IOException {
    return create(FSTFactory.create(classpath, classLoader), outputPrefix);
  }

  /**
   * 指定词典文件创建分词器
   *
   * @param dictionaries 词典文件列表
   * @param outputPrefix 如果输入不能完全匹配，只匹配了一部分，是否将匹配的一部分输出
   */
  public static CompleteFSTAnalyzer create(@NotNull Iterable<File> dictionaries, boolean outputPrefix)
          throws IOException {
    return create(FSTFactory.create(dictionaries), outputPrefix);
  }

  /**
   * 指定词条创建分词器
   *
   * @param sortedWords  所有词条，因为FST的创建过程中需要词条排好序，所以使用SortedSet,
   *                     使用字符串的默认排序，不要使用字符串的自定义排序
   * @param outputPrefix 如果输入不能完全匹配，只匹配了一部分，是否将匹配的一部分输出
   */
  public static CompleteFSTAnalyzer create(@NotNull SortedSet<String> sortedWords, boolean outputPrefix)
          throws IOException {
    return create(FSTFactory.create(sortedWords), outputPrefix);
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    return new TokenStreamComponents(new FSTTokenizer(fst, outputPrefix));
  }

  /**
   * 对输入进行分词，真正的分词实现，是有状态的
   */
  static class FSTTokenizer extends BaseTokenizer {

    private boolean               outputPrefix;
    private int                   lastMatched;
    private IntArrayStringBuilder currentAppender;

    FSTTokenizer(FST<CharsRef> fst, boolean outputPrefix) {
      super(fst);
      this.outputPrefix = outputPrefix;
    }

    @Override
    protected String nextWorld() throws IOException {
      if (state == TokenState.FINISHED) {
        return null;
      }
      if (currentAppender == null) {
        doToken();
      }

      if (lastMatched != 0) {
        String word = currentAppender.toString(0, lastMatched);
        lastMatched = 0;
        return word;
      }
      currentAppender = null;
      lastMatched = 0;
      return nextWorld();
    }

    @Override
    public void reset() throws IOException {
      super.reset();
      lastMatched = 0;
      currentAppender = null;
    }

    @Override
    protected void onMatchFinished(IntArrayStringBuilder appender) {
      capture(appender);
    }

    @Override
    protected boolean onWordMatched(IntArrayStringBuilder appender) {
      capture(appender);
      return true;
    }

    @Override
    protected void onUnmatched(IntArrayStringBuilder appender) {
      //如果有最近匹配,则将不能完全匹配的字符压回
      if (lastMatched > 0) {
        for (int i = appender.length() - 1; i >= lastMatched; -- i) {
          bufStack.push(appender.element(i));
        }
        return;
      }
      if (outputPrefix) {
        capture(appender);
      }
    }

    private void capture(IntArrayStringBuilder appender) {
      lastMatched = appender.length();
      if (currentAppender == null || currentAppender != appender) {
        currentAppender = appender;
      }
    }
  }
}
