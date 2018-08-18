import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;
import processing.video.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.awt.Rectangle;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class ExtractROIDataSet extends PApplet {

	/**
	 * Frames by Andres Colubri.
	 * 
	 * Moves through the video one frame at the time by using the arrow keys. It
	 * estimates the frame counts using the framerate of the movie file, so it
	 * might not be exact in some cases.
	 */

	PImage imgSrc = null;
	int indexImg = 0;
	int newFrame = 100;

	Rectangle currentROI = new Rectangle();
	PVector topLeft = new PVector();
	PVector bottomRight = new PVector();
	XML xml;
	XML imagesXML;

	boolean drawing = false;

	PImage img;

	int train_w = 10;
	int train_h = 10;

	SimpleVideoInputWithProcessing_100Inputs wekinatorVideo;

	boolean sendHits = true;

	String path = "C:/Users/motografica/git/HitBallDetectorDataSet/hitsAndBalls/";

	public void setup() {

		background(0);

		// imgs = new PImage[13];

		// imgs = new PImage[1];

		// for (int i = 0; i < imgs.length; i++) {
		// String id = "";
		// if (i < 10) {
		// id = "0";
		// }
		// imgs[i] = loadImage("hits/capture" + id + i + ".png");
		// // imgs[i] = loadImage("faces/capture" + id + i + ".jpg");
		// }

		xml = loadXML(path + "training.xml");
		// xml = loadXML("faces/testload.xml");
		imagesXML = xml.getChild("images");
		getCurrentBox();

		img = createImage(train_w, train_h, RGB);

		wekinatorVideo = new SimpleVideoInputWithProcessing_100Inputs();
		wekinatorVideo.setup(this, train_w, train_h);

		noSmooth();
	}

	public void movieEvent(Movie m) {
		m.read();
	}

	public void draw() {
		background(0);
		if (imgSrc == null) {
			return;
		}
		image(imgSrc, 0, 0, width, height);
		fill(255);

		noFill();
		stroke(255, 0, 0);
		rectMode(CORNER);
		rect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y
				- topLeft.y);

		text("currentIndex " + indexImg, 10, 10);

		image(img, 0, 20, 100, 100);

		if (topLeft.x > 0)
			wekinatorVideo.send(g, img);

		if (keyPressed && key == ' ') {
			int temp = indexImg + 1;
			if (temp < imagesXML.getChildren("image").length) {
				indexImg = temp;
			}
			getCurrentBox();
		}
	}

	private String getName() {
		return Integer.toString(indexImg) + ".jpg";
	}

	private XML getCurrentXML() {
		XML[] images = imagesXML.getChildren("image");

		return images[indexImg];
	}

	private void getCurrentBox() {

		XML xml = getCurrentXML();

		String file = xml.getString("file");

		imgSrc = loadImage(path + file);

		if (xml != null) {
			XML[] boxes = xml.getChildren("box");
			if (boxes != null && boxes.length > 0) {
				boolean withHit = false;
				boolean withBall = false;

				XML box = null;
				XML boxHit = null;
				XML boxBall = null;
				for (int i = 0; i < boxes.length; i++) {
					XML xmlTemp = boxes[i];
					XML label = xmlTemp.getChild("label");
					String ignore = xmlTemp.getString("ignore");
					if (ignore != null && xmlTemp != null
							&& ignore.equals("1")) {
						continue;
					}

					if (label != null && label.getContent().equals("hit")) {
						boxHit = xmlTemp;
						withHit = true;
					}

					if (label != null && label.getContent().equals("ball")) {
						boxBall = xmlTemp;
						withBall = true;
					}
				}

				if (sendHits) {
					box = boxHit;
				} else {
					if (boxHit == null)
						box = boxBall;
				}

				if (box != null) {
					XML label = box.getChild("label");

					float scale = 1.3f;

					float h = box.getInt("height") * scale;
					float w = box.getInt("width") * scale;
					int x = box.getInt("left");
					int y = box.getInt("top");

					topLeft.x = x - box.getInt("width")*0.15f;
					topLeft.y = y - box.getInt("height")*0.15f;

					bottomRight.x = x + w;
					bottomRight.y = y + h;
				} else {
					topLeft.x = 0;
					topLeft.y = 0;

					bottomRight.x = 0;
					bottomRight.y = 0;

				}
			}
		}

		if (img != null && bottomRight.x > 0) {
			img.copy(imgSrc, (int) topLeft.x, (int) topLeft.y,
					(int) (bottomRight.x - topLeft.x),
					(int) (bottomRight.y - topLeft.y), 0, 0, img.width,
					img.height);

			img.save("hits/" + indexImg + ".jpg");
		}
	}

	public void keyPressed() {

		int temp = indexImg;
		if (key == 'q') {
			temp = indexImg + 1;
		}
		if (key == 'w') {
			temp = indexImg - 1;
		}

		indexImg = temp;
		getCurrentBox();
	}

	public void settings() {
		size(1280, 720, P2D);
		// size(500, 375);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "ExtractROIDataSet" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
