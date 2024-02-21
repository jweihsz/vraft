package com.vraft.facade.common;

/**
 * @author jweihsz
 * @version 2024/2/20 20:46
 **/
public class Pair<L, R> {
    private final L left;
    private final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

}
