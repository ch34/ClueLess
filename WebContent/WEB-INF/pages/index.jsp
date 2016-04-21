<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" %>
<%@ page import="java.util.*" %>

<html>
<head>
<title>WebSocket Test</title>
<script src="resources/stomp.min.js"></script>
<script type="text/javascript">

// Globals
// The action vars map to ws MessageMapping
var ACTION_SUGGEST = "suggest";
var ACTION_RESPOND = "respond-suggest";
var ACTION_MOVE = "move";
var ACTION_ACCUSE = "accuse";
var ACTION_CHAT = "chat";
var stompClient;
// let the server generate the id
//var playerId = (new Date()).getTime();
var playerId = "${clientId}";
console.log(playerId);

// This class mimics the JAVA class
// function names must match those of the Java class
// in order for Jackson to convert successfully
class ClientAction {
	constructor(act){
		this.action = act;
		this.playerId = playerId;
		this.gameId = "54321";
	}
	getAction(){ return this.action; }
	getPlayerId(){ return this.playerId; }
	getGameId() { return this.gameId; }
	setGameId(id){ this.gameId = id; }
}

// create global client actions which will be reused multiple times
// and this way keeps memory down
var clientMoveAction = new ClientAction(ACTION_MOVE);
var clientAccuseAction = new ClientAction(ACTION_ACCUSE);
var clientRespondAction = new ClientAction(ACTION_RESPOND);
var clientSuggestAction = new ClientAction(ACTION_SUGGEST);
var clientChatAction = new ClientAction(ACTION_CHAT);

function setConnected(connected) {
	document.getElementById('connect').disabled = connected;
    document.getElementById('disconnect').disabled = !connected;
    document.getElementById('conversationDiv').style.visibility = connected ? 'visible' : 'hidden';
    document.getElementById('response').innerHTML = '';
}

function connect(gameId) {
    if ('WebSocket' in window){
    	var url = "ws://localhost:8080/clueless/stomp";
		stompClient = Stomp.client(url);
		
		// connect via websocket. username/password are
		// ignored by Spring as it pulls from HTTP request
		stompClient.connect("","", function(frame1) {
			// connect callback
			//window.console.log("I have connected!");
			setConnected(true);
			// subscribe to game queue
			stompClient.subscribe('/queue/game-'+gameId, function(frame2) {
				parseGameMessage(frame2.body);
			});
			
			// subscribe to this users queue
			stompClient.subscribe('/queue/user-'+playerId, function(frame3) {
				parseUserMessage(frame3.body);
			});
			
			// Now send the request to join the game
			// If join fails, client should unsubscribe
			// from the game queue
			// since we only need it once we are not creating a global clientAction
			var opt = new ClientAction("joinGame");
			opt.setGameId(gameId);
			stompClient.send("/app/joinGame", {}, JSON.stringify(opt) );
			
		}, function(errorFrame) {
			// error callback
			if (errorFrame && errorFrame.headers){
				alert(errorFrame.headers.message);
			} else {
				alert("Error connecting to game socket");
			}
		});
	} else {
		alert("Sorry, Your Browser does not support WebSockets");
	}
}

function disconnect() {
	setConnected(false);
 	if (stompClient){
 		stompClient.disconnect();
 	}
}

function parseGameMessage(msg){
	// parse and do something meaningful
	if( typeof msg == 'object'){
		// Json returned
		msg = JSON.parse(msg);
		// pullout the data you want to show
		// Example
		if (msg.action == "accuse"){
			// do something
		}
		msg = msg.action + " " + msg.playerId;
	}
	
	document.getElementById("response").innerHTML = msg;
}

function parseUserMessage(msg){
	// parse and do something meaningful with it
	document.getElementById("response").innerHTML = msg;
}

function sendChat() {
    console.log("Sending players a message");
    // TODO - Update ClientAction to have message field
    // Must be done on both JS and JAVA classes
    // This would then support Chat between clients
    
    // clientChatAction.setMessage(document.getElementById("message").value);
    // until updated just store it in a generic object
    var opt = {};
    opt.action = document.getElementById("message").value;
    opt.playerId = playerId;
	stompClient.send('/app/chat', {}, JSON.stringify(opt));
}

function move(){
	// TODO populate clientMoveAction as needed before sending
	stompClient.send('/app/' + clientMoveAction.getAction(), {}, JSON.stringify(clientMoveAction));
}

function suggest(){
	// TODO populate clientSuggestAction as needed before sending
	stompClient.send('/app/' + clientSuggestAction.getAction(), {}, JSON.stringify(clientSuggestAction));
}

function respond(){
	// TODO populate clientRespondAction as needed before sending
	stompClient.send('/app/' + clientRespondAction.getAction(), {}, JSON.stringify(clientRespondAction));
}

function showMessage(message) {
    var response = document.getElementById('response');
    var p = document.createElement('p');
    p.style.wordWrap = 'break-word';
    p.appendChild(document.createTextNode(message));
    response.appendChild(p);
}

function startGame(){
	alert("Starting game with gameId 54321.\n" +
			"Note that the client should Select a game first");
	
	// This next 2 lines should be done in the Select Game section
	<% session.setAttribute("gameId", "54321");%> // needed so we can determine who disconnected
	gameId = "54321";
	
	connect(gameId);
}

</script>
</head>
<body>
Game Version ${version}<br/>
<button type="button" id="connect" onclick="startGame();">Start Game</button>
<button type="button" id="disconnect" disabled="true" onclick="disconnect();">Quit Game</button>
<br/>
<input type="text" id="message" name="message">
<button type="button" id="send" onclick="sendChat();">Send Chat</button>
<br/>
<button type="button" onclick="move();">Move</button> 
<button type="button" onclick="suggest();">Suggest</button> 
<button type="button" onclick="respond();">Respond</button>
<br/>

<div id="conversationDiv">
 <div id="response"> </div>
</div>

</body>
</html>