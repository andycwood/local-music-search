package com.werdnadoow;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Component;

import com.werdnadoow.data.Song;
import com.werdnadoow.data.SongRepository;

// this is the class that reads the lucene index to deliver
// search results 
@Component
public class MusicSearch extends MusicBase {

	private final String searchField = "contents";
	private final SongRepository songRepository;
	
	public MusicSearch(SongRepository s)
	{
		this.songRepository = s;
		indexDirProperty = null;
	}
	
	public List<Song> search(String terms, int hitSize)
	{	
		List<Song> result = new ArrayList<Song>();
		Directory dir = getIndexDirectory();

		if (null == dir)
			return result;
		
		try (IndexReader reader = DirectoryReader.open(dir))
		{
			IndexSearcher searcher = new IndexSearcher(reader);
			QueryParser parser = new QueryParser(searchField, analyzer);
		    Query query = parser.parse(terms);
		    
		    TopDocs results = searcher.search(query,hitSize);
		    ScoreDoc[] hits = results.scoreDocs;
		    
		    int numTotalHits = results.totalHits;
		    System.out.println(numTotalHits + " total matching documents");
		    
		    if (numTotalHits > 0)
		    {
		    	for (int i = 0; i < Integer.min(hitSize,numTotalHits); i++)
		    	{
		            Document doc = searcher.doc(hits[i].doc);
		            IndexableField idx = doc.getField("songid");
		            if (null != idx)
		            {
		            	String id = idx.stringValue();
		            	
			            Song s = new Song();
			            s.setId(id);
			            List<Song> songs = songRepository.findById(id);
			            result.addAll(songs);
		            }

		    	}
		    }
		    
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}
}
