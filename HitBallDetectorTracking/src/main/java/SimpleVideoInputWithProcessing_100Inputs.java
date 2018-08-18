import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import processing.video.*;
import oscP5.*;
import netP5.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class SimpleVideoInputWithProcessing_100Inputs {

	/**
	 * REALLY simple processing sketch for using webcam input This sends 100
	 * input values to port 6448 using message /wek/inputs
	 **/

	int numPixelsOrig;
	int numPixels;
	boolean first = true;

	int boxWidth = 64;
	int boxHeight = 48;

	int numHoriz = 640 / boxWidth;
	int numVert = 480 / boxHeight;

	int[] downPix = new int[numHoriz * numVert];

	OscP5 oscP5;
	NetAddress dest;

	PApplet parent;

	public void setup(PApplet parent, int w, int h) {
		// colorMode(HSB);

		numPixelsOrig = w * h;
		parent.loadPixels();
		parent.noStroke();

		/* start oscP5, listening for incoming messages at port 12000 */
		oscP5 = new OscP5(this, 9000);
		dest = new NetAddress("127.0.0.1", 6448);

		this.parent = parent;
	}

	public void send(PGraphics canvas, PImage img) {

		/*
		 * for (int i = 0; i < numPixels; i++) { int x = i % video.width; int y
		 * = i / video.width; float xscl = (float) width / (float) video.width;
		 * float yscl = (float) height / (float) video.height;
		 * 
		 * float gradient = diff(i, -1) + diff(i, +1) + diff(i, -video.width) +
		 * diff(i, video.width); fill(color(gradient, gradient, gradient));
		 * rect(x * xscl, y * yscl, xscl, yscl); }
		 */
		int boxNum = 0;
		int tot = boxWidth * boxHeight;
		// for (int x = 0; x < 640; x += boxWidth) {
		// for (int y = 0; y < 480; y += boxHeight) {
		// float red = 0, green = 0, blue = 0;
		//
		// for (int i = 0; i < boxWidth; i++) {
		// for (int j = 0; j < boxHeight; j++) {
		// int index = (x + i) + (y + j) * 640;
		// red += canvas.red(img.pixels[index]);
		// green += canvas.green(img.pixels[index]);
		// blue += canvas.blue(img.pixels[index]);
		// }
		// }
		// downPix[boxNum] = canvas.color(red / tot, green / tot, blue
		// / tot);
		// // downPix[boxNum] = color((float)red/tot, (float)green/tot,
		// // (float)blue/tot);
		// canvas.fill(downPix[boxNum]);
		//
		// int index = x + 640 * y;
		// red += canvas.red(img.pixels[index]);
		// green += canvas.green(img.pixels[index]);
		// blue += canvas.blue(img.pixels[index]);
		// // fill (color(red, green, blue));
		// canvas.rect(x, y, boxWidth, boxHeight);
		// boxNum++;
		// /*
		// * if (first) { println(boxNum); }
		// */
		// }
		// }
		img.loadPixels();

		if (parent.frameCount % 2 == 0)
			sendOsc(img.pixels);

		first = false;
		canvas.fill(0);
		canvas.text(
				"Sending 100 inputs to port 6448 using message /wek/inputs",
				10, 10);

	}

	public void sendOsc(int[] px) {
		OscMessage msg = new OscMessage("/wek/inputs");
		// msg.add(px);
		for (int i = 0; i < px.length; i++) {
			msg.add(PApplet.parseFloat(px[i]));
		}
		oscP5.send(msg, dest);
	}

}
