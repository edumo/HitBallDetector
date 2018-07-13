import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;
import spout.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class SpoutReceiver extends PApplet {

	//
	// SpoutReceiver
	//
	// Receive from a Spout sender
	//
	// spout.zeal.co
	//
	// http://spout.zeal.co/download-spout/
	//

	// IMPORT THE SPOUT LIBRARY

	PGraphics pgr; // Canvas to receive a texture
	PImage img; // Image to receive a texture

	// DECLARE A SPOUT OBJECT
	Spout spout;

	public void setup() {

		// Initial window size

		// Needed for resizing the window to the sender size
		// Processing 3+ only
		surface.setResizable(true);

		// Create a canvas or an image to receive the data.
		pgr = createGraphics(width, height, PConstants.P2D);
		img = createImage(width, height, ARGB);

		// Graphics and image objects can be created
		// at any size, but their dimensions are changed
		// to match the sender that the receiver connects to.

		// CREATE A NEW SPOUT OBJECT
		spout = new Spout(this);

		// OPTION : CREATE A NAMED SPOUT RECEIVER
		//
		// By default, the active sender will be detected
		// when receiveTexture is called. But you can specify
		// the name of the sender to initially connect to.
		spout.createReceiver("VideoSpoutDown");
	}

	public void draw() {

		background(0);

		//
		// RECEIVE A SHARED TEXTURE
		//

		// OPTION 1: Receive and draw the texture
//		spout.receiveTexture();

		// OPTION 2: Receive into PGraphics texture
		// pgr = spout.receiveTexture(pgr);
		// image(pgr, 0, 0, width, height);

		// OPTION 3: Receive into PImage texture
		 img = spout.receiveTexture(img);
		 image(img, 0, 0, width, height);

		// OPTION 4: Receive into PImage pixels
		// img = spout.receivePixels(img);
		// image(img, 0, 0, width, height);

		// Optionally resize the window to match the sender
		// spout.resizeFrame();

	}

	// SELECT A SPOUT SENDER
	public void mousePressed() {
		// RH click to select a sender
		if (mouseButton == RIGHT) {
			// Bring up a dialog to select a sender.
			// Spout installation required
			spout.selectSender();
		}
	}

	public void settings() {
		size(1280/2, 720/2, P3D);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "SpoutReceiver" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
