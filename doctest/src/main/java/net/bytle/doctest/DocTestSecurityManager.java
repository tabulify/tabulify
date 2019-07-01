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
        // throwing is the only way to prevent an exit in case of error
        if (status!=0) {
            throw new RuntimeException("An error has occurred and have the status "+status);
        } else {
            throw new PreventExitException("A system exit with the status 0 was seen");
        }

    }

    @Override
    public void checkPermission(Permission perm) {
        // Allow other activities by default
    }

}
