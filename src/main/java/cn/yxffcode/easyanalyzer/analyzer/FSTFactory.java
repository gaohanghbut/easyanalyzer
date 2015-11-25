package cn.yxffcode.easyanalyzer.analyzer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cn.yxffcode.easyanalyzer.collection.IteratorAdapter;
import cn.yxffcode.easyanalyzer.io.IOStreams;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static cn.yxffcode.easyanalyzer.utils.StringUtils.isBlank;

/**
 * @author gaohang on 15/11/19.
 */
abstract class FSTFactory {
    private FSTFactory() {
    }

    /**
     * @param classpath    词典文件的类路径，支持目录
     *
     * @return 基于FST的分词器
     *
     * @throws IOException 读取字典或创建FST出错
     */
    public static FST<CharsRef> create(String classpath) throws IOException {
        return create(classpath, Thread.currentThread().getContextClassLoader());
    }

    /**
     * @param classpath    词典文件的类路径，支持目录
     * @param classLoader  用于加载词典文件的类加载器
     *
     * @return 基于FST的分词器
     *
     * @throws IOException 读取字典或创建FST出错
     */
    public static FST<CharsRef> create(String classpath, ClassLoader classLoader) throws IOException {
        final Enumeration<URL> resources = checkNotNull(classLoader.getResources(classpath));
        SortedSet<String>      set       = Sets.newTreeSet();
        for (URL url : new Iterable<URL>() {
            @Override
            public Iterator<URL> iterator() {
                return IteratorAdapter.create(resources);
            }
        }) {
            File file = new File(url.getFile());
            if (file.isFile()) {
                try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                    readDictionary(set, in);
                }
            } else if (file.isDirectory()) {
                //广度优先搜索/层次遍历
                File[] files = list(file);
                if (files == null) {
                    continue;
                }
                LinkedList<File> queue = Lists.newLinkedList(Arrays.asList(files));
                while (!queue.isEmpty()) {
                    File f = queue.removeFirst();
                    if (f.isFile()) {
                        try (BufferedReader in = new BufferedReader(new FileReader(f))) {
                            readDictionary(set, in);
                        }
                    } else if (f.isDirectory()) {
                        queue.addAll(Arrays.asList(list(f)));
                    }
                }
            }
        }
        return create(set);
    }

    private static File[] list(File file) {
        return file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".dic");
            }
        });
    }

    /**
     * @param dictionaries 词典文件列表
     */
    public static FST<CharsRef> create(@NotNull Iterable<File> dictionaries) throws IOException {
        checkNotNull(dictionaries);
        SortedSet<String> set = Sets.newTreeSet();
        for (File dictionary : dictionaries) {
            try (BufferedReader in = new BufferedReader(new FileReader(dictionary))) {
                readDictionary(set, in);
            }
        }
        return create(set);
    }

    private static void readDictionary(SortedSet<? super String> set, BufferedReader in) {
        for (String line : IOStreams.lines(in)) {
            if (isBlank(line)) {
                continue;
            }
            set.add(line.trim().toLowerCase());
        }
    }

    /**
     * @param sortedWords 所有词条，因为FST的创建过程中需要词条排好序，所以使用SortedSet,
     *                    使用字符串的默认排序，不要使用字符串的自定义排序
     */
    public static FST<CharsRef> create(@NotNull SortedSet<String> sortedWords) throws IOException {
        checkNotNull(sortedWords);
        final CharSequenceOutputs outputs  = CharSequenceOutputs.getSingleton();
        final Builder<CharsRef>   builder  = new Builder<>(FST.INPUT_TYPE.BYTE4, outputs);
        final IntsRefBuilder      scratch  = new IntsRefBuilder();
        final CharsRef            noOutput = outputs.getNoOutput();
        for (String word : sortedWords) {
            builder.add(Util.toIntsRef(new BytesRef(word.getBytes()), scratch), noOutput);
        }
        return builder.finish();
    }

}
