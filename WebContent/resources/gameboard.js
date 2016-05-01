// global variables
var ctx;
var roomWidth = 100;
var roomHeight = 100;
var hallWidth = roomWidth*0.3;
var hallHeight = roomHeight*0.3;
var canvasWidth = roomWidth*5;
var canvasHeight = roomHeight*5;

var greenPawn = new Image();
var mustardPawn = new Image();
var peacockPawn = new Image();
var plumPawn = new Image();
var scarletPawn = new Image();
var whitePawn = new Image();


var gameSquares = {
	Top_Left: {
		x: 0,
		y: 0
	},
	Top_Right: {
		x: canvasWidth,
		y: 0
	},
	Bottom_Left: {
		x: 0,
		y: canvasHeight
	},
	Bottom_Right: {
		x: canvasWidth,
		y: canvasHeight
	},
	Center_X: {
		x: canvasWidth*0.5,
		y: 0
	},
	Center_Y: {
		x: 0,
		y: canvasHeight*0.5
	}
 };
  
 var rooms = {
	Conservatory: {
		x: gameSquares.Top_Left.x + roomWidth*0.5,
		y: gameSquares.Top_Left.y + roomHeight*0.5
	},
	Ballroom: {
		x: gameSquares.Top_Left.x + roomWidth*0.5,
		y: canvasHeight*0.5
	},
	Kitchen: {
		x: gameSquares.Bottom_Left.x + roomWidth*0.5,
		y: gameSquares.Bottom_Left.y - roomWidth*0.5
	},
	Library: {
		x: canvasWidth*0.5,
		y: gameSquares.Top_Left.y + roomHeight*0.5
	},
	Billiard_Room: {
		x: canvasWidth*0.5,
		y: canvasHeight*0.5
	},
	Dining_Room: {
		x: canvasWidth*0.5,
		y: gameSquares.Bottom_Left.y - roomWidth*0.5
	},
	Study: {
		x: gameSquares.Top_Right.x - roomWidth*0.5,
		y: gameSquares.Top_Right.y - roomWidth*0.5
	},
	Hall: {
		x: gameSquares.Top_Right.x - roomWidth*0.5,
		y: canvasHeight*0.5
	},
	Lounge: {
		x: gameSquares.Bottom_Right.x + roomWidth*0.5,
		y: gameSquares.Bottom_Right.y - roomWidth*0.5
	}
 };
  
 var halls = {
	Hall_1: {
		x: rooms.Conservatory.x,
		y: gameSquares.Center_Y.y*0.5
	},
	Hall_2: {
		x: rooms.Conservatory.x,
		y: gameSquares.Center_Y.y*0.5 + gameSquares.Center_Y.y
	},
	Hall_3: {
		x: gameSquares.Center_X.x*0.5,
		y: rooms.Conservatory.x
	},
	Hall_4: {
		x: gameSquares.Center_X.x*0.5,
		y: gameSquares.Center_Y.y
	},
	Hall_5: {
		x: gameSquares.Center_X.x*0.5,
		y: rooms.Kitchen.y
	},
	Hall_6: {
		x: gameSquares.Center_X.x,
		y: gameSquares.Center_Y.y*0.5
	},
	Hall_7: {
		x: gameSquares.Center_X.x,
		y: gameSquares.Center_Y.y*0.5 + gameSquares.Center_Y.y
	},
	Hall_8: {
		x: gameSquares.Center_X.x*0.5 + gameSquares.Center_X.x,
		y: rooms.Conservatory.x
	},
	Hall_9: {
		x: gameSquares.Center_X.x*0.5 + gameSquares.Center_X.x,
		y: gameSquares.Center_Y.y
	},
	Hall_10: {
		x: gameSquares.Center_X.x*0.5 + gameSquares.Center_X.x,
		y: rooms.Kitchen.y
	},
	Hall_11: {
		x: rooms.Study.x,
		y: gameSquares.Center_Y.y*0.5
	},
	Hall_12: {
		x: rooms.Study.x,
		y: gameSquares.Center_Y.y*0.5 + gameSquares.Center_Y.y
	}
 };
 
 var pawns = {
	Green:{
		suspect: "MR_GREEN",
		image: "resources/images/pieces/green_pawn.png",
		defaultLocation:{
			x: 1,
			y: 0
		}
	},
	Mustard: {
		suspect: "COLONEL_MUSTARD",
		image: "resources/images/pieces/mustard_pawn.png",
		defaultLocation:{
			x: 4,
			y: 3
		}
	},
	Peacock: {
		suspect: "MRS_PEACOCK",
		image: "resources/images/pieces/peacock_pawn.png",
		defaultLocation:{
			x: 0,
			y: 1
		}
	},
	Plum: {
		suspect: "PROFESSOR_PLUM",
		image: "resources/images/pieces/plum_pawn.png",
		defaultLocation:{
			x: 0,
			y: 3
		}
	},
	Scarlet: {
		suspect: "MISS_SCARLET",
		image: "resources/images/pieces/scarlett_pawn.png",
		defaultLocation:{
			x: 3,
			y: 4
		}
	},
	White: {
		suspect: "MRS_WHITE",
		image: "resources/images/pieces/white_pawn.png",
		defaultLocation:{
			x: 3,
			y: 0
		}
	}
 };
	

function draw() {
  ctx = document.getElementById('canvas').getContext('2d');
  for (var i=0;i<5;i++) {
	for (var j=0;j<5;j++) {
	  ctx.save();
	  ctx.fillStyle = 'rgb('+(51*i)+','+(255-51*i)+',255)';
	  ctx.translate(j*roomWidth,i*roomHeight);
	  if(i%2==0 && j%2!=0){
	  ctx.fillRect(0,rooms.Conservatory.x-hallWidth*0.5,roomWidth,hallWidth);
	  }else if(j%2==0 && i%2!=0){
	  ctx.fillRect(rooms.Conservatory.y-hallWidth*0.5,0,hallWidth,roomHeight);
	  }else{
	  if(j%2==0 && i%2==0){
	 
			ctx.fillRect(0,0,roomWidth,roomHeight);
	  }
   }
		 ctx.restore();
	}
  }
  //alert("Center of canvas is: " + gameSquares.Center_X.x + ", " + gameSquares.Center_Y.y);
  //alert("The location of Hall 1 x,y is: " + halls.Hall_1.x + ", " + halls.Hall_1.y);
  //drawCircle(rooms.Kitchen.x, rooms.Kitchen.y);
  //drawCircle(55, rooms.Library.y);
  //drawCircle(halls.Hall_12.x, halls.Hall_12.y);
  drawPawn();
  
}

function drawPawn(){
	//drawing green pawn
	greenPawn.onload = function () {
		ctx.drawImage(greenPawn, pawns.Green.defaultLocation.x, pawns.Green.defaultLocation.y);
	}
	greenPawn.src = pawns.Green.image;
	
	//drawing mustard pawn
	mustardPawn.onload = function () {
		ctx.drawImage(mustardPawn, pawns.Mustard.defaultLocation.x, pawns.Mustard.defaultLocation.y, 40, 40);
	}
	mustardPawn.src = pawns.Mustard.image;
	
	//drawing peacock pawn
	peacockPawn.onload = function () {
		ctx.drawImage(peacockPawn, pawns.Peacock.defaultLocation.x, pawns.Peacock.defaultLocation.y);
	}
	peacockPawn.src = pawns.Peacock.image;
	
	//drawing plum pawn
	plumPawn.onload = function () {
		ctx.drawImage(plumPawn, pawns.Plum.defaultLocation.x, pawns.Plum.defaultLocation.y);
	}
	plumPawn.src = pawns.Plum.image;
	
	//drawing scarllet pawn
	scarletPawn.onload = function () {
		ctx.drawImage(scarletPawn, pawns.Scarlet.defaultLocation.x, pawns.Scarlet.defaultLocation.y);
	}
	scarletPawn.src = pawns.Scarlet.image;
	
	//drawing white pawn
	whitePawn.onload = function () {
		ctx.drawImage(whitePawn, pawns.White.defaultLocation.x, pawns.White.defaultLocation.y);
	}
	whitePawn.src = pawns.White.image;
}

// this function will be used to move pawn for different players
function movePawn(){
	
}

function drawCircle(x, y){
	//alert("The x value" + x);
	ctx.beginPath();
	ctx.arc(x,y,10,0,2*Math.PI);
	ctx.stroke();
}