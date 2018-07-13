import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;
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

public class LoopSpout extends PApplet {

	/**
	 * Loop.
	 * 
	 * Shows how to load and play a QuickTime movie file.
	 *
	 */

	Movie movie;

	// DECLARE A SPOUT OBJECT
	Spout spout;

	public void setup() {

		background(0);
		// Load and play the video in a loop
		movie = new Movie(this, "test1.mp4");
		movie.loop();
		frameRate(60);

		// CREATE A NEW SPOUT OBJECT
		spout = new Spout(this);

		// CREATE A NAMED SENDER
		// A sender can be created now with any name.
		// Otherwise a sender is created the first time
		// "sendTexture" is called and the sketch
		// folder name is used.
		spout.createSender("VideoSpout");
	}

	public void movieEvent(Movie m) {
		m.read();
	}

	public void draw() {
		// if (movie.available() == true) {
		// movie.read();
		// }
		image(movie, 0, 0, width, height);
		spout.sendTexture(movie);
	}

	public void settings() {
		size(1280, 720, P2D);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "LoopSpout" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
