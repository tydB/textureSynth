import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import javax.imageio.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Synth2 extends JComponent implements KeyListener {
	public BufferedImage sample;
	public BufferedImage results;
	public BufferedImage mask;
	public double maxErrorThreshold = 0.3;
	public double errorThreshold = 0.1;
	public boolean wrap = false;
	public Synth2(String sampleUrl, int finalWidth, int finalHeight, int nSize, int seedSize) {
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
	public void synthTexture(int nSize) {
		System.out.println("nSize " + nSize);
		PixelCon[] next = getNextPixels();
		while (next.length > 0) {
			System.out.println(".");
			int highestNcount = next[0].n;
			for (int i = 0; i < next.length; ++i) {
				if (next[i].n < highestNcount) {
					break;
				}
				// System.out.println(next[i]);
				// get template
				PixelCon[] template = getTemplate(next[i].x, next[i].y, nSize);
				// for (int j = 0; j < template.length; ++j) { // show that it is finding set neighbors correctly
				// 	setPixel(next[i].x + template[j].x, next[i].y + template[j].y, makeColour(255,0,0));
				// }
				// check the sample image for close matches
				PixelCon best = getBestPixel(template);
				setPixel(next[i].x, next[i].y, best.color);
			}
			next = getNextPixels();
		}
		System.out.println("Done");
	}

	public PixelCon getBestPixel(PixelCon[] template) {
		ArrayList<PixelCon> possiblePixels = new ArrayList<PixelCon>();
		for (int i = 0; i < sample.getWidth(); i++) {
			for (int j = 0; j < sample.getHeight(); j++) {
				int pixColor = sample.getRGB(i,j);
				PixelCon match = new PixelCon(i,j, 0.0, pixColor);
				double error = 0;
				for (int t = 0; t < template.length; ++t) {
					int x = i + template[t].x;
					int y = j + template[t].y;
					// System.out.println(y);
					if (!(x < 0 || x >= sample.getWidth() || y < 0 || y >= sample.getHeight())) {
						int color = sample.getRGB(x,y);
						error += colorDistance(template[t].color, color);
					}
					// must have a penalty for missing
					else {
						error += colorDistance(0, makeColour(255,255,255));
					}
				}
				match.error = error;
				possiblePixels.add(match);
			}
		}
		PixelCon[] pixs = possiblePixels.toArray(new PixelCon[possiblePixels.size()]);
		Arrays.sort(pixs);
		// System.out.println("best: " + pixs[0]);
		return pixs[pixs.length - 1];
	}
	public double colorDistance(int color, int otherColor) {
		return Math.sqrt(Math.pow(getRed(color) - getRed(otherColor), 2) + Math.pow(getGreen(color) - getGreen(otherColor), 2) + Math.pow(getBlue(color) - getBlue(otherColor), 2));
	}
	public PixelCon[] getTemplate(int startX, int startY, int nSize) {
		ArrayList<PixelCon> template = new ArrayList<PixelCon>();
		// System.out.println("Starting " + startX + ", " + startY);
		int offset = nSize / 2;
		for (int i = 0; i < nSize; i++) {
			for (int j = 0; j < nSize; j++) {
				int x = startX - offset + i;
				int y = startY - offset + j;
				if (!wrap) {
					if (!(x < 0 || x >= mask.getWidth() || y < 0 || y >= mask.getHeight() || (i == 1 && j == 0))) {
						int color = mask.getRGB(x, y);
						if (getRed(color) > 0) { // not black
							// System.out.println("Location" + x + ", " + y);
							int resultsColor = results.getRGB(x,y);
							// System.out.println("Color" + resultsColor);
							template.add(new PixelCon(x - startX, y - startY,0,resultsColor));
						}
					}
				}
				else {
					if (!(i == 1 && j == 0)) { // edge looping
						int wrapX = (x < 0) ? results.getWidth() + x: x % results.getWidth();
						int wrapY = (y < 0) ? results.getHeight() + y: y % results.getHeight();
						int color = mask.getRGB(wrapX, wrapY);
						if (getRed(color) > 0) { // not black
							int resultsColor = results.getRGB(wrapX, wrapY);
							template.add(new PixelCon(x - startX, y - startY,0,resultsColor));
						}
					}
				}
			}
		}
		PixelCon[] pixs = template.toArray(new PixelCon[template.size()]);
		return pixs;
	}
	public PixelCon[] getNextPixels() {
		ArrayList<PixelCon> possible = new ArrayList<PixelCon>();
		for (int i = 0; i < mask.getWidth(); ++i) {
			for (int j = 0; j < mask.getHeight(); ++j) {
				if (getRed(mask.getRGB(i,j)) == 0) { // black
					int count = nCount(mask, i, j);
					if (count > 0 && count < 8) {
						PixelCon data = new PixelCon(i,j,count);
						possible.add(data);
					}
				}
			}
		}
		PixelCon[] pixs = possible.toArray(new PixelCon[possible.size()]);
		Arrays.sort(pixs);
		return pixs;
	}
	public void seed(int seedSize) {
		int x = (int)(Math.random() * (sample.getWidth() - (seedSize - 1)));
		// int x = 20;
		int y = (int)(Math.random() * (sample.getHeight() - (seedSize - 1)));
		// int y = 20;
		System.out.println("Seeding From " + x + ", " + y);
		for (int sampleI = 0; sampleI < seedSize; sampleI++) {
			for (int sampleJ = 0; sampleJ < seedSize; sampleJ++) {
				setPixel(results.getWidth() / 2 + sampleI - 1,
					results.getHeight() / 2 + sampleJ - 1,
					sample.getRGB(x + sampleI,y + sampleJ));
			}
		}
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
	public void setPixel(int x, int y, int color) {
		// System.out.println(getRed(color) + ", " + getGreen(color) + ", " + getBlue(color));
		results.setRGB(x, y, color);
		mask.setRGB(x, y, makeColour(255,255,255));
	}
	// boilerplate functions
	public void paint(Graphics g) {
		g.drawImage(sample, 0, 0, null);
		g.drawImage(results, sample.getWidth(null), 0, null);
		g.drawImage(mask, sample.getWidth(null) + results.getWidth(null), 0, null);
	}
	public Dimension getPreferredSize() {
		if(results == null) {
			return new Dimension(100,100);
		}
		else {
			return new Dimension(results.getWidth(null) * 2 + sample.getWidth(null), results.getHeight(null));
		} 
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
		int width = 128;
		int height = 128;
		int nSize = 3;
		int seedSize = 5;
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
				else if (args[i].equals("-t")) {
					i++;
					seedSize = Integer.parseInt(args[i]);
					System.out.println("Setting seedSize to: " + seedSize);
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
		
		Synth2 window = new Synth2(sample, width, height, nSize, seedSize); 
		// Synth window = new Synth(sample); 
		
		f.add(window);// put the panel with the image in the frame
		f.pack();// layout the frame
		f.setVisible(true);// show the frame
		f.addKeyListener(window); // make the window respond to keyboard button presses
	}
}