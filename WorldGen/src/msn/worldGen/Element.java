package msn.worldGen;

public enum Element {
	W, // WATER
	O, // OCEAN
	D, // DESERT
	G, // GRASSLANDS
	T, // TUNDRA
	F, // FOREST
	A; // ALPINE

	@Override
	public String toString() {
		switch (this) {
		case W:
			return "~";
		case O:
			return ".";
//		case D:
//			return "<";
		case G:
			return "`";
		case T:
			return "_";
		case F:
			return "+";
		case A:
			return "^";
		}
		return super.toString();
	}

	/**
	 * Defines the likelihood of moving from element in dim1 to dim2
	 */
	public static int[][] probability = new int[][] {
			// maybe make each add up to 100%?

			// W, _O, _D, _G, _T, _F, _A
			{ 40, 05, 05, 05, 05, 05, 05 }, // WATER -> Element
			{ 05, 95, 05, 15, 05, 05, 05 }, // OCEAN
			{ 05, 05, 50, 10, 05, 05, 05 }, // DESERT
			{ 15, 05, 05, 90, 10, 20, 05 }, // GRASSLANDS
			{ 05, 05, 05, 05, 70, 05, 10 }, // TUNDRA
			{ 10, 05, 05, 10, 05, 90, 10 }, // FOREST
			{ 05, 05, 05, 05, 05, 05, 50 } // ALPINE
	};
}
