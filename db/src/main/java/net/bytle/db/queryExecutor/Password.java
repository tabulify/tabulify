package net.bytle.db.queryExecutor;


import net.bytle.db.DbLoggers;

import java.util.logging.Logger;

public class Password {
    private final static Logger logger = DbLoggers.LOGGER_DB_QUERY;

    // -e appel password ==> goem/mS5qf0=
    // -d goem/mS5qf0= password ==> appel
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            logger.severe("Usage : -d <encrypted> <password> or -e <unencrypted> <password>");
            return;
        }

        Protector protector = new Protector(args[2]);

        if ("-d".equals(args[0]))
            System.out.println(protector.decrypt(args[1]));
        else if ("-e".equals(args[0]))
            System.out.println(protector.encrypt(args[1]));
    }
}
