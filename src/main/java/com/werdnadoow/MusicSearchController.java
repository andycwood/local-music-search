package com.werdnadoow;

import org.springframework.web.bind.annotation.RestController;

import com.werdnadoow.data.LoadStatus;
import com.werdnadoow.data.LoadStatusResult;
import com.werdnadoow.data.MongoDbHelper;
import com.werdnadoow.data.MusicFacts;
import com.werdnadoow.data.MusicFactsRepository;
import com.werdnadoow.data.QueueRequest;
import com.werdnadoow.data.QueuedSong;
import com.werdnadoow.data.QueuedSongRepository;
import com.werdnadoow.data.Song;
import com.werdnadoow.data.SongRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    
    @Autowired
    private MusicFactsRepository musicFactsRepository;
    
    // Wrappers around MongoDB helper functions
    private Song lookupSong(String songId) {
		MongoDbHelper mhelp = new MongoDbHelper();
		return mhelp.lookup(songId, songRepository);
    }
    
    private Integer getMaxQueuedSequence() {
		MongoDbHelper mhelp = new MongoDbHelper();
		return mhelp.getMaxQueuedSequence(queuedSongRepository);
    }
    
    private String getValue(String name) {
    	MongoDbHelper mhelp = new MongoDbHelper();
    	MusicFacts m = mhelp.getMusicFacts(name, musicFactsRepository);
    	return m.getValue();
    }
    
    private void setValue(String name, String value) {
    	MongoDbHelper mhelp = new MongoDbHelper();
    	mhelp.setMusicFacts(name, value, musicFactsRepository);
    }
    
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
				ls.setLoadStartTime();
				setValue("lastLoadStartTime",ls.getLoadStartTime().toString());
				
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
		            	// I am storing the data in mongo partly to avoid concurrency issues
		            	setValue("lastLoadEndTime",ls.getLoadEndTime().toString());
		            	setValue("lastLoadCompletion","SUCCESS");
		            	setValue("lastLoadTotalTracks", ls.getProcessedCount().toString());
		            }
	
		            @Override
		            public void onFailure(Throwable t) {
		            	System.out.println("Task failed! " + t.getMessage() );
		            	setValue("lastLoadCompletion","ERROR");
		            	setValue("lastLoadErrorMessage",t.getMessage());
		            }
		        });
	
			}
			else {
				lsr.status = "idle";			
			}
	
			// How many tracks found by folder crawling?
			if (ls.getTrackCount() < 0)
			{
				lsr.currentLoadTracks=0;
			}
			else 
			{
				lsr.currentLoadTracks = ls.getTrackCount();
			}
			
			// how many tracks processed so far
			if (ls.getProcessedCount() < 0)
			{
				lsr.currentLoadTracksProcessed = 0;
			}
			else 
			{
				lsr.currentLoadTracksProcessed = ls.getProcessedCount();
			}
			
			// When did we start/end the latest load?
			// (pull these from Mongo)
			lsr.loadStartTime = getValue("lastLoadStartTime");
			lsr.loadEndTime = getValue("lastLoadEndTime");
			lsr.lastLoadCompletion = getValue("lastLoadCompletion");
			
			// try to parse the total tracks
			String tracks = getValue("lastLoadTotalTracks");
			if (tracks != null && tracks.length() > 0) {
				try {
					lsr.lastLoadTotalTracks = Integer.parseInt(tracks);
				}
				catch (NumberFormatException e) {
					e.printStackTrace();
					lsr.lastLoadTotalTracks = -1;
				}
			}
			else {
				lsr.lastLoadTotalTracks = -1;
			}

			
			// calculate last load total seconds
			if (getValue("lastLoadStartTime") != null && getValue("lastLoadEndTime") != null) {
				
				try 
				{
					Instant start = Instant.parse(getValue("lastLoadStartTime"));
					Instant end = Instant.parse(getValue("lastLoadEndTime"));
					// if start it less than end, then the last load is done computing
					if (start.isBefore(end)) {
						Long seconds = Duration.between(start,end).getSeconds();
						if (seconds > 0) {
							lsr.lastLoadSeconds = seconds;
						}
						else {
							lsr.lastLoadSeconds= -1L;
						}
					}
					else {
						lsr.lastLoadSeconds = -1L;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					lsr.lastLoadSeconds = -1L;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<LoadStatusResult>(lsr,HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<LoadStatusResult>(lsr,HttpStatus.OK);
    }
   
    
    // POST /queue
    // adds a song to the queue
    // returns the full queue
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
			Song s = lookupSong(queueRequest.songId);
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
				Integer sequence = getMaxQueuedSequence();
				qs.setSequence(++sequence);
				
				// add the song to the end of the queue
				queuedSongRepository.save(qs);
				System.out.println("Song " + s + " Added to playback queue");
				setValue("lastQueueAddEvent",LocalDateTime.now().toString());
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
    
    // DELETE /queue 
    // removes a song from the queue
    // returns the full queue
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
    		List<QueuedSong> query = null;
    		
    		// If there is no request body, then this is a "delete ALL request"

    		if (queueRequest.songId.compareTo("ALL")==0)
    		{
        		query = queuedSongRepository.findAll();
				System.out.println("deleting " + query.size() + " songs from playback queue");
			}	
        	else {
				// Lookup the Song in mongo, get the details
				Song s = lookupSong(queueRequest.songId);
				if (s == null) {
					// song not found? caller error
		    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.BAD_REQUEST);
		    	}
				System.out.println("deleting " + s + " from playback queue");
				query = queuedSongRepository.findBySongId(s.getId());

				// song is not in the queue, its an error
				if (query == null || query.size()==0) {
					return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.BAD_REQUEST);
				}
			}				

			// delete from the queue
			queuedSongRepository.delete(query);
			setValue("lastQueueDeleteEvent",LocalDateTime.now().toString());

			// return the whole queue for the response
			queue = queuedSongRepository.findAll();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		return new ResponseEntity<List<QueuedSong>>(queue,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	return new ResponseEntity<List<QueuedSong>>(queue, HttpStatus.OK);
    }   

    // GET queue endpoint
    // returns list of songs in the queue
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
    // /songs endpoint
    // returns all songs in mongoDB collection "songs"
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
    
    // songs/{id} endpoint
    // returns details for one song
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/songs/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Song> songById(@PathVariable("id") String songId) {
    	
    	Song song = null;

    	if (songRepository == null){
    		return new ResponseEntity<Song>(song,HttpStatus.INTERNAL_SERVER_ERROR);
    	}

		song = lookupSong(songId);
    	
    	if (song == null) {
    		return new ResponseEntity<Song>(song,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	return new ResponseEntity<Song>(song,HttpStatus.OK);
    }
    
    // GET /playing endpoint
    // returns the songId that is currently playing (list containing one song)
    // returns and empty list if no song is playing
    //TODO : implement pagination
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/playing", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<QueueRequest>> playingList() {

    	List<QueueRequest> songs = new ArrayList<QueueRequest>();

    	if (musicFactsRepository == null){
    		return new ResponseEntity<List<QueueRequest>>(songs,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	// return the whole list
    	String songId = getValue("nowPlayingSongId");
    	
    	if (songId != null && songId.length() > 0) {
    		QueueRequest qr = new QueueRequest();
    		qr.songId = songId;
    		songs.add(qr);
    	}
    	return new ResponseEntity<List<QueueRequest>>(songs,HttpStatus.OK);
    }

    // POST /playing
    // set the song that is now playing
    // returns a list of one song
    // songId= id of the song to play
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/playing", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<List<QueueRequest>> PlayingSet(@RequestBody QueueRequest queueRequest ) {

    	List<QueueRequest> playing = new ArrayList<QueueRequest>();
    	
    	if (queueRequest == null || queueRequest.songId == null) {
    		return new ResponseEntity<List<QueueRequest>>(playing,HttpStatus.BAD_REQUEST);
    	}
    	
    	if (musicFactsRepository == null){
    		return new ResponseEntity<List<QueueRequest>>(playing,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	try {
    		
			// Lookup the Song in mongo, get the details
			Song s = lookupSong(queueRequest.songId);
			if (s == null) {
				// song not found? caller error
	    		return new ResponseEntity<List<QueueRequest>>(playing,HttpStatus.BAD_REQUEST);
	    	}
			
			setValue("nowPlayingSongId",queueRequest.songId);
			QueueRequest qr = new QueueRequest();
			qr.songId = queueRequest.songId;
			playing.add(qr);
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		return new ResponseEntity<List<QueueRequest>>(playing,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	return new ResponseEntity<List<QueueRequest>>(playing,HttpStatus.OK);
    }
    
    // DELETE /queue 
    // removes a song from the queue
    // returns the full queue
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/playing", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<List<QueueRequest>> PlayingDelete() {

    	List<QueueRequest> playing = new ArrayList<QueueRequest>();
    	
    	if (musicFactsRepository == null){
    		return new ResponseEntity<List<QueueRequest>>(playing,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	
    	try {    		
			// Lookup the Song in mongo, get the details
			// clear out the song id value
			setValue("nowPlayingSongId","");
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		return new ResponseEntity<List<QueueRequest>>(playing,HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    	return new ResponseEntity<List<QueueRequest>>(playing,HttpStatus.OK);
    }
}