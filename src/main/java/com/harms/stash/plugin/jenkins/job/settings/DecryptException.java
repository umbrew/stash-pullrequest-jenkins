package com.harms.stash.plugin.jenkins.job.settings;

/**
 * Throw to indicate it's not able to decrypt the value
 * @author fharms
 *
 */
public class DecryptException extends Exception {

    private static final long serialVersionUID = 3545086984655863875L;

    public DecryptException() {
        super();
    }

    public DecryptException(String message) {
        super(message);
    }
    
    public DecryptException(Throwable cause) {
        super(cause);
    }
    
    public DecryptException(String message,Throwable cause) {
        super(message,cause);
    }
}
