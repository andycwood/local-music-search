package com.werdnadoow;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

@Configuration
public class MusicBase {	
	
	protected Analyzer analyzer;
	
	protected String indexDirProperty;
	
	protected Directory indexDirectory;	

	public MusicBase()
	{
		indexDirProperty = null;
		indexDirectory = null;
		analyzer = null;
	}
	
	public void setIndexDir(String indexDir)
	{
		indexDirProperty = indexDir;
	}
	
	public void setAnalyzer(Analyzer a)
	{
		analyzer = a;
	}
	
    public Directory getIndexDirectory()
    {
    	if (null == indexDirectory)
    	{
    		if (null == indexDirProperty)
    		{
    			return null;
    		}
    		try
    		{
    			indexDirectory = FSDirectory.open(Paths.get(indexDirProperty));
    		}
    		catch (IOException e)
    		{
    			e.printStackTrace();
    			return null;
    		}
    	}
    	return indexDirectory;
    }
}
