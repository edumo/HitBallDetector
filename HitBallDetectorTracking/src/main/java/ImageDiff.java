import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;
import spout.Spout;
import gab.opencv.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class ImageDiff extends PApplet {

	OpenCV opencv;
	PImage before, after, grayDiff;
	Spout spout;
	PImage img;

	PGraphics videoDownsampling;

	public void setup() {
		// before = loadImage("before.jpg");
		// after = loadImage("after.jpg");

		opencv = new OpenCV(this, 1280 / 2, 720 / 2);
		// opencv.diff(after);
		// grayDiff = opencv.getSnapshot();

		// opencv.useColor();
		// opencv.loadImage(after);
		// opencv.diff(after);
		// colorDiff = opencv.getSnapshot();
		videoDownsampling = createGraphics(640, 360, P2D);

		spout = new Spout(this);

		spout.createReceiver("VideoSpoutDown");
		img = createImage(1280 / 2, 720 / 2, ARGB);

	}

	public void draw() {

		img = spout.receiveTexture(img);

		if (img == null) {
			return;
		}

		videoDownsampling.beginDraw();
		videoDownsampling.background(0);
		videoDownsampling.image(img, 0, 0);
		videoDownsampling.endDraw();
		
		opencv.diff(videoDownsampling);
		
//		image(opencv.getSnapshot(), 640, 0);
		opencv.dilate();
		opencv.blur(8);
		opencv.threshold(50);
		
		
		image(opencv.getSnapshot(), 0, 0);
		
//		image(img, 0, 0);

		opencv.loadImage(videoDownsampling);

//		opencv.threshold(50);
		
	
		
		text(frameRate,10,10);

		/*
		 * pushMatrix(); scale(0.5f); image(before, 0, 0); image(after,
		 * before.width, 0); // image(colorDiff, 0, before.height);
		 * image(grayDiff, before.width, before.height); popMatrix();
		 * 
		 * fill(255); text("before", 10, 20); text("after", before.width / 2 +
		 * 10, 20); text("gray diff", before.width / 2 + 10, before.height / 2 +
		 * 20);
		 * 
		 * // text("color diff", 10, before.height/2+ 20);
		 */
	}

	public void settings() {
		size(1280, 720, P2D);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "ImageDiff" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
