package com.werdnadoow;

import org.springframework.web.bind.annotation.RestController;

import com.werdnadoow.data.LoadStatus;
import com.werdnadoow.data.LoadStatusResult;
import com.werdnadoow.data.MongoDbHelper;
import com.werdnadoow.data.QueueRequest;
import com.werdnadoow.data.QueuedSong;
import com.werdnadoow.data.QueuedSongRepository;
import com.werdnadoow.data.Song;
import com.werdnadoow.data.SongRepository;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class MusicSearchController {

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
    
    @Autowired
    private QueuedSongRepository queuedSongRepository;
        
    // This end point is called by a react application
    // It is the search end point
    // /search
    // param : terms
    // param : size (optional)
    // defaults to returning 10 results
    // 
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<Song>> search(@RequestParam(value="terms") String terms, @RequestParam(value="size", defaultValue="10") String hitSize) 
    {
		
		ApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
		
		Analyzer analyzer = ctx.getBean("analyzer",Analyzer.class);
		    	
		((ConfigurableApplicationContext)ctx).close();
		
		List<Song> results = null;
		
		try 
		{
	    	MusicSearch search = new MusicSearch(songRepository);
	    	
	    	search.setAnalyzer(analyzer);
	    	search.setIndexDir(indexFolder);
			
	    	if (null == terms || terms.length()==0)
	    		return new ResponseEntity<List<Song>>(results,HttpStatus.BAD_REQUEST);
	    	
	    	// Parse the hits parameter
	    	int hits = Integer.parseInt(hitSize);
	    	results = search.search(terms, hits);
	    	
	    	if (results != null)
	    	{
	    		return new ResponseEntity<List<Song>>(results,HttpStatus.OK);
	    	}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new ResponseEntity<List<Song>>(results,HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<List<Song>>(results,HttpStatus.OK);
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
    public ResponseEntity<LoadStatusResult> load(@RequestParam(value="action", defaultValue="status") String action) 
    {
		LoadStatusResult lsr = new LoadStatusResult();
    	
		try
		{
	    	// Using synchronized object to communicate status
	    	// status of load (load may be in progress)
	    	LoadStatus ls = new LoadStatus();
			if (ls.getIsRunning())
			{
				lsr.status = "running";
			}
			// if load is not in progress, check to see if a start was requested
			else if (action.equals("start"))
			{
				lsr.status = "starting";
				
				ApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
				
				// Get the thread pool
		    	ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) ctx.getBean("loadTaskExecutor");
				((ConfigurableApplicationContext)ctx).close();
				
		    	// always start a thread, but it will do nothing if another one is already running
				System.out.println("starting new task");
		        ListenableFuture<String> listenableFuture = taskExecutor.submitListenable(new LoadMusicTask(songRepository, indexFolder, musicFolder));
		        listenableFuture.addCallback(new ListenableFutureCallback<String>() {
		            @Override
		            public void onSuccess(String str) {
		            	System.out.println("Task returned status: " + str);
		            	// pass task status back to caller
		            	lsr.message = str;
		            }
	
		            @Override
		            public void onFailure(Throwable t) {
		            	System.out.println("Task failed! " + t.getMessage() );
		            	// pass failure message back to caller
		            	lsr.message = t.getMessage();
		            }
		        });
	
			}
			else {
				lsr.status = "idle";			
			}
	
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
		}
		catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<LoadStatusResult>(lsr,HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<LoadStatusResult>(lsr,HttpStatus.OK);
    }
   
    
    // Play manages additions to the playback queue
    // songId= id of the song to play
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/queue", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<List<QueuedSong>> queueAdd(@RequestBody QueueRequest queueRequest ) {

    	List<QueuedSong> queue = null;
    	
    	if (queueRequest == null || queueRequest.songId == null) {
    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.BAD_REQUEST);
    	}
    	
    	if (queuedSongRepository == null){
    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	try {
    		
			// Lookup the Song in mongo, get the details
    		MongoDbHelper mhelp = new MongoDbHelper();
			Song s = mhelp.lookup(queueRequest.songId, songRepository);
			if (s == null) {
				// song not found? caller error
	    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.BAD_REQUEST);
	    	}
			
			List<QueuedSong> query = queuedSongRepository.findBySongId(s.getId());
			if (query == null) {
	    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.BAD_REQUEST);				
			}
			// song is not in the queue, so go ahead and add it
			if (query.size() <= 0) {
				QueuedSong qs = new QueuedSong();
				qs.setSongId(s.getId());
				
				// get the max value of all in the queue
				Integer sequence = mhelp.getMaxQueuedSequence(queuedSongRepository);
				qs.setSequence(++sequence);
				
				// add the song to the end of the queue
				queuedSongRepository.save(qs);
				System.out.println("Song " + s + " Added to playback queue");
			}
			
			// return the whole queue for the response
			queue = queuedSongRepository.findAll();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	if (queue == null) {
    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.OK);
    }
    
    // Play manages additions to the playback queue
    // songId= id of the song to play
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/queue", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<List<QueuedSong>> queueDelete(@RequestBody QueueRequest queueRequest ) {

    	List<QueuedSong> queue = null;
    	
    	if (queueRequest == null || queueRequest.songId == null) {
    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.BAD_REQUEST);
    	}
    	// repo not setup? server error
    	if (queuedSongRepository == null){
    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	try {
    		
			// Lookup the Song in mongo, get the details
    		MongoDbHelper mhelp = new MongoDbHelper();
			Song s = mhelp.lookup(queueRequest.songId, songRepository);
			if (s == null) {
				// song not found? caller error
	    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.BAD_REQUEST);
	    	}
			
			List<QueuedSong> query = queuedSongRepository.findBySongId(s.getId());
			// song is not in the queue, lets call this an error for now but may be ok
			if (query == null || query.size()==0) {
				return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.BAD_REQUEST);
			}
			// delete from the queue
			queuedSongRepository.delete(query.get(0));
			System.out.println("deleted Song " + s + " from playback queue");
			
			// return the whole queue for the response
			queue = queuedSongRepository.findAll();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	return new ResponseEntity<List<QueuedSong>>(queue, HttpStatus.OK);
    }   
    
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/queue", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<QueuedSong>> queueList() {

    	List<QueuedSong> queue = null;

    	if (queuedSongRepository == null){
    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	// return the whole list
    	queue = queuedSongRepository.findAll();
    	
    	if (queue == null) {
    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.OK);
    }
    
    //TODO : implement pagination
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/songs", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<Song>> songsList() {

    	List<Song> songs = null;

    	if (songRepository == null){
    		return new ResponseEntity<List<Song>>(songs,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	// return the whole list
    	songs = songRepository.findAll();
    	
    	if (songs == null) {
    		return new ResponseEntity<List<Song>>(songs,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	return new ResponseEntity<List<Song>>(songs,HttpStatus.OK);
    }
    
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/songs/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Song> songById(@PathVariable("id") String songId) {
    	
    	Song song = null;

    	if (songRepository == null){
    		return new ResponseEntity<Song>(song,HttpStatus.INTERNAL_SERVER_ERROR);
    	}

		MongoDbHelper mhelp = new MongoDbHelper();
		song = mhelp.lookup(songId, songRepository);
    	
    	if (song == null) {
    		return new ResponseEntity<Song>(song,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	return new ResponseEntity<Song>(song,HttpStatus.OK);
    }
}