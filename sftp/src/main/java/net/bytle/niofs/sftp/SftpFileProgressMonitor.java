package net.bytle.niofs.sftp;

import com.jcraft.jsch.SftpProgressMonitor;

/**
 * Created by gerard on 22-11-2015.
 */
public class SftpFileProgressMonitor implements SftpProgressMonitor {


    public long max; // the final count (i.e. length of file to transfer).
    public String destinationFileName;


    private long count; // the number of bytes transferred so far
    private int direction; // either SftpOverWriteByteChannel.PUT or get
    private String stringBeingTransferred;


    public void init(int i, String src, String destinationFileName, long max) {
        this.direction = i;
        this.stringBeingTransferred = src;
        this.destinationFileName = destinationFileName;
        this.max = max;
    }


    // Called periodically as more data is transfered.
    public boolean count(long count) {

        this.count = count;
        // true if the transfer should go on, false if the transfer should be cancelled.
        return true;
    }

    // called when the transfer ended, either because all the data was transferred,
    // or because the transfer was cancelled.
    public void end() {

    }

    public long getCount() {
        return count;
    }


}
