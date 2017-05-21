package com.werdnadoow;

import static java.nio.file.FileVisitResult.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MusicFileVisitor extends SimpleFileVisitor<Path> 
{
	// filter out non-music files
	//TODO : move to settings 
	private final String regex = "([^.]+(\\.(?i)(mp3|aac|m4a|flac|wav|wma|alac|ogg))$)";

	private Pattern pattern = null;
	
	private final List<String> paths;
	
	public MusicFileVisitor()
	{
		pattern = null;
		paths = new ArrayList<String>();
	}

	public Boolean initialize() 
	{
		try
		{
			pattern = Pattern.compile(regex);
		}
		catch (PatternSyntaxException pe)
		{
			pe.printStackTrace();
			return false;
		}
		return true;
	}
	
	public final List<String> getPaths()
	{
		return paths;
	}
	
    // Print information about
    // each type of file.
    @Override
    public FileVisitResult visitFile(Path file,
                                   BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            Matcher matcher = pattern.matcher(file.toString());
            if (matcher.matches())
            {	
            	String s = matcher.group();
            	paths.add(s);
            }
        } 
        return CONTINUE;
    }

    // Print each directory visited.
    @Override
    public FileVisitResult postVisitDirectory(Path dir,
                                          IOException exc) {
        System.out.format("Directory: %s%n", dir);
        return CONTINUE;
    }

    // If there is some error accessing
    // the file, let the user know.
    // If you don't override this method
    // and an error occurs, an IOException 
    // is thrown.
    @Override
    public FileVisitResult visitFileFailed(Path file,
                                       IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }
}
