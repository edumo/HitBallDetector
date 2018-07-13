import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;
import gab.opencv.*;
import processing.video.*;
import spout.Spout;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class BackgroundSubtraction extends PApplet {

	Movie video;
	OpenCV opencv;

	PGraphics videoDownsampling;

	Spout spout;
	PImage img;

	public void setup() {

		// video = new Movie(this, "test1.mp4");
		opencv = new OpenCV(this, 640, 360);

		opencv.startBackgroundSubtraction(5, 3, 0.5f);

		// video.loop();
		// video.play();

		frameRate(60);

		videoDownsampling = createGraphics(640, 360, P2D);

		spout = new Spout(this);

		spout.createReceiver("VideoSpoutDown");
		img = createImage(1280, 720, ARGB);
	}

	public void draw() {

		img = spout.receiveTexture(img);

		if (img == null) {
			return;
		}

		// image(img, 0, 0);

		videoDownsampling.beginDraw();
		videoDownsampling.background(0);
		videoDownsampling.image(img, 0, 0);
		videoDownsampling.endDraw();

		image(videoDownsampling, 0, 0);

		opencv.loadImage(videoDownsampling);

		opencv.updateBackground();

//		 image(opencv.getSnapshot(),0,0);

		opencv.dilate();
		opencv.erode();
		
//		opencv.contrast(1.5f);

		noFill();
		stroke(255, 0, 0);
		strokeWeight(3);
		for (Contour contour : opencv.findContours()) {
			contour.draw();
		}

		text(frameRate, 10, 10);
	}

	public void movieEvent(Movie m) {
		m.read();
	}

	public void settings() {
		size(1280, 720, P3D);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "BackgroundSubtraction" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
