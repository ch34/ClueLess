<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>WebSocket Test</title>
<script type="text/javascript">

var socket;

function setConnected(connected) {
	document.getElementById('connect').disabled = connected;
    document.getElementById('disconnect').disabled = !connected;
    document.getElementById('conversationDiv').style.visibility = connected ? 'visible' : 'hidden';
    document.getElementById('response').innerHTML = '';
}

function connect() {
    if ('WebSocket' in window){
    	console.log('Websocket supported');
        socket = new WebSocket('ws:/localhost:8080/clueless/message/websocket');

        console.log('Connection attempted');

        socket.onopen = function(){
        	console.log('Connection open!');
        	setConnected(true);
        }

        socket.onclose = function(){
        	console.log('Disconnecting connection');
       	}

        socket.onmessage = function (evt) 
        { 
        	var received_msg = evt.data;
        	console.log(received_msg);
        	console.log('message received!');
        	showMessage(received_msg);
        }

        } else {
        	console.log('Websocket not supported');
        }
}

function disconnect() {
	setConnected(false);
    socket.close();
}

function sendName() {
    var message = document.getElementById('message').value;
    socket.send(JSON.stringify({ 'message': message }));
}

function showMessage(message) {
    var response = document.getElementById('response');
    var p = document.createElement('p');
    p.style.wordWrap = 'break-word';
    p.appendChild(document.createTextNode(message));
    response.appendChild(p);
}

</script>
</head>
<body>
<button type="button" id="connect" onclick="connect()">Connect</button>
<button type="button" id="disconnect" onclick="disconnect()">Disconnect</button>
<br/>
<input type="text" id="message" name="message"> <button type="button" id="send" onclick="sendName()">Send Message</button>
<br/>

<div id="conversationDiv">
 <div id="response"> </div>
</div>

</body>
</html>