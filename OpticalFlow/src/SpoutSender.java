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

public class SpoutSender extends PApplet {

//
//            SpoutSender
//
//      Send to a Spout receiver
//
//           spout.zeal.co
//
//       http://spout.zeal.co/download-spout/
//
//
//      Rotating cube by Dave Bollinger
//      http://processing.org/examples/texturecube.html
//

// IMPORT THE SPOUT LIBRARY


PImage img; // image to use for the rotating cube demo
PGraphics pgr; // Graphics for demo

// DECLARE A SPOUT OBJECT
Spout spout;

public void setup() {

  // Initial window size
  
  textureMode(NORMAL);
  
  // Create a graphics object
  pgr = createGraphics(1280, 720, P3D);
  
  // Load an image
  img = loadImage("SpoutLogoMarble3.bmp");
  
  // The dimensions of graphics or image objects
  // do not have to be the same as the sketch window
    
  // CREATE A NEW SPOUT OBJECT
  spout = new Spout(this);
  
  // CREATE A NAMED SENDER
  // A sender can be created now with any name.
  // Otherwise a sender is created the first time
  // "sendTexture" is called and the sketch
  // folder name is used.  
  spout.createSender("Spout Processing");
  
} 

public void draw()  { 

    background(0, 90, 100);
    noStroke();
    
    // Draw the graphics   
    pushMatrix();
    translate(width/2.0f, height/2.0f, -100);
    rotateX(frameCount * 0.01f);
    rotateY(frameCount * 0.01f);      
    scale(110);
    TexturedCube(img);
    popMatrix();
    
    // OPTION 1: SEND THE TEXTURE OF THE DRAWING SURFACE
    // Sends at the size of the window    
    spout.sendTexture();
    
    /*
    // OPTION 2: SEND THE TEXTURE OF GRAPHICS
    // Sends at the size of the graphics
    pgr.beginDraw();
    pgr.lights();
    pgr.background(0, 90, 100);
    pgr.fill(255);
    pushMatrix();
    pgr.translate(pgr.width/2, pgr.height/2);
    pgr.rotateX(frameCount/100.0);
    pgr.rotateY(frameCount/100.0);
    pgr.fill(192);
    pgr.box(pgr.width/4); // box is not textured
    popMatrix();
    pgr.endDraw();
    spout.sendTexture(pgr);
    image(pgr, 0, 0, width, height);
    */
    
    /*
    // OPTION 3: SEND THE TEXTURE OF AN IMAGE
    // Sends at the size of the image
    spout.sendTexture(img);
    image(img, 0, 0, width, height);
    */
    
}


public void TexturedCube(PImage tex) {
  
  beginShape(QUADS);
  texture(tex);

  // +Z "front" face
  vertex(-1, -1,  1, 0, 0);
  vertex( 1, -1,  1, 1, 0);
  vertex( 1,  1,  1, 1, 1);
  vertex(-1,  1,  1, 0, 1);

  // -Z "back" face
  vertex( 1, -1, -1, 0, 0);
  vertex(-1, -1, -1, 1, 0);
  vertex(-1,  1, -1, 1, 1);
  vertex( 1,  1, -1, 0, 1);

  // +Y "bottom" face
  vertex(-1,  1,  1, 0, 0);
  vertex( 1,  1,  1, 1, 0);
  vertex( 1,  1, -1, 1, 1);
  vertex(-1,  1, -1, 0, 1);

  // -Y "top" face
  vertex(-1, -1, -1, 0, 0);
  vertex( 1, -1, -1, 1, 0);
  vertex( 1, -1,  1, 1, 1);
  vertex(-1, -1,  1, 0, 1);

  // +X "right" face
  vertex( 1, -1,  1, 0, 0);
  vertex( 1, -1, -1, 1, 0);
  vertex( 1,  1, -1, 1, 1);
  vertex( 1,  1,  1, 0, 1);

  // -X "left" face
  vertex(-1, -1, -1, 0, 0);
  vertex(-1, -1,  1, 1, 0);
  vertex(-1,  1,  1, 1, 1);
  vertex(-1,  1, -1, 0, 1);

  endShape();
}
  public void settings() {  size(640, 360, P3D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "SpoutSender" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
