package com.werdnadoow.data;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface QueuedSongRepository extends MongoRepository<QueuedSong, Long> {
	
	List<QueuedSong> findBySongId(String songId);
	List<QueuedSong> findBySequence(String sequence);
}
