package cn.yxffcode.easyanalyzer.analyzer;

import cn.yxffcode.easyanalyzer.collection.IntStack;
import cn.yxffcode.easyanalyzer.lang.IntArrayStringBuilder;
import com.google.common.base.Strings;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.FST;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

/**
 * 对输入进行分词，真正的分词实现，是有状态的
 *
 * @author gaohang on 15/11/18.
 */
abstract class BaseTokenizer extends Tokenizer {

  /**
   * FST中的字节与此int做&，对于UTF-8编码，很多字符不只一个字节，
   * FST在存储字节里将多字节字符中每个字节做了一个按位与，因此在匹
   * 配的时候需要将输入的字节转换成FST中的格式
   */
  private static final int FST_LABEL_FLAG = 0xFF;
  /**
   * 读到了{@link #reader}的最后
   */
  private static final int EOF = -1;
  /**
   * 中文字符的起始数字
   */
  private static final int CN_CHAR_FIRST = 19968;
  /**
   * 中文字符的最大字符数字
   */
  private static final int CN_CHAR_LAST = 171941;

  /**
   * 底层字典的存储，用于分词时将输入与之做匹配
   */
  private final FST<CharsRef> fst;
  /**
   * 分词后的词条结果
   */
  private final CharTermAttribute termAtt;
  private final OffsetAttribute offsetAtt;
  private final TypeAttribute typeAtt;
  /**
   * 用于临时存放没有完全匹配的字符，{@link PushbackReader}默认只支持一个字符的pushback，
   * 如果指定pushback的buffer大小，则每一次{@link #reset()}的调用都需要重新创建buffer。
   */
  protected IntStack bufStack;
  protected TokenState state;
  private Reader reader;

  protected BaseTokenizer(FST<CharsRef> fst) {
    this.fst = fst;
    this.termAtt = addAttribute(CharTermAttribute.class);
    this.offsetAtt = addAttribute(OffsetAttribute.class);
    this.typeAtt = addAttribute(TypeAttribute.class);
    this.bufStack = new IntStack(50);//最多50个字符，够用了
  }

  @Override
  public boolean incrementToken() throws IOException {
    clearAttributes();
    String word = nextWorld();
    if (Strings.isNullOrEmpty(word)) {
      return false;
    }
    char[] buffer = word.toCharArray();
    termAtt.copyBuffer(buffer, 0, buffer.length);
    offsetAtt.setOffset(0, buffer.length);
    typeAtt.setType(TypeAttribute.DEFAULT_TYPE);
    return true;
  }

  protected abstract String nextWorld() throws IOException;

  @Override
  public void reset() throws IOException {
    super.reset();
    reader = super.input;
    state = TokenState.ING;
  }

  /**
   * 获取下一个词条
   */
  protected void doToken() throws IOException {

    final FST.BytesReader fstReader = fst.getBytesReader();

    //用于判断最后一次匹配结束后，是否完全匹配了一个词
    FST.Arc<CharsRef> follow = fst.getFirstArc(new FST.Arc<CharsRef>());

    //存储已匹配的输入，最终形成输出
    IntArrayStringBuilder appender = new IntArrayStringBuilder();
    /*
     * 如果第一个（从最起始位置开始连续的几个）字符没有匹配上，
     * 则说明没匹配上的字符不会是词典中的词，直接丢弃，不需要pushback到reader中
     */
    boolean first = true;
    int read;
    /*
     * 比如词典中有“宝马”和“宝马X6”，输入是”宝马X“，那么应该能识别出”宝马“这个词，
     * lastMatchedWord用来存储最近一次完全匹配，如果最终不能匹配，则返回最近一次完全匹配的词
     */
    outer:
    while ((read = readNextChar()) != -1) {
      //忽略换行符
      if (isLineDelimiter(read)) {
        continue;
      }
      /*
       * UTF-8中，字符可能不是单字节（有些汉字是3字节，有些汉字是4字节）.
       * 需要将读取到的int按照字符的编码顺序转换成字节数组
       */
      byte[] input = Character.toString((char) Character.toLowerCase(read)).getBytes();

      //需要一次读取的int表示的字符中的所有字节都能匹配上，才认为成功匹配了一个字符
      for (byte b : input) {
        final FST.Arc<CharsRef> current = new FST.Arc<>();
        if (fst.findTargetArc(b & FST_LABEL_FLAG, follow, current, fstReader) == null) {
          /*
           * 最近一次匹配失败，词条匹配结束，将最近一次读取压回输入流，
           * 如果是空白字符，则不需要压回
           */
          if (!first && !Character.isWhitespace(read)) {
            bufStack.push(read);
          } else if (isEnglishChar(read) || Character.isDigit(read)) {
            appender.append(read);
          } else if (isChineseCharacter(read)) {
            appender.append(read);
            onMatchFinished(appender);
            return;
          }
          break outer;
        }
        follow = current;
      }
      //存储将匹配成功的字符
      appender.append(read);
      first = false;
      //已经匹配成功了一个词条，匹配还没完成（可能不是最大匹配），存储最近匹配成功的词条
      if (follow.isFinal() && !appender.isBlank() && !onWordMatched(appender)) {
        return;
      }
    }
    if (appender.isBlank()) {
      checkState(read);
      return;
    }
    //如果能最大匹配，则返回最大匹配结果
    if (follow.isFinal()) {
      onMatchFinished(appender);
    } else {
      //check english words
      if (isEnglishWord(appender)) {
        while ((read = readNextChar()) != -1) {
          if (isEnglishChar(read)) {
            appender.append(read);
          } else {
            bufStack.push(read);
            break;
          }
        }
        onMatchFinished(appender);

      } else if (isDigitWord(appender)) {
        //check digits
        while ((read = readNextChar()) != -1) {
          if (Character.isDigit(read)) {
            appender.append(read);
          } else {
            bufStack.push(read);
            break;
          }
        }
        onMatchFinished(appender);
      } else {
        onUnmatched(appender);
      }
    }

    checkState(read);
  }

  private int readNextChar() throws IOException {
    return bufStack.isEmpty() ? reader.read() : bufStack.poll();
  }

  private boolean isLineDelimiter(int read) {
    return read == '\n' || read == '\r';
  }

  private boolean isEnglishChar(int c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  /**
   * 判断字符是不是中文字符
   */
  private boolean isChineseCharacter(int ch) {
    return ch >= CN_CHAR_FIRST && ch <= CN_CHAR_LAST;
  }

  protected abstract void onMatchFinished(IntArrayStringBuilder appender);

  protected abstract boolean onWordMatched(IntArrayStringBuilder appender);

  private void checkState(int read) {
    if (bufStack.isEmpty() && read == EOF) {
      state = TokenState.FINISHED;
    }
  }

  private boolean isEnglishWord(IntArrayStringBuilder appender) {
    for (int i = 0, j = appender.length(); i < j; i++) {
      int element = appender.element(i);
      if (!isEnglishChar(element)) {
        return false;
      }
    }
    return true;
  }

  private boolean isDigitWord(IntArrayStringBuilder appender) {
    for (int i = 0, j = appender.length(); i < j; i++) {
      int element = appender.element(i);
      if (!Character.isDigit(element)) {
        return false;
      }
    }
    return true;
  }

  protected abstract void onUnmatched(IntArrayStringBuilder appender);

  protected enum TokenState {
    FINISHED,
    ING
  }

}
