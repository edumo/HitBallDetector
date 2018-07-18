import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;
import gab.opencv.*;
import processing.video.*;
import spout.Spout;

import java.util.HashMap;
import java.util.ArrayList;
import java.awt.Rectangle;
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

	ArrayList<Contour> contours;

	// List of detected contours parsed as blobs (every frame)
	ArrayList<Contour> newBlobContours;

	// List of my blob objects (persistent)
	ArrayList<Blob> blobList;

	int blobSizeThreshold = 20;
	int blobCount = 0;

	boolean withBackgorundSubtraction = true;

	public void setup() {

		// video = new Movie(this, "test1.mp4");
		opencv = new OpenCV(this, 640, 360);

		opencv.startBackgroundSubtraction(5, 3, 0.5f);

		frameRate(60);

		videoDownsampling = createGraphics(640, 360, P2D);

		spout = new Spout(this);

		spout.createReceiver("VideoSpoutDown");
		img = createImage(1280 / 2, 720 / 2, ARGB);

		contours = new ArrayList<Contour>();

		// Blobs list      
		blobList = new ArrayList<Blob>();
	}

	public void draw() {

		if (!keyPressed) {
			blendMode(BLEND);
			background(0);
		} else {
			blendMode(LIGHTEST);
		}

		img = spout.receiveTexture(img);

		if (img == null) {
			return;
		}

		// image(img, 0, 0);

		videoDownsampling.beginDraw();
		videoDownsampling.background(0);
		videoDownsampling.image(img, 0, 0);
		videoDownsampling.endDraw();

		// background(0);
		image(videoDownsampling, 0, 0);

		if (withBackgorundSubtraction) {
			bgSubstraction();
		} else {
			frameDiff();
		}

		noTint();
		tint(255, 100);
//		image(opencv.getSnapshot(), 0, 360);
		noTint();
		noFill();
		stroke(255, 0, 0);
		strokeWeight(3);
		// for (Contour contour : opencv.findContours()) {
		// contour.draw();
		// }
		pushStyle();
		detectBlobs();

		analyzeBlobs();

		displayBlobs();
		popStyle();

		fill(255);
		text(frameRate, 10, 10);

		opencv.loadImage(videoDownsampling);
	}

	private void bgSubstraction() {
		// noTint();()
		// opencv.blur(4);
		// opencv.threshold(10);
		// opencv.invert();
		// image(opencv.getSnapshot(), 0, 0);
		opencv.updateBackground();

		 opencv.dilate();
		 opencv.dilate();
		 opencv.dilate();
		 opencv.blur(12);

	}

	private void frameDiff() {
		blendMode(LIGHTEST);

		opencv.diff(videoDownsampling);

		opencv.blur(4);
		// opencv.erode();

		opencv.threshold(30);
		opencv.erode();

		// opencv.erode();
		opencv.dilate();
		opencv.dilate();
		opencv.blur(4);
		// opencv.dilate();
		// opencv.blur(4);

		// opencv.dilate();
		// opencv.dilate();
		// opencv.dilate();

	}

	private void analyzeBlobs() {
		for (Blob b : blobList) {

		}
	}

	public void displayBlobs() {
		pushStyle();
		for (Blob b : blobList) {
			strokeWeight(1);
			b.display(g);
		}
		popStyle();
	}

	// //////////////////
	// Blob Detection
	// //////////////////

	public void detectBlobs() {

		// Contours detected in this frame
		// Passing 'true' sorts them by descending area.
		contours = opencv.findContours(true, true);

		newBlobContours = getBlobsFromContours(contours);

		int maxDistance = 60;

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
					b.update(newBlobContours.get(index));
				}
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
					b.update(newBlobContours.get(i));
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
			blobSizeThreshold = 3;
			if (// (contour.area() > 0.9 * src.width * src.height) ||
			(r.width < blobSizeThreshold || r.height < blobSizeThreshold))
				continue;

			newBlobs.add(contour);
		}

		return newBlobs;
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
