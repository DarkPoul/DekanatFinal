package com.esvar.dekanat.document;

/**
 * Thrown when requested document template or generator is missing.
 */
public class MissingTemplateException extends DocumentException {
    public MissingTemplateException(String message) {
        super(message);
    }
}