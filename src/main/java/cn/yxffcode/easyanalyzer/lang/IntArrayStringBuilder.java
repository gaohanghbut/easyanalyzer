package cn.yxffcode.easyanalyzer.lang;

/**
 * 在分词过程中使用的是int类型的数据,使用面向int类型数据的StringBuilder性能表现更好,
 * 此工具类用于取代{@link StringBuilder}
 *
 * @author gaohang on 15/11/17.
 */
public class IntArrayStringBuilder {

    private int[] dest;
    private int   pos;

    public IntArrayStringBuilder() {
        this(10);
    }

    public IntArrayStringBuilder(int initSize) {
        this.dest = new int[initSize];
    }

    public IntArrayStringBuilder append(int c) {
        if (pos == dest.length) {
            resize();
        }
        dest[pos++] = c;
        return this;
    }

    public IntArrayStringBuilder append(int... cs) {
        if (pos + cs.length > dest.length) {
            resize();
        }
        for (int c : cs) {
            dest[pos++] = c;
        }
        return this;
    }

    public int element(int index) {
        return dest[index];
    }

    public void clear() {
        pos = 0;
    }

    public int length() {
        return pos;
    }

    public boolean isEmpty() {
        return pos == 0;
    }

    public boolean isBlank() {
        for (int i = 0; i < pos; i++) {
            if (! Character.isWhitespace(dest[i])) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return new String(toCharArray());
    }

    public String toString(int off,
                           int len) {
        return new String(toCharArray(off, len));
    }

    public char[] toCharArray() {
        return toCharArray(0, pos);
    }

    public boolean endWith(IntArrayStringBuilder appender) {
        int thisLength = length();
        int thatLength = appender.length();
        if (thisLength < thatLength) {
            return false;
        }
        if (thisLength == thatLength) {
            return equals(appender);
        }
        for (int i = thatLength - 1, j = thisLength - 1; i >= 0; -- i, -- j) {
            if (appender.element(i) != element(j)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final IntArrayStringBuilder that = (IntArrayStringBuilder) o;
        if (this.pos != that.pos) {
            return false;
        }
        for (int i = 0; i < pos; i++) {
            if (this.dest[i] != that.dest[i]) {
                return false;
            }
        }
        return true;
    }

    public char[] toCharArray(int off,
                              int len) {
        char[] chars = new char[len];
        for (int i = off, j = len + off; i < j; i++) {
            chars[i] = (char) dest[i];
        }
        return chars;
    }

    private void resize() {
        int[] ndest = new int[dest.length * 2];
        System.arraycopy(dest, 0, ndest, 0, pos);
        this.dest = ndest;
    }

}
