# local-music-search

A Java Spring Local Search server.
Uses Lucene, MongoDB, and Jaudiotagger (http://www.jthink.net/jaudiotagger/)

Built using Java 8

Tested with MongoDB version 3.2.7

The rest of the details can be determined from the pom.xml file

Build using maven.

## Configure the application:
* modify application.properties:
  * lucene.indexFolder : location of your lucene index files
  * lucene.musicFolder : location of your local music files
  * spring.data.mongodb.* : mongodb details

* modify beans.xml
  * readTaskExecutor : you may want to adjust thread pool settings here

## Run the application

Be sure mongod is running

Endpoints:
* /
  * this will list some details about the endpoints

* /load?action=[start|status]
  * parameter : action=start : kick off indexing
  * parameter : action=status : check status of indexing

* /search?terms=<terms>&size=<size>
  * parameter : terms : the search terms
  * parameter : size : the number of results (default=10)

TODO : 
* Implement Playback API
* Implement React.js UI

