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

public class Frames extends PApplet {

	/**
	 * Frames by Andres Colubri.
	 * 
	 * Moves through the video one frame at the time by using the arrow keys. It
	 * estimates the frame counts using the framerate of the movie file, so it
	 * might not be exact in some cases.
	 */

	Movie mov;
	int newFrame = 100;
	
	Rectangle currentROI = new Rectangle();
	PVector topLeft = new PVector();
	PVector bottomRight = new PVector();
	XML xml;
	XML imagesXML;

	
	public void setup() {

		background(0);
		// Load and set the video to play. Setting the video
		// in play mode is needed so at least one frame is read
		// and we can get duration, size and other information from
		// the video stream.
		mov = new Movie(this, "test1__.mp4");

		// Pausing the video at the first frame.
		mov.play();
		// mov.jump(0);
		// mov.pause();
		xml = loadXML("empty_test.xml");
		imagesXML = xml.getChild("images");
	}

	public void movieEvent(Movie m) {
		m.read();
	}

	public void draw() {
		background(0);
		image(mov, 0, 0, width, height);
		fill(255);
		text(getFrame() + " / " + (getLength() - 1), 10, 30);
		
		noFill();
		stroke(255,0,0);
		rectMode(CORNER);
		rect(topLeft.x,topLeft.y,bottomRight.x-topLeft.x,bottomRight.y-topLeft.y);
	}
	
	public void mousePressed()
	{
		topLeft.x = mouseX;
		topLeft.y = mouseY;
		bottomRight.x = mouseX;
		bottomRight.y = mouseY;
		
	}
	public void mouseReleased()
	{
		bottomRight.x = mouseX;
		bottomRight.y = mouseY;
				
	}

	private void saveCurrentXML() {
		XML imageChild = imagesXML.addChild("image");
		String fileName = Integer.toString(newFrame) + ".jpg";
		imageChild.setString("file", fileName);
		XML boxChild = imageChild.addChild("box");
		boxChild.setInt("top", (int)topLeft.y);
		boxChild.setInt("left", (int)topLeft.x);
		boxChild.setInt("width", (int)(bottomRight.x - topLeft.x));
		boxChild.setInt("height", (int)(bottomRight.y - topLeft.y));
		saveXML(xml, "data/testing_balls.xml");
		mov.save("data/" + fileName);
	}
	
	public void keyPressed() {

		if (key == 'q') {
			if (0 < newFrame)
				newFrame -= 1;
		} 
		if (key == 'w') {
			if (newFrame < getLength() - 1)
				newFrame += 1;
		}
		
		if (key == 'a') {
			if (0 < newFrame)
				newFrame -= 50;
		} 
		if (key == 's') {
			if (newFrame < getLength() - 1)
				newFrame += 50;
		}
		if( key == ' '){
			saveCurrentXML();
		} else {
			setFrame(newFrame);
		}
	}

	public int getFrame() {
		return ceil(mov.time() * 30) - 1;
	}

	public void setFrame(int n) {
		mov.play();
		float fr = mov.frameRate;
		// The duration of a single frame:
		float frameDuration = 1.0f / fr;

		// We move to the middle of the frame by adding 0.5:
		float where = (n + 0.5f) * frameDuration;

		// Taking into account border effects:
		float diff = mov.duration() - where;
		if (diff < 0) {
			where += diff - 0.25f * frameDuration;
		}

		mov.jump(where);
		mov.pause();
	}

	public int getLength() {
		return PApplet.parseInt(mov.duration() * mov.frameRate);
	}

	public void settings() {
		size(640, 360);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "Frames" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
