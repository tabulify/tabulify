package net.bytle.niofs.sftp;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Set;

/**
 * Created by gerard on 21-11-2015.
 */
public class SftpPrincipal implements Principal {

    private final int uId;

    protected SftpPrincipal(int uId) {
        this.uId = uId;
    }

    public String getName() {
        return String.valueOf(uId);
    }

    public boolean implies(Subject subject) {
        if (subject !=null) {
            Set<Principal> principals = subject.getPrincipals();
            if (principals.contains(this)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }
}
