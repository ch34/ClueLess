package edu.jhu.clueless;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class GameDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {

	@Autowired
	private SimpMessagingTemplate msgTemplate;
	
	@Override
	public void onApplicationEvent(SessionDisconnectEvent event) {
		StompHeaderAccessor header = StompHeaderAccessor.wrap(event.getMessage());
		
		// The session attributes should contain
		// clientId, sessionid, version (game version),
		// and gameId (provided they were in a game)
		String clientId;
		String gameId;
		
		Map<String, Object> sessionAttrs = header.getSessionAttributes();
		clientId = (null != sessionAttrs.get(GameController.SESSION_CLIENT_ID)) ?
				sessionAttrs.get(GameController.SESSION_CLIENT_ID).toString() : "";
				
		gameId = (null != sessionAttrs.get(GameController.SESSION_GAME_ID)) ?
				sessionAttrs.get(GameController.SESSION_GAME_ID).toString() : "";
		
		/* TODO uncomment and update code when ready to implement
		 * Right now we're just notifying everyone in every game
		 */
		// Now notify the game
//		Game game = GameRegistry.get(gameId);
//		if (null != game){
//			game.removePlayer(clientId);
		//	notify the members of that game only
		//  You could even lookup the player in the game to get
		//  a user friend name for the player
		//  game.getPlayer(clientId).getName();
//			msgTemplate.convertAndSend("/queue/game-"+gameid,
//					"User " + clientId + " has left the game");
		msgTemplate.convertAndSend("/queue/game",
				"User " + clientId + " has left the game");
//		}
		
		// since no logging is configured, we have to just print	
		System.out.printf("User [%s] has been disconnected\n", clientId);
	}

   
}
