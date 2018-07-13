import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import gab.opencv.*;
import java.awt.Rectangle;
import processing.video.*;
import controlP5.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class ImageFilteringWithBlobPersistence extends PApplet {

	/**
	 * Image Filtering This sketch will help us to adjust the filter values to
	 * optimize blob detection
	 * 
	 * Persistence algorithm by Daniel Shifmann:
	 * http://shiffman.net/2011/04/26/opencv-matching-faces-over-time/
	 *
	 * @author: Jordi Tost (@jorditost)
	 * @url: https://github.com/jorditost/ImageFiltering/tree/master/
	 *       ImageFilteringWithBlobPersistence
	 *
	 *       University of Applied Sciences Potsdam, 2014
	 *
	 *       It requires the ControlP5 Processing library:
	 *       http://www.sojamo.de/libraries/controlP5/
	 */

	OpenCV opencv;
	Movie video;
	PImage src, preProcessedImage, processedImage, contoursImage;

	ArrayList<Contour> contours;

	// List of detected contours parsed as blobs (every frame)
	ArrayList<Contour> newBlobContours;

	// List of my blob objects (persistent)
	ArrayList<Blob> blobList;

	// Number of blobs detected over all time. Used to set IDs.
	int blobCount = 0;

	float contrast = 1.35f;
	int brightness = 0;
	int threshold = 75;
	boolean useAdaptiveThreshold = false; // use basic thresholding
	int thresholdBlockSize = 489;
	int thresholdConstant = 45;
	int blobSizeThreshold = 20;
	int blurSize = 4;

	// Control vars
	ControlP5 cp5;
	int buttonColor;
	int buttonBgColor;

	PGraphics videoDownsampling;

	public void setup() {
		frameRate(60);

		video = new Movie(this, "test1.mp4");
		// video = new Capture(this, 640, 480, "USB2.0 PC CAMERA");
		video.loop();

		opencv = new OpenCV(this, 640, 480);
		contours = new ArrayList<Contour>();

		// Blobs list
		blobList = new ArrayList<Blob>();

		// Init Controls
		cp5 = new ControlP5(this);
		initControls();

		// Set thresholding
		toggleAdaptiveThreshold(useAdaptiveThreshold);

		videoDownsampling = createGraphics(640, 480,P2D);
	}

	public void draw() {

		// Read last captured frame
		if (video.available()) {
			video.read();

			videoDownsampling.beginDraw();
			videoDownsampling.image(video,0, 0, videoDownsampling.width,
					videoDownsampling.height);
			videoDownsampling.endDraw();

			// Load the new frame of our camera in to OpenCV
			opencv.loadImage(videoDownsampling);
			src = opencv.getSnapshot();

			// /////////////////////////////
			// <1> PRE-PROCESS IMAGE
			// - Grey channel
			// - Brightness / Contrast
			// /////////////////////////////

			// Gray channel
			opencv.gray();

			// opencv.brightness(brightness);
			opencv.contrast(contrast);

			// Save snapshot for display
			preProcessedImage = opencv.getSnapshot();

			// /////////////////////////////
			// <2> PROCESS IMAGE
			// - Threshold
			// - Noise Supression
			// /////////////////////////////

			// Adaptive threshold - Good when non-uniform illumination
			if (useAdaptiveThreshold) {

				// Block size must be odd and greater than 3
				if (thresholdBlockSize % 2 == 0)
					thresholdBlockSize++;
				if (thresholdBlockSize < 3)
					thresholdBlockSize = 3;

				opencv.adaptiveThreshold(thresholdBlockSize, thresholdConstant);

				// Basic threshold - range [0, 255]
			} else {
				opencv.threshold(threshold);
			}

			// Invert (black bg, white blobs)
			opencv.invert();

			// Reduce noise - Dilate and erode to close holes
			opencv.dilate();
			opencv.erode();

			// Blur
			opencv.blur(blurSize);

			// Save snapshot for display
			processedImage = opencv.getSnapshot();

			// /////////////////////////////
			// <3> FIND CONTOURS
			// /////////////////////////////

			detectBlobs();
			// Passing 'true' sorts them by descending area.
			// contours = opencv.findContours(true, true);

			// Save snapshot for display
			contoursImage = opencv.getSnapshot();

			// Draw
			pushMatrix();

			// Leave space for ControlP5 sliders
			translate(width - src.width, 0);

			// Display images
			displayImages();

			// Display contours in the lower right window
			pushMatrix();
			scale(0.5f);
			translate(src.width, src.height);

			// Contours
			// displayContours();
			// displayContoursBoundingBoxes();

			// Blobs
			displayBlobs();

			popMatrix();

			popMatrix();
		}
	}

	// /////////////////////
	// Display Functions
	// /////////////////////

	public void displayImages() {

		pushMatrix();
		scale(0.5f);
		image(src, 0, 0);
		image(preProcessedImage, src.width, 0);
		image(processedImage, 0, src.height);
		image(src, src.width, src.height);
		popMatrix();

		stroke(255);
		fill(255);
		textSize(12);
		text("Source", 10, 25);
		text("Pre-processed Image", src.width / 2 + 10, 25);
		text("Processed Image", 10, src.height / 2 + 25);
		text("Tracked Points", src.width / 2 + 10, src.height / 2 + 25);
	}

	public void displayBlobs() {

		for (Blob b : blobList) {
			strokeWeight(1);
			b.display();
		}
	}

	public void displayContours() {

		// Contours
		for (int i = 0; i < contours.size(); i++) {

			Contour contour = contours.get(i);

			noFill();
			stroke(0, 255, 0);
			strokeWeight(3);
			contour.draw();
		}
	}

	public void displayContoursBoundingBoxes() {

		for (int i = 0; i < contours.size(); i++) {

			Contour contour = contours.get(i);
			Rectangle r = contour.getBoundingBox();

			if (// (contour.area() > 0.9 * src.width * src.height) ||
			(r.width < blobSizeThreshold || r.height < blobSizeThreshold))
				continue;

			stroke(255, 0, 0);
			fill(255, 0, 0, 150);
			strokeWeight(2);
			rect(r.x, r.y, r.width, r.height);
		}
	}

	// //////////////////
	// Blob Detection
	// //////////////////

	public void detectBlobs() {

		// Contours detected in this frame
		// Passing 'true' sorts them by descending area.
		contours = opencv.findContours(true, true);

		newBlobContours = getBlobsFromContours(contours);

		// println(contours.length);

		// Check if the detected blobs already exist are new or some has
		// disappeared.

		// SCENARIO 1
		// blobList is empty
		if (blobList.isEmpty()) {
			// Just make a Blob object for every face Rectangle
			for (int i = 0; i < newBlobContours.size(); i++) {
				println("+++ New blob detected with ID: " + blobCount);
				blobList.add(new Blob(this, blobCount, newBlobContours.get(i)));
				blobCount++;
			}

			// SCENARIO 2
			// We have fewer Blob objects than face Rectangles found from OpenCV
			// in this frame
		} else if (blobList.size() <= newBlobContours.size()) {
			boolean[] used = new boolean[newBlobContours.size()];
			// Match existing Blob objects with a Rectangle
			for (Blob b : blobList) {
				// Find the new blob newBlobContours.get(index) that is closest
				// to blob b
				// set used[index] to true so that it can't be used twice
				float record = 50000;
				int index = -1;
				for (int i = 0; i < newBlobContours.size(); i++) {
					float d = dist(newBlobContours.get(i).getBoundingBox().x,
							newBlobContours.get(i).getBoundingBox().y,
							b.getBoundingBox().x, b.getBoundingBox().y);
					// float d = dist(blobs[i].x, blobs[i].y, b.r.x, b.r.y);
					if (d < record && !used[i]) {
						record = d;
						index = i;
					}
				}
				// Update Blob object location
				used[index] = true;
				b.update(newBlobContours.get(index));
			}
			// Add any unused blobs
			for (int i = 0; i < newBlobContours.size(); i++) {
				if (!used[i]) {
					println("+++ New blob detected with ID: " + blobCount);
					blobList.add(new Blob(this, blobCount, newBlobContours
							.get(i)));
					// blobList.add(new Blob(blobCount, blobs[i].x, blobs[i].y,
					// blobs[i].width, blobs[i].height));
					blobCount++;
				}
			}

			// SCENARIO 3
			// We have more Blob objects than blob Rectangles found from OpenCV
			// in this frame
		} else {
			// All Blob objects start out as available
			for (Blob b : blobList) {
				b.available = true;
			}
			// Match Rectangle with a Blob object
			for (int i = 0; i < newBlobContours.size(); i++) {
				// Find blob object closest to the newBlobContours.get(i)
				// Contour
				// set available to false
				float record = 50000;
				int index = -1;
				for (int j = 0; j < blobList.size(); j++) {
					Blob b = blobList.get(j);
					float d = dist(newBlobContours.get(i).getBoundingBox().x,
							newBlobContours.get(i).getBoundingBox().y,
							b.getBoundingBox().x, b.getBoundingBox().y);
					// float d = dist(blobs[i].x, blobs[i].y, b.r.x, b.r.y);
					if (d < record && b.available) {
						record = d;
						index = j;
					}
				}
				// Update Blob object location
				Blob b = blobList.get(index);
				b.available = false;
				b.update(newBlobContours.get(i));
			}
			// Start to kill any left over Blob objects
			for (Blob b : blobList) {
				if (b.available) {
					b.countDown();
					if (b.dead()) {
						b.delete = true;
					}
				}
			}
		}

		// Delete any blob that should be deleted
		for (int i = blobList.size() - 1; i >= 0; i--) {
			Blob b = blobList.get(i);
			if (b.delete) {
				blobList.remove(i);
			}
		}
	}

	public ArrayList<Contour> getBlobsFromContours(
			ArrayList<Contour> newContours) {

		ArrayList<Contour> newBlobs = new ArrayList<Contour>();

		// Which of these contours are blobs?
		for (int i = 0; i < newContours.size(); i++) {

			Contour contour = newContours.get(i);
			Rectangle r = contour.getBoundingBox();

			if (// (contour.area() > 0.9 * src.width * src.height) ||
			(r.width < blobSizeThreshold || r.height < blobSizeThreshold))
				continue;

			newBlobs.add(contour);
		}

		return newBlobs;
	}

	// ////////////////////////
	// CONTROL P5 Functions
	// ////////////////////////

	public void initControls() {
		// Slider for contrast
		cp5.addSlider("contrast").setLabel("contrast").setPosition(20, 50)
				.setRange(0.0f, 6.0f);

		// Slider for threshold
		cp5.addSlider("threshold").setLabel("threshold").setPosition(20, 110)
				.setRange(0, 255);

		// Toggle to activae adaptive threshold
		cp5.addToggle("toggleAdaptiveThreshold")
				.setLabel("use adaptive threshold").setSize(10, 10)
				.setPosition(20, 144);

		// Slider for adaptive threshold block size
		cp5.addSlider("thresholdBlockSize").setLabel("a.t. block size")
				.setPosition(20, 180).setRange(1, 700);

		// Slider for adaptive threshold constant
		cp5.addSlider("thresholdConstant").setLabel("a.t. constant")
				.setPosition(20, 200).setRange(-100, 100);

		// Slider for blur size
		cp5.addSlider("blurSize").setLabel("blur size").setPosition(20, 260)
				.setRange(1, 20);

		// Slider for minimum blob size
		cp5.addSlider("blobSizeThreshold").setLabel("min blob size")
				.setPosition(20, 290).setRange(0, 60);

		// Store the default background color, we gonna need it later
		buttonColor = cp5.getController("contrast").getColor().getForeground();
		buttonBgColor = cp5.getController("contrast").getColor()
				.getBackground();
	}

	public void toggleAdaptiveThreshold(boolean theFlag) {

		useAdaptiveThreshold = theFlag;

		if (useAdaptiveThreshold) {

			// Lock basic threshold
			setLock(cp5.getController("threshold"), true);

			// Unlock adaptive threshold
			setLock(cp5.getController("thresholdBlockSize"), false);
			setLock(cp5.getController("thresholdConstant"), false);

		} else {

			// Unlock basic threshold
			setLock(cp5.getController("threshold"), false);

			// Lock adaptive threshold
			setLock(cp5.getController("thresholdBlockSize"), true);
			setLock(cp5.getController("thresholdConstant"), true);
		}
	}

	public void setLock(Controller theController, boolean theValue) {

		theController.setLock(theValue);

		if (theValue) {
			theController.setColorBackground(color(150, 150));
			theController.setColorForeground(color(100, 100));

		} else {
			theController.setColorBackground(color(buttonBgColor));
			theController.setColorForeground(color(buttonColor));
		}
	}

	/**
	 * Blob Class
	 *
	 * Based on this example by Daniel Shiffman:
	 * http://shiffman.net/2011/04/26/opencv-matching-faces-over-time/
	 * 
	 * @author: Jordi Tost (@jorditost)
	 * 
	 *          University of Applied Sciences Potsdam, 2014
	 */

	class Blob {

		private PApplet parent;

		// Contour
		public Contour contour;

		// Am I available to be matched?
		public boolean available;

		// Should I be deleted?
		public boolean delete;

		// How long should I live if I have disappeared?
		private int initTimer = 5; // 127;
		public int timer;

		// Unique ID for each blob
		int id;

		// Make me
		Blob(PApplet parent, int id, Contour c) {
			this.parent = parent;
			this.id = id;
			this.contour = new Contour(parent, c.pointMat);

			available = true;
			delete = false;

			timer = initTimer;
		}

		// Show me
		public void display() {
			Rectangle r = contour.getBoundingBox();

			float opacity = map(timer, 0, initTimer, 0, 127);
			fill(0, 0, 255, opacity);
			stroke(0, 0, 255);
			rect(r.x, r.y, r.width, r.height);
			fill(255, 2 * opacity);
			textSize(26);
			text("" + id, r.x + 10, r.y + 30);
		}

		// Give me a new contour for this blob (shape, points, location, size)
		// Oooh, it would be nice to lerp here!
		public void update(Contour newC) {

			contour = new Contour(parent, newC.pointMat);

			// Is there a way to update the contour's points without creating a
			// new one?
			/*
			 * ArrayList<PVector> newPoints = newC.getPoints(); Point[]
			 * inputPoints = new Point[newPoints.size()];
			 * 
			 * for(int i = 0; i < newPoints.size(); i++){ inputPoints[i] = new
			 * Point(newPoints.get(i).x, newPoints.get(i).y); }
			 * contour.loadPoints(inputPoints);
			 */

			timer = initTimer;
		}

		// Count me down, I am gone
		public void countDown() {
			timer--;
		}

		// I am deed, delete me
		public boolean dead() {
			if (timer < 0)
				return true;
			return false;
		}

		public Rectangle getBoundingBox() {
			return contour.getBoundingBox();
		}
	}

	public void settings() {
		size(840, 480, P2D);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "ImageFilteringWithBlobPersistence" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
