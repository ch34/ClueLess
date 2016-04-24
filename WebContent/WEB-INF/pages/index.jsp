<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" %>
<%@ page import="java.util.*" %>

<html>
<head>
<title>WebSocket Test</title>
<script src="resources/stomp.min.js"></script>
<link rel="stylesheet" href="http://code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
<link rel="stylesheet" href="resources/clueless.css">
<script src="http://code.jquery.com/jquery-1.10.2.js"></script>
<script src="http://code.jquery.com/ui/1.11.4/jquery-ui.js"></script>
<script src="resources/clueless.js"></script>
<script type="text/javascript">

// Globals
// The action vars
var ACTION_SUGGEST = "suggest";
var ACTION_RESPOND = "respondsuggest";
var ACTION_MOVE = "move";
var ACTION_ACCUSE = "accuse";
var ACTION_CHAT = "chat";
var stompClient;

// let the server generate the id
var playerId = "${clientId}";
var character = "";   // the suspect the client will be playing
var gameId = ""; // set after the client successfully joins a game

//This class mimics the JAVA class
//function names must match those of the Java class
//in order for Jackson to convert successfully
class ClientAction {
	constructor(act){
		this.action = act;
		this.playerId = playerId;
		this.gameId = "";
		this.locationX = 0;
		this.locationY = 0;
		this.message = "";
	}
	getAction(){ return this.action; }
	getPlayerId(){ return this.playerId; }
	getGameId() { return this.gameId; }
	setGameId(id){ this.gameId = id; }
	getLocation(){ return {x:this.locationX,y:this.locationY} }
	setLocation(x,y){this.locationX = x; this.locationY = y}
	getMessage(){ return this.message; }
	setMessage(msg){ this.message = msg; }
}

//create global client actions which will be reused multiple times
//and this way keeps memory down
var clientMoveAction = new ClientAction(ACTION_MOVE);
var clientAccuseAction = new ClientAction(ACTION_ACCUSE);
var clientRespondAction = new ClientAction(ACTION_RESPOND);
var clientSuggestAction = new ClientAction(ACTION_SUGGEST);
var clientChatAction = new ClientAction(ACTION_CHAT);

// calls server for available games
$(document).ready(this.selectGame);

</script>
</head>
<body>
<!--
<div style="float:right;">
Game Version ${version}<br/>
PlayerId <span id="currentPlayerId"></span>
</div>

<table style="width:100%;">
 <tr>
  <td style="width:50%;">
    <span id="joiningGame">
    Available Games <select id="gameList"></select>
	<button type="button" id="joinGame" onclick="joinGame();">Join Game</button>
	<button type="button" id="createGame" onclick="createGame();">Create Game</button>
	</span>
	<span id="startendGame">
	<button type="button" id="connect" onclick="startGame();">Start Game</button>
	<button type="button" id="disconnect" onclick="disconnect();">Leave Game</button>
	</span>
	<div id="playerCards" style="display:none"></div>

	<div id="suspects" style="display:none"></div>
	<div id="weapons" style="display:none"></div>
	<div id="rooms" style="display:none"></div>
  </td>
  <td style="width:50%;">
   Game Board Layout here <br/>
   <div id="toolbar" class="ui-widget-header ui-corner-all">
    <span id="buttonsMove">
     <button id="moveLeft" onclick='actionMove("Left");'></button>
     <button id="moveRight" onclick='actionMove("Right");'></button>
     <button id="moveUp" onclick='actionMove("Up");'></button>
  	 <button id="moveDown" onclick='actionMove("Down");'></button>
  	</span>
  	<button id="suggest" onclick="actionSuggest();">Suggest</button>
  	<button id="respond" onclick="actionRespondSuggest();">Respond</button>
  	<button id="accuse" onclick="actionAccuse();">Accuse</button>
   </div>
  </td>
 </tr>
 <tr>
  <td colspan="2" style="width:100%;">
   <div id="conversationDiv" style="margin-left:5px;">
 	<textarea id="response" rows="5" cols="60"></textarea><br/>
 	<input type="text" size="50" id="message" name="message">
 	<button type="button" id="send" onclick="sendChat();">Send Chat</button>
   </div>
  </td>
 </tr>
</table>
-->
<div class="container">
	<header>
		<div class="title">
			<h1>ClueLess</h1>
		</div>
		<div class="subTitle">
			Game Version ${version}<br/>
			Player ID <span id="currentPlayerId"></span>
		</div>
	</header>
	<div class="clearfix">
		<div class="sidebar">
			<div id="chatArea">
				<textarea id="response" readonly></textarea>
				  <div id="chatInput"> 
					<input type="text" id="chatMessage"/><button type="button" onclick="sendChat();">Send</button>
				  </div>
			</div>
		</div>
		<div class="content">
		    <div class="startupSection">
		      <span id="joiningGame"> Available Games 
    			<select id="gameList"></select>
				<button type="button" id="joinGame" onclick="joinGame();">Join Game</button>
				<button type="button" id="createGame" onclick="createGame();">Create Game</button>
			  </span>
			  <span id="startendGame">
				<button type="button" id="connect" onclick="startGame();">Start Game</button>
				<button type="button" id="disconnect" onclick="disconnect();">Leave Game</button>
			  </span>
		    </div>
			<div class="gameBoard">
			  <h2>Welcome to ClueLess</h2>
			  <p>Game Board Here</p>
			  <div id="toolbar" class="ui-widget-header ui-corner-all">
    			<span id="buttonsMove">
     			  <button id="moveLeft" onclick='actionMove("Left");'></button>
     			  <button id="moveRight" onclick='actionMove("Right");'></button>
     			  <button id="moveUp" onclick='actionMove("Up");'></button>
  	 			  <button id="moveDown" onclick='actionMove("Down");'></button>
  				</span>
  				<button id="suggest" onclick="actionSuggest();">Suggest</button>
  				<button id="respond" onclick="actionRespondSuggest();">Respond</button>
  				<button id="accuse" onclick="actionAccuse();">Accuse</button>
   			  </div>
			</div>
			<div class="gameCards">
				<p>Game Cards Here. Players can select Room,Suspect, or Weapon. 
				This section and options should only be shown depending on the action requested by the player
				</p>
				Characters 
				<select id="characters" name="characters">
					<option value="Miss Scarlet">Miss Scarlet</option>
					<option value="Mrs. Peacock">Mrs. Peacock</option>
					<option value="Mrs. White">Mrs. White</option>
					<option value="Professor Plum">Professor Plum</option>
					<option value="Colonel Mustard">Colonel Mustard</option>
					<option value="Mr. Green">Mr. Green</option>
				</select>
				Rooms 
				<select id="rooms" name="rooms">
					<option value="Kitchen">Kitchen</option>
					<option value="Study">Study</option>
					<option value="Conservatory">Conservatory</option>
					<option value="Dining Room">Dining Room</option>
					<option value="Billiard Room">Billiard Room</option>
					<option value="Ballroom">Ballroom</option>
					<option value="Lounge">Lounge</option>
					<option value="Hall">Hall</option>
					<option value="Library">Library</option>
				</select>
				Weapons 
				<select id="weapons" name="weapons">
					<option value="Candlestick">Candlestick</option>
					<option value="Rope">Rope</option>
					<option value="Knife">Knife</option>
					<option value="Lead Pipe">Lead Pipe</option>
					<option value="Wrench">Wrench</option>
					<option value="Pistol">Pistol</option>
				</select>
			</div>
			<div class="playerHand">
				<p>Players Hand here. Shows the player what cards they have</p>
			</div>
		</div>
	</div>
	<footer>
		<p>Footer contents here if any</p>
	</footer>
</div>

<script type="text/javascript">
//UI settings for the action toolbar
//jquery icons can be found at
//http://www.petefreitag.com/cheatsheets/jqueryui-icons/
$(function() {
    $( "#moveLeft" ).button({
      text: false,
      icons: {
        primary: "ui-icon-arrowthick-1-w"
      }
    });
    $( "#moveRight" ).button({
      text: false,
      icons: {
        primary: "ui-icon-arrowthick-1-e"
      }
    });
    $( "#moveUp" ).button({
        text: false,
        icons: {
          primary: "ui-icon-arrowthick-1-n"
        }
    });
   $( "#moveDown" ).button({
       text: false,
       icons: {
         primary: "ui-icon-arrowthick-1-s"
       }
   });
});

$("#currentPlayerId").append(playerId);
</script>

</body>
</html>