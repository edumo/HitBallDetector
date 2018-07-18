import java.util.ArrayList;
import java.util.List;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

public class Debouncing {

	List<PVector> lastHits = new ArrayList<PVector>();

	float dist = 0;

	float timeToDieSegs = 50;

	PApplet parent;

	public Debouncing(List<PVector> lastHits, float dist, float timeToDieSegs,
			PApplet parent) {
		super();
		this.lastHits = lastHits;
		this.dist = dist;
		this.timeToDieSegs = timeToDieSegs;
		this.parent = parent;
	}

	public boolean addHit(PVector newHit) {
		boolean pass = true;

		for (PVector pos : lastHits) {
			if (pos.dist(newHit) < dist) {
				return false;
			}
		}

		newHit.z = parent.millis()/1000f;
		lastHits.add(newHit);

		return pass;
	}

	public void update() {

		List<PVector> toRemove = new ArrayList<PVector>();

		for (PVector pos : lastHits) {
			if (parent.millis()/1000f > pos.z + timeToDieSegs) {
				toRemove.add(pos);
			}
		}

		lastHits.removeAll(toRemove);

	}

	public void display(PGraphics g) {
		g.pushStyle();
		g.fill(255, 0, 0, 150);
		g.strokeWeight(2);
		g.stroke(255);
		for (PVector pos : lastHits) {
			g.ellipse(pos.x, pos.y, dist, dist);
		}
		g.popStyle();
	}
}
