import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';

var search_url = "http://localhost:8080/search?terms=";
var queue_url = "http://localhost:8080/queue";
var play_url = "http://localhost:8080/play";
 
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
//<a onClick={ () => this.props.onDelete(this.props.songid)} > &nbsp;X&nbsp; </a>
class QueueRow extends React.Component {
  render() {
    return (
        <tr id={this.props.row} className="queueItem">
        <td>&quot;{this.props.value}&quot;
        &nbsp;by&nbsp;{this.props.artist}&nbsp;
        </td><td>
        <a onClick={ () => this.props.onDelete(this.props.songid)} > &nbsp;X&nbsp; </a>
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


class Queue extends React.Component {
  render() {
    var rows = [];
    var idx = 0;
    if (this.props.Queue != null) {

      var callback = this.props.onDeleteSong;

      this.props.Queue.forEach(function(Song) {
        // use path when title is absent
        // but also add those to the end of the list

        if (Song.title != null) {
          var row = "row" + (idx %2);
         rows.push(<QueueRow row={row} key={Song.id} songid={Song.id} value={Song.title} artist={Song.artist} onDelete={callback} />);
         idx++;
        }
      });
    }
      
    return (
      <table cellSpacing="5">
        <thead>
          <tr>
            <th>Playback Queue</th>          
          </tr>
        </thead>
        <tbody className="QueueList">{rows}</tbody>
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
      Queue: []
    };
    
    this.handleSearchTextInput = this.handleSearchTextInput.bind(this);
    this.queueSong = this.queueSong.bind(this);
    this.deleteSong = this.deleteSong.bind(this);
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
                        this.setState( {Queue: json} )
                    });
                  })
      .catch(e => console.log(e));
    };

// todo : implement play song function

  render() {
    return (
      <div className="App">
        <div className="App-header">
          <h2>Music Search</h2>
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
                Queue={this.state.Queue} onDeleteSong={this.deleteSong}/>
            </div>       
          </div>
        </div>
      </div>
    );
  }
}

export default App;
