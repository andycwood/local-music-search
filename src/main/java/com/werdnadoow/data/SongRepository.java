package com.werdnadoow.data;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.werdnadoow.data.Song;

// This will be AUTO IMPLEMENTED by Spring into a Bean called songRepository
// CRUD refers Create, Read, Update, Delete

public interface SongRepository extends MongoRepository<Song, Long> {

	List<Song> findByPath(String path);

	List<Song> findById(String id);
}