package edu.jhu.clueless;

import edu.jhu.clueless.Constants.EntityType;

/**
 * Container interface for {@link Constants.Suspect}, {@link Constants.Weapon}, and {@link Constants.Room}
 */
public interface Card {

	/**
	 * Get the type of this card
	 * @return
	 */
	EntityType getType();

}
