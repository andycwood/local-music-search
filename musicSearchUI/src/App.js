import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';

var search_url = "http://localhost:8080/search?terms=";

class SongRow extends React.Component {

  render() {
    var url = "file:" + this.props.path;
    return (
        <tr id={this.props.row} className="SongItem"><a href={url} className="SongTitle">&quot;{this.props.value}&quot;</a>
        &nbsp;by&nbsp;{this.props.artist}
        &nbsp;on&nbsp;&quot;{this.props.album}&quot;
        </tr>
    );
  }
}

class Songs extends React.Component {
  render() {
    var rows = [];
    var idx = 0;
    if (this.props.Songs != null) {

      this.props.Songs.forEach(function(Song) {
        // use path when title is absent
        // but also add those to the end of the list

        if (Song.title != null) {
          var row = "row" + (idx %2);
         rows.push(<SongRow row={row} key={Song.id} value={Song.title} artist={Song.artist} path={Song.path} album={Song.album} />);
         idx++;
        }
      });
    }
      
    return (
      <table cellSpacing="5">
        <thead>
          <tr>
            <th>Song / Artist / Album</th>          
          </tr>
        </thead>
        <tbody class="SongList">{rows}</tbody>
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
          Search for Music:
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
      Songs: []
    };
    
    this.handleSearchTextInput = this.handleSearchTextInput.bind(this);
  }

  handleSearchTextInput(searchText) {
      this.setState( { searchText : searchText } );

      if (searchText.length <= 0) {
        this.setState( {Songs: null} )
        return;
      }

      fetch(search_url + searchText + "*&size=100") 
      .then( (response) => {

           response.json()
                    .then( (json) => {
                        this.setState( {Songs: json} )
                    });
                  })
      .catch(e => console.log(e));
    };

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
          <div className="Songs">
            <Songs 
              Songs={this.state.Songs}/>
          </div>        
        </div>
      </div>
    );
  }
}

export default App;
