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

public class FramesByImage extends PApplet {

	/**
	 * Frames by Andres Colubri.
	 * 
	 * Moves through the video one frame at the time by using the arrow keys. It
	 * estimates the frame counts using the framerate of the movie file, so it
	 * might not be exact in some cases.
	 */

	PImage[] imgs = null;
	int indexImg = 0;
	int newFrame = 100;

	Rectangle currentROI = new Rectangle();
	PVector topLeft = new PVector();
	PVector bottomRight = new PVector();
	XML xml;
	XML imagesXML;

	boolean drawing = false;

	public void setup() {

		background(0);

		imgs = new PImage[13];

		// imgs = new PImage[1];

		for (int i = 0; i < imgs.length; i++) {
			String id = "";
			if (i < 10) {
				id = "0";
			}
			imgs[i] = loadImage("hits/capture" + id + i + ".png");
			// imgs[i] = loadImage("faces/capture" + id + i + ".jpg");
		}

		xml = loadXML("testing_balls.xml");
		// xml = loadXML("faces/testload.xml");
		imagesXML = xml.getChild("images");
		getCurrentBox();
	}

	public void movieEvent(Movie m) {
		m.read();
	}

	public void draw() {
		background(0);
		image(imgs[indexImg], 0, 0, width, height);
		fill(255);

		noFill();
		stroke(255, 0, 0);
		rectMode(CORNER);
		rect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y
				- topLeft.y);

		if (drawing) {
			bottomRight.x = mouseX;
			bottomRight.y = mouseY;
		}

		text("currentIndex " + indexImg, 10, 10);
	}

	public void mousePressed() {
		topLeft.x = mouseX;
		topLeft.y = mouseY;
		bottomRight.x = mouseX;
		bottomRight.y = mouseY;
		drawing = true;
	}

	public void mouseReleased() {
		bottomRight.x = mouseX;
		bottomRight.y = mouseY;
		drawing = false;
	}

	private String getName() {
		return Integer.toString(indexImg) + ".jpg";
	}

	private void saveCurrentXML() {
		XML boxChild = null;
		String fileName = getName();

		XML exists = getCurrentXML();
		if (exists == null) {
			XML imageChild = imagesXML.addChild("image");

			imageChild.setString("file", fileName);
			boxChild = imageChild.addChild("box");
		} else {
			boxChild = exists.getChild("box");
		}

		boxChild.setInt("top", (int) topLeft.y);
		boxChild.setInt("left", (int) topLeft.x);
		boxChild.setInt("width", (int) (bottomRight.x - topLeft.x));
		boxChild.setInt("height", (int) (bottomRight.y - topLeft.y));
		saveXML(xml, "data/testing_balls.xml");
		imgs[indexImg].save("data/" + fileName);
	}

	private XML getCurrentXML() {
		XML[] images = imagesXML.getChildren("image");
		for (int i = 0; i < images.length; i++) {
			XML xml = images[i];
			String name = xml.getString("file");
			if (name.equals(getName())) {
				return xml;
			}
		}
		return null;
	}

	private void getCurrentBox() {
		XML xml = getCurrentXML();

		if (xml != null) {
			XML box = xml.getChild("box");
			int h = box.getInt("height");
			int w = box.getInt("width");
			int x = box.getInt("left");
			int y = box.getInt("top");

			topLeft.x = x;
			topLeft.y = y;

			bottomRight.x = x + w;
			bottomRight.y = y + h;
		} else {
			topLeft.x = 0;
			topLeft.y = 0;

			bottomRight.x = 0;
			bottomRight.y = 0;

		}
	}

	public void keyPressed() {

		int temp = 0;
		if (key == 'q') {
			temp = indexImg + 1;
		}
		if (key == 'w') {
			temp = indexImg - 1;
		}
		if (temp < 0) {
			temp = 0;
		}
		if (temp >= imgs.length) {
			temp = 0;
		}

		if (key == ' ') {
			saveCurrentXML();
		}

		indexImg = temp;
		getCurrentBox();
	}

	public void settings() {
		size(1280, 720, P2D);
		// size(500, 375);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "FramesByImage" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
