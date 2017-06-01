package com.werdnadoow.data;

import java.util.List;

public class MongoDbHelper {
 
    // helper function to get a song object from a song id
    public Song lookup(String songid, SongRepository songRepository) {
		// Lookup the Song in mongo, get the details
		Song s = null;
		if (songRepository != null) {
			List<Song> ls = songRepository.findById(songid);
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
    
    public Integer getMaxQueuedSequence(QueuedSongRepository queuedSongRepository) {
    	Integer max = 0;
    	// TODO: implement this the right way!
    	if (queuedSongRepository != null) {
	    	List<QueuedSong> q = queuedSongRepository.findAll();
	    	for (QueuedSong s : q) {
	    		if (s.getSequence() > max) {
	    			max = s.getSequence();
	    		}
	    	}
    	}
    	return max;
    }
}
