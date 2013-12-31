package com.harms.stash.plugin.jenkins.job.settings;

/**
 * Throw to indicate it's not able to encrypt the value
 * @author fharms
 *
 */
public class EncryptException extends Exception {

    private static final long serialVersionUID = 3545086984655863875L;

    public EncryptException() {
        super();
    }

    public EncryptException(String message) {
        super(message);
    }
    
    public EncryptException(Throwable cause) {
        super(cause);
    }
    
    public EncryptException(String message,Throwable cause) {
        super(message,cause);
    }
}
