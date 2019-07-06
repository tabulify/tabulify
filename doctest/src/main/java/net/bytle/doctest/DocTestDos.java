package net.bytle.doctest;

import java.util.ArrayList;
import java.util.List;

public class DocTestDos {

    /**
     *
     * @param code - the input dos code
     * @return Building the commands (each command is represented as an array of args)
     */
    protected static List<String[]> parseDosCommand(String code) {

        final int defaultState = 1;
        final int spaceCapture = 2;
        final int quoteCapture = 3;
        final char spaceChar = ' ';
        final char quoteChar = '"';
        final char[] comments = {':',':'};

        List<String[]> commands = new ArrayList<>();
        String[] lines = code.trim().split("\n|\r\n");
        for (String line:lines) {

            line = line.trim();

            // Comments skipping
            if (line.length()>=comments.length) {
                if (line.trim().substring(0, comments.length).equals(new String(comments))) {
                    continue;
                }
            }

            int state = defaultState;
            char[] dst = new char[line.length()];
            line.getChars(0, line.length(), dst, 0);
            StringBuilder arg = new StringBuilder();
            List<String> args = new ArrayList<>();
            for (char c : dst) {
                switch (state) {
                    case defaultState:
                        switch (c) {
                            case spaceChar:
                                state = spaceCapture;
                                continue;
                            case quoteChar:
                                state = quoteCapture;
                                break;
                            default:
                                arg.append(c);
                                state = spaceCapture;
                                break;
                        }
                        break;
                    case spaceCapture:
                        switch (c) {
                            case spaceChar:
                                if (!arg.toString().equals("")) {
                                    args.add(arg.toString());
                                    arg = new StringBuilder();
                                }
                                state = defaultState;
                                break;
                            case quoteChar:
                                if (!arg.toString().equals("")) {
                                    arg.append(c);
                                } else {
                                    state = quoteCapture;
                                }
                                break;
                            default:
                                arg.append(c);
                                break;
                        }
                        break;
                    case quoteCapture:
                        switch (c) {
                            case quoteChar:
                                if (!arg.toString().equals("")) {
                                    args.add(arg.toString());
                                    arg = new StringBuilder();
                                }
                                state = defaultState;
                                break;
                            default:
                                arg.append(c);
                                break;
                        }
                        break;
                }
            }

            if (!arg.toString().trim().equals("")) {
                args.add(arg.toString());
            }
            commands.add(args.toArray(new String[args.size()]));
        }
        return commands;
    }
}
