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

public class VideoRectangleXML extends PApplet {

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

	XML outputFile;
	int newFrame = 0;

	public void setup() {

		background(0);
		// Load and play the video in a loop
		movie1 = new Movie(this, "test1.mp4");
		movie2 = new Movie(this, "test2.mp4");
		movie3 = new Movie(this, "test3.mp4");

		movie = movie1;
		movie.pause();
		
		setFrame(40);
	}

	public void movieEvent(Movie m) {
		m.read();
	}

	public void draw() {
		//movie.play();
		image(movie,0,0);
	}

	@Override
	public void keyPressed() {
		//if (key == CODED) {
			if (keyCode == LEFT) {
				if (0 < newFrame)
					newFrame--;
			} else if (keyCode == RIGHT) {
				if (newFrame < getLength() - 1)
					newFrame++;
			}
		//}
		setFrame(newFrame);
	}

	public int getFrame() {
		return ceil(movie.time() * 30) - 1;
	}

	public void setFrame(int n) {
		movie.play();

		// The duration of a single frame:
		float frameDuration = 1.0f / movie.frameRate;

		// We move to the middle of the frame by adding 0.5:
		float where = (n + 0.5f) * frameDuration;

		// Taking into account border effects:
		float diff = movie.duration() - where;
		if (diff < 0) {
			where += diff - 0.25 * frameDuration;
		}

		movie.jump(where);
		movie.pause();
		
		println(where);
		//movie.read();
	}

	public int getLength() {
		return (int) (movie.duration() * movie.frameRate);
	}

	public void settings() {
		size(1280, 720, P2D);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "VideoRectangleXML" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
