package com.wordmind.exception;

public class DuplicateWordException extends RuntimeException {
    public DuplicateWordException(String word) {
        super("单词已存在: " + word);
    }
}
