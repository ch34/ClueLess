/**
 * Disables / hides selections on load of the page
 */
function startup(){
	hidGameCardSelection();
	// disable the chat feature on load
	$("#chatMessage").prop("disabled",true);
	$("#messageBtn").prop("disabled",true);
	$("#connect").hide();
	$("#disconnect").hide();
	$("#gameInfo").hide();
}

/**
 * 
 * @param myGameId
 */
function connect(myGameId) {
    if ('WebSocket' in window){
    	var protocol = "ws://";
    	var hostname = window.location.hostname;
    	var port = window.location.port;
    	var path = "/clueless/stomp";
    	var url = protocol + hostname;
    	if (port && port != 0 && port != ""){
    		url = url + ":" + port;
    	}
    	url = url + path;
		stompClient = Stomp.client(url);
		
		// connect via websocket. username/password are
		// ignored by Spring as it pulls from HTTP request
		stompClient.connect("","", function(frame1) {
			// connect callback
			//window.console.log("I have connected!");
			// subscribe to game queue
			stompClient.subscribe('/queue/game-'+myGameId, function(frame2) {
				parseGameMessage(frame2.body);
			});
			
			// subscribe to this users queue
			stompClient.subscribe('/queue/user-'+playerId, function(frame3) {
				parseUserMessage(frame3.body);
			});
			
			// enable the chat feature
			$("#chatMessage").prop("disabled",false);
			$("#messageBtn").prop("disabled",false);
			
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

/**
 * disconnects player from the game
 */
function disconnect() {
	$("#joiningGame").show();
	gameId = null; // clear gameId
	character = null; // clear character
	//disable chat
	$("#chatMessage").prop("disabled",true);
	$("#messageBtn").prop("disabled",true);
	// hide game start/leave actions
	$("#connect").hide();
	$("#disconnect").hide();
	// hide gameInfo section
	$("#gameInfo").hide();
	$("#selectedSuspect").html("");
	$("#currentGameId").html("");
	$("#gameInfo").hide();
	
 	if (stompClient){
 		stompClient.disconnect();
 	}
 	
 	// now make a call to selectGame to load
 	// all available games again
 	selectGame();
}

/**
 * Sends a game chat message
 */
function sendChat() {
    clientChatAction.setMessage( $("#chatMessage").val() );
    sendAction(clientChatAction);
    // now clear the typing area
    $("#chatMessage").val("");
}

/**
 * 
 * @param event
 */
function sendChatKeyPress(event){
	var chCode = event.keyCode;
	if ( chCode === 13 ){ // Enter Key
		sendChat();
	}
}

/**
 * Sends a request/action to the server via WebSocket
 * @param action ClientAction to send
 */
function sendAction(action){
	stompClient.send('/app/' + action.getAction(), {}, JSON.stringify(action));
}

function actionMove(direction){
	// TODO
	// update direction before sending
	window.console.log("moving " + direction);
	var loc = clientMoveAction.getLocation();
	
	window.console.log("Old Location " + loc.x + ", " + loc.y);
	if(direction == "Left")
	{
		clientMoveAction.setLocation(loc.x-1, loc.y);
	}
	else if(direction == "Right")
	{
		clientMoveAction.setLocation(loc.x+1, loc.y);
	}
	else if(direction == "Up")
	{
		clientMoveAction.setLocation(loc.x, loc.y+1);
	}
	else if(direction == "Down")
	{
		clientMoveAction.setLocation(loc.x, loc.y-1);
	}
	var newloc = clientMoveAction.getLocation();
	window.console.log("Old Location " + newloc.x + ", " + newloc.y);
	sendAction(clientMoveAction);
}

function actionSuggest(){
	// first check to make sure client is in a room
	var loc = clientSuggestAction.getLocation();
	var inRoom = isPlayerInRoom(loc.x,loc.y);
	if(!inRoom){
		updateChatArea("Sorry, you must be in a room");
		console.log("location is [" + loc.x + ":" + loc.y + "]");
		return;
	}
	var cards = [];
	cards.push( selectSuspect() );
	cards.push( selectWeapon() );
	cards.push(getClientsRoomName(loc.x, loc.y) );
	clientSuggestAction.setCards(cards);
	sendAction(clientSuggestAction);
	hidGameCardSelection();
}


/**
 * 
 * @param locx
 * @param locy
 * @returns {Boolean}
 */
function isPlayerInRoom(locx, locy){
	if( locx == null || locy == null){
		return false;
	}
	// rooms are at the following points
	// {0,0}, {0,2}, {0,4}
	// {2,0}, {2,2}, {2,4}
	// {4,0}, {4,2}, {4,4}
	// x & y must both be either 0, 2, or 4
	// therefore we just check to see if they
	// are both 0, 2, and/or 4
	var rooms = [0,2,4];
	if ( ( $.inArray(locx,rooms) >= 0) &&
		 ( $.inArray(locy,rooms) >= 0)){
		return true;
	}
	
	return false;
}


/**
 * TODO - merge with Muhamads code once done
 */
function getClientsRoomName(locx, locy){
	var myRooms = {
			Conservatory: {x:0 , y:0 , name: "CONSERVATORY"},
			Ballroom: {x:0 , y:2 , name: "BALLROOM"},
			Kitchen:  {x:0 , y:4 , name: "KITCHEN"},
			Library:  {x:2 , y:0 , name: "LIBRARY"},
			Billiard_Room:  {x:2 , y:2 , name: "BILLIARD_ROOM"},
			Dining_Room:  {x:2 , y:4 , name: "DINING_ROOM"},
			Study:  {x:4 , y:0 , name: "STUDY"},
			Hall:  {x:4 , y:2 , name: "HALL"},
			Lounge:  {x:4 , y:4 , name: "LOUNGE"}
	};
	
	var clientRoom;
	for( var room in myRooms) {
		if ( !myRooms.hasOwnProperty(room)){ continue; }
		var obj = myRooms[room];
		if(obj.x == locx && obj.y == locy){
			clientRoom = obj.name;
			window.console.log("clients room is " + clientRoom);
		}
	}
	
	return clientRoom
}

function actionAccuse(){
	var cards = [];
	cards.push( selectSuspect() );
	cards.push( selectWeapon() );
	cards.push( selectRoom() );
	clientAccuseAction.setCards(cards);
	sendAction(clientAccuseAction);
	hidGameCardSelection();
}

function actionEndTurn(){
	sendAction(clientEndTurnAction);
}

function actionRespondSuggest(){
	// TODO
	// selectCard();
	sendAction(clientRespondAction);
}

/**
 * Selects a character pawn for the player
 */
function selectCharacter(){
	var myCharacter = $("#characters").val();
	if (myCharacter){
		character = myCharacter;
		hidGameCardSelection();
	}
}

/**
 * selects a room
 */
function selectRoom() {
	return $("#rooms").val();
}

/**
 * selects a suspect
 */
function selectSuspect() {
	return $("#characters").val();
}

/**
 * selects a weapon
 */
function selectWeapon() {
	return $("#weapons").val();
}

/**
 * Obtains the available games and updates the dropdown list
 * This is called on page load but can be called again later if needed
 */
function selectGame(){
	$.ajax({
		url: "home/getGames", 
		success: function(result){
			// empty list so we don't have duplicates if method
			// is called multiple times
			$("#gameList").empty();
			$.each(result, function(index, item){
				var current = "<option value=" +item + ">" + item + "</option>";
				$("#gameList").append(current);
			});
			hidGameCardSelection();
			showCharacterSelection();
		},
		error: function(result){
			alert("Failed to retrieve game list");
		}
	});
}

/**
 * Creates a new game and joins the player to it
 */
function createGame(){
	// TODO
	// make Ajax call to create game
	// if success then show characters for the user to select
	$.ajax({
		url: "home/createGame", 
		success: function(result){
			// result should just be a string, gameId
			gameId = result;
			// show characters for selection
			showCharacterSelection();
			// make a call to getAvailableGames which will
			// load the newly created game
			selectGame();
			// now set the Available games to the gameId
			//$("#gameList").val(gameId);
		},
		error: function(result){
			updateChatArea(result);
		}
	});
	window.console.log("TODO");
}

/**
 * Joins a player to a game. If not gameid is passed in
 * it looks for a selected game in the available games list
 */
function joinGame(){
	if( !character ){
		updateChatArea("Sorry, please select a character first.");
		return;
	}
	
	if( null == gameId || gameId == "" ){
		gameId = $("#gameList").val();
	  // double check to make sure the selection list was not empty
	  if ( null == gameId || gameId == ""){
		updateChatArea("Sorry, please create a game first");
		return;
	  }
	}
	$.ajax({
		url:'home/joinGame?id='+gameId+"&suspect="+character,
		success: function(result){
			// result should be the gameId but could be anything
			// we really don't care as once we enter here the 
			// client has successfully joined on the server side
			connect(gameId);
			$("#connect").show();
			$("#disconnect").show();
			$("#joiningGame").hide();
			// update the global actions for gameId
			clientMoveAction.setGameId(gameId);
			clientAccuseAction.setGameId(gameId);
			clientRespondAction.setGameId(gameId);
			clientSuggestAction.setGameId(gameId);
			clientChatAction.setGameId(gameId);
			clientEndTurnAction.setGameId(gameId);

			$("#selectedSuspect").append(character);
			$("#currentGameId").append(gameId);
			$("#gameInfo").show();
		},
		error: function(result){
			// show character selection again ??
			showCharacterSelection();
			updateChatArea(result.responseText);
		}
	});
}

/**
 * Makes a request to the Game to begin
 */
function startGame(){	
	var startAction = new ClientAction("start");
	startAction.setGameId(gameId);
	sendAction(startAction);
}

/**
 * 
 */
function showCharacterSelection(){
	$("#gameCards").show(); // show main section
	$("#characterCards").show();
	$("#charactersBtn").show();
}

/**
 * 
 */
function showAccuseSelection(){
	$("#gameCards").show(); // show main section
	$("#characterCars").show();
	$("#roomCards").show();
	$("#weaponCards").show();
	$("#accuseBtn").show();
}

/**
 * 
 */
function showSuggestSelection(){
	// we don't need to show a room because
	// the player must be in a room to make a suggestion
	// so just grab the room he is in instead of showing it
	$("#gameCards").show(); // show main section
	$("#characterCards").show();
	$("#weaponCards").show();
	$("#suggestBtn").show();
}

/**
 *
 */
function showAccuseSelection(){
	$("#gameCards").show(); // show main section
	$("#characterCards").show();
	$("#weaponCards").show();
	$("#roomCards").show();
	$("#accuseBtn").show();
}

/**
 * 
 */
function hidGameCardSelection(){
	// we hide all elements so when others need to show
	// only the options they need will be shown
	$("#gameCards").children().hide();
	$("#gameCards").hide();
}

/**
 * Moves the pawns on the game board
 */
function updateSuspectPawns(){
	// TODO update the pawn locations on the board
	window.console.log("TODO");
}

/**
 * Updates the chat message textarea
 * @param msg message to display
 */
function updateChatArea(msg){
	var time = new Date();
	var timePrefix = time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds();
	msg = timePrefix + ' ' + msg;
	var current = $("#response").val();
	$("#response").val(msg + "\n" + current);
}

/**
 * Draws the users cards on the page
 * @param cards as array
 * @returns
 */
function showPlayersCards(cards){
	// we start by making sure the area is clear
	$("#playerHand").empty();
	
	cards.forEach(function(card,index){
		var newCard = "<div class=\"card " + card + "\"><div>";
		$("#playerHand").append(newCard);
	});
	
//	for( var card in cards){
		// <div class="card MRS_WHITE"></div>
//		var newCard = "<div class=\"card " + card + "\"><div>";
//		$("#playerHand").append(newCard);
//	}
	
}

// Map of functions called upon Server Response
// These functions expect a clientAction object as their parameter
responseActionMap = {};
responseActionMap.move = function(msg){
	// TODO do something with the response
	window.console.log("Move Response: " + msg);
}

responseActionMap.accuse = function(msg){
	// TODO do something with the response
}

responseActionMap.respondsuggest = function(msg){
	// TODO do something with the response
}

responseActionMap.chat = function(msg){
	updateChatArea(msg.message);
}

responseActionMap.start = function(msg){
	// TODO
	// disable un-needed fields
	// notify player game has started
}

responseActionMap.location = function(msg){
	// TODO
	// get players locations from msg
	// to start just notify via chat where the players are
	// To finish, update the pawns on the game board
}

responseActionMap.set_hand = function(msg){
	$("#connect").hide();
	// so we can verify the data
	window.console.log(msg);
	showPlayersCards(msg.cards);
}

/**
 * Processes a Game message returned from the server
 */
function parseGameMessage(msg){
	try {
		// Json returned
		msg = JSON.parse(msg);
		// execute the action
		responseActionMap[msg.action](msg);
	} catch(e){
		// server did not retun a valid JSON string
		// display message to user
		updateChatArea(msg);
	}
}

/**
 * Processes a User message returned from the server
 * This was kept seperate in case a user message needed
 * to be processed differently than a game message
 */
function parseUserMessage(msg){
	// parse and do something meaningful with it
	// kept seperate from parseGameMessage in case
	// we need to do something specific for these
	try {
		// Json returned
		msg = JSON.parse(msg);
		// execute the action
		responseActionMap[msg.action](msg);
	} catch(e){
		// server did not retun a valid JSON string
		// display message to user
		updateChatArea(msg);
	}
}