package com.werdnadoow;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;

// This class is responsible for indexing the music files 
// It uses a FileVisitor to walk the file tree
// Then it uses Spring / Hibernate to connect to a datasource 
// (We need to acquire an ID from the data source in order to have stable data for React)
// Next we pass the list of songs to Lucene to index
//TODO : convert from batch to per item, call from microservice
//TODO : implement loading of recently changed files only
@Component
public class MusicIndex extends MusicBase implements Closeable {
	
	private IndexWriter writer;
	
	private MusicFileVisitor filevisitor = null;
	
	private String musicFolder = null;
	
	public MusicIndex()
	{
		indexDirProperty = null;
		writer = null;
		filevisitor = null;
		
		// Need Synchronization for multi-threading
		//songs = Collections.synchronizedList(new ArrayList<Song>());
	}
	
	Boolean initialize(Analyzer a, String indexFolder)
	{
		if (null == indexFolder)
			return false;
		
		if (null == a)
			return false;
		
		setIndexDir(indexFolder);
		
		setAnalyzer(a);
		
		Directory dir = getIndexDirectory();
		if (null == dir)
			return false;

		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
	    iwc.setOpenMode(OpenMode.CREATE);
	    try
	    {
	    	writer = new IndexWriter(dir, iwc);
			filevisitor = new MusicFileVisitor();
	    }
	    catch(IOException e)
	    {
	    	e.printStackTrace();
	    	return false;
	    }
		if (!filevisitor.initialize())
			return false;

		return true;
	}
	
	// Final clear up only
	public Boolean save()
	{
		try
		{
			writer.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	// Walk the file structure and save paths of music files
	// results are obtained from getMusicFileCount & getMusicFileList
	//TODO: detect recently changed files
	public Boolean readMusicFolder(String dir)
	{
		
		// save the musicFolder
		musicFolder = new String(dir);
		
		// walk the directory tree
		Path startingDir = Paths.get(dir);
		
		try
		{
			Files.walkFileTree(startingDir, filevisitor);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	// provide total file count to caller
	public Integer getMusicFileCount() {
		
		if (null == filevisitor)
			return 0;

		if (filevisitor.getPaths() == null)
			return 0;
		
		return filevisitor.getPaths().size();
	}
	
	// provide total file count to caller
	public List<String> getMusicFileList() {
		
		if (null == filevisitor)
			return new ArrayList<String>();

		if (filevisitor.getPaths() == null)
			return new ArrayList<String>();
		
		// file visitor returns a list in a concurrency wrapper
		return filevisitor.getPaths();
	}
	
	// Update Lucene documents for all song objects
	public Boolean indexSong(Song s)
	{
		Boolean ret = true;
		
		if (null == s)
			return false;
		
		Document doc = new Document();
		// Add the path of the file as a field named "path".  Use a
		// field that is indexed (i.e. searchable), but don't tokenize 
		// the field into separate words and don't index term frequency
		// or positional information:
		// the full path is useful for playback:
		StringField pathField = new StringField("fullpath", s.getPath(), Field.Store.YES);
		doc.add(pathField);

		// store the id
		StoredField idField = new StoredField("songid", s.getId());
		doc.add(idField);
		
		// Add the path again the file to a field named "contents".  Specify a Reader,
		// so that the text of the file is tokenized and indexed, but not stored.
		// Note that FileReader expects the file to be in UTF-8 encoding.
		// If that's not the case searching for special characters will fail.
		// chop off base directory
		// this abbreviated path is more readable
		String path = s.getPath().substring(musicFolder.length());
		StringField pathField2 = new StringField("path", path, Field.Store.YES);
		doc.add(pathField2);
		
		String terms;
		// if there is no artist and title metadata, then use the file path
		if (s.getArtist() != null && s.getTitle() != null && s.getArtist().length() > 0 && s.getTitle().length() > 0) {
			terms = s.getArtist() + " " + s.getTitle() + " " + s.getAlbum();
		}
		else {
			// the path that is useful for searching
			// by taking out the slashes to create words that can be parsed for search
			terms = path.replace('/',' ');
		}
		InputStream stream = new ByteArrayInputStream(terms.getBytes(StandardCharsets.UTF_8));
		Reader r = new InputStreamReader(stream, StandardCharsets.UTF_8);
		BufferedReader br = new BufferedReader(r);
		TextField tx = new TextField("contents", br);

	    doc.add(tx);  
		try
		{
	      if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
	        // New index, so we just add the document (no old document can be there):
	        //System.out.println("adding " + path);
	        writer.addDocument(doc);
	      } else {
	        // Existing index (an old copy of this document may have been indexed) so 
	        // we use updateDocument instead to replace the old one matching the exact 
	        // path, if present:
	        //System.out.println("updating " + path);
	        writer.updateDocument(new Term("path", path.toString()), doc);
	      }
		}
		catch (IOException e)
		{
			e.printStackTrace();
			ret = false;
		}
		
		// ret = false means we had *some* errors
		return ret;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
		if (writer != null)
			writer.close();
	}

}
