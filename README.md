# local-music-search

A Java Spring Local Search server.
Uses Lucene, MongoDB, React.js 
and Jaudiotagger (http://www.jthink.net/jaudiotagger/)

Built using Java 8

Tested with MongoDB version 3.2.7

The rest of the details can be determined from the pom.xml file
and the package.json file

* Build the Spring server using maven
* Build the UI as follows:
  * cd ./musicSearchUI
  * npm install

## Configure the application:
* modify application.properties:
  * lucene.indexFolder : location of your lucene index files
  * lucene.musicFolder : location of your local music files
  * spring.data.mongodb.
    * : mongodb details

* modify beans.xml
  * readTaskExecutor : you may want to adjust thread pool settings here

## Run the application

Be sure mongod is running

Run Java Server
* java -jar ./target/musicSearch-0.0.1-SNAPSHOT.jar

## Run React Web front end
* cd ./musicSearchUI
* npm start

## React front end:
* locahost:3000
* there is a simple search box
  * search is dynamic : every letter you add or remove updates the results
* click on the ">>" in search results to add to the playback queue
* click on the "X" in the playback queue to remove the item from the queue
* click on the ">>" in the playback queue to start playback

You won't see any search results until you kick off an indexing job:
* http://localhost:8080/load?action=start

## Java Server Endpoints:
* GET localhost:8080/
  * this will list some details about the endpoints

* GET localhost:8080/load?action=[start|status]
  * parameter : action=start : kick off indexing
  * parameter : action=status : check status of indexing

* GET localhost:8080/search?terms=<terms>&size=<size>
  * parameter : terms : the search terms
  * parameter : size : the number of results (default=10)

* GET localhost:8080/queue
  * this lists the songs in the playback queue

* POST localhost:8080/queue
  * add a song to the queue
  * json body is : { "songId": "<song id>"}

* DELETE localhost:8080/queue
  * remove a song from the queue
  * json body is : { "songId": "<song id>"}

* GET localhost:8080/songs
  * list ALL songs
  * this is slow to return since it is not paginated yet
  * it is not used by the UI yet

* GET localhost:8080/songs/{id}
  * returns the details for one song 

* GET localhost:8080/playing
  * returns the currently playing song

* POST localhost:8080/playing
  * starts playback of a song in the queue
  * json body is : { "songId": "<song id>"}

* DELETE localhost:8080/playing
  * stops playback

## Sample Output of an API workflow:
1. Query for music, but none has been indexed yet:
* GET /search?terms=fancy
```
[]
```

2. Check load status, but we have never done a load:
* GET /load?action=status
```
{"status":"idle","tracks":0,"tracksprocessed":0}
```

3. Start a load
* GET /load?action=start
```
{"status":"starting","currentLoadTracks":0,"currentLoadTracksProcessed":0,"loadStartTime":null,"loadEndTime":null,"lastLoadCompletion":null,"lastLoadSeconds":null,"lastLoadTotalTracks":-1}
``` 

4. Check on load status
* GET /load?action=status
```
{"status":"running","currentLoadTracks":7534,"currentLoadTracksProcessed":1221,"loadStartTime":"2017-06-02T04:04:14.296Z","loadEndTime":null,"lastLoadCompletion":null,"lastLoadSeconds":null,"lastLoadTotalTracks":-1}
```

5. Look for tracks processed to match tracks found:
* GET /load?action=status
```
{"status":"idle","currentLoadTracks":7534,"currentLoadTracksProcessed":7534,"loadStartTime":"2017-06-02T04:04:14.296Z","loadEndTime":"2017-06-02T04:07:21.799Z","lastLoadCompletion":"SUCCESS","lastLoadSeconds":187,"lastLoadTotalTracks":7534}
```

6. Query music after load is complete
* GET /search?terms=fancy
```
[{"id":"591f81989f865d10a250d90f","path":"/Users/andywood/Music/Iggy_Azalea_Fancy_featuring_Charli_XCX.mp3","artist":"Iggy Azalea","album":"Fancy","title":"Fancy featuring Charli XCX"},{"id":"591f819b9f865d10a250dda1","path":"/Users/andywood/Music/iTunes/iTunes Music/Music/Iggy Azalea/Fancy/Fancy featuring Charli XCX.mp3","artist":"Iggy Azalea","album":"Fancy","title":"Fancy featuring Charli XCX"}]
```

7. Add a song to the playback queue
* POST /queue {"songId": "591f81989f865d10a250d90f"}
```
[{"id":"592f7d769f865d3457f7ab95","songId":"591f81989f865d10a250d90f","sequence":7}]
```

7.1 React UI will call songs API to get details:
* GET /songs/591f81989f865d10a250d90f
```
{"id":"591f81989f865d10a250d90f","path":"/Users/andywood/Music/Iggy_Azalea_Fancy_featuring_Charli_XCX.mp3","artist":"Iggy Azalea","album":"Fancy","title":"Fancy featuring Charli XCX"}
```
8. Start playback of song
* POST /playing {"songId": "591f81989f865d10a250d90f"}

9. Stop playback of song
* DELETE /playing

10. Remove song from the playback queue
* DELETE /queue {"songId": "591f81989f865d10a250d90f"}
```
[]
```

Mongo Collections:
1. songs
* the details of all songs found in your music folder

2. queuedSong
*  the list of songs in the playback queue

3. musicFacts
* general collection of settings (name / value pairs)

React Components
1. SearchForm
* captures the search characters and performs search for each change to the search box

2. Songs
* the list of search results

3. Queue
* the list of songs in the playback queue

4. SongRow
* one row in the list of search results

5. QueueRow
* one row in the playback queue

TODO : 
* Implement Playback API
* Implement incremental file change detection
* Implement indexing on start up

