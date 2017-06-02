package com.werdnadoow.data;

import java.util.List;

public class MongoDbHelper {
 
    // helper function to get a song object from a song id
    public Song lookup(String songid, SongRepository songRepo) {
		// Lookup the Song in mongo, get the details
		Song s = null;
		if (songRepo != null) {
			List<Song> ls = songRepo.findById(songid);
			// if start, then add this Song on top
			// else add to end of queue
			if (ls != null) {
				if (ls.size() > 0) {
					s = ls.get(0);
				}
			}
		}
		return s;
    }
    
    public Integer getMaxQueuedSequence(QueuedSongRepository queuedSongRepo) {
    	Integer max = 0;
    	// TODO: implement this the right way!
    	if (queuedSongRepo != null) {
	    	List<QueuedSong> q = queuedSongRepo.findAll();
	    	for (QueuedSong s : q) {
	    		if (s.getSequence() > max) {
	    			max = s.getSequence();
	    		}
	    	}
    	}
    	return max;
    }
    // assume there is only one of any name
    public MusicFacts getMusicFacts(String name, MusicFactsRepository musicFactsRepo) {
    	if (musicFactsRepo != null) {
    		List<MusicFacts> mf = musicFactsRepo.findById(name);
    		if (mf != null) {
    			if (mf.size() > 0) {
    				return mf.get(0);
    			}
    		}
    	}
    	return new MusicFacts(name);
    }
    
    // set will delete and re-insert. will delete duplicates if found
    public void setMusicFacts(String name, String value,  MusicFactsRepository musicFactsRepo) {
    	
    	if (musicFactsRepo != null) {
    		// delete the old values
    		List<MusicFacts> mf = musicFactsRepo.findById(name);
    		if (mf != null) {
    			if (mf.size() > 0) {
    				musicFactsRepo.delete(mf);
    			}
    		}
    		MusicFacts f = new MusicFacts(name, value);

        	// insert the new
        	musicFactsRepo.save(f);
    	}
    }
}
