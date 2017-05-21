# local-music-search

A Java Spring Local Search server.
Uses Lucene, MongoDB, and Jaudiotagger (http://www.jthink.net/jaudiotagger/)

Built using Java 8

Tested with MongoDB version 3.2.7

The rest of the details can be determined from the pom.xml file

Build using maven.

Running the application:
* modify application.properties:
** lucene.indexFolder : location of your lucene index files
** lucene.musicFolder : location of your local music files
** spring.data.mongodb.* : mongodb details

Be sure mongod is running

Endpoints:
* /
** this will list some details about the endpoints
* /search
** parameter : terms
*** the search terms
** parameter : size
*** the number of results 
*** (default is 10)
* /load
** parameter : action
*** action=start
**** kick off a new indexing job
*** action=status
**** provide high level status of the indexing job
