import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import gab.opencv.Contour;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

/**
 * Blob Class
 *
 * Based on this example by Daniel Shiffman:
 * http://shiffman.net/2011/04/26/opencv-matching-faces-over-time/
 * 
 * @author: Jordi Tost (@jorditost)
 * 
 *          University of Applied Sciences Potsdam, 2014
 */

class Blob {

	private PApplet parent;

	// Contour
	public Contour contour;

	// Am I available to be matched?
	public boolean available;

	// Should I be deleted?
	public boolean delete;

	// How long should I live if I have disappeared?
	private int initTimer = 5; // 127;
	public int timer;

	// Unique ID for each blob
	int id;

	List<PVector> path = new ArrayList<PVector>();
	List<PVector> vels = new ArrayList<PVector>();

	PVector velocityAvg = new PVector();

	boolean hited = false;
	boolean hitedDnn = false;
	boolean processed = false;
	PVector hitPosition = new PVector();

	boolean movingUp = false;

	float angle = -33;
	float lastAngle = -33;
	float lastAngleVariation = -33;

	OscP5 oscP5;
	NetAddress dest;
	
	List<PVector> lastWekinatorHits = new ArrayList<PVector>();

	// Make me
	public Blob(PApplet parent, int id, Contour c, OscP5 oscP5, NetAddress dest) {
		this.parent = parent;
		this.id = id;
		this.contour = new Contour(parent, c.pointMat);
		this.oscP5 = oscP5;
		this.dest = dest;
		available = true;
		delete = false;

		timer = initTimer;
	}

	// Show me
	public void display(PGraphics canvas) {

		canvas.pushStyle();
		Rectangle r = contour.getBoundingBox();

		float opacity = PApplet.map(timer, 0, initTimer, 0, 127);
		// canvas.fill(0, 0, 255, opacity);
		// canvas.stroke(0, 0, 255);
		canvas.noFill();
		canvas.rect(r.x, r.y, r.width, r.height);
		// canvas.fill(255, 2 * opacity);
		canvas.textSize(8);
		canvas.text("" + id + " " + angle, r.x + 10, r.y + 30);

		PVector last = null;
		if (hitedDnn){
			canvas.ellipseMode(PApplet.CENTER);
			canvas.noStroke();
			canvas.fill(255);
			for (int i = 0; i < lastWekinatorHits.size(); i++) {
				PVector pos = lastWekinatorHits.get(i);
				canvas.ellipse(pos.x, pos.y, 10, 10);
			}
			canvas.strokeWeight(4);
		}else
			canvas.strokeWeight(1);
		for (PVector v : path) {

			if (hited) {
				canvas.stroke(0, 255, 0);
			} else if (movingUp) {
				canvas.stroke(255, 0, 255);
			} else {
				canvas.stroke(255, 0, 0);
			}
			if (last != null)
				canvas.line(last.x, last.y, v.x, v.y);
			last = v;
		}
		//
		float scale = 5;
		if (last != null) {
			canvas.strokeWeight(4);
			canvas.stroke(velocityAvg.y * scale * 10, 0, 0, 100);
			canvas.line(last.x, last.y, last.x + velocityAvg.x * scale, last.y
					+ velocityAvg.y * scale);
		}

		if (hited) {
			canvas.fill(255, 0, 0);
			canvas.ellipse(hitPosition.x, hitPosition.y, 20, 20);
		}

		canvas.popStyle();
	}

	// Give me a new contour for this blob (shape, points, location, size)
	// Oooh, it would be nice to lerp here!
	public void update(Contour newC, float velocityUpThreshold,
			float velocityDownThreshold) {

		contour = new Contour(parent, newC.pointMat);

		// PVector pos = new PVector((float) newC.getBoundingBox().getCenterX(),
		// (float) newC.getBoundingBox().getCenterY());
		//
		// pos.add(0,newC.getBoundingBox().height/2);

		List<PVector> points = newC.getPoints();
		PVector top = points.get(0);
		for (PVector point : points) {
			if (point.y < top.y) {
				top = point;
			}
		}

		path.add(top);

		if (path.size() > 1) {
			PVector vel = PVector.sub(top, path.get(path.size() - 2));
			this.vels.add(vel);

			velocityAvg.x = 0;
			velocityAvg.y = 0;

			int num = 2;

			if (vels.size() > num) {
				for (int i = vels.size() - 1; i > path.size() - 5; i--) {
					velocityAvg.x += vels.get(i).x;
					velocityAvg.y += vels.get(i).y;
				}
				velocityAvg.x /= num;
				velocityAvg.y /= num;
			}
			lastAngle = angle;

			angle = PApplet.atan2(velocityAvg.y, velocityAvg.x);
			// OscMessage msg = new OscMessage("angle/" + id);
			// msg.add(top.y);
			// // msg.add(top.x);
			// oscP5.send(msg, dest);
			if (lastAngle > -33) {
				// now we have moments to compare

				float variation = PApplet.abs(angle - lastAngle);
				if (lastAngleVariation > -33) {
					float differentialVarationTime = PApplet.abs(variation
							- lastAngleVariation);
					// PApplet.println(id + " dif " + differentialVarationTime);

					// if (differentialVarationTime > 1.5f) {
					// if (!hited && movingUp) {
					// hited = true;
					// PApplet.println(id + " hited");
					// hitPosition.set((float) newC.getBoundingBox()
					// .getCenterX(), (float) newC
					// .getBoundingBox().getCenterY());
					// }
					// }
				}

				lastAngleVariation = variation;

			}

			if (velocityAvg.y < -velocityDownThreshold) {
				movingUp = true;
			}

			if (!hited && movingUp) {
				if (velocityAvg.y > velocityUpThreshold) {
					PVector pos2;
					if (path.size() > num + 2)
						pos2 = path.get(path.size() - num - 2);
					else
						pos2 = path.get(path.size() - num);

					hitPosition.set(pos2.x, pos2.y);
					hited = true;
					PApplet.println(id + " hited");
				}
			}

		}

		timer = initTimer;
	}

	// Count me down, I am gone
	public void countDown() {
		timer--;
	}

	// I am deed, delete me
	public boolean dead() {
		if (timer < 0)
			return true;
		return false;
	}

	public Rectangle getBoundingBox() {
		return contour.getBoundingBox();
	}
}