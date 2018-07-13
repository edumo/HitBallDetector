import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import processing.video.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class Loop extends PApplet {

	/**
	 * Loop.
	 * 
	 * Shows how to load and play a QuickTime movie file.
	 *
	 */

	Movie movie;

	public void setup() {

		background(0);
		// Load and play the video in a loop
		movie = new Movie(this, "transit.mov");
		movie.loop();
	}

	public void movieEvent(Movie m) {
		m.read();
	}

	public void draw() {
		// if (movie.available() == true) {
		// movie.read();
		// }
		image(movie, 0, 0, width, height);
	}

	public void settings() {
		size(640, 360, P2D);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "Loop" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
