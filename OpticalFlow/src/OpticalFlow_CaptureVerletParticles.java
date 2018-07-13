import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwOpticalFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Capture;
import processing.video.Movie;

import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class OpticalFlow_CaptureVerletParticles extends PApplet {

	/**
	 * 
	 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
	 * 
	 * A Processing/Java library for high performance GPU-Computing (GLSL). MIT
	 * License: https://opensource.org/licenses/MIT
	 * 
	 */

	//
	// This Demo-App combines Optical Flow (based on Webcam capture frames)
	// and VerletParticle simulation.
	// The resulting velocity vectors of the Optical Flow are used to change the
	// velocity of the Particles.
	//

	int cam_w = 1280;
	int cam_h = 720;

	int view_w = 1200;
	int view_h = (int) (view_w * cam_h / (float) cam_w);
	int view_x = 230;
	int view_y = 0;

	int gui_w = 200;
	int gui_x = view_w;
	int gui_y = 0;

	// main library context
	DwPixelFlow context;

	// optical flow
	DwOpticalFlow opticalflow;

	// buffer for the capture-image
	PGraphics2D pg_cam_a, pg_cam_b;

	// offscreen render-target
	PGraphics2D pg_oflow;

	// camera capture (video library)
	Movie cam;

	// some state variables for the GUI/display
	int BACKGROUND_COLOR = 0;
	boolean DISPLAY_SOURCE = true;
	boolean APPLY_GRAYSCALE = true;
	boolean APPLY_BILATERAL = true;
	boolean COLLISION_DETECTION = true;

	// particle system, cpu
	ParticleSystem particlesystem;

	DwPhysics.Param param_physics = new DwPhysics.Param();

	// verlet physics, handles the update-step
	DwPhysics<DwParticle2D> physics;

	public void settings() {
		size(view_w + gui_w, view_h, P2D);
		smooth(4);
	}

	public void setup() {

		surface.setLocation(view_x, view_y);

		// main library context
		context = new DwPixelFlow(this);
		context.print();
		context.printGL();

		// optical flow
		opticalflow = new DwOpticalFlow(context, cam_w, cam_h);

		// optical flow parameters
		opticalflow.param.display_mode = 3;

		// cam = new Capture(this, cam_w, cam_h, 30);
		// cam.start();

		cam = new Movie(this, "test1.mp4");
		cam.loop();

		pg_cam_a = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
		pg_cam_a.noSmooth();

		pg_cam_b = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
		pg_cam_b.noSmooth();

		pg_oflow = (PGraphics2D) createGraphics(view_w, view_h, P2D);
		pg_oflow.smooth(4);

		param_physics.GRAVITY = new float[] { 0, 0.1f };
		param_physics.bounds = new float[] { 0, 0, view_w, view_h };
		param_physics.iterations_collisions = 4;
		param_physics.iterations_springs = 0; // no springs in this demo

		physics = new DwPhysics<DwParticle2D>(param_physics);

		// particle system object
		particlesystem = new ParticleSystem(this, view_w, view_h);

		// set some parameters
		particlesystem.PARTICLE_COUNT = 1000;
		particlesystem.PARTICLE_SCREEN_FILL_FACTOR = 0.70f;
		particlesystem.PARTICLE_SHAPE_IDX = 0;

		particlesystem.MULT_FLUID = 0.40f;
		particlesystem.MULT_GRAVITY = 0.00f;

		particlesystem.particle_param.DAMP_BOUNDS = 1f;
		particlesystem.particle_param.DAMP_COLLISION = 0.80f;
		particlesystem.particle_param.DAMP_VELOCITY = 0.90f;

		particlesystem.initParticles();

		createGUI();

		background(BACKGROUND_COLOR);
		frameRate(60);

	}

	// float buffer for pixel transfer from OpenGL to the host application
	float[] flow_velocity = new float[cam_w * cam_h * 2];
	float[] flow_velocity_last = new float[cam_w * cam_h * 2];

	public void draw() {

		if (cam.available()) {
			cam.read();

			// render to offscreenbuffer
			pg_cam_a.beginDraw();
			pg_cam_a.image(cam, 0, 0);
			pg_cam_a.endDraw();

			// apply filters (not necessary)
			if (APPLY_GRAYSCALE) {
				DwFilter.get(context).luminance.apply(pg_cam_a, pg_cam_a);
			}
			if (APPLY_BILATERAL) {
				DwFilter.get(context).bilateral.apply(pg_cam_a, pg_cam_b, 5,
						0.10f, 4);
				swapCamBuffer();
			}

			// update Optical Flow
			opticalflow.update(pg_cam_a);

			// render Optical Flow
			pg_oflow.beginDraw();
			pg_oflow.background(BACKGROUND_COLOR);
			if (DISPLAY_SOURCE) {
				pg_oflow.image(pg_cam_a, 0, 0, view_w, view_h);
			}
			pg_oflow.endDraw();

			// add flow-vectors to the image
			if (opticalflow.param.display_mode == 2) {
				opticalflow.renderVelocityShading(pg_oflow);
			}
			opticalflow.renderVelocityStreams(pg_oflow, 10);

			// Transfer velocity data from the GPU to the host-application
			// This is in general a bad idea because such operations are very
			// slow. So
			// either do everything in shaders, and avoid memory transfer when
			// possible,
			// or do it very rarely. however, this is just an example for
			// convenience.
			flow_velocity = opticalflow.getVelocity(flow_velocity);

		}

		// add force: Optical Flow

		// add force: Middle Mouse Button (MMB) -> particle[0]
		if (mousePressed) {
			float[] mouse = { mouseX, mouseY };
			particlesystem.particles[0].moveTo(mouse, 0.3f);
			particlesystem.particles[0].enableCollisions(false);
		} else {
			particlesystem.particles[0].enableCollisions(true);
		}

		// update physics step
		boolean collision_detection = COLLISION_DETECTION
				&& particlesystem.particle_param.DAMP_COLLISION != 0.0f;

		physics.param.GRAVITY[1] = 0.05f * particlesystem.MULT_GRAVITY;
		physics.param.iterations_collisions = collision_detection ? 4 : 0;

		physics.setParticles(particlesystem.particles,
				particlesystem.particles.length);
		physics.update(1);

		// display result
		background(0);
		image(pg_oflow, 0, 0);

		// draw particlesystem
		PGraphics pg = this.g;
		pg.hint(DISABLE_DEPTH_MASK);
		pg.blendMode(BLEND);
		// pg.blendMode(ADD);
		// particlesystem.display(pg);
		pg.blendMode(BLEND);

		float[] fluid_vxy = new float[2];
		float[] fluid_vxy_before = new float[2];

		// for (DwParticle2D particle : particlesystem.particles) {
		for (int i = 0; i < view_w; i += 10) {
			for (int j = 0; j < view_h; j += 10) {

				int px_view = i;// Math.round(particle.cx);
				int py_view = j;// Math.round(height - 1 - particle.cy); //
								// invert y

				float scale_X = view_w / (float) cam_w;
				float scale_Y = view_h / (float) cam_h;

				int px_grid = (int) (px_view / scale_X);
				int py_grid = (int) (py_view / scale_Y);

				int w_grid = opticalflow.frameCurr.velocity.w;

				int PIDX = py_grid * w_grid + px_grid;

				fluid_vxy[0] = +flow_velocity[PIDX * 2 + 0]
						* particlesystem.MULT_FLUID;
				fluid_vxy[1] = -flow_velocity[PIDX * 2 + 1]
						* particlesystem.MULT_FLUID; // invert y

				fluid_vxy_before[0] = +flow_velocity_last[PIDX * 2 + 0]
						* particlesystem.MULT_FLUID;
				fluid_vxy_before[1] = -flow_velocity_last[PIDX * 2 + 1]
						* particlesystem.MULT_FLUID; // invert y

//				if (abs(fluid_vxy[1]) > 0.1f) {
//
//					if (fluid_vxy_before[1] <= -0.1f) {
//
//						stroke(255, 0, 0);
//					} else {
//						stroke(255);
//					}
//				}

				fill(255, 125);

				
				if (fluid_vxy_before[1] < -0.4f && fluid_vxy[1] > 0.1) {
					float total = fluid_vxy[1] - fluid_vxy_before[1];
					// que nos e ni grande ni pequeño
					if (total > 0.5f && total < 5) {

						// vemos lo que tenemos encima

						int PIDXTop = (py_grid + 30) * w_grid + px_grid;
						float xtop = +flow_velocity[PIDXTop * 2 + 0]
								* particlesystem.MULT_FLUID;
						float ytop = -flow_velocity[PIDXTop * 2 + 1]
								* particlesystem.MULT_FLUID; // invert y

						if (abs(xtop) < 0.1 && abs(ytop) < 0.1) {
							println("colisión" + frameCount);
							ellipse(px_view, view_h - py_view, 10 * total,
									10 * total);
						}
					}
				}

				// line(px_view, view_h-py_view, px_view+fluid_vxy[0]*10,
				// view_h-py_view+fluid_vxy[1]*10);

				flow_velocity_last[PIDX * 2 + 0] = fluid_vxy[0];
				flow_velocity_last[PIDX * 2 + 1] = fluid_vxy[1];
				// particle.addForce(fluid_vxy);
			}
		}

		// info
		String txt_fps = String.format(getClass().getName()
				+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", cam_w, cam_h,
				opticalflow.UPDATE_STEP, frameRate);
		surface.setTitle(txt_fps);

		// System.arraycopy(flow_velocity, 0, flow_velocity_last, 0,
		// flow_velocity.length);
	}

	public void swapCamBuffer() {
		PGraphics2D tmp = pg_cam_a;
		pg_cam_a = pg_cam_b;
		pg_cam_b = tmp;
	}

	public void opticalFlow_setDisplayMode(int val) {
		opticalflow.param.display_mode = val;
	}

	public void activeFilters(float[] val) {
		APPLY_GRAYSCALE = (val[0] > 0);
		APPLY_BILATERAL = (val[1] > 0);
	}

	public void setOptionsGeneral(float[] val) {
		DISPLAY_SOURCE = (val[0] > 0);
	}

	public void activateCollisionDetection(float[] val) {
		COLLISION_DETECTION = (val[0] > 0);
	}

	ControlP5 cp5;

	public void createGUI() {

		cp5 = new ControlP5(this);

		int sx, sy, px, py, oy;

		sx = 100;
		sy = 14;
		oy = (int) (sy * 1.5f);

		// //////////////////////////////////////////////////////////////////////////
		// GUI - OPTICAL FLOW
		// //////////////////////////////////////////////////////////////////////////
		Group group_oflow = cp5.addGroup("Optical Flow");
		{
			group_oflow.setSize(gui_w, 165).setHeight(20)
					.setBackgroundColor(color(16, 180))
					.setColorBackground(color(16, 180));
			group_oflow.getCaptionLabel().align(CENTER, CENTER);

			px = 10;
			py = 15;

			cp5.addSlider("blur input").setGroup(group_oflow).setSize(sx, sy)
					.setPosition(px, py).setRange(0, 30)
					.setValue(opticalflow.param.blur_input)
					.plugTo(opticalflow.param, "blur_input");

			cp5.addSlider("blur flow").setGroup(group_oflow).setSize(sx, sy)
					.setPosition(px, py += oy).setRange(0, 10)
					.setValue(opticalflow.param.blur_flow)
					.plugTo(opticalflow.param, "blur_flow");

			cp5.addSlider("temporal smooth").setGroup(group_oflow)
					.setSize(sx, sy).setPosition(px, py += oy).setRange(0, 1)
					.setValue(opticalflow.param.temporal_smoothing)
					.plugTo(opticalflow.param, "temporal_smoothing");

			cp5.addSlider("flow scale").setGroup(group_oflow).setSize(sx, sy)
					.setPosition(px, py += oy).setRange(0, 200f)
					.setValue(opticalflow.param.flow_scale)
					.plugTo(opticalflow.param, "flow_scale");

			cp5.addSlider("threshold").setGroup(group_oflow).setSize(sx, sy)
					.setPosition(px, py += oy).setRange(0, 3.0f)
					.setValue(opticalflow.param.threshold)
					.plugTo(opticalflow.param, "threshold");

			cp5.addRadio("opticalFlow_setDisplayMode").setGroup(group_oflow)
					.setSize(18, 18).setPosition(px, py += oy)
					.setSpacingColumn(40).setSpacingRow(2).setItemsPerRow(3)
					.addItem("dir", 0).addItem("normal", 1)
					.addItem("Shading", 2)
					.activate(opticalflow.param.display_mode);
		}

		// //////////////////////////////////////////////////////////////////////////
		// GUI - PARTICLES
		// //////////////////////////////////////////////////////////////////////////
		Group group_particles = cp5.addGroup("Particles");
		{

			group_particles.setHeight(20).setSize(gui_w, 260)
					.setBackgroundColor(color(16, 180))
					.setColorBackground(color(16, 180));
			group_particles.getCaptionLabel().align(CENTER, CENTER);

			sx = 100;
			px = 10;
			py = 10;
			oy = (int) (sy * 1.4f);

			cp5.addButton("reset particles").setGroup(group_particles)
					.setWidth(160).setPosition(10, 10)
					.plugTo(particlesystem, "initParticles");

			cp5.addSlider("Particle count").setGroup(group_particles)
					.setSize(sx, sy).setPosition(px, py += oy + 10)
					.setRange(10, 10000)
					.setValue(particlesystem.PARTICLE_COUNT)
					.plugTo(particlesystem, "setParticleCount");

			cp5.addSlider("Fill Factor").setGroup(group_particles)
					.setSize(sx, sy).setPosition(px, py += oy)
					.setRange(0.2f, 1.5f)
					.setValue(particlesystem.PARTICLE_SCREEN_FILL_FACTOR)
					.plugTo(particlesystem, "setFillFactor");

			cp5.addSlider("VELOCITY").setGroup(group_particles).setSize(sx, sy)
					.setPosition(px, py += oy + 10).setRange(0.85f, 1.0f)
					.setValue(particlesystem.particle_param.DAMP_VELOCITY)
					.plugTo(particlesystem.particle_param, "DAMP_VELOCITY");

			cp5.addSlider("GRAVITY").setGroup(group_particles).setSize(sx, sy)
					.setPosition(px, py += oy).setRange(0, 10f)
					.setValue(particlesystem.MULT_GRAVITY)
					.plugTo(particlesystem, "MULT_GRAVITY");

			cp5.addSlider("FLOW").setGroup(group_particles).setSize(sx, sy)
					.setPosition(px, py += oy).setRange(0, 1f)
					.setValue(particlesystem.MULT_FLUID)
					.plugTo(particlesystem, "MULT_FLUID");

			cp5.addSlider("SPRINGINESS").setGroup(group_particles)
					.setSize(sx, sy).setPosition(px, py += oy).setRange(0, 1f)
					.setValue(particlesystem.particle_param.DAMP_COLLISION)
					.plugTo(particlesystem.particle_param, "DAMP_COLLISION");

			cp5.addCheckBox("activateCollisionDetection")
					.setGroup(group_particles).setSize(40, 18)
					.setPosition(px, py += (int) (oy * 1.5f)).setItemsPerRow(1)
					.setSpacingColumn(3).setSpacingRow(3)
					.addItem("collision detection", 0)
					.activate(COLLISION_DETECTION ? 0 : 2);

			RadioButton rgb_shape = cp5.addRadio("setParticleShape")
					.setGroup(group_particles).setSize(50, 18)
					.setPosition(px, py += (int) (oy * 1.5f))
					.setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(3)
					.plugTo(particlesystem, "setParticleShape")
					.addItem("disk", 0).addItem("spot", 1).addItem("donut", 2)
					.addItem("rect", 3).addItem("circle", 4)
					.activate(particlesystem.PARTICLE_SHAPE_IDX);
			for (Toggle toggle : rgb_shape.getItems())
				toggle.getCaptionLabel().alignX(CENTER);
		}

		// //////////////////////////////////////////////////////////////////////////
		// GUI - DISPLAY
		// //////////////////////////////////////////////////////////////////////////
		Group group_display = cp5.addGroup("display");
		{
			group_display.setHeight(20).setSize(gui_w, height)
					.setBackgroundColor(color(16, 180))
					.setColorBackground(color(16, 180));
			group_display.getCaptionLabel().align(CENTER, CENTER);

			px = 10;
			py = 15;

			cp5.addSlider("BACKGROUND").setGroup(group_display).setSize(sx, sy)
					.setPosition(px, py).setRange(0, 255)
					.setValue(BACKGROUND_COLOR)
					.plugTo(this, "BACKGROUND_COLOR");

			cp5.addCheckBox("setOptionsGeneral").setGroup(group_display)
					.setSize(38, 18).setPosition(px, py += oy)
					.setItemsPerRow(1).setSpacingColumn(3).setSpacingRow(3)
					.addItem("display source", 0)
					.activate(DISPLAY_SOURCE ? 0 : 100);

			cp5.addCheckBox("activeFilters").setGroup(group_display)
					.setSize(18, 18).setPosition(px, py += (int) (oy * 1.5f))
					.setItemsPerRow(1).setSpacingColumn(3).setSpacingRow(3)
					.addItem("grayscale", 0)
					.activate(APPLY_GRAYSCALE ? 0 : 100)
					.addItem("bilateral filter", 1)
					.activate(APPLY_BILATERAL ? 1 : 100);
		}

		// //////////////////////////////////////////////////////////////////////////
		// GUI - ACCORDION
		// //////////////////////////////////////////////////////////////////////////
		cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w)
				.setSize(gui_w, height).setCollapseMode(Accordion.MULTI)
				.addItem(group_oflow).addItem(group_particles)
				.addItem(group_display).open(0, 1, 2);
	}

	/**
	 * 
	 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
	 * 
	 * A Processing/Java library for high performance GPU-Computing (GLSL). MIT
	 * License: https://opensource.org/licenses/MIT
	 * 
	 */

	static public class ParticleSystem {

		// particle system
		public float PARTICLE_SCREEN_FILL_FACTOR = 0.9f;
		public int PARTICLE_COUNT = 500;
		public int PARTICLE_SHAPE_IDX = 1;

		// particle behavior
		public float MULT_FLUID = 0.50f;
		public float MULT_GRAVITY = 0.50f;

		DwParticle2D.Param particle_param = new DwParticle2D.Param();

		public PApplet papplet;

		public DwParticle2D[] particles;
		public PShape shp_particlesystem;

		public int size_x;
		public int size_y;

		public ParticleSystem(PApplet papplet, int size_x, int size_y) {
			this.papplet = papplet;

			this.size_x = size_x;
			this.size_y = size_y;
		}

		public void setParticleCount(int count) {
			if (count == PARTICLE_COUNT && particles != null
					&& particles.length == PARTICLE_COUNT) {
				return;
			}
			PARTICLE_COUNT = count;
			initParticles();
		}

		public void setFillFactor(float screen_fill_factor) {
			if (screen_fill_factor == PARTICLE_SCREEN_FILL_FACTOR) {
				return;
			}
			PARTICLE_SCREEN_FILL_FACTOR = screen_fill_factor;
			initParticlesSize();
			initParticleShapes();
		}

		public void setParticleShape(int val) {
			PARTICLE_SHAPE_IDX = val;
			if (PARTICLE_SHAPE_IDX != -1) {
				initParticleShapes();
			}
		}

		public void initParticles() {
			particles = new DwParticle2D[PARTICLE_COUNT];
			for (int i = 0; i < PARTICLE_COUNT; i++) {
				particles[i] = new DwParticle2D(i);
				particles[i].setCollisionGroup(i);
				particles[i].setParamByRef(particle_param);
			}
			initParticlesSize();
			initParticlesPosition();
			initParticleShapes();
		}

		public void initParticlesSize() {

			float radius = (float) Math
					.sqrt((size_x * size_y * PARTICLE_SCREEN_FILL_FACTOR)
							/ PARTICLE_COUNT) * 0.5f;
			radius = Math.max(radius, 1);
			float rand_range = 0.5f;
			float r_min = radius * (1.0f - rand_range);
			float r_max = radius * (1.0f + rand_range);

			DwParticle2D.MAX_RAD = r_max;
			papplet.randomSeed(0);
			for (int i = 0; i < PARTICLE_COUNT; i++) {
				float pr = papplet.random(r_min, r_max);
				particles[i].setRadius(pr);
				particles[i].setMass(r_max * r_max / (pr * pr));
			}

			particles[0].setRadius(r_max * 1.5f);
		}

		public void initParticlesPosition() {
			papplet.randomSeed(0);
			for (int i = 0; i < PARTICLE_COUNT; i++) {
				float px = papplet.random(0, size_x - 1);
				float py = papplet.random(0, size_y - 1);
				particles[i].setPosition(px, py);
			}
		}

		public void initParticleShapes() {
			papplet.shapeMode(PConstants.CORNER);
			shp_particlesystem = papplet.createShape(PShape.GROUP);

			PImage sprite = createSprite();
			for (int i = 0; i < PARTICLE_COUNT; i++) {
				PShape shp_particle = createParticleShape(particles[i], sprite);
				particles[i].setShape(shp_particle);
				shp_particlesystem.addChild(shp_particle);
			}
		}

		// just some shape presets
		public PShape createParticleShape(DwParticle2D particle,
				PImage sprite_img) {

			final float rad = particle.rad;

			PShape shp_particle = papplet.createShape(PShape.GROUP);

			if (PARTICLE_SHAPE_IDX >= 0 && PARTICLE_SHAPE_IDX < 4) {

				PShape sprite = papplet.createShape(PShape.GEOMETRY);
				sprite.beginShape(PConstants.QUAD);
				sprite.noStroke();
				sprite.noFill();
				sprite.textureMode(PConstants.NORMAL);
				sprite.texture(sprite_img);
				sprite.normal(0, 0, 1);
				sprite.vertex(-rad, -rad, 0, 0);
				sprite.vertex(+rad, -rad, 1, 0);
				sprite.vertex(+rad, +rad, 1, 1);
				sprite.vertex(-rad, +rad, 0, 1);
				sprite.endShape();

				shp_particle.addChild(sprite);
			} else if (PARTICLE_SHAPE_IDX == 4) {

				float threshold1 = 1; // radius shortening for arc segments
				float threshold2 = 140; // arc between segments

				double arc1 = Math.acos(Math.max((rad - threshold1), 0) / rad);
				double arc2 = (180 - threshold2) * Math.PI / 180;
				double arc = Math.min(arc1, arc2);

				int num_vtx = (int) Math.ceil(2 * Math.PI / arc);

				// System.out.println(num_vtx);

				PShape circle = papplet.createShape(PShape.GEOMETRY);
				circle.beginShape();
				circle.noStroke();
				circle.fill(200, 100);
				for (int i = 0; i < num_vtx; i++) {
					float vx = (float) Math.cos(i * 2 * Math.PI / num_vtx)
							* rad;
					float vy = (float) Math.sin(i * 2 * Math.PI / num_vtx)
							* rad;
					circle.vertex(vx, vy);
				}
				circle.endShape(PConstants.CLOSE);

				PShape line = papplet.createShape(PShape.GEOMETRY);
				line.beginShape(PConstants.LINES);
				line.stroke(0, 100);
				line.strokeWeight(1);
				line.vertex(0, 0);
				line.vertex(-(rad - 1), 0);
				line.endShape();

				// PShape circle = papplet.createShape(PConstants.ELLIPSE, 0, 0,
				// rad*2, rad*2);
				// circle.setStroke(false);
				// circle.setFill(papplet.color(200,100));
				//
				// PShape line = papplet.createShape(PConstants.LINE, 0, 0,
				// -(rad-1), 0);
				// line.setStroke(papplet.color(0,200));
				// line.setStrokeWeight(1);

				shp_particle.addChild(circle);
				shp_particle.addChild(line);
			}

			return shp_particle;

		}

		// create sprite on the fly
		public PImage createSprite() {

			int size = (int) (DwParticle2D.MAX_RAD * 1.5f);
			size = Math.max(9, size);

			PImage pimg = papplet.createImage(size, size, PConstants.ARGB);
			pimg.loadPixels();

			float center_x = size / 2f;
			float center_y = size / 2f;

			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					int pid = y * size + x;

					float dx = center_x - (x + 0.5f);
					float dy = center_y - (y + 0.5f);
					float dd = (float) Math.sqrt(dx * dx + dy * dy) * 1f;

					dd = dd / (size * 0.5f); // normalize

					// DISC
					if (PARTICLE_SHAPE_IDX == 0) {
						if (dd < 0)
							dd = 0;
						else if (dd > 1)
							dd = 1;
						dd = dd * dd;
						dd = dd * dd;
						dd = dd * dd;

						dd = 1 - dd;
						int a = (int) (dd * 255);
						pimg.pixels[pid] = a << 24 | 0x00FFFFFF;
					}
					// SPOT
					else if (PARTICLE_SHAPE_IDX == 1) {
						if (dd < 0)
							dd = 0;
						else if (dd > 1)
							dd = 1;
						dd = 1 - dd;
						// dd = dd*dd;
						int a = (int) (dd * 255);
						pimg.pixels[pid] = a << 24 | 0x00FFFFFF;
					}
					// DONUT
					else if (PARTICLE_SHAPE_IDX == 2) {
						dd = Math.abs(0.6f - dd);
						dd *= 1.8f;
						dd = 1 - dd;
						dd = dd * dd * dd;
						if (dd < 0)
							dd = 0;
						else if (dd > 1)
							dd = 1;
						int a = (int) (dd * 255);
						pimg.pixels[pid] = a << 24 | 0x00FFFFFF;
					}
					// RECT
					else if (PARTICLE_SHAPE_IDX == 3) {
						int a = 255;
						if (Math.abs(dx) < size / 3f
								&& Math.abs(dy) < size / 3f)
							a = 0;
						pimg.pixels[pid] = a << 24 | 0x00FFFFFF;
					} else {
						pimg.pixels[pid] = 0;
					}

				}
			}
			pimg.updatePixels();

			return pimg;
		}

		// not sure if this is necessary, but i guess opengl stuff needs to be
		// released internally.
		public void clearShapes() {
			if (shp_particlesystem != null) {
				for (int i = shp_particlesystem.getChildCount() - 1; i >= 0; i--) {
					shp_particlesystem.removeChild(i);
				}
			}
		}

		public void display(PGraphics pg) {
			if (PARTICLE_SHAPE_IDX != -1) {
				pg.shape(shp_particlesystem);
			}
		}

	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "OpticalFlow_CaptureVerletParticles" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
