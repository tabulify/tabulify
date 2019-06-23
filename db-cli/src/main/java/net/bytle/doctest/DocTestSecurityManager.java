package net.bytle.doctest;

import java.security.Permission;

/**
 * Security Manager to avoid
 * exiting during test
 * Doc: https://stackoverflow.com/questions/5549720/how-to-prevent-calls-to-system-exit-from-terminating-the-jvm
 */
public class DocTestSecurityManager extends SecurityManager {

    private static DocTestSecurityManager docTestSecurityManager;

    public static DocTestSecurityManager get() {
        if (docTestSecurityManager == null) {
            docTestSecurityManager = new DocTestSecurityManager();
        }
        return docTestSecurityManager;
    }

    @Override
    public void checkExit(int status) {
        // Doing nothing means that the JVM will exit
        // throwing is the only way to prevent it
        throw new SecurityException();
    }

    @Override
    public void checkPermission(Permission perm) {
        // Allow other activities by default
    }

}
