package net.bytle.niofs.sftp;

import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by gerard on 02-05-2016.
 * Manage the permissions
 *
 * For the dates, see {@link SftpBasicFileAttributes}
 */
public class SftpPosixFileAttributeView implements PosixFileAttributeView {

    private final SftpPath path;

    public SftpPosixFileAttributeView(SftpPath path) {
        this.path = path;
    }

    public String name() {
            return "Sftp Posix File Attribute View";
    }

    public UserPrincipal getOwner() throws IOException {

        throw new UnsupportedOperationException();

    }

    public void setOwner(UserPrincipal owner) throws IOException {

        throw new UnsupportedOperationException();

    }

    public PosixFileAttributes readAttributes() throws IOException {

        throw new UnsupportedOperationException();

    }

    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {

        throw new UnsupportedOperationException();

    }

    /**
     * Set the Posix permissions
     * @param perms
     * @throws IOException
     */
    public void setPermissions(Set<PosixFilePermission> perms) throws IOException {

        // Create the textual representation of the permission
        // for owner, group and other
        char[] ownerPerm = "---".toCharArray();
        char[] groupPerm = "---".toCharArray();
        char[] otherPerm = "---".toCharArray();
        for (PosixFilePermission posixFilePermission: perms) {

            switch(posixFilePermission) {
                case OWNER_READ:
                    ownerPerm[0]="r".toCharArray()[0];
                    break;
                case OWNER_WRITE:
                    ownerPerm[1]="w".toCharArray()[0];
                    break;
                case OWNER_EXECUTE:
                    ownerPerm[2]="x".toCharArray()[0];
                    break;
                case GROUP_READ:
                    groupPerm[0]="r".toCharArray()[0];
                    break;
                case GROUP_WRITE:
                    groupPerm[1]="w".toCharArray()[0];
                    break;
                case GROUP_EXECUTE:
                    groupPerm[2]="x".toCharArray()[0];
                    break;
                case OTHERS_READ:
                    otherPerm[0]="r".toCharArray()[0];
                    break;
                case OTHERS_WRITE:
                    otherPerm[1]="w".toCharArray()[0];
                    break;
                case OTHERS_EXECUTE:
                    otherPerm[2]="x".toCharArray()[0];
                    break;
            }
        }

        // Create a mapping between textual and digital representations
        HashMap<String, Integer> textToDigitMap = new HashMap<>();
        textToDigitMap.put("---",0);
        textToDigitMap.put("--x",1);
        textToDigitMap.put("-w-",2);
        textToDigitMap.put("-wx",3);
        textToDigitMap.put("r--",4);
        textToDigitMap.put("r-x",5);
        textToDigitMap.put("rw-",6);
        textToDigitMap.put("rwx",7);

        // Calculate the digital representations
        // because this is the only one that accepts Jsch
        int permissions = 0; // All access are denied
        permissions += textToDigitMap.get(String.valueOf(ownerPerm))*100;
        permissions += textToDigitMap.get(String.valueOf(groupPerm))*10;
        permissions += textToDigitMap.get(String.valueOf(otherPerm))*1;
        int permissionOctal = Integer.parseInt(String.valueOf(permissions),8); // Data must be in octal !
        // Set the permission
        try {
            this.path.getChannelSftp().chmod(permissionOctal,this.path.toAbsolutePath().toString());
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }


    }

    public void setGroup(GroupPrincipal group) throws IOException {

        throw new UnsupportedOperationException();

    }

    public static <V extends FileAttributeView> V get(Path path) {

        return (V) new SftpPosixFileAttributeView((SftpPath) path);

    }
}
