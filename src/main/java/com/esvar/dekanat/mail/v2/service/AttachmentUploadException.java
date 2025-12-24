package com.esvar.dekanat.mail.v2.service;

public class AttachmentUploadException extends RuntimeException {
    public AttachmentUploadException(String message) {
        super(message);
    }

    public AttachmentUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
