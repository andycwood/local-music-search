package com.werdnadoow;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.werdnadoow.data.LoadStatus;
import com.werdnadoow.data.Song;
import com.werdnadoow.data.SongRepository;


// This class is the Worker thread that 
// manages the indexing of music paths in the music folder
// It also create additional threads of GetMusicDetailsTask
public class LoadMusicTask implements Callable<String> {
	
	private static final Lock lock = new ReentrantLock();
	
    private String indexFolder;
    private String musicFolder;
    private SongRepository songRepository;
	private final LoadStatus statusFlags;
	
	private final ThreadPoolTaskExecutor taskExecutor;
	
    public LoadMusicTask(SongRepository songRepo, String indexFldr, String musicFldr) {
    	songRepository = songRepo;
    	indexFolder = indexFldr;
    	musicFolder = musicFldr;
    	statusFlags = new LoadStatus();
    	   
		// Get the thread pool
		ApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");		
    	taskExecutor = (ThreadPoolTaskExecutor) ctx.getBean("readTaskExecutor");
		((ConfigurableApplicationContext)ctx).close();
		
    }
    
	@Override
	public String call() throws Exception 
	{
		Boolean hasLock = false;
		String ret;
		try 
		{
			// if the lock is set then exit
			if (lock.tryLock(0, TimeUnit.NANOSECONDS))
			{
				statusFlags.setIsRunning(true);;
				hasLock = true;
				System.out.println("acquired lock!");
				
				// This is where the work happens
				if (!load()) 
				{
					ret = "load Music had errors!"; 
				}
				else
				{
					ret = "load Music successful!";
				}
				statusFlags.setIsRunning(false);;
				lock.unlock();
			}
			else
			{
				ret = "Load music already in progress!";
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			ret = "Load music had errors!";
			if (hasLock) {
				lock.unlock();
			}
		}
		System.out.println(ret);
		return ret;
	}
	// this function is managing the various stages of work
	// that are being performed by MusicIndex class
	// go look at MusicIndex for details
	 public Boolean load() 
	 {
	    ApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
		Analyzer analyzer = ctx.getBean("analyzer",Analyzer.class);
		((ConfigurableApplicationContext)ctx).close();
		   
		try (MusicIndex mi = new MusicIndex())
		{	
			if (!mi.initialize(analyzer, indexFolder)) {
				System.out.println("failed Music index initialization");
				return false;
			}
			if (!mi.readMusicFolder(musicFolder))
			{
				System.out.println("failed Music Folder traversal");
				return false;
			}
			
			System.out.println("Files found: " + mi.getMusicFileCount());
			statusFlags.setTrackCount(mi.getMusicFileCount());
			
			if (mi.getMusicFileCount() <= 0) 
				return false;
			

			Boolean ret = true;
			try 
			{
				// load all the ID3 tag info into repository
				if (!storeDetails(mi.getMusicFileList())) 
				{
					ret = false;
				}
				// load repository into search index
				for (Song s : songRepository.findAll()) {
					mi.indexSong(s);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				ret = false;
			}
			if (!ret) {
				System.out.println("failed to store file paths");
				return false;
			}
			if (!mi.save())
			{
				ret = false;
			}
			else
			{
				System.out.println("Successfully saved Music Folder");					
			}
	   } 
	   catch (IOException e) 
	   {
	       e.printStackTrace();
	       return false;
	   }
	   return true;
   }
   
   // hand off to threads : 
   // iterate over the file paths
   // add tasks to collect metadata
   // store result in song repository
   public Boolean storeDetails(List<String> paths) {
	   
	   // getting ready to increment the processed file count
	   statusFlags.setProcessedCount(0);
	   
	   for (String path : paths) {
			
			System.out.println("starting new music file read task");
	        ListenableFuture<Song> listenableFuture = taskExecutor.submitListenable(new GetMusicDetailsTask(path));
	        listenableFuture.addCallback(new ListenableFutureCallback<Song>() {
	        	
	            @Override
	            public void onSuccess(Song s) {
	            	System.out.println("Task returned details: " + s);
	            	
	            	// store song details in repository
	            	// store one song object
	            	// acquire id that corresponds to the path
	            	// fill collection of Song objects
	            	if (s != null) {
	            		try {
	            			List<Song> result = songRepository.findByPath(s.getPath());
	            			if (result.isEmpty()) {
	            				// save song to repository
	            				songRepository.save(s);
	            				
	            				// add song to internal list for future indexing
	            				//songs.add(s);
	            			}
	            			else if (result.size() > 1) {
	            				System.out.println("unexpected duplicate paths found in repository!");
	            			}
	            			else {
	            				// add the id already created in repository
	            				Song tmp = result.get(0);
	            				if (tmp != null) {
	            					s.setId(tmp.getId());
	            				}
	            				songRepository.save(s);
	            				//songs.add(s);
	            			}
	            		}
	            		catch (Exception e) {
	            			e.printStackTrace();
	            		}
	            	}
	            	// at this point music file details are a obtained
	            	// and stored in repository
	            	statusFlags.incrementProcessedCount();
	            }

	            @Override
	            public void onFailure(Throwable t) {
	            	System.out.println("Task failed! " + t.getMessage() );
	            }
	        });

		   
	   }
	   // wait for all the threads to be done
	   try {
			   
		   while (statusFlags.getProcessedCount() < statusFlags.getTrackCount()) {
			   Thread.sleep(1000);
		   }
		} 
		 catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	   
	return true;
   }
}
