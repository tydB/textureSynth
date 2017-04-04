public class PixelCon implements Comparable<PixelCon> {
	public int x = 0;
	public int y = 0;
	public int n = -1;
	public int color = 0;
	public double error = 0;
	public PixelCon(int _x, int _y, int _n) {
		x = _x;
		y = _y;
		n = _n;
	}
	public PixelCon(int _x, int _y, int _n, int _color) {
		x = _x;
		y = _y;
		n = _n;
		color = _color;
	}
	public PixelCon(int _x, int _y, double _error, int _color) {
		x = _x;
		y = _y;
		error = _error;
		color = _color;
	}
	public int compareTo(PixelCon other) {
		if (n == -1) {
			if (other.error < error) {
				return -1;
			}
			else if (other.error > error) {
				return 1;
			}
			return 0;
		}
		return other.n - n;
	}
	public String toString() {
		if (n == -1) {
			return "" + x + ", " + y + ": " + error;
		}
		return "" + x + ", " + y + ": " + n;
	}
	// public static int getRed(int pixelColour) {
	// 	return   (0x00FF0000 & pixelColour)>>>16;
	// }
	// public static int getGreen(int pixelColour) {
	// 	return (0x0000FF00 & pixelColour)>>>8;
	// }
	// public static int getBlue(int pixelColour) {
	// 	return  (0x000000FF & pixelColour);
	// }
	// public static int makeColour(int red, int green, int blue) {
	// 	return (255<<24 | red<<16 | green << 8 | blue);
	// }
	// public double colorDistance(int otherColor) {
	// 	return Math.sqrt(Math.pow(getRed(color) - getRed(otherColor), 2) + Math.pow(getGreen(color) - getGreen(otherColor), 2) + Math.pow(getBlue(color) - getBlue(otherColor), 2));
	// }
}