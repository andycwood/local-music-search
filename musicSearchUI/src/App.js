import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';

// search for music
var search_url = "http://localhost:8080/search?terms=";

// view playback queue, add the queue, delete from queue
var queue_url = "http://localhost:8080/queue";

// get song details
var songs_url = "http://localhost:8080/songs/";

// start and stop playing a song
var playing_url = "http://localhost:8080/playing";

// view indexing load statistics
var load_url = "http://localhost:8080/load";
 
class SongRow extends React.Component {
  render() {
    return (
        <tr id={this.props.row} className="SongItem">
        <td className="SongTitle">&quot;{this.props.value}&quot;
        &nbsp;by&nbsp;{this.props.artist}
        &nbsp;on&nbsp;&quot;{this.props.album}&quot;
        </td>
        <td>
        <a onClick={ () => this.props.onQueue(this.props.songid)} > &nbsp;&gt;&gt;&nbsp; </a>
        </td>
        </tr>
    );
  }
}

class Songs extends React.Component {
  render() {
    var rows = [];
    var idx = 0;

    var callback = this.props.onQueueSong;

    if (this.props.Songs != null) {

      this.props.Songs.forEach(function(Song) {
        // use path when title is absent
        // but also add those to the end of the list

        if (Song.title != null) {
          var row = "row" + (idx %2);
         rows.push(<SongRow row={row} key={Song.id} songid={Song.id} value={Song.title} artist={Song.artist} 
          album={Song.album} onQueue={callback} />);
         idx++;
        }
      });
    }
    return (
      <table cellSpacing="5">
        <thead>
          <tr>
            <th>Search results</th>          
          </tr>
        </thead>
        <tbody className="SongList">{rows}</tbody>
      </table>
    );
  }
}

class QueueRow extends React.Component {
    constructor(props) {
    super(props);
    this.state = {
      Song: null
    };
    this.getSongData = this.getSongData.bind(this);
  }

  getSongData(songId) {
    if (songId) {
      if (this.state.Song == null || this.state.Song.songId != songId) {
        fetch(songs_url + songId)
          .then( (response) => {
               response.json()
                        .then( (json) => {
                              this.setState({ Song: json })
                        });
                      })
          .catch(e => console.log(e));  
        }
      }
  }
  
  componentWillMount() {
    if (this.props.Song && this.props.Song.songId) {
      this.getSongData(this.props.Song.songId);
    }
  }

// be careful to not trigger extra fetches when there was no state change
  componentWillUpdate(nextProps, nextState) {
    if (nextProps.Song && nextProps.Song.songId) {
      if (this.props.Song && nextProps.Song.songId != this.props.Song.songId) {
        this.getSongData(nextProps.Song.songId);
      }
    }
  }

  render() {
    var rowid = "row0";

    if (this.state.Song == null)
      return null;

    if (this.props.sequence) {
      rowid = "row" + this.props.sequence%2;
    }
    
    if (this.props.Playing && this.props.Playing.songId == this.state.Song.id) {
      rowid = "playing";
    }    

    return (
        <tr id={rowid} className="queueItem">
        <td>&quot;{this.state.Song.title}&quot;
        &nbsp;by&nbsp;{this.state.Song.artist}&nbsp;
        </td><td>
        <a href="#" onClick={ () => this.props.onDelete(this.state.Song.id)} > &nbsp;X&nbsp; </a>
        <a href="#" onClick={ () => this.props.onPlay(this.state.Song.id)} > &nbsp;>>&nbsp; </a>
        </td>
        </tr>
     );
  }
}

class Queue extends React.Component {
render() {
    // extract the currently playing song
    var songPlaying;
    this.props.Playing.forEach( (s) => { songPlaying = s; });

    // expand the Queue into a list of Songs
    var songs = this.props.Queue.map( Song =>
      <QueueRow key={Song.songId} Song={Song} Playing={songPlaying} sequence={Song.sequence} onDelete={this.props.onDeleteSong} onPlay={this.props.onPlaySong}/>
    );
    return (
      <table cellSpacing="5">
        <thead>
          <tr>
            <th>Playback Queue  <a href="#" onClick={ () => this.props.onStopPlay()} > &nbsp;(stop playback)&nbsp; </a></th>          
          </tr>
        </thead>
        <tbody className="QueueList">{songs}</tbody>
      </table>
    );
  }
}

class SearchForm extends Component {
  constructor(props) {
    super(props);

    this.handleChange = this.handleChange.bind(this);
  }

  handleChange(event) {
      this.props.onSearchTextInput(event.target.value);
  }

  render() {
    return (
      <form className="SearchBox">
        <label>
          Search for Music:&nbsp;
          <input type="text" 
                 value={this.props.searchText} 
                 onChange={this.handleChange} />
        </label>
      </form>
    );
  }
}

class App extends Component {
    constructor(props) {
    super(props);
    this.state = {
      searchText: '',
      Songs: [],
      Queue: [],
      Playing: [],
      Stats: null
    };
    
    this.handleSearchTextInput = this.handleSearchTextInput.bind(this);
    this.queueSong = this.queueSong.bind(this);
    this.deleteSong = this.deleteSong.bind(this);
    this.playSong = this.playSong.bind(this);
    this.stopPlayback = this.stopPlayback.bind(this);
  }

  handleSearchTextInput(searchText) {
      this.setState( { searchText : searchText } );

      if (searchText.length <= 0) {
        this.setState( {Songs: null} )
        return;
      }

      fetch(search_url + searchText + "*&size=50") 
      .then( (response) => {
           response.json()
                    .then( (json) => {
                        this.setState( {Songs: json} )
                    });
                  })
      .catch(e => console.log(e));
    };

// this is called from search results to add a song to the queue
queueSong(songid) {

    if (songid == null)
      return;

    if (songid.length <= 0)
      return;

    // convert response to json
    var reqBody = JSON.stringify({songId: songid});

    // set json headers, post method, send json
    fetch(queue_url,
      {     
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        method: "POST", 
        body: reqBody
      })
      .then( (response) => {
           response.json()
                    .then( (json) => {
                        this.setState( {Queue: json} )
                    });
                  })
      .catch(e => console.log(e));
};

// this is called from the playback queue, to remove a song
deleteSong(songid) {

    if (songid == null)
      return;

    if (songid.length <= 0)
      return;


    // convert response to json
    var reqBody = JSON.stringify({songId: songid});

    // set json headers, post method, send json
    fetch(queue_url,
      {     
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        method: "DELETE", 
        body: reqBody
      })
      .then( (response) => {
           response.json()
                    .then( (json) => {
                        this.setState( {Queue: json} )
                    });
                  })
      .catch(e => console.log(e));
};

componentDidMount() {
  // use read only interface to load playback queue
  fetch(queue_url)
      .then( (response) => {
           response.json()
                    .then( (json) => {
                        this.setState( {Queue: json} );
                    });
                  })
      .catch(e => console.log(e));

  // use read only interface to get active song
  fetch(playing_url)
      .then( (response) => {
           response.json()
                    .then( (json) => {
                        this.setState( {Playing: json} );
                    });
                  })
      .catch(e => console.log(e));      

  // use read only interface to get index loading details
  fetch(load_url)
      .then( (response) => {
           response.json()
                    .then( (json) => {
                        this.setState( {Stats: json} );
                    });
                  })
      .catch(e => console.log(e));  
};

stopPlayback() {

    // set json headers, post method, send json
    fetch(playing_url,
      {     
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        method: "DELETE"
      })
      .then( (response) => {
           response.json()
                    .then( (json) => {
                        this.setState( {Playing: json} )
                    });
                  })
      .catch(e => console.log(e));
};

// this is called from search results to add a song to the queue
playSong(songid) {

    if (songid == null)
      return;

    if (songid.length <= 0)
      return;

    // convert response to json
    var reqBody = JSON.stringify({songId: songid});

    // set json headers, post method, send json
    fetch(playing_url,
      {     
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        method: "POST", 
        body: reqBody
      })
      .then( (response) => {
           response.json()
                    .then( (json) => {
                        this.setState( {Playing: json} )
                    });
                  })
      .catch(e => console.log(e));
};

  render() {
    var tracks;
    var ts;
    if (this.state.Stats) {
      if (this.state.Stats.lastLoadTotalTracks) {
        tracks = this.state.Stats.lastLoadTotalTracks;
      }
      if (this.state.Stats.loadStartTime) {
        ts = this.state.Stats.loadStartTime;
      }
    }
    return (
      <div className="App">
        <div className="App-header">
          <div className="title">
            <h2>Music Server</h2>
          </div>
          <div className="Stats">
            total songs {tracks}, last indexed {ts}
          </div>
        </div>
        <div className="App-body">
          <SearchForm         
            searchText={this.state.searchText}
            onSearchTextInput={this.handleSearchTextInput} 
            />  
          <div className="QueueSearch">
            <div className="Songs">
              <Songs 
                Songs={this.state.Songs} onQueueSong={this.queueSong}/>
            </div>
            <div className="Queue">
              <Queue 
                Queue={this.state.Queue} Playing={this.state.Playing} onDeleteSong={this.deleteSong} onPlaySong={this.playSong} onStopPlay={this.stopPlayback}/>
            </div>       
          </div>
        </div>
      </div>
    );
  }
}

export default App;
