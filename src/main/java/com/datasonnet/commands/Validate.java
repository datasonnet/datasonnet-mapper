package com.datasonnet.commands;


import com.datasonnet.Mapper;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

@CommandLine.Command(
        name = "validate",
        description = "Validate a DataSonnet map"
)
public class Validate implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            description = "Map file"
    )
    private File datasonnet;

    @CommandLine.Option(names = {"-i", "--includes-function"})
    boolean includesFunction;


    @Override
    public void run() {
        Mapper mapper = new Mapper(datasonnet, new ArrayList<>(), !includesFunction);
        System.out.println("Validates!");
    }
}
