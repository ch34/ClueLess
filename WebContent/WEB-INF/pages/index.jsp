<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" %>
<%@ page import="java.util.*" %>

<html>
<head>
<title>ClueLess</title>
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
var ACTION_END_TURN = "end_turn";
var ACTION_CHAT = "chat";
var HAND_UPDATE = "set_hand";
var stompClient;

// let the server generate the id
var playerId = "${clientId}";
var character;   // the suspect the client will be playing
var gameId; // set after the client successfully joins a game

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
		this.cards = [];
		this.suspect = "";
	}
	getAction(){ return this.action; }
	getPlayerId(){ return this.playerId; }
	getGameId() { return this.gameId; }
	setGameId(id){ this.gameId = id; }
	getLocation(){ return {x:this.locationX,y:this.locationY} }
	setLocation(x,y){this.locationX = x; this.locationY = y}
	getMessage(){ return this.message; }
	setMessage(msg){ this.message = msg; }
	setCards(cards){ this.cards = cards; }
	getCards(){ return this.cards; }
	getSuspect() { return this.suspect; }
}

//create global client actions which will be reused multiple times
//and this way keeps memory down
var clientMoveAction = new ClientAction(ACTION_MOVE);
var clientAccuseAction = new ClientAction(ACTION_ACCUSE);
var clientRespondAction = new ClientAction(ACTION_RESPOND);
var clientSuggestAction = new ClientAction(ACTION_SUGGEST);
var clientEndTurnAction = new ClientAction(ACTION_END_TURN);
var clientChatAction = new ClientAction(ACTION_CHAT);

// calls server for available games
$(document).ready(this.selectGame);

</script>
</head>
<body>
<div class="container">
	<header>
		<div class="title">
			<h1>ClueLess</h1>
		</div>
		<div class="subTitle">
			Player ID: <span id="currentPlayerId"></span>
			<div id="gameInfo">
				Current Game: <span id="currentGameId"></span><br/>
				My Character: <span id="selectedSuspect"></span>
			</div>
		</div>
	</header>
	<div class="clearfix">
		<div class="sidebar">
			<div id="chatArea">
				<textarea id="response" readonly></textarea>
				  <div id="chatInput"> 
					<input type="text" id="chatMessage" onkeyup="sendChatKeyPress(event,this);"/><button type="button" id="messageBtn" onclick="sendChat();">Send</button>
				  </div>
			</div>
		</div>
		<div class="content">
		    <div id="startupSection">
			  <div id="createGameSection">
    			<input id="gameNameInput" placeholder="Enter name of new game..."></input>
				<button type="button" id="createGame" onclick="createGame();">Create Game</button>
			  </div>
				<select id="characters" name="characters">
					<option value="" disabled selected>Select your character</option>
					<option value="MISS_SCARLET">Miss Scarlet</option>
					<option value="MRS_PEACOCK">Mrs. Peacock</option>
					<option value="MRS_WHITE">Mrs. White</option>
					<option value="PROFESSOR_PLUM">Professor Plum</option>
					<option value="COLONEL_MUSTARD">Colonel Mustard</option>
					<option value="MR_GREEN">Mr. Green</option>
				</select>
    			<select id="gameList">
					<option value="" disabled selected>Select game</option>
				</select>
				<button type="button" id="joinGame" onclick="joinGame();">Join Game</button>
		    </div>
			<div>
				<button type="button" id="connect" onclick="startGame();">Start Game</button>
				<button type="button" id="disconnect" onclick="disconnect();">Leave Game</button>
			</div>
			<div id="activeGame">
				<div class="gameBoard">
					<p>Game Board Here</p>
					<div id="toolbar" class="ui-widget-header ui-corner-all">
    			<span id="buttonsMove">
     			  <button id="moveLeft" onclick='actionMove("Left");'></button>
     			  <button id="moveRight" onclick='actionMove("Right");'></button>
     			  <button id="moveUp" onclick='actionMove("Up");'></button>
  	 			  <button id="moveDown" onclick='actionMove("Down");'></button>
  				</span>
						<button id="suggest" onclick="showSuggestSelection();">Suggest</button>
						<button id="respond" onclick="actionRespondSuggest();">Respond</button>
						<button id="accuse" onclick="showAccuseSelection();">Accuse</button>
						<button id="endTurn" onclick="actionEndTurn();">End turn</button>
					</div>
				</div>
				<div class="gameCards" id="gameCards">
					<p>Select your Card(s)</p>
				<span id="suspectCards">
				Suspects
				<select id="suspects" name="suspects">
					<option value="MISS_SCARLET">Miss Scarlet</option>
					<option value="MRS_PEACOCK">Mrs. Peacock</option>
					<option value="MRS_WHITE">Mrs. White</option>
					<option value="PROFESSOR_PLUM">Professor Plum</option>
					<option value="COLONEL_MUSTARD">Colonel Mustard</option>
					<option value="MR_GREEN">Mr. Green</option>
				</select>
				</span>
				<span id="roomCards">
				Rooms
				<select id="rooms" name="rooms">
					<option value="KITCHEN">Kitchen</option>
					<option value="STUDY">Study</option>
					<option value="CONSERVATORY">Conservatory</option>
					<option value="DINING_ROOM">Dining Room</option>
					<option value="BILLIARD_ROOM">Billiard Room</option>
					<option value="BALLROOM">Ballroom</option>
					<option value="LOUNGE">Lounge</option>
					<option value="HALL">Hall</option>
					<option value="LIBRARY">Library</option>
				</select>
				</span>
				<span id="weaponCards">
				Weapons
				<select id="weapons" name="weapons">
					<option value="CANDLESTICK">Candlestick</option>
					<option value="ROPE">Rope</option>
					<option value="KNIFE">Knife</option>
					<option value="LEAD_PIPE">Lead Pipe</option>
					<option value="WRENCH">Wrench</option>
					<option value="PISTOL">Pistol</option>
				</select>
				</span>
					<button id="accuseBtn" type="button" onclick="actionAccuse();">OK</button>
					<button id="suggestBtn" type="button" onclick="actionSuggest();">OK</button>
				</div>
				<div class="playerHand" id="playerHand"></div>
			</div>
		</div>
		<div class="detective">
			<div class="rightSidebar-spacer"></div>
			<div class="CASE_FILE"></div>
			<div class="card CARD_BACK"></div>
		</div>
	</div>
	<footer>
	    <p>
		Team Centurions <br/>
		Foundation of Software Engineering<br/>
 		(EN.605.401.91.SP16)<br/>
 		Game Version ${version}
 		</p>
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
startup();
</script>

</body>
</html>
