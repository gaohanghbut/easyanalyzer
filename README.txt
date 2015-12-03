基于Lucene中的有限状态机(见org.apache.lucene.util.fst.FST)实现的基于字典的中文分词器

词典:
    湖北
    工业
    大学
    湖北工业
    工业大学
    湖北工业大学
    学生
    大学生
    工业大学生
    奥迪Q5
    奥迪

测试代码:

/**
 * @author gaohang on 15/11/19.
 */
public class Test {
    public static void main(String[] args) throws IOException {
        System.out.print("前缀词匹配:");
        token(PrefixWordFSTAnalyzer.create("test/", true));
        System.out.print("前缀词优先匹配:");
        token(PrefixFirstAnalyzer.create("test/", true));

        System.out.print("最长匹配:");
        token(CompleteFSTAnalyzer.create("test/", true));

        System.out.print("最短匹配:");
        token(ShortestFSTAnalyzer.create("test/", true));

        System.out.print("最多数量匹配:");
        token(MaxCountAnalyzer.create("test/", true));
    }

    private static void token(Analyzer analyzer) throws IOException {
        final TokenStream tokenStream = analyzer.tokenStream("test", "奥迪Q湖北工业大学生");
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


分词效果:

前缀词匹配:奥迪 Q 湖北工业大学 湖北工业 湖北 生
前缀词优先匹配:奥迪Q 奥迪 湖北工业大学 湖北工业 湖北 生
最长匹配:奥迪 Q 湖北工业大学 生
最短匹配:奥迪 Q 湖北 工业 大学 生
最多数量匹配:奥迪 Q 湖北工业大学 湖北工业 湖北 工业大学生 工业大学 工业 大学生 大学
