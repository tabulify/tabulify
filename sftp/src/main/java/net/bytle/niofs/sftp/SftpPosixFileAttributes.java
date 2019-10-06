package net.bytle.niofs.sftp;

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.*;

/**
 * Created by gerard on 21-11-2015.
 */
public class SftpPosixFileAttributes extends SftpBasicFileAttributes implements PosixFileAttributes  {

    protected SftpPosixFileAttributes(SftpPath path) throws IOException {
        super(path);
    }

    public UserPrincipal owner() {
        return new SftpUserPrincipal(this.attrs.getUId());
    }

    public GroupPrincipal group() {
        return new SftpGroupPrincipal(this.attrs.getUId());
    }

    public Set<PosixFilePermission> permissions() {


        // The permission string in a list of PosixFilePermission
        List<PosixFilePermission> listPermissions = new ArrayList<PosixFilePermission>();
        listPermissions.add(PosixFilePermission.OWNER_READ);
        listPermissions.add(PosixFilePermission.OWNER_WRITE);
        listPermissions.add(PosixFilePermission.OWNER_EXECUTE);
        listPermissions.add(PosixFilePermission.GROUP_READ);
        listPermissions.add(PosixFilePermission.GROUP_WRITE);
        listPermissions.add(PosixFilePermission.GROUP_EXECUTE);
        listPermissions.add(PosixFilePermission.OTHERS_READ);
        listPermissions.add(PosixFilePermission.OTHERS_WRITE);
        listPermissions.add(PosixFilePermission.OTHERS_EXECUTE);

        // We get the permission string and we create it by looking up
        String permissionString = this.attrs.getPermissionsString();
        Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>();
        char nothing = "-".charAt(0);
        // We skip the first character as it's the file type
        for (int i=1; i< permissionString.length();i++) {
            if (permissionString.charAt(i) != nothing) {
                permissions.add(listPermissions.get(i-1));
            }
        }

        return permissions;

    }

}
