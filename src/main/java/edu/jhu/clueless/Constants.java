package edu.jhu.clueless;

public class Constants {

	public enum EntityType {
		SUSPECT, WEAPON, ROOM
	}

	public enum Suspect implements Card {
		COLONEL_MUSTARD, MISS_SCARLET, PROFESSOR_PLUM, MR_GREEN, MRS_WHITE, MRS_PEACOCK;

		public EntityType getType() {
			return EntityType.SUSPECT;
		}
	}

	public enum Weapon implements Card {
		ROPE, LEAD_PIPE, KNIFE, WRENCH, CANDLESTICK, PISTOL;

		public EntityType getType() {
			return EntityType.WEAPON;
		}
	}

	public enum Room implements Card {
		HALL, LOUNGE, LIBRARY, BILLIARD_ROOM, DINING_ROOM, CONSERVATORY, BALLROOM, KITCHEN;

		public EntityType getType() {
			return EntityType.ROOM;
		}

	}

}
