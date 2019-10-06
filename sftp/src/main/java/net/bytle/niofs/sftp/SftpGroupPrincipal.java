package net.bytle.niofs.sftp;

import java.nio.file.attribute.GroupPrincipal;

/**
 * Created by gerard on 21-11-2015.
 */
public class SftpGroupPrincipal extends SftpUserPrincipal implements GroupPrincipal {

    protected SftpGroupPrincipal(int uId) {
        super(uId);
    }

}
