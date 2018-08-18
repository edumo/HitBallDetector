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

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;

public class ImageDiff extends PApplet {

	OpenCV opencv;
	PImage before, after, grayDiff;
	Spout spout;
	PImage img;

	PGraphics videoDownsampling;

	public void setup() {
		// before = loadImage("before.jpg");
		// after = loadImage("after.jpg");

		opencv = new OpenCV(this, 64,64);
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
		
		before = loadImage("hit.jpg");
		after = loadImage("ball.jpg");

	}

	public void draw() {
		
		opencv.loadImage(after);
		
		opencv.loadImage(before);
		

//		img = spout.receiveTexture(img);
//
//		if (img == null) {
//			return;
//		}
//
//		videoDownsampling.beginDraw();
//		videoDownsampling.background(0);
//		videoDownsampling.image(img, 0, 0);
//		videoDownsampling.endDraw();
//		
//		opencv.diff(videoDownsampling);
//		
////		image(opencv.getSnapshot(), 640, 0);
//		opencv.dilate();
//		opencv.blur(8);
//		opencv.threshold(50);
//		
//		
//		image(opencv.getSnapshot(), 0, 0);
//		
////		image(img, 0, 0);
//
//		opencv.loadImage(videoDownsampling);

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
	
	private Mat detectFeatures(Mat skeleton, Mat edges) {
	    FeatureDetector star = FeatureDetector.create(FeatureDetector.ORB);
	    DescriptorExtractor brief = DescriptorExtractor.create(DescriptorExtractor.ORB);

	    MatOfKeyPoint keypoints = new MatOfKeyPoint();
	    star.detect(skeleton, keypoints);
	    MatOfKeyPoint keypointsField = keypoints;

	    KeyPoint[] keypointArray = keypoints.toArray();
	    ArrayList<KeyPoint> filteredKeypointArray = new ArrayList<>(keypointArray.length);

	    int filterCount = 0;
	    for (KeyPoint k : keypointArray) {
	        if (edges.get((int)k.pt.y, (int)k.pt.x)[0] <= 0.0) {
	            k.size /= 8;
	            filteredKeypointArray.add(k);
	        } else {
	            filterCount++;
	        }
	    }
//	    Log.d(TAG, String.format("Filtered %s Keypoints", filterCount));

	    keypoints.fromList(filteredKeypointArray);

	    Mat descriptors = new Mat();
	    brief.compute(skeleton, keypoints, descriptors);
	    Mat descriptorsField = descriptors;

	    Mat results = new Mat();
	    Scalar color = new Scalar(255, 0, 0); // RGB
	    Features2d.drawKeypoints(skeleton, keypoints, results, color, Features2d.DRAW_RICH_KEYPOINTS);
	    return results;
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
