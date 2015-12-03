package cn.yxffcode.easyanalyzer.collection;

/**
 * @author gaohang on 15/11/18.
 */
public class IntStack {

    private int[] stack;
    private int   top;

    public IntStack() {
        this(10);
    }

    public IntStack(int initSize) {
        stack = new int[initSize];
    }

    public void push(int value) {
        if (top == stack.length) {
            int[] desc = new int[stack.length * 2];
            System.arraycopy(stack, 0, desc, 0, top);
            this.stack = desc;
        }

        stack[top++] = value;
    }

    public int poll() {
        int value = stack[top - 1];
        top--;
        return value;
    }

    public int botton() {
        if (isEmpty()) {
            throw new IllegalStateException("stack is empty");
        }
        return stack[0];
    }

    public int peak() {
        return stack[top - 1];
    }

    public boolean isEmpty() {
        return top == 0;
    }
}
