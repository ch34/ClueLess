/**
 * Disables / hides selections on load of the page
 */
function startup(){
	hideAllCardInput();
	// disable the chat feature on load
	$("#chatMessage").prop("disabled",true);
	$("#messageBtn").prop("disabled",true);
	$("#connect").hide();
	$("#disconnect").hide();
	$("#gameInfo").hide();
	$("#toolbar :button").attr("disabled", true);
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

function actionMove(direction) {
	var x = playerLocation.x;
	var y = playerLocation.y;
	var currentRoom = coordsToSquare[x + ',' + y];
	switch (direction) {
		case 'Left': x = x - 1; break;
		case 'Right': x = x + 1; break;
		case 'Up': y = y + 1; break;
		case 'Down': y = y - 1; break;
		case 'secret': if (secretPassageMap[currentRoom]) {
			x = secretPassageMap[currentRoom].x;
			y = secretPassageMap[currentRoom].y;
		} else {
			updateChatArea('Current room does not contain a secret passage', 'error');
			return;
		}
	}
	clientMoveAction.locationX = x;
	clientMoveAction.locationY = y;
	sendAction(clientMoveAction);
}

function actionSuggest(){
	var inRoom = isPlayerInRoom(playerLocation.x, playerLocation.y);
	if(!inRoom){
		updateChatArea("Sorry, you must be in a room", 'error');
		return;
	}

	var cards = [];
	cards.push( selectSuspect() );
	cards.push( selectWeapon() );
	cards.push(coordsToSquare[playerLocation.x + ',' + playerLocation.y]);
	clientSuggestAction.setCards(cards);
	sendAction(clientSuggestAction);
	hideAllCardInput();
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
	hideAllCardInput();
}

function actionEndTurn(){
	sendAction(clientEndTurnAction);
}

function showResponseSelection() {
	hideAllCardInput();

	$("#responseList option[value!='']").remove();
	$.each(playerHand, function(index, card){
		var current = "<option value=" +card + ">" + card + "</option>";
		$("#responseList").append(current);
	});
	$("#responseInput").show();
}

function actionRespondSuggest(disproving){
	if (disproving) {
		clientRespondAction.setCards([ $("#responseList").val() ]);
	} else {
		clientRespondAction.setCards(null);
	}
	sendAction(clientRespondAction);
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
	return $("#suspects").val();
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
			$("#gameList option[value!='']").remove();
			$.each(result, function(index, item){
				var current = "<option value=" +index + ">" + item + "</option>";
				$("#gameList").append(current);
			});
			hideAllCardInput();
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
	var url = "home/createGame";
	var gameName = $("#gameNameInput").val().trim();
	if (gameName.length > 0) {
		url = url + "?name=" + gameName;
	}
	$.ajax({
		url: url,
		success: function(){
			// make a call to getAvailableGames which will
			// load the newly created game
			selectGame();
			// now set the Available games to the gameId
			//$("#gameList").val(gameId);
		},
		error: function(result){
			updateChatArea(result, 'error');
		}
	});
	window.console.log("TODO");
}

/**
 * Joins a player to a game. If not gameid is passed in
 * it looks for a selected game in the available games list
 */
function joinGame(){
	var myCharacter = $("#characters").val();
	if( !myCharacter ){
		updateChatArea("Sorry, please select a character first.", 'error');
		return;
	}
	character = myCharacter;

	gameId = $("#gameList").val();
	gameName = $("#gameList option:selected").text();
	// double check to make sure the selection list was not empty
	if ( null == gameId || gameId == ""){
		updateChatArea("Sorry, please select a game first", 'error');
		return;
	}
	$.ajax({
		url:'home/joinGame?id='+gameId+"&suspect="+character,
		success: function(result){
			// result should be the gameId but could be anything
			// we really don't care as once we enter here the 
			// client has successfully joined on the server side
			connect(gameId);
			$("#connect").show();
			$("#startupSection").hide();
			// update the global actions for gameId
			clientMoveAction.setGameId(gameId);
			clientAccuseAction.setGameId(gameId);
			clientRespondAction.setGameId(gameId);
			clientSuggestAction.setGameId(gameId);
			clientChatAction.setGameId(gameId);
			clientEndTurnAction.setGameId(gameId);

			$("#selectedSuspect").append(characterMap[character]);
			$("#gameName").append(gameName);
			$("#gameInfo").show();
		},
		error: function(result){
			// show character selection again ??
			updateChatArea(result.responseText, 'error');
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
function showSuggestSelection(){
	hideAllCardInput();

	// we don't need to show a room because
	// the player must be in a room to make a suggestion
	// so just grab the room he is in instead of showing it
	$("#proposalInput").show();
	$("#roomCards").hide();
	$("#accuseBtn").hide();
	$("#suspectCards").show();
	$("#weaponCards").show();
	$("#suggestBtn").show();
}

/**
 *
 */
function showAccuseSelection(){
	hideAllCardInput();

	$("#proposalInput").show();
	$("#suggestBtn").hide();
	$("#suspectCards").show();
	$("#weaponCards").show();
	$("#roomCards").show();
	$("#accuseBtn").show();
}

/**
 * 
 */
function hideAllCardInput(){
	// we hide all elements so when others need to show
	// only the options they need will be shown
	$("#proposalInput").hide();
	$("#responseInput").hide();

	$('#suspects').prop('selectedIndex', 0);
	$('#rooms').prop('selectedIndex', 0);
	$('#weapons').prop('selectedIndex', 0);
	$('#responseList').prop('selectedIndex', 0);
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
function updateChatArea(msg, type){
	var time = new Date();
	var timePrefix = time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds();
	msg = timePrefix + ' ' + msg;
	var current = $("#notifications").val();

	var formatted = msg;
	if (type) {
		formatted = '<span class="' + type + '">' + msg + '</span>';
	}
	$("#notifications").append(formatted + '<br>');
	var elem = document.getElementById('data');
	$("#notifications").scrollTop($("#notifications").height());
}

/**
 * Draws the users cards on the page
 * @param cards as array
 * @returns
 */
function showPlayersCards(cards){
	// we start by making sure the area is clear
	$("#playerHand").empty();
	playerHand = [];
	cards.forEach(function(card,index){
		playerHand.push(card);
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
	var suspect = msg.suspect;
	if (suspect == character) {
		playerLocation.x = msg.locationX;
		playerLocation.y = msg.locationY;

		$("#buttonsMove :button").prop("disabled", true);
		$("#buttonsMove :button").addClass("ui-button-disabled ui-state-disabled");
	}
	var newSquare = coordsToSquare[msg.locationX + ',' + msg.locationY];
	$("#" + suspect).remove();
	var newPawnElement = '<div class="pawn" id="' + suspect + '"></div>';
	$('#'+ newSquare).append(newPawnElement);
}

responseActionMap.accuse = function(msg){
	// TODO do something with the response
}

responseActionMap.respondsuggest = function(msg){
	if (msg.cards && msg.cards.length > 0) {
		updateChatArea(msg.message, 'response');
	}
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
	$("#activeGame").show();
	// so we can verify the data
	window.console.log(msg);
	showPlayersCards(msg.cards);
	initializeCoords();
}

responseActionMap.main_turn = function(msg){
	$("#toolbar :button:not(#respond)").prop("disabled", false);
	$("#buttonsMove :button").removeClass("ui-button-disabled ui-state-disabled");

	var x = playerLocation.x;
	var y = playerLocation.y;

	if (!coordsToSquare[x-1 + ',' + y]) {
		$("#moveLeft").prop("disabled", true);
		$("#moveLeft").addClass("ui-button-disabled ui-state-disabled");
	}
	if (!coordsToSquare[x+1 + ',' + y]) {
		$("#moveRight").prop("disabled", true);
		$("#moveRight").addClass("ui-button-disabled ui-state-disabled");
	}
	var possibleY = y - 1;
	if (!coordsToSquare[x + ',' + possibleY]) {
		$("#moveDown").prop("disabled", true);
		$("#moveDown").addClass("ui-button-disabled ui-state-disabled");
	}
	possibleY = y + 1;
	if (!coordsToSquare[x + ',' + possibleY]) {
		$("#moveUp").prop("disabled", true);
		$("#moveUp").addClass("ui-button-disabled ui-state-disabled");
	}

	var currentRoom = coordsToSquare[x + ',' + y];
	if (!secretPassageMap[currentRoom]) {
		$("#movePassage").prop("disabled", true);
		$("#movePassage").addClass("ui-button-disabled ui-state-disabled");
	}

}

responseActionMap.end_turn = function(msg){
	$("#toolbar :button").prop("disabled", true);
	$("#buttonsMove :button").addClass("ui-button-disabled ui-state-disabled");
}

responseActionMap.response_turn = function(msg){
	$("#respond").prop("disabled", false);
}

responseActionMap.end_response = function(msg){
	hideAllCardInput();
	$("#respond").prop("disabled", true);
}

function initializeCoords() {
	switch (character) {
		case 'MISS_SCARLET': playerLocation = {x: 3,y: 4}; break;
		case 'COLONEL_MUSTARD': playerLocation = {x: 4,y: 3}; break;
		case 'MR_GREEN': playerLocation = {x: 1,y: 0}; break;
		case 'PROFESSOR_PLUM': playerLocation = {x: 0,y: 3}; break;
		case 'MRS_WHITE': playerLocation = {x: 3,y: 0}; break;
		case 'MRS_PEACOCK': playerLocation = {x: 0,y: 1};
	}
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
		// Server responded with a simple string, assume it's an update message about game state
		updateChatArea(msg, 'update');
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
		// Server responded with a simple string, assume it's an error message for user
		updateChatArea(msg, 'error');
	}
}