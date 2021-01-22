package com.devexperts.common;

import org.apache.ignite.Ignite;

import java.io.PrintStream;

public class Utils {

    public static void printNodeStats(Ignite ignite, PrintStream out) {
        out.print("Node info:\n" +
                "\tNode ID: " + ignite.cluster().localNode().id() + "\n" +
                "\tOS: " + System.getProperty("os.name") + "\n" +
                "\tJRE: " + System.getProperty("java.runtime.name") + "\n");
    }
}
