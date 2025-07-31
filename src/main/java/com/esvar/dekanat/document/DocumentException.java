package com.esvar.dekanat.document;

/**
 * Base exception for document generation errors.
 */
public class DocumentException extends RuntimeException {
    public DocumentException(String message) {
        super(message);
    }

    public DocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}