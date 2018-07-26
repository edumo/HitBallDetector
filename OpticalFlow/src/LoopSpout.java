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

	Movie movie1;
	Movie movie2;
	Movie movie3;

	Movie movie;

	// DECLARE A SPOUT OBJECT
	Spout spout;
	Spout spoutDown;

	PGraphics down = null;

	public void setup() {

		background(0);
		// Load and play the video in a loop
		movie1 = new Movie(this, "test4.mp4");
		movie2 = new Movie(this, "test2.mp4");
		movie3 = new Movie(this, "test3.mp4");
		
		movie = movie1;
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

		spoutDown = new Spout(this);

		// CREATE A NAMED SENDER
		// A sender can be created now with any name.
		// Otherwise a sender is created the first time
		// "sendTexture" is called and the sketch
		// folder name is used.
		spoutDown.createSender("VideoSpoutDown");

		down = createGraphics(1280 / 2, 720 / 2, P2D);
		
		
		// movie.speed(0.2f);
	}

	public void movieEvent(Movie m) {
		m.read();
	}

	public void draw() {
		// if (movie.available() == true) {
		// movie.read();
		// }
		movie.speed(1f);
		image(movie, 0, 0, width, height);

		down.beginDraw();
		down.image(movie, 0, 0, down.width, down.height);
		down.endDraw();

		//spout.sendTexture(movie);
		spoutDown.sendTexture(down);

	}

	@Override
	public void keyPressed() {

		if (key == '1') {
			movie.stop();
			movie = movie1;
			movie.loop();
		} else if (key == '2') {
			movie.stop();
			movie = movie2;
			movie.loop();
		} else if (key == '3') {
			movie.stop();
			movie = movie3;
			movie.loop();
		}
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
