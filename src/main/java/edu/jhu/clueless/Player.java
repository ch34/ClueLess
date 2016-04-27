package edu.jhu.clueless;

import edu.jhu.clueless.Constants.Suspect;

import java.util.*;

public class Player implements Comparable<Player> {

	private String id;
	private Suspect suspect;
	private Set<Card> cards;
	private boolean active;

	public Player(Suspect suspect) {
		this(UUID.randomUUID().toString(), suspect);
	}

	public Player(String id, Suspect suspect) {
		this.id = id;
		this.suspect = suspect;
		this.cards = new HashSet<>();
		active = true;
	}

	public void addCard(Card card) {
		this.cards.add(card);
	}

	public boolean hasCard(Card card) {
		return this.cards.contains(card);
	}

	/**
	 * Checks whether there is any overlap between the given cards and this player's cards.
	 * @param cards Cards for which to check overlap between this player's hand.
	 * @return True if any of the given cards exist in this player's hand, false otherwise.
	 */
	public boolean hasAnyCard(Collection<Card> cards) {
		for (Card card : cards) {
			if (this.cards.contains(card)) {
				return true;
			}
		}
		return false;
	}

	public Set<Card> getCards() {
		return Collections.unmodifiableSet(cards);
	}

	public Suspect getSuspect() {
		return suspect;
	}

	public String getID() {
		return id;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	@Override
	public String toString() {
		return String.format("Player[id=%s, suspect=%s]", id, suspect);
	}


	@Override
	public int compareTo(Player o) {
		return this.id.compareTo(o.getID());
	}
}
