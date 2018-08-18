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

public class SendImagesWekinator extends PApplet {

	PImage img; // Image to receive a texture

	String[] files = null;

	String path = "C:/Users/motografica/git/HitBallDetector/HitBallDetectorTracking/balls";

	SimpleVideoInputWithProcessing_100Inputs wekinator = null;

	public void setup() {

		wekinator = new SimpleVideoInputWithProcessing_100Inputs();
		wekinator.setup(this, 10, 10);

		File file = new File(path);
		files = file.list();
		println(files);

		img = loadImage(path + "/" + files[0]);

	}

	int i = 0;

	public void draw() {

		background(0);

		image(img, 0, 0, 300, 300);

		if (keyPressed && key == ' ') {
			i++;
			img = loadImage(path + "/" + files[i]);
			if (i >= files.length) {
				exit();
			} else {

			}
			wekinator.send(g, img);
		}
		

	}

	public void settings() {
		size(1280 / 2, 720 / 2, P3D);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "SendImagesWekinator" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
