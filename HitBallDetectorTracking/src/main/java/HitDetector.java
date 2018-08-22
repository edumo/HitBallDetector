import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;
import gab.opencv.*;

import java.awt.Rectangle;

import processing.video.*;
import spout.Spout;
import controlP5.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import deadpixel.keystone.CornerPinSurface;
import deadpixel.keystone.Keystone;

public class HitDetector extends PApplet {

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
	// Movie video;
	Spout spout;
	PImage img;

	PImage src, preProcessedImage, processedImage, contoursImage;

	ArrayList<Contour> contours;

	// List of detected contours parsed as blobs (every frame)
	ArrayList<Contour> newBlobContours;

	// List of my blob objects (persistent)
	ArrayList<Blob> blobList;

	// Number of blobs detected over all time. Used to set IDs.
	int blobCount = 0;

	float contrast = 1.05f;
	int brightness = 0;
	int threshold = 105;

	int thresholdBlockSize = 100;
	int thresholdConstant = 35;
	int minBlobSizeThreshold = 5;
	int maxBlobSizeThreshold = 80;

	float velocityUpThreshold = 0.1f;
	float velocityDownThreshold = 4f;

	int blurSize = 12;

	// Control vars
	ControlP5 cp5;
	int buttonColor;
	int buttonBgColor;

	PGraphics videoDownsampling;

	boolean debug = false;

	boolean useBackgroundSubtraction = true;
	boolean withThreshold = false;
	boolean useAdaptiveThreshold = false; // use basic thresholding

	Keystone ks;
	CornerPinSurface surface;

	PGraphics offscreen;

	Debouncing debouncing;

	OscP5 oscP5Wekinator;
	OscP5 oscP5Hits;
	NetAddress destWekinator;
	NetAddress destHits;

	int receiveDataOnOSCPort = 12000;
	int sendReceivedDataToPort = 7448;
	int sendReceivedDataToPortHits = 7010;

	PImage imgCut;

	SimpleVideoInputWithProcessing_100Inputs wekinator;

	long time = 0;

	PVector lastBlobPosition = new PVector();

	LinkedList<WekinatorJob> positionsWaitWekinator = new LinkedList<WekinatorJob>();
	LinkedList<Blob> lastBlobs = new LinkedList<Blob>();

	public void setup() {
		frameRate(60);

		// video = new Movie(this, "test1.mp4");
		// // video = new Capture(this, 640, 480, "USB2.0 PC CAMERA");
		// video.loop();

		spout = new Spout(this);

		spout.createReceiver("VideoSpoutDown");
		img = createImage(640, 360, ARGB);

		contours = new ArrayList<Contour>();

		// Blobs list
		blobList = new ArrayList<Blob>();

		// Init Controls
		cp5 = new ControlP5(this);
		cp5.setPosition(0, 0);
		initControls();

		// Set thresholding
		videoDownsampling = createGraphics(640, 360, P2D);

		bar(2);

		ks = new Keystone(this);
		surface = ks.createCornerPinSurface(640, 360, 20);

		// We need an offscreen buffer to draw the surface we
		// want projected
		// note that we're matching the resolution of the
		// CornerPinSurface.
		// (The offscreen buffer can be P2D or P3D)
		offscreen = createGraphics(640, 360, P3D);

		debouncing = new Debouncing(new ArrayList<PVector>(), 100, .2f, this);
		try {
			ks.load();
		} catch (Exception e) {

		}
		// OSC messages
		oscP5Wekinator = new OscP5(this, receiveDataOnOSCPort); // listen for
																// incoming
		oscP5Hits = new OscP5(this, receiveDataOnOSCPort + 1); // listen for
																// incoming

		destWekinator = new NetAddress("127.0.0.1", sendReceivedDataToPort); // Set
																				// up
		destHits = new NetAddress("127.0.0.1", sendReceivedDataToPortHits); // Set
																			// up

		// sender to
		// send to
		// desired
		// port

		imgCut = createImage(10, 10, RGB);

		wekinator = new SimpleVideoInputWithProcessing_100Inputs();
		wekinator.setup(this, 10, 10);
	}

	void oscEvent(OscMessage theOscMessage) {

		WekinatorJob job = positionsWaitWekinator.removeLast();
		PVector pos = job.pos;
		// println((millis() - time) +
		// "### received an osc message.pending "+positionsWaitWekinator.size());
		if (theOscMessage.get(0).floatValue() == 1.0f) {
//			print(" addrpattern: " + theOscMessage.addrPattern() + " pending "
//					+ positionsWaitWekinator.size());
//			println(" typetag: hit ");
			job.blob.lastWekinatorHits.add(pos);
			job.blob.hitedDnn = true;

		}
	}

	public void draw() {

		background(0);

		img = spout.receiveTexture(img);

		if (img == null) {
			return;
		}

		// image(img, 0, 0);

		videoDownsampling.beginDraw();
		videoDownsampling.background(0);
		videoDownsampling.image(img, 0, 0);
		videoDownsampling.endDraw();

		videoDownsampling.beginDraw();
		videoDownsampling.image(videoDownsampling, 0, 0,
				videoDownsampling.width, videoDownsampling.height);
		videoDownsampling.endDraw();

		// Load the new frame of our camera in to OpenCV

		if (debug) {
			src = opencv.getSnapshot();
		} else {

		}

		// /////////////////////////////
		// <1> PRE-PROCESS IMAGE
		// - Grey channel
		// - Brightness / Contrast
		// /////////////////////////////

		// Gray channel
		// opencv.gray();

		// opencv.brightness(brightness);
		opencv.contrast(contrast);

		// Save snapshot for display
		if (debug) {
			preProcessedImage = opencv.getSnapshot();
		}

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

			opencv.invert();

			// Reduce noise - Dilate and erode to close holes
			opencv.dilate();
			opencv.erode();

			// Blur
			opencv.blur(blurSize);

			// Basic threshold - range [0, 255]
		} else if (useBackgroundSubtraction) {
			opencv.updateBackground();
			opencv.dilate();
			opencv.dilate();
			opencv.dilate();
			opencv.blur(blurSize);
		} else {
			opencv.invert();
			opencv.threshold(threshold);
		}

		// Invert (black bg, white blobs)

		// Save snapshot for display
		if (debug) {
			processedImage = opencv.getSnapshot();
		}

		// /////////////////////////////
		// <3> FIND CONTOURS
		// /////////////////////////////

		detectBlobs();
		// Passing 'true' sorts them by descending area.
		// contours = opencv.findContours(true, true);

		// Save snapshot for display
		// if (debug) {
		// contoursImage = opencv.getSnapshot();
		// }

		// Draw
		pushMatrix();

		// Leave space for ControlP5 sliders
		translate(width - videoDownsampling.width, 0);

		// Display images
		displayImages();

		// Display contours in the lower right window
		pushMatrix();

		if (debug) {
			scale(0.5f);
			translate(videoDownsampling.width, videoDownsampling.height);
		}

		// Contours
		// displayContours();
		displayContoursBoundingBoxes();

		analyzeBlobs();

		debouncing.display(g);

		// Blobs
		displayBlobs(blobList);
		displayBlobs(lastBlobs);

		fill(255);

		popMatrix();

		popMatrix();

		text(frameRate, 10, 30);

		opencv.loadImage(videoDownsampling);

		if (!debug) {
			// offscreen.beginDraw();
			// // if (ks.isCalibrating())
			// if (!keyPressed) {
			// offscreen.blendMode(BLEND);
			// offscreen.background(255, 0, 0, 40);
			// } else {
			// offscreen.blendMode(LIGHTEST);
			// }

			surface.render(offscreen);
		}

	}

	@Override
	public void mousePressed() {
		// debouncing.addHit(new PVector(mouseX,mouseY));
	}

	int lastId = 0;

	private void analyzeBlobs() {
		debouncing.update();

		boolean sended = false;

		for (Blob b : blobList) {

			// TESTS for wekinator
			// if (b.id == lastId && b.lastAngleVariation > -33) {
			// sended = true;
			// OscMessage msg = new OscMessage("angle/");
			// msg.add(b.velocityAvg.y);
			// if (b.hited)
			// msg.add(0f);
			// else
			// msg.add(1f);
			//
			// oscP5Wekinator.send(msg, destWekinator);
			// }

			Rectangle r = b.contour.getBoundingBox();

			lastBlobPosition.x = (float) r.getCenterX();
			lastBlobPosition.y = (float) r.getCenterY();

			float w = r.width * 1.3f;
			float h = r.height * 1.3f;

			float wh = r.width * .15f;
			float hh = r.height * .15f;

			if (b.movingUp) {
				imgCut.copy(videoDownsampling, (int) (r.x - wh),
						(int) (r.y - hh), (int) w, (int) h, 0, 0, imgCut.width,
						imgCut.height);

				positionsWaitWekinator.add(new WekinatorJob(b, b.path
						.get(b.path.size() - 1)));
				wekinator.send(g, imgCut);
			}

			// image(imgCut,x,height-100,50,50);

			// imgCut.save("imgs/image-"+counter+".jpg");
			// counter++;

			if (b.hited && !b.processed && millis() > b.hitedTime + 100) {

				time = millis();

				boolean toadd = debouncing.addHit(b.hitPosition);

				if (toadd) {
					// we have a hit!!
					PVector pos = surface.getTransformedCursor(
							(int) b.hitPosition.x, (int) b.hitPosition.y);

					pos.x = norm(pos.x, 0, offscreen.width);
					pos.y = norm(pos.y, 0, offscreen.height);

					float confidence = 0.8f;// TODO

					float dist = 100000000;
					for (int i = 0; i < b.lastWekinatorHits.size(); i++) {
						PVector posHitDnn = b.lastWekinatorHits.get(i);
						float d = posHitDnn.dist(new PVector(b.hitPosition.x,b.hitPosition.y));
						if (d < dist) {
							dist = d;
						}
					}

					println(dist);
					confidence += min(0.2f,max(0,map(dist, 100, 0, 0, 0.2f)));
					hited(pos, confidence);

				} else {
					println("descartado");
				}

				b.processed = true;
			}
		}

		if (!sended && !blobList.isEmpty()) {
			lastId = blobList.get(0).id;

		}
	}

	// /////////////////////
	// Display Functions
	// /////////////////////

	private void hited(PVector pos, float confidence) {
		OscMessage msg = new OscMessage("/Touch");
		msg.add(pos.x);
		msg.add(pos.y);
		msg.add(confidence);

		oscP5Hits.send(msg, destHits);
		
		println("hited confidence "+confidence);
	}

	public void displayImages() {

		pushMatrix();
		if (debug)
			scale(0.5f);

		if (debug) {
			image(src, 0, 0);
			image(preProcessedImage, src.width, 0);
			image(processedImage, 0, src.height);
			image(src, src.width, src.height);
		} else {
			image(videoDownsampling, 0, 0);
		}
		fill(255);

		popMatrix();

		stroke(255);
		fill(255);
		textSize(12);
		text("Source", 10, 25);
		if (debug) {
			text("Pre-processed Image", src.width / 2 + 10, 25);
			text("Processed Image", 10, src.height / 2 + 25);
			text("Tracked Points", src.width / 2 + 10, src.height / 2 + 25);
		}
	}

	int counter = 0;

	public void displayBlobs(List<Blob> blobs) {

		int x = 50;
		float scale = 2;
		for (Blob b : blobs) {
			strokeWeight(1);
			b.display(g);
			noFill();
			stroke(255);
			strokeWeight(1);

			showVelGraph(x, scale, b);

			x += 150;
		}
	}

	private void showVelGraph(int x, float scale, Blob b) {
		rect(x - 20, height - 70, 40, 40);
		stroke(255, 0, 0);
		rect(x - velocityDownThreshold * scale / 2, height - 50
				- velocityDownThreshold * scale / 2, velocityDownThreshold
				* scale, velocityDownThreshold * scale);
		stroke(0, 255, 0);
		float xx = x - velocityUpThreshold * scale / 2;
		float yy = height - 50 - velocityUpThreshold * scale / 2;
		rect(xx, yy, velocityUpThreshold * scale, velocityUpThreshold * scale);
		strokeWeight(2);
		stroke(255, 150);
		// draw all vels in blending
		for (PVector vel : b.vels) {
			line(x, height - 50, x + vel.x * scale, height - 50 + vel.y * scale);
		}
		stroke(255, 0, 0);
		line(x, height - 50, x + b.velocityAvg.x * scale, height - 50
				+ b.velocityAvg.y * scale);
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
			(r.width < minBlobSizeThreshold || r.height < minBlobSizeThreshold)
					|| r.width > maxBlobSizeThreshold
					|| r.height > maxBlobSizeThreshold)
				continue;

			stroke(255, 0, 0);
			fill(255, 0, 0, 150);
			strokeWeight(2);
			rect(r.x, r.y, r.width, r.height);
			// println("" + r.width + " " + r.height);
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

		int maxDistance = 80;

		// println(contours.length);

		// Check if the detected blobs already exist are new or some has
		// disappeared.

		// SCENARIO 1
		// blobList is empty
		if (blobList.isEmpty()) {
			// Just make a Blob object for every face Rectangle
			for (int i = 0; i < newBlobContours.size(); i++) {
				// println("+++ New blob detected with ID: " + blobCount);
				blobList.add(new Blob(this, blobCount, newBlobContours.get(i),
						oscP5Wekinator, destWekinator));
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
				float record = maxDistance;
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
				if (index >= 0) {
					// Update Blob object location
					used[index] = true;
					b.update(newBlobContours.get(index), velocityDownThreshold,
							velocityUpThreshold);
				}
			}
			// Add any unused blobs
			for (int i = 0; i < newBlobContours.size(); i++) {
				if (!used[i]) {
					// println("+++ New blob detected with ID: " + blobCount);
					blobList.add(new Blob(this, blobCount, newBlobContours
							.get(i), oscP5Wekinator, destWekinator));
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
				float record = maxDistance;
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
				if (index >= 0) {
					Blob b = blobList.get(index);
					b.available = false;
					b.update(newBlobContours.get(i), velocityDownThreshold,
							velocityUpThreshold);
				}

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
				Blob blob = blobList.remove(i);
				if (blob.movingUp) {
					lastBlobs.add(blob);
					if (lastBlobs.size() > 4) {
						lastBlobs.removeFirst();
					}
				}
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
			(r.width < minBlobSizeThreshold || r.height < minBlobSizeThreshold)
					|| r.width > maxBlobSizeThreshold
					|| r.height > maxBlobSizeThreshold)
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

		// // Toggle to activae adaptive threshold
		// cp5.addToggle("toggleAdaptiveThreshold").setLabel("adaptive threshold")
		// .setSize(10, 10).setValue(useAdaptiveThreshold)
		// .setPosition(20, 144);
		//
		// // Toggle to activae adaptive threshold
		// cp5.addToggle("toggleBackgorundSubtraction").setLabel("Backgorund Subtraction")
		// .setSize(10, 10).setValue(useAdaptiveThreshold)
		// .setPosition(20, 184);
		//
		//
		// // Toggle to activae adaptive threshold
		cp5.addToggle("toggleAdaptiveDebug").setLabel("show images")
				.setSize(10, 10).setPosition(20, 184);

		ButtonBar b = cp5.addButtonBar("bar").setPosition(0, 0)
				.setSize(400, 20)
				.addItems(split("threshold adaptative bg-substraction", " "));
		b.changeItem("bg-substraction", "selected", true);

		Slider velocityDownThreshold;
		Slider velocityUpThreshold;
		Slider max;
		Slider blur;
		Slider threshold;
		Slider min;
		Slider thresholdBlock;

		// Slider for adaptive threshold block size
		thresholdBlock = cp5.addSlider("thresholdBlockSize")
				.setLabel("a.t. block size").setPosition(20, 240)
				.setRange(1, 300);

		// Slider for adaptive threshold constant
		threshold = cp5.addSlider("thresholdConstant")
				.setLabel("a.t. constant").setPosition(20, 280)
				.setRange(-100, 100);

		// Slider for blur size
		blur = cp5.addSlider("blurSize").setLabel("blur size")
				.setPosition(20, 260).setRange(1, 20);

		// Slider for minimum blob size
		min = cp5.addSlider("minBlobSizeThreshold").setLabel("min blob size")
				.setPosition(20, 320).setRange(0, 60);
		max = cp5.addSlider("maxBlobSizeThreshold").setLabel("max blob size")
				.setPosition(20, 340).setRange(0, 160);

		// Slider for filtering blob movement
		velocityUpThreshold = cp5.addSlider("velocityUpThreshold")
				.setLabel("velocity up trheshold filtering blob")
				.setPosition(20, 360).setRange(0.01f, 4f);

		velocityDownThreshold = cp5.addSlider("velocityDownThreshold")
				.setLabel("velocity down trheshold filtering blob")
				.setPosition(20, 380).setRange(0.1f, 18f);

		// Store the default background color, we gonna need it later
		buttonColor = cp5.getController("contrast").getColor().getForeground();
		buttonBgColor = cp5.getController("contrast").getColor()
				.getBackground();
	}

	public void toggleAdaptiveDebug(boolean theFlag) {
		debug = !debug;
	}

	// public void toggleBackgorundSubtraction(boolean theFlag) {
	// useBackgorundSubtraction = theFlag;
	// }

	public void bar(int n) {
		println("bar clicked, item-value:", n);

		if (n == 0) {
			setLock(cp5.getController("thresholdBlockSize"), true);
			setLock(cp5.getController("thresholdConstant"), true);
			useAdaptiveThreshold = false;
			useBackgroundSubtraction = false;

			opencv = new OpenCV(this, 640, 360);

			setLock(cp5.getController("threshold"), false);
		} else if (n == 1) {
			useBackgroundSubtraction = false;

			opencv = new OpenCV(this, 640, 360);
			// // Lock basic threshold
			setLock(cp5.getController("threshold"), true);
			useAdaptiveThreshold = true;
			//
			// // Unlock adaptive threshold
			setLock(cp5.getController("thresholdBlockSize"), false);
			setLock(cp5.getController("thresholdConstant"), false);

		} else {
			setLock(cp5.getController("thresholdBlockSize"), true);
			setLock(cp5.getController("thresholdConstant"), true);
			useAdaptiveThreshold = false;

			opencv = new OpenCV(this, 640, 360);
			opencv.startBackgroundSubtraction(5, 3, 0.5f);

			useBackgroundSubtraction = true;

			// // Unlock basic threshold

			//
			// // Lock adaptive threshold
			// setLock(cp5.getController("thresholdBlockSize"), true);
			// setLock(cp5.getController("thresholdConstant"), true);
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

	String id = null;

	public void saveParams() {
		XML xml = new XML("hitdetector-settings.xml");

		xml.setString("id", id);
		xml.setFloat("contrast", contrast);
		xml.setInt("threshold", threshold);

		xml.setInt("thresholdBlockSize", thresholdBlockSize);
		xml.setInt("thresholdConstant", thresholdConstant);
		xml.setInt("minBlobSizeThreshold", minBlobSizeThreshold);
		xml.setInt("maxBlobSizeThreshold", maxBlobSizeThreshold);

		xml.setFloat("velocityUpThreshold", velocityUpThreshold);
		xml.setFloat("velocityDownThreshold", velocityDownThreshold);

		saveXML(xml, "hitdetector-settings.xml");
	}

	public void loadParams() {
		XML xml = loadXML("hitdetector-settings.xml");

		id = xml.getString("id");
		contrast = xml.getFloat("contrast");
		threshold = xml.getInt("threshold");

		thresholdBlockSize = xml.getInt("thresholdBlockSize");
		thresholdConstant = xml.getInt("thresholdConstant");
		minBlobSizeThreshold = xml.getInt("minBlobSizeThreshold");
		maxBlobSizeThreshold = xml.getInt("maxBlobSizeThreshold");

		velocityUpThreshold = xml.getFloat("velocityUpThreshold");
		velocityDownThreshold = xml.getFloat("velocityDownThreshold");

		// float contrast = 1.05f;
		// int brightness = 0;
		// int threshold = 105;
		//
		// int thresholdBlockSize = 100;
		// int thresholdConstant = 35;
		// int minBlobSizeThreshold = 5;
		// int maxBlobSizeThreshold = 80;
		//
		// float velocityUpThreshold = 0.1f;
		// float velocityDownThreshold = 4f;
		//
		// int blurSize = 12;

	}

	public void keyPressed() {
		switch (key) {
		case 'c':
			// enter/leave calibration mode, where surfaces can be warped
			// and moved
			ks.toggleCalibration();
			break;

		case 'l':
			// loads the saved layout
			ks.load();
			loadParams();
			break;

		case 's':
			// saves the layout
			ks.save();
			saveParams();
			break;
		}
	}

	public void settings() {
		size(840, 480, P3D);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "HitDetector" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
