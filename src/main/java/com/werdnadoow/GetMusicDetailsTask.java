package com.werdnadoow;

import java.io.File;
import java.util.concurrent.Callable;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import com.werdnadoow.data.Song;

public class GetMusicDetailsTask implements Callable<Song> {
	
	private String path;
	
	public GetMusicDetailsTask(String path) {
		this.path  = path;
	}

	@Override
	public Song call() throws Exception {
		Song s = new Song();
		s.setPath(path);
		
		try {

			AudioFile f = AudioFileIO.read(new File(path));
			if (f != null) {
				Tag tag = f.getTag();
				if (tag != null) {
					String artist = tag.getFirst(FieldKey.ARTIST);
					if (artist != null && artist.length()>0)
						s.setArtist(artist);
					
					String title = tag.getFirst(FieldKey.TITLE);
					if (title != null && title.length()>0)
						s.setTitle(title);			
		
					String album = tag.getFirst(FieldKey.ALBUM);
					if (album != null && album.length()>0)
						s.setAlbum(album);
				}
			}

		}
		catch(Exception e) {
			e.printStackTrace();
			// The path is most critical
		}
		return s;
	}

}
