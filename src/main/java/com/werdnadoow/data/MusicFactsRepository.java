package com.werdnadoow.data;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MusicFactsRepository extends MongoRepository<MusicFacts, String> {

	List<MusicFacts> findById(String id);
}