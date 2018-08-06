import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import controlP5.*;
import oscP5.*;
import netP5.*;
import java.util.LinkedList;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class OSCDataPlotter extends PApplet {

	// Code by Rebecca Fiebrink, updated 15 November 2017
	// Please share only with attribution
	// Plots incoming OSC data
	// Adapts to number of channels and scale of each channel
	// Optionally also sends the received OSC messages on to another port.
	// By default this listens on port 6448 and sends to port 6449, but you can
	// change that below
	// You will need to add the oscP5 and controlP5 libraries to Processing if
	// you haven't already

	// For the GUI elements

	// Necessary for OSC communication with Wekinator:

	OscP5 oscP5;
	NetAddress dest;

	// ******Change the following variables to alter OSC and drawing behaviour:
	// **********//

	int receiveDataOnOSCPort = 6448;
	// If you want to use this to monitor features usually sento to Wekinator,
	// receiveDataOnOSCPort should be 6448
	// Or, if you want to use this to monitor output from Wekinator,
	// receiveDataOnOSCPort should be 12000

	boolean sendReceivedFeaturesToAnotherPort = true; // True if you want to
														// forward on your
														// features somewhere
														// else (e.g., to
														// Wekinator)
	int sendReceivedDataToPort = 6449; // Port to forward messages to

	// Number of datapoints per row to display: You may want to adjust this
	int pointsPerRow = 100;

	// ******Probably no need to edit below this line **********//

	// Gap at top of screen
	int topGap = 30;

	// Vertical gap between plots
	int vertGap = 10;

	// Store a list of all plot rows, and adapt size according to incoming OSC
	// messages
	ArrayList<Plot> plots = new ArrayList<Plot>(1);

	// GUI:
	ControlP5 cp5;

	// Pause:
	boolean isPaused = false;

	public void setup() {

		// Initialize OSC communication
		oscP5 = new OscP5(this, receiveDataOnOSCPort); // listen for incoming
														// OSC messages
		dest = new NetAddress("127.0.0.1", sendReceivedDataToPort); // Set up
																	// sender to
																	// send to
																	// desired
																	// port

		// Add toggle for sending OSC data out
		cp5 = new ControlP5(this);
		cp5.addToggle("sendReceivedFeaturesToAnotherPort").setPosition(385, 10)
				.setSize(15, 15).setValue(true).setLabel("")
				.setColorActive(color(0, 255, 0));

		cp5.addToggle("isPaused").setPosition(210, 10).setSize(15, 15)
				.setValue(false).setLabel("").setColorActive(color(255, 0, 0));

		// Create a single plot for starters
		Plot p = new Plot(width - 20, height - topGap - 10, pointsPerRow, 10,
				topGap);
		plots.add(p);
	}

	public void draw() {
		stroke(0);
		background(255);
		writeText();

		// Synchronized keeps us from changing and reading the ArrayList at the
		// same time in separate threads
		synchronized (this) {
			for (Plot p : plots) {
				p.plotPoints();
			}
		}
	}

	// This is called automatically when OSC message is received
	public void oscEvent(OscMessage theOscMessage) {

		if (sendReceivedFeaturesToAnotherPort) {
			resendOsc(theOscMessage);
		}

		if (isPaused) {
			return;
		}

		Object[] arguments = theOscMessage.arguments();

		// Has our number of data channels changed? If so, add/remove plots.
		while (arguments.length < plots.size()) {
			synchronized (this) {
				plots.remove(plots.size() - 1);
			}
			resizePlots();
		}
		while (arguments.length > plots.size()) {
			Plot p = new Plot(100, 200, 100, 10, 10);
			synchronized (this) {
				plots.add(p);
			}
			resizePlots();
		}

		// Grab the data and store it in the appropriate plot for each channel
		for (int i = 0; i < arguments.length; i++) {
			float nextFloat = theOscMessage.get(i).floatValue();
			plots.get(i).addPoint(nextFloat);
		}

	}

	public void resizePlots() {
		int totalPlotHeight = (height - topGap) / plots.size();
		int num = 0;
		synchronized (this) {
			for (Plot p : plots) {
				p.resize(width - 10, totalPlotHeight - vertGap, 0, topGap + num
						* totalPlotHeight);
				num++;
			}
		}
	}

	// Resends OSC message elsewhere
	public void resendOsc(OscMessage theMessage) {
		OscMessage msg = new OscMessage(theMessage.addrPattern(),
				theMessage.arguments());
		oscP5.send(msg, dest);
	}

	public void writeText() {
		fill(0);
		textSize(10);
		textAlign(RIGHT);
		text("RE-SEND OSC:", 380, 20);
		text("PAUSE:", 205, 20);

		textAlign(LEFT);
		text("Receiving OSC on port " + receiveDataOnOSCPort, 20, 20);
		if (sendReceivedFeaturesToAnotherPort) {
			text("re-sending to port " + sendReceivedDataToPort, 410, 20);

		}
	}

	// Simple plotting class by Rebecca Fiebrink, 24 October 2017
	// Please share only with attribution

	class Plot {
		// The points to plot
		protected LinkedList<Float> points;

		// Determines how plot is shown on screen
		protected int pHeight = 100;
		protected int totalWidth = 200;
		protected int labelWidth = 50;
		protected int plotWidth = totalWidth - labelWidth;
		protected int numPointsToPlot = 100;
		protected int x = 0;
		protected int y = 0;
		protected float min = 0.0001f;
		protected float max = 0.f;
		protected double horizontalScale = 1;

		// Store string versions of axis labels so we don't have to recompute
		// each frame
		String sMin = "0.0001";
		String sMax = "0.0";

		// Store last point so we can draw lines between subsequent points
		float lastPointX;
		float lastPointY;

		// Constructor
		public Plot(int plotWidth, int plotHeight, int numPoints, int x, int y) {
			this.pHeight = plotHeight;
			this.totalWidth = plotWidth;
			this.plotWidth = totalWidth - labelWidth;
			this.numPointsToPlot = numPoints;
			this.x = x;
			this.y = y;
			points = new LinkedList<Float>();
		}

		// Resize plot after it's been created
		public void resize(int newWidth, int newHeight, int newX, int newY) {
			this.pHeight = newHeight;
			this.totalWidth = newWidth;
			this.plotWidth = totalWidth - labelWidth;
			this.x = newX;
			this.y = newY;
			rescale();
		}

		// Add a new point to the data series we're plotting
		public void addPoint(float p) {
			if (points.size() == 0) {
				min = p - 0.0001f;
				max = p + 0.0001f;
				rescale();
			}

			if (p < min) {
				min = p;
				rescale();
			}
			if (p > max) {
				max = p;
				rescale();
			}

			// Use synchronized so we don't read from and edit linkedlist
			// simultaneously
			synchronized (this) {
				points.add(p);
				while (points.size() > numPointsToPlot) {
					points.removeFirst();
				}
			}
		}

		// Plots the current set of points for the chosen graph position and
		// size
		public void plotPoints() {
			// Plot area
			stroke(153);
			fill(255);
			rect(x + labelWidth, y, plotWidth, pHeight);

			// Plot labels
			putLabels();

			// Data points
			stroke(255, 0, 0);
			int n = 0;
			synchronized (this) {
				for (Float f : points) {
					float thisX = labelWidth + (float) (n * horizontalScale)
							+ x;
					n++;
					float thisY = y + pHeight - ((f - min) / (max - min))
							* pHeight;

					if (n == 1) {
						// It's the first point
						lastPointX = (float) thisX;
						lastPointY = (float) thisY;
					} else {
						// Draw a line from the last point to this point
						line(lastPointX, lastPointY, thisX, thisY);
						lastPointX = thisX;
						lastPointY = thisY;
					}
				}
			}
		}

		// Draw axis bounds
		protected void putLabels() {
			fill(0);
			textSize(8);
			textAlign(RIGHT);
			text(sMin, x + labelWidth, y + pHeight);
			text(sMax, x + labelWidth, y + 10);
		}

		// Call when min, max, width, or number of points to plot changes
		protected void rescale() {
			horizontalScale = (double) plotWidth / numPointsToPlot;
			sMin = Float.toString(min);
			sMax = Float.toString(max);
		}
	}

	public void settings() {
		size(600, 400);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "OSCDataPlotter" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
