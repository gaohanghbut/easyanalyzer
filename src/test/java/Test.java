import cn.yxffcode.easyanalyzer.analyzer.CompleteFSTAnalyzer;
import cn.yxffcode.easyanalyzer.analyzer.PrefixWordFSTAnalyzer;
import cn.yxffcode.easyanalyzer.analyzer.ShortestFSTAnalyzer;
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
        token(PrefixWordFSTAnalyzer.create("test/", true));

        System.out.println("========");
        token(CompleteFSTAnalyzer.create("test/", true));


        System.out.println("========");
        token(ShortestFSTAnalyzer.create("test/", true));

    }

    private static void token(Analyzer analyzer) throws IOException {
//        final TokenStream tokenStream = analyzer.tokenStream("test", "X6benz");
        final TokenStream tokenStream = analyzer.tokenStream("test", "中国人民厘米库放心可靠二手车");
//        final TokenStream tokenStream = analyzer.tokenStream("test", "benz bmw ok 奔\n驰porsche\r宝马X5宝benz宝马X60987中国人");
//        final TokenStream tokenStream = analyzer.tokenStream("test", "benz宝马X6");
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
