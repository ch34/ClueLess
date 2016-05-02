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
var gameName;
var playerHand;
var playerLocation;

var characterMap = {
	MISS_SCARLET: 'Miss Scarlet',
	MRS_PEACOCK: 'Mrs. Peacock',
	MRS_WHITE: 'Mrs. White',
	PROFESSOR_PLUM: 'Professor Plum',
	COLONEL_MUSTARD: 'Colonel Mustard',
	MR_GREEN: 'Mr. Green'
}

var coordsToSquare = {
	'0,0': 'CONSERVATORY',
	'1,0': 'HALL1',
	'2,0': 'BALLROOM',
	'3,0': 'HALL2',
	'4,0': 'KITCHEN',

	'0,1': 'HALL3',
	'2,1': 'HALL4',
	'4,1': 'HALL5',

	'0,2': 'LIBRARY',
	'1,2': 'HALL6',
	'2,2': 'BILLIARD_ROOM',
	'3,2': 'HALL7',
	'4,2': 'DINING_ROOM',

	'0,3': 'HALL8',
	'2,3': 'HALL9',
	'4,3': 'HALL10',

	'0,4': 'STUDY',
	'1,4': 'HALL11',
	'2,4': 'HALL',
	'3,4': 'HALL12',
	'4,4': 'LOUNGE'
};

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
		<div class="logo">
		</div>
		<div id="gameInfo" class="subTitle">
			<span id="gameName"></span><br/>
			<span id="selectedSuspect"></span>
		</div>
	</header>
	<div class="clearfix">
		<div class="sidebar">
			<div id="chatArea">
				<div id="notifications"></div>
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
				<table id="boardGrid">
					<tr>
						<td id="STUDY"></td>
						<td id="HALL11" class="hallway"></td>
						<td id="HALL"></td>
						<td id="HALL12" class="hallway"><div class="pawn" id="MISS_SCARLET"></div></td>
						<td id="LOUNGE"></td>
					</tr>
					<tr>
						<td id="HALL8" class="hallway"><div class="pawn" id="PROFESSOR_PLUM"></div></td>
						<td class="empty"></td>
						<td id="HALL9" class="hallway"></td>
						<td class="empty"></td>
						<td id="HALL10" class="hallway"><div class="pawn" id="COLONEL_MUSTARD"></div></td>
					</tr>
					<tr>
						<td id="LIBRARY"></td>
						<td id="HALL6" class="hallway"></td>
						<td id="BILLIARD_ROOM"></td>
						<td id="HALL7" class="hallway"></td>
						<td id="DINING_ROOM"></td>
					</tr>
					<tr>
						<td id="HALL3" class="hallway"><div class="pawn" id="MRS_PEACOCK"></div></td>
						<td class="empty"></td>
						<td id="HALL4" class="hallway"></td>
						<td class="empty"></td>
						<td id="HALL5" class="hallway"></td>
					</tr>
					<tr>
						<td id="CONSERVATORY"></td>
						<td id="HALL1" class="hallway"><div class="pawn" id="MR_GREEN"></div></td>
						<td id="BALLROOM"></td>
						<td id="HALL2" class="hallway"><div class="pawn" id="MRS_WHITE"></div></td>
						<td id="KITCHEN"></td>
					</tr>
					</table>
					<div id="toolbar" class="ui-widget-header ui-corner-all">
    			<span id="buttonsMove">
     			  <button id="moveLeft" onclick='actionMove("Left");'></button>
     			  <button id="moveRight" onclick='actionMove("Right");'></button>
     			  <button id="moveUp" onclick='actionMove("Up");'></button>
  	 			  <button id="moveDown" onclick='actionMove("Down");'></button>
  				</span>
						<button id="suggest" onclick="showSuggestSelection();">Suggest</button>
						<button id="respond" onclick="showResponseSelection();">Respond</button>
						<button id="accuse" onclick="showAccuseSelection();">Accuse</button>
						<button id="endTurn" onclick="actionEndTurn();">End turn</button>
					</div>
				<div id="proposalInput">
				<span id="suspectCards">
				<select id="suspects" name="suspects">
					<option value="" disabled selected>Select a suspect</option>
					<option value="MISS_SCARLET">Miss Scarlet</option>
					<option value="MRS_PEACOCK">Mrs. Peacock</option>
					<option value="MRS_WHITE">Mrs. White</option>
					<option value="PROFESSOR_PLUM">Professor Plum</option>
					<option value="COLONEL_MUSTARD">Colonel Mustard</option>
					<option value="MR_GREEN">Mr. Green</option>
				</select>
				</span>
				<span id="roomCards">
				<select id="rooms" name="rooms">
					<option value="" disabled selected>Select a room</option>
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
				<select id="weapons" name="weapons">
					<option value="" disabled selected>Select a weapon</option>
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
				<div id="responseInput">
					<select id="responseList">
						<option value="" disabled selected>Select card</option>
					</select>
					<button type="button" onclick="actionRespondSuggest(true);">Disprove</button>
					<button type="button" onclick="actionRespondSuggest(false);">Cannot disprove</button>
				</div>
				<div class="playerHand" id="playerHand"></div>
			</div>
		</div>
		<div class="detective">
			<div class="rightSidebar-spacer"></div>
			<div class="CASE_FILE"></div>
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
