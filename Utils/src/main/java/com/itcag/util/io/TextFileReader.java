package com.itcag.util.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;

/**
 * <p>This class opens and reads a text file.</p>
 */
public final class TextFileReader {

    /**
     * @param filePath String holding a local path to a text file.
     * @return Array list of strings - each string containing a line in the file.
     * @throws Exception if anything goes wrong.
     */
    public final synchronized static ArrayList<String> read(String filePath) throws Exception {
        
        File file = new File(filePath);
        if (!file.exists()) throw new FileNotFoundException(filePath);

        ArrayList<String> retVal = new ArrayList<>();
        
        try (InputStream input = new FileInputStream(file)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line = reader.readLine();
            while (line != null){
                line = line.trim();
                if (!line.isEmpty()) retVal.add(line);
                line = reader.readLine();
            }
        }
        
        return retVal;
    
    }

    public final static String readAll(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    }
    
}