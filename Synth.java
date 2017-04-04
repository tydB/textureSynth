import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import javax.imageio.*;
import java.util.ArrayList;


public class Synth extends JComponent implements KeyListener {
	public BufferedImage sample;
	public BufferedImage results;
	public BufferedImage mask;
	public double maxErrorThreshold = 0.3;
	public double errorThreshold = 0.1;
	public Synth(String sampleUrl, String startingPointUrl, String maskUrl, int nSize, int seedSize) {
		try {
			sample = ImageIO.read(new File(sampleUrl));
			results = ImageIO.read(new File(startingPointUrl));
			mask = ImageIO.read(new File(maskUrl));
		}
 		catch (Exception e) {
			System.out.println("There was a problem loading the Sample image\n"+e);
			return;
		}
		// 
	}
	public Synth(String sampleUrl) {
		try {
			sample = ImageIO.read(new File(sampleUrl));
		}
 		catch (Exception e) {
			System.out.println("There was a problem loading the Sample image\n"+e);
			return;
		}
		System.out.println(getRed(sample.getRGB(0,0)));
		System.out.println(getGreen(sample.getRGB(0,0)));
		System.out.println(getBlue(sample.getRGB(0,0)));
		System.out.println(getRed(sample.getRGB(1,0)));
		System.out.println(getGreen(sample.getRGB(1,0)));
		System.out.println(getBlue(sample.getRGB(1,0)));
		System.out.println(getRed(sample.getRGB(2,0)));
		System.out.println(getGreen(sample.getRGB(2,0)));
		System.out.println(getBlue(sample.getRGB(2,0)));
	}
	public Synth(String sampleUrl, int finalWidth, int finalHeight, int nSize, int seedSize) {
		try {
			sample = ImageIO.read(new File(sampleUrl));
			results = new BufferedImage(finalWidth, finalHeight, sample.getType());
			mask = new BufferedImage(finalWidth, finalHeight, sample.getType());
		}
		catch (Exception e) {
			System.out.println("There was a problem loading the Sample image\n"+e);
			return;
		}
		seed(seedSize);
		synthTexture(nSize);
		try {
			File outputfile = new File("saved.png");
			ImageIO.write(results, "png", outputfile);
		} catch (IOException e) {
			System.out.println("Could not save image");
		}
	}
	public void seed(int seedSize) {
		// seed
		int x = (int)(Math.random() * (sample.getWidth() - (seedSize - 1)));
		int y = (int)(Math.random() * (sample.getHeight() - (seedSize - 1)));
		System.out.println("Seeding From " + x + ", " + y);
		for (int sampleI = 0; sampleI < seedSize; sampleI++) {
			for (int sampleJ = 0; sampleJ < seedSize; sampleJ++) {
				setPixel(results.getWidth() / 2 + sampleI - 1,
					results.getHeight() / 2 + sampleJ - 1,
					sample.getRGB(x + sampleI,y + sampleJ));
			}
		}
	}
	public void setPixel(int x, int y, int color) {
		// System.out.println(getRed(color) + ", " + getGreen(color) + ", " + getBlue(color));
		results.setRGB(x, y, color);
		mask.setRGB(x, y, makeColour(255,255,255));
	}
	public void synthTexture(int nSize) {
		// count the number of not set pixels
		int pixelCount = emptyCount();
		while (pixelCount > 0) {
			boolean progress = false;
			ArrayList<int[]> nextPixels = getPossiblePixels();
			System.out.println("Total empty pixels: " + pixelCount);
			while (nextPixels.size() > 0) {
				// get pixel with most neighbors I REALLY SHOULD JUST SORT THIS AND NOT DO THIS
				int largest = 0;
				int index = 0;
				for (int p = 0; p < nextPixels.size(); ++p) {
					int c = nextPixels.get(p)[2];
					if (c > largest) {
						largest = c;
						index = p;
					}
				}
				int[] pixelData = nextPixels.remove(index);
				// System.out.println("Working on Pixel: " + pixelData[0] + ", " + pixelData[1] + ": " + pixelData[2]);
				double[][][] template = getTemplate(pixelData[0], pixelData[1], nSize);
				ArrayList<double[]> matches = findMatches(template, nSize);
				double[] selected = matches.get((int)(Math.random() * matches.size()));
				// System.out.println("Selected Pixel: " + selected[0] + ", " + selected[1] + ": " + selected[2]);
				if (selected[2] < maxErrorThreshold) {
					setPixel(pixelData[0], pixelData[1], sample.getRGB((int)selected[0], (int)selected[1]));
					--pixelCount;
					progress = true;
					repaint();
				}
			}
			if (!progress) {
				maxErrorThreshold *= 1.1;
			}
		}
		System.out.println("Finished");
		repaint();
	}
	public void printArray(double[][] a) {
		for (int i = 0; i < a.length; ++i) {
			String x = a[i][0] + "";
			for (int j = 1; j < a[i].length; ++j) {
				x += ", " + a[i][j];
			}
			System.out.println(x);
		}
	}
	public ArrayList<double[]> findMatches(double[][][] template, int nSize) {
		ArrayList<double[]> matches = new ArrayList<double[]>();
		double[][] validPixels = new double[nSize][nSize];
		for (int i = 0; i < nSize; ++i) {
			for (int j = 0; j < nSize; ++j) {
				if (template[i][j][0] >= 0 && template[i][j][1] >= 0 && template[i][j][2] >= 0) {
					validPixels[i][j] = 1.0;
				}
				else {
					validPixels[i][j] = 0.0;
				}
			}
		}
		// printArray(validPixels);
		double[][] gauss = gaussian2D(nSize, (double)nSize / 6.4);
		double totalWeight = 0;
		for (int i = 0; i < nSize; ++i) {
			for (int j = 0; j < nSize; ++j) {
				totalWeight += gauss[i][j] * validPixels[i][j];
			}
		}
		double[][] ssd = new double[sample.getWidth()][sample.getHeight()];
		double minSSD = -1;
		for (int i = 0; i < sample.getWidth(); ++i) {
			for (int j = 0; j < sample.getHeight(); ++j) {
				ssd[i][j] = 0;
				for (int sampleI = 0; sampleI < nSize; ++sampleI) {
					for (int sampleJ = 0; sampleJ < nSize; ++sampleJ) {
						int x = i + sampleI - (nSize / 2);
						int y = j + sampleJ - (nSize / 2);
						if (x >= 0 && x < sample.getWidth() && y >= 0 && y < sample.getHeight()) {
							double dist = colorDistance(template[sampleI][sampleJ][0],
								template[sampleI][sampleJ][1],
								template[sampleI][sampleJ][2],
								getRed(sample.getRGB(x, y)),
								getRed(sample.getRGB(x, y)),
								getRed(sample.getRGB(x, y)));
							// System.out.println(x + ", " + y + ": " + dist);
							ssd[i][j] += dist * gauss[sampleI][sampleJ] * validPixels[sampleI][sampleJ];
						}
						else { 
							// what do i do when it is out of bounds?
							// I cant just add nothing because then i just get random crap
							// plus one is okish but i get a lot of noise
							
							ssd[i][j] += 0.75;
						}
					}
				}
				ssd[i][j] /= totalWeight;
				// System.out.println(i + "," + j + ": " + ssd[i][j]);
				if (ssd[i][j] < minSSD || minSSD == -1) {
					minSSD = ssd[i][j];
				}
			}
		}
		// System.out.println("minSSD" + minSSD);
		for (int i = 0; i < sample.getWidth(); ++i) {
			for (int j = 0; j < sample.getHeight(); ++j) {
				if (ssd[i][j] <= minSSD * (1 + errorThreshold)) {
					double[] data = new double[3];
					data[0] = i + 0.0; 
					data[1] = j + 0.0;
					data[2] = ssd[i][j];
					matches.add(data);
				}
			}
		}
		return matches;
	}
	public double colorDistance(double r0, double g0, double b0, double r1, double g1, double b1) {
		// go to 0 - 1 from 0 - 255
		// r0 /= 255;
		// g0 /= 255;
		// b0 /= 255;
		r1 /= 255.0;
		g1 /= 255.0;
		b1 /= 255.0;
		return Math.pow(r0 - r1, 2) + Math.pow(g0 - g1, 2) + Math.pow(b0 - b1, 2);
		// return 0.0;
	}
	public double[][] gaussian2D(int nSize, double sigmna) {
		double[][] gMask = new double[nSize][nSize];
		double sum = 0;
		for (int i = 0; i < nSize; ++i) {
			for (int j = 0; j < nSize; ++j) {
				gMask[i][j] = gaussian(sigmna, i, j);
				sum += gMask[i][j];
			}
		}
		// System.out.println(sum);
		return gMask;
	}
	// http://groups.inf.ed.ac.uk/vision/GRASSIN/SkinSpotTool/skinSpotTool/GaussianSmooth.java
	// Gaussian code is being a pain. This is some code i need to figure out to simplify
	public double gaussian(double sigmna, int x, int y) {
		double g = 0;
		for(double ySubPixel = y - 0.5; ySubPixel < y + 0.55; ySubPixel += 0.1){
			for(double xSubPixel = x - 0.5; xSubPixel < x + 0.55; xSubPixel += 0.1){
				g = g + ((1/(2*Math.PI*sigmna*sigmna)) *
				 Math.pow(Math.E,-(xSubPixel*xSubPixel+ySubPixel*ySubPixel)/
					(2*sigmna*sigmna)));
			}
		}
		g = g/121;
		return g;
	}
	// template info needs the color information
	public double[][][] getTemplate(int x, int y, int nSize) {
		double[][][] temp = new double[nSize][nSize][3];
		int start = -(nSize / 2);
		int end = nSize / 2;
		for (int i = start; i <= end; i++) {
			for (int j = start; j <= end; j++) {
				if (x + i >= 0 && x + i < mask.getWidth() && y + j >= 0 && y + j < mask.getHeight()) {
					// System.out.println(x + i + ", " + (y + j));
					if (getRed(mask.getRGB(x + i, y + j)) > 0) { // white
						temp[i - start][j - start][0] = getRed(results.getRGB(x + i, y + j)) / 255.0;
						temp[i - start][j - start][1] = getGreen(results.getRGB(x + i, y + j)) / 255.0;
						temp[i - start][j - start][2] = getBlue(results.getRGB(x + i, y + j)) / 255.0;
						// setPixel(x + i, y + j, makeColour(255,0,0));
					}
					else {
						temp[i - start][j - start][0] = -1.0;
						temp[i - start][j - start][1] = -1.0;
						temp[i - start][j - start][2] = -1.0;
					}
				}
				else {
					temp[i - start][j - start][0] = -1.0;
					temp[i - start][j - start][1] = -1.0;
					temp[i - start][j - start][2] = -1.0;
				}
			}
		}
		return temp;
	}
	public ArrayList<int[]> getPossiblePixels() {
		ArrayList<int[]> possible = new ArrayList<int[]>();
		for (int i = 0; i < mask.getWidth(); ++i) {
			for (int j = 0; j < mask.getHeight(); ++j) {
				if (getRed(mask.getRGB(i,j)) == 0) { // black
					int count = nCount(mask, i, j);
					if (count > 0) {
						int[] data = new int[3];
						data[0] = i;
						data[1] = j;
						data[2] = count;
						possible.add(data);
					}
				}
			}
		}
		return possible;
	}
	public int nCount(BufferedImage img, int x, int y) {
		int count = 0;
		for(int j = -1; j < 2; j++) {
			for(int i = -1; i < 2; i++) {
				// inbounds check
				if (x + j >= 0 && x + j < img.getWidth() && y + i >= 0 && y + i < img.getHeight()) {
					if (i != 0 || j != 0) {
						if (getRed(img.getRGB(x + j, y + i)) > 0) {
							++count;
						}
					}
				}
			}
		}
		return count;
	}
	public int emptyCount() {
		int count = 0;
		for (int i = 0; i < mask.getWidth(); ++i) {
			for (int j = 0; j < mask.getHeight(); ++j) {
				if (getRed(mask.getRGB(i,j)) == 0) {
					++count;
				}
			}
		}
		return count;
	}




	// boilerplate functions
	public void paint(Graphics g) {
		g.drawImage(sample, 0, 0, null);
		g.drawImage(results, sample.getWidth(null), 0, null);
		// g.drawImage(mask, sample.getWidth(null) + results.getWidth(null), 0, null);
	}
	public Dimension getPreferredSize() {
		if(results == null) {
			return new Dimension(100,100);
		}
		else {
			return new Dimension(results.getWidth(null) + sample.getWidth(null), results.getHeight(null));
		} 
	}
	public static int getAlpha(int pixelColour) {
		return (0xFF000000 & pixelColour)>>>24;
	}
	public static int getRed(int pixelColour) {
		return   (0x00FF0000 & pixelColour)>>>16;
	}
	public static int getGreen(int pixelColour) {
		return (0x0000FF00 & pixelColour)>>>8;
	}
	public static int getBlue(int pixelColour) {
		return  (0x000000FF & pixelColour);
	}
	public static int makeColour(int red, int green, int blue) {
		return (255<<24 | red<<16 | green << 8 | blue);
	}
		// These function definitions must be included to satisfy the KeyListener interface
	public void keyPressed(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == KeyEvent.VK_ESCAPE)  System.exit(0); 	// exit when escape is pressed
		// else if (e.getKeyChar() == '3') synthTexture(5);
	}
	public static void printHelp() {
		System.out.println("TODO: Real Help");
	}
	public static void main(String[] args) {
		String sample = "sampleImages/dots.jpg";
		String starting = "";
		int width = 64;
		int height = 64;
		int nSize = 3;
		int seedSize = 3;
		try {
			for (int i = 0; i < args.length; ++i) {
				System.out.println("Processing: " + args[i]);
				if (args[i].equals("-help")) {
					printHelp();
					return;
				}
				else if (args[i].equals("-n")) {
					i++;
					nSize = Integer.parseInt(args[i]);
					System.out.println("Setting nSize to: " + nSize);
				}
				else if (args[i].equals("-s")) {
					i++;
					sample = args[i];
					System.out.println("Setting sampleUrl to: " + sample);
				}
				else if (args[i].equals("-w")) {
					i++;
					width = Integer.parseInt(args[i]);
					System.out.println("Setting width to: " + width);
				}
				else if (args[i].equals("-h")) {
					i++;
					height = Integer.parseInt(args[i]);
					System.out.println("Setting height to: " + height);
				}
				else {
					System.out.println("Unknown Parameter");
				}
			}
		}
		catch (Exception e) {
			System.out.println("Error Parsing Parameters");
			printHelp();
			return;
		}
		System.out.println("Parameters: " + sample + ", " + width + ", " + height + ", " + nSize + ", " + seedSize);

		// if (args.length == 0) {
		// 	printHelp();
		// 	return;
		// }

		JFrame f = new JFrame ("Non-Parametric Texture Synthesis");
		f.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
		
		Synth window = new Synth(sample, width, height, nSize, seedSize); 
		// Synth window = new Synth(sample); 
		
		f.add(window);// put the panel with the image in the frame
		f.pack();// layout the frame
		f.setVisible(true);// show the frame
		f.addKeyListener(window); // make the window respond to keyboard button presses
	}
}