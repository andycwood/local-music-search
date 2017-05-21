package com.werdnadoow;

import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class MusicSearchController {

	private class LoadStatusResult {
		public String status;
		public Integer tracks;
		public Integer tracksprocessed;
	}
	
    private String indexFolder;
    private String musicFolder;
    
	@Value("${lucene.indexFolder}")
    public void setIndexFolder(String s) {
        indexFolder = s;
    }
	
	@Value("${lucene.musicFolder}")
    public void setMusicFolder(String s) {
        musicFolder = s;
    }
	// index page provides explanation of how to use the other endpoints
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "text/html")
    public String index() {
        return "<html><body><heading>Greetings from Music Search!</heading><ol>" 
        		+"<li><a href=\"./load?action=start\">./load?action=start</a></li><ul><li>start loading music from " + musicFolder + "</li></ul><br>"
         		+"<li><a href=\"./load?action=status\">./load?action=status</a></li><ul><li>check progress of music load</li></ul><br>" 
                +"<li><a href=\"./search?terms=a*\">./search/?terms=a*</a></li><ul><li>search for music</li></ul></ol>"; 

    }
	
    @Autowired
    private SongRepository songRepository;
    
    // This end point is called by a react application
    // It is the search end point
    // /search
    // param : terms
    // param : size (optional)
    // defaults to returning 10 results
    // 
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = "application/json")
    public String search(@RequestParam(value="terms") String terms, @RequestParam(value="size", defaultValue="10") String hitSize) 
    {
		
		ApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
		
		Analyzer analyzer = ctx.getBean("analyzer",Analyzer.class);
		    	
		((ConfigurableApplicationContext)ctx).close();
		
		try 
		{
			ObjectMapper objectMapper = new ObjectMapper();
			//TODO : create result status object that includes status
			
	    	MusicSearch search = new MusicSearch(songRepository);
	    	
	    	search.setAnalyzer(analyzer);
	    	search.setIndexDir(indexFolder);

	    	if (null == terms)
	    		return objectMapper.writeValueAsString("missing terms");
	    	
	    	if (terms.length()==0)
	    		return objectMapper.writeValueAsString("missing terms");
	    	
	    	// Parse the hits parameter
	    	int hits = Integer.parseInt(hitSize);
	    	List<Song> results = search.search(terms, hits);
	    	if (results.isEmpty())
	    	{
	    		return objectMapper.writeValueAsString(results);   
	    	}
	    	else
	    	{
	    		return objectMapper.writeValueAsString(results);
	    	}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "Fatal Error";
		}
    }

    // This is the end point used to start loading music from disk,
    // and monitor progress of a load in progress
    // /load
    // param: action
    //  'status' = provide status of loading
    //  'start' = start a load
    // default action is to provide status
    // start will ignore if a load it in progress
    // result is always a status result
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/load", method = RequestMethod.GET, produces = "application/json")
    public String load(@RequestParam(value="action", defaultValue="status") String action) 
    {
    	String status = new String();
    	
    	// Using synchronized object to communicate status
    	// status of load (load may be in progress)
    	LoadStatus ls = new LoadStatus();
		if (ls.getIsRunning())
		{
			status = "running";
		}
		// if load is not in progress, check to see if a start was requested
		else if (action.equals("start"))
		{
			status = "starting";
			
			ApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
			
			// Get the thread pool
	    	ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) ctx.getBean("loadTaskExecutor");
			
	    	// always start a thread, but it will do nothing if another one is already running
			System.out.println("starting new task");
	        ListenableFuture<String> listenableFuture = taskExecutor.submitListenable(new LoadMusicTask(songRepository, indexFolder, musicFolder));
	        listenableFuture.addCallback(new ListenableFutureCallback<String>() {
	            @Override
	            public void onSuccess(String str) {
	            	System.out.println("Task returned status: " + str);
	            }

	            @Override
	            public void onFailure(Throwable t) {
	            	System.out.println("Task failed! " + t.getMessage() );
	            }
	        });

			((ConfigurableApplicationContext)ctx).close();
		}
		else {
			status = "idle";			
		}
		LoadStatusResult lsr = new LoadStatusResult();
		lsr.status = status;

		// How many tracks found by folder crawling?
		if (ls.getTrackCount() < 0)
		{
			lsr.tracks=0;
		}
		else 
		{
			lsr.tracks = ls.getTrackCount();
		}
		
		// how many tracks processed so far
		if (ls.getProcessedCount() < 0)
		{
			lsr.tracksprocessed = 0;
		}
		else 
		{
			lsr.tracksprocessed = ls.getProcessedCount();
		}
		ObjectMapper objectMapper = new ObjectMapper();
		try 
		{
			return objectMapper.writeValueAsString(lsr);
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "{\"status\"=\"error\"}";
    }
}