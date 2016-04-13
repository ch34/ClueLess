package edu.jhu.clueless;

/**
 * Container interface for {@link Constants.Suspect}, {@link Constants.Weapon}, and {@link Constants.Room}
 */
public interface Card {

	/**
	 * Get the type of this card
	 * @return
	 */
	Constants.EntityType getType();

}
