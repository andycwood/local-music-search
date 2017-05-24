# local-music-search

A Java Spring Local Search server.
Uses Lucene, MongoDB, React.js 
and Jaudiotagger (http://www.jthink.net/jaudiotagger/)

Built using Java 8

Tested with MongoDB version 3.2.7

The rest of the details can be determined from the pom.xml file
And the package.json file

Build the Spring server using maven
Build the UI as follows:
cd ./musicSearchUI
npm install

## Configure the application:
* modify application.properties:
  * lucene.indexFolder : location of your lucene index files
  * lucene.musicFolder : location of your local music files
  * spring.data.mongodb.* : mongodb details

* modify beans.xml
  * readTaskExecutor : you may want to adjust thread pool settings here

## Run the application

Be sure mongod is running

Run Java Server
java -jar ./target/musicSearch-0.0.1-SNAPSHOT.jar

## Run React Web front end
cd ./musicSearchUI
npm start

## React front end:
locahost:3000
* there is a simple search box

You won't see any search results until you kick off an indexing job:
http://localhost:8080/load?action=start

## Java Server Endpoints:
* localhost:8080/
  * this will list some details about the endpoints

* localhost:8080/load?action=[start|status]
  * parameter : action=start : kick off indexing
  * parameter : action=status : check status of indexing

* localhost:8080/search?terms=<terms>&size=<size>
  * parameter : terms : the search terms
  * parameter : size : the number of results (default=10)

## Sample Output of an API workflow:
1. Query for music, but none has been indexed yet:
* /search?terms=fancy
```
[]
```

2. Check load status, but we have never done a load:
* /load?action=status
```
{"status":"idle","tracks":0,"tracksprocessed":0}
```

3. Start a load
* /load?action=start
```
{"status":"starting","tracks":0,"tracksprocessed":0}
``` 

4. Check on load status
* /load?action=status
```
{"status":"running","tracks":7534,"tracksprocessed":1221}
```

5. Look for tracks processed to match tracks found:
* /load?action=status
```
{"status":"idle","tracks":7534,"tracksprocessed":7534}
```

6. Query music after load is complete
* /search?terms=fancy
```
[{"id":"591f81989f865d10a250d90f","path":"/Users/andywood/Music/Iggy_Azalea_Fancy_featuring_Charli_XCX.mp3","artist":"Iggy Azalea","album":"Fancy","title":"Fancy featuring Charli XCX"},{"id":"591f819b9f865d10a250dda1","path":"/Users/andywood/Music/iTunes/iTunes Music/Music/Iggy Azalea/Fancy/Fancy featuring Charli XCX.mp3","artist":"Iggy Azalea","album":"Fancy","title":"Fancy featuring Charli XCX"}]
```

TODO : 
* Implement Playback API
* Implement incremental file change detection
* Implement indexing on start up
* Store indexing statistics in mongo

