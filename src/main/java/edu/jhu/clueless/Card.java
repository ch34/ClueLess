package edu.jhu.clueless;

import edu.jhu.clueless.Constants.EntityType;
import edu.jhu.clueless.Constants.Suspect;
import edu.jhu.clueless.Constants.Room;
import edu.jhu.clueless.Constants.Weapon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Container interface for {@link Constants.Suspect}, {@link Constants.Weapon}, and {@link Constants.Room}
 */
public interface Card {

	/**
	 * @return The type of this card
	 */
	EntityType getType();

	/**
	 * @return Full set of all Clue cards.
	 */
	static Set<Card> getFullSet() {
		Set<Card> cards = new HashSet<>();
		Collections.addAll(cards, Suspect.values());
		Collections.addAll(cards, Room.values());
		Collections.addAll(cards, Weapon.values());
		return cards;
	}

}
