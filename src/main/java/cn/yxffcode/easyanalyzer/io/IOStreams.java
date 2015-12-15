package cn.yxffcode.easyanalyzer.io;

import cn.yxffcode.easyanalyzer.collection.ImmutableIterator;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 输入输出流相关的工具
 *
 * @author gaohang on 15/10/23.
 */
public final class IOStreams {
  private IOStreams() {
  }

  /**
   * @see #lines(Reader)
   */
  public static Iterable<String> lines(@NotNull InputStream in) {
    return lines(new InputStreamReader(checkNotNull(in)));
  }

  /**
   * 按行读取
   * <p/>
   * 使用lazy的读，只有在返回的Iterable对象上迭代一次才会读一行
   * <p/>
   * 当读取的文件比较大时，不合适一次将事个文件的行读入内存，需要逐行读并处理。在使用guava的CharStreams时比较繁锁，例如：
   * <pre>
   *      CharStreams.readLine(reader, new LineProcessor<Void>() {
   *          public boolean processLine(String line) throws IOException {
   *              if (line ....) {
   *                  return false;//不需要再继续读
   *              }
   *              ...
   *          }
   *          public Void getResult() {
   *              return null;
   *          }
   *      });
   * </pre>
   * 换成此方法，代码如下：
   * <pre>
   *     for (String line : Streams.lines(reader)) {
   *         if (line...) {
   *             break;//停止读，因为是lazy的，当前行处理结束前，下一行不会被读入到内存
   *         }
   *         ...
   *     }
   * </pre>
   */
  public static Iterable<String> lines(@NotNull final Reader reader) {
    checkNotNull(reader);
    return new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return new ImmutableIterator<String>() {

          private String line;

          private BufferedReader in;

          @Override
          public boolean hasNext() {
            ensureInit();
            try {
              return (line = in.readLine()) != null;
            } catch (IOException e) {
              throw new IOReaderException(e);
            }
          }

          private void ensureInit() {
            if (in == null) {
              if (reader instanceof BufferedReader) {
                in = (BufferedReader) reader;
              } else {
                in = new BufferedReader(reader);
              }
            }
          }

          @Override
          public String next() {
            return line;
          }
        };
      }
    };
  }

}
