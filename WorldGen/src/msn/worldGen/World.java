package msn.worldGen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class World {
	private static final int NORTH = 0;
	private static final int EAST = 1;
	private static final int SOUTH = 2;
	private static final int WEST = 3;
	private int width;
	private int height;
	private Element[][] world;
	private int[][] elevations;
	private int iterations = 0;
	private Set<Coordinate> seeded = new HashSet<Coordinate>();
	private Random rand = new Random();

	public World() {
		this(256);
	}

	public World(int size) {
		this(size, size);
	}

	public World(int width, int height) {
		this.width = width;
		this.height = height;
		elevations = new int[width][height];
		world = new Element[width][height];
		build();
		printElevations();

		persist();
	}

	private String printElevations() {
		StringBuilder sb = new StringBuilder();
		int ocean = 0;
		int plains = 0;
		int foothills = 0;
		int midmountain = 0;
		int mountains = 0;
		int sky = 0;

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int elev = elevations[i][j];
				if (elev > 50)
					sky++;
				else if (elev > 48)
					mountains++;
				else if (elev > 32)
					midmountain++;
				else if (elev > 16)
					foothills++;
				else if (elev > 8)
					plains++;
				else
					ocean++;
				sb.append(String.format("%2d ", elev));
			}
			sb.append("\r\n");
		}

		sb.append("\r\nocean: " + ocean);
		sb.append("\r\nplains " + plains);
		sb.append("\r\nfoothills " + foothills);
		sb.append("\r\nmidmountain " + midmountain);
		sb.append("\r\nmountain " + mountains);
		sb.append("\r\nsky " + sky);

		System.out.print("**************\r\n");
		System.out.print("* Elevations *\r\n");
		System.out.print("**************\r\n");
		System.out.println(sb.toString());
		return sb.toString();
	}

	private void persist() {
		URL url = ClassLoader.getSystemClassLoader().getResource("msn/world");
			PrintWriter writer;
			try {
				writer = new PrintWriter(url.getPath(), "UTF-8");
				writer.print(toString());
				writer.flush();
				writer.close();
				
				url = ClassLoader.getSystemClassLoader().getResource("msn/elevations");
				writer = new PrintWriter(url.getPath(), "UTF-8");
				writer.print(printElevations());
				writer.flush();
				writer.close();
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public String toString() {
		StringBuilder line = new StringBuilder();
		for (int w = 0; w < width; w++) {
			for (int h = 0; h < height; h++) {
				line.append(world[w][h]);
			}
			line.append("\r\n");
		}
		return line.toString();
	}

	private void build(int seed) {
		Coordinate elev = new Coordinate(seed, rand.nextInt(height));
		Set<Coordinate> seeds = new HashSet<Coordinate>();
		seeds.add(elev);
		elevate(seeds);
		erode(20);

		// initialize as ocean.
		for (int w = 0; w < width; w++) {
			for (int h = 0; h < height; h++) {
				world[w][h] = Element.O;
			}
		}

		// drop a mountain island (should make the element type random in the
		// future too)
		world[seed][seed / 2] = Element.A;

		// grow from there.
		Coordinate start = new Coordinate(seed, seed / 2);
		seeds.clear();
		seeds.add(start);
		do {
			build(seeds);
			seeds.clear();
			Coordinate randCoord = new Coordinate(rand.nextInt(width), rand.nextInt(height));
			seeds.add(randCoord);
		} while (iterations < width);
	}

	private void erode(int remaining) {
		for (int w = 0; w < width; w++) {
			for (int h = 0; h < height; h++) {
				int curr = elevations[w][h];
				int chance = rand.nextInt(10);
				if (curr > 48) {
					// don't erode mountains much.
					if (chance > 9) {
						elevations[w][h] = curr - rand.nextInt(2);
					}

				} else if (curr > 32) {
					// don't erode mountains much.
					if (chance > 8) {
						elevations[w][h] = curr - rand.nextInt(3);
					}
				} else if (curr > 16) {
					// erode foothills slightly more
					if (chance > 4) {
						elevations[w][h] = curr - rand.nextInt(5);
					}
				} else if (curr > 8) {
					// erode plains slightly less
					if (chance > 8) {
						elevations[w][h] = curr - rand.nextInt(2);
					}
				} else {
					// between sea level and plains - don't erode.
				}
				if (elevations[w][h] < 0) {
					elevations[w][h] = 0;
				}
			}
		}

	}

	private void elevate(Set<Coordinate> seeds) {
		// design elevations
		// 0 == sea level
		// 8 == plains
		// 16 == foothills
		// 32 == mid mountain
		// 50 == mountain tops
		// 64 == top of the world?

		if (seeds.isEmpty())
			return;

		if (iterations++ > width) {
			return;
		}

		Set<Coordinate> reseed = new HashSet<Coordinate>();
		for (Coordinate seed : seeds) {
			int x = seed.getX();
			int y = seed.getY();
			if (x < 0 || x >= width || y < 0 || y >= height) {
				continue;
			}
			int curr = elevations[x][y];
			int n = y > 0 ? elevations[x][y - 1] : curr;
			int e = x < width - 1 ? elevations[x + 1][y] : curr;
			int s = y < height - 1 ? elevations[x][y + 1] : curr;
			int w = x > 0 ? elevations[x - 1][y] : curr;

			// if(curr == 0){
			int surround = n + e + s + w;
			if (rand.nextBoolean()) {
				// go higher
				elevations[x][y] = curr + rand.nextInt(10);
			} else if (rand.nextBoolean()) {
				// go lower
				elevations[x][y] = curr - rand.nextInt(10);
			} else {
				// erode
				elevations[x][y] = surround / 4;
			}

			if (elevations[x][y] < 0) {
				elevations[x][y] = 0;
			} else if (elevations[x][y] > 50) {
				elevations[x][y] = 50;
			}

			if (rand.nextBoolean()) {
				// move S/E
				reseed.add(new Coordinate(x + 1, y));
				reseed.add(new Coordinate(x, y + 1));
			} else {
				// move N/W
				reseed.add(new Coordinate(x - 1, y));
				reseed.add(new Coordinate(x, y - 1));
			}

		}
		elevate(reseed);
	}

	private void build(Set<Coordinate> seeds) {
		if (seeds.isEmpty()) {
			System.out.println(toString());
			return;
		}

		if (iterations++ % 100 == 0) {
			System.out.println(iterations + " " + seeds.size() + " *********************************");
			System.out.println(toString());
		}

		Set<Coordinate> reseed = new HashSet<Coordinate>();
		for (Coordinate c : seeds) {
			// if (seeds.size() > 50) {
			// if (rand.nextBoolean())
			// continue;
			// }
			// breadth first out from seeds.
			int x = c.getX();
			int y = c.getY();

			// determine neighboring seeds.
			Element north = getNext(x, y, NORTH);
			Element east = getNext(x, y, EAST);
			Element south = getNext(x, y, SOUTH);
			Element west = getNext(x, y, WEST);
			if (north != null) {
				if (world[x - 1][y] != north) {
					world[x - 1][y] = north;
					Coordinate coordinate = new Coordinate(x - 1, y);
					if (rand.nextBoolean() && !seeded.contains(coordinate)) {
						seeded.add(coordinate);
						reseed.add(coordinate);
					}
				}
			}
			if (east != null) {
				if (world[x][y + 1] != east) {
					world[x][y + 1] = east;
					Coordinate coordinate = new Coordinate(x, y + 1);
					if (rand.nextBoolean() && !seeded.contains(coordinate)) {
						seeded.add(coordinate);
						reseed.add(coordinate);
					}
				}
			}
			if (south != null) {
				if (world[x + 1][y] != south) {
					world[x + 1][y] = south;
					Coordinate coordinate = new Coordinate(x + 1, y);
					if (rand.nextBoolean() && !seeded.contains(coordinate)) {
						seeded.add(coordinate);
						reseed.add(coordinate);
					}
				}
			}
			if (west != null) {
				if (world[x][y - 1] != west) {
					world[x][y - 1] = west;
					Coordinate coordinate = new Coordinate(x, y - 1);
					if (rand.nextBoolean() && !seeded.contains(coordinate)) {
						seeded.add(coordinate);
						reseed.add(coordinate);
					}
				}
			}
		}

		build(reseed);
	}

	private Element getNext(int x, int y, int dir) {
		// off the map.
		if (x == 0 && dir == NORTH) {
			return null;
		}
		if (x == height - 1 && dir == SOUTH) {
			return null;
		}
		if (y == 0 && dir == WEST) {
			return null;
		}
		if (y == width - 1 && dir == EAST) {
			return null;
		}

		Element current = world[x][y];
		Element result = null;
		int toWater = rand.nextInt(100) + Element.probability[current.ordinal()][Element.W.ordinal()];
		int toOcean = rand.nextInt(100) + Element.probability[current.ordinal()][Element.O.ordinal()];
		int toDesert = rand.nextInt(100) + Element.probability[current.ordinal()][Element.D.ordinal()];
		int toGrass = rand.nextInt(100) + Element.probability[current.ordinal()][Element.G.ordinal()];
		int toTundra = rand.nextInt(100) + Element.probability[current.ordinal()][Element.T.ordinal()];
		int toForest = rand.nextInt(100) + Element.probability[current.ordinal()][Element.F.ordinal()];
		int toAlpine = rand.nextInt(100) + Element.probability[current.ordinal()][Element.A.ordinal()];

		// add some more logic maybe to increase/decrease probability depending
		// on current and direction looking?
		if (dir == NORTH) {
			// north
		} else if (dir == EAST) {
			// east
		} else if (dir == SOUTH) {
			// south
		} else if (dir == WEST) {
			// west
		}

		int max = getMax(toWater, toOcean, toDesert, toGrass, toTundra, toForest, toAlpine);
		if (max == toWater) {
			result = Element.W;
		}
		if (max == toOcean) {
			result = Element.O;
		}
		if (max == toDesert) {
			result = Element.D;
		}
		if (max == toGrass) {
			result = Element.G;
		}
		if (max == toTundra) {
			result = Element.T;
		}
		if (max == toForest) {
			result = Element.F;
		}
		if (max == toAlpine) {
			result = Element.A;
		}
		// if(result == current){
		// // kill it
		// return null;
		// }

		return result;
	}

	private int getMax(int... toCompare) {
		int max = 0;
		for (int tC : toCompare) {
			if (tC > max)
				max = tC;
		}
		return max;
	}

	private void build() {
		build(rand.nextInt(width));
	}
}
