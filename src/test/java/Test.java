import cn.yxffcode.easyanalyzer.analyzer.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.Attribute;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author gaohang on 15/11/19.
 */
public class Test {
  public static void main(String[] args) throws IOException {
    System.out.print("前缀词匹配:");
    token(PrefixWordFSTAnalyzer.create("test/", false));
    System.out.print("前缀词优先匹配:");
    token(PrefixWordFirstAnalyzer.create("test/", false));

    System.out.print("最长匹配:");
    token(CompleteFSTAnalyzer.create("test/", false));

    System.out.print("最短匹配:");
    token(ShortestFSTAnalyzer.create("test/", false));

    System.out.print("最多数量匹配:");
    token(MaxCountAnalyzer.create("test/", false));
  }

  private static void token(Analyzer analyzer) throws IOException {
    final TokenStream tokenStream = analyzer.tokenStream("test", "湖湖湖北");
    tokenStream.reset();
    while (tokenStream.incrementToken()) {
      final Iterator<Class<? extends Attribute>> iterator = tokenStream.getAttributeClassesIterator();
      while (iterator.hasNext()) {
        final Class<? extends Attribute> attr = iterator.next();
        System.out.print(tokenStream.getAttribute(attr));
        System.out.print(' ');
        break;
      }
    }
    System.out.println();
    tokenStream.close();
  }

}
