import spout.*;

/**
 * Getting Started with Capture.
 * 
 * Reading and displaying an image from an attached Capture device. 
 */

import processing.video.*;

Capture cam;

// DECLARE A SPOUT OBJECT
Spout spout;
Spout spoutDown;
PGraphics down = null;

void setup() {
  size(1280, 720, P2D);
  
  frameRate(60);


  String[] cameras = Capture.list();

  if (cameras == null) {
    println("Failed to retrieve the list of available cameras, will try the default...");
    cam = new Capture(this, 1280, 720);
  } 
  if (cameras.length == 0) {
    println("There are no cameras available for capture.");
    exit();
  } else {
    println("Available cameras:");
    printArray(cameras);

    // The camera can be initialized directly using an element
    // from the array returned by list():

    cam = new Capture(this, 1280, 720,cameras[0]);

    // Or, the settings can be defined based on the text in the list
    //cam = new Capture(this, 640, 480, "Built-in iSight", 30);

    // Start capturing the images from the camera
    cam.start();
  }

 // spout = new Spout(this);


//  spout.createSender("VideoSpout");


  spoutDown = new Spout(this);

  // CREATE A NAMED SENDER
  // A sender can be created now with any name.
  // Otherwise a sender is created the first time
  // "sendTexture" is called and the sketch
  // folder name is used.

  spoutDown.createSender("VideoSpoutDown",1280/2, 720/2);

  down = createGraphics(1280/2, 720/2,P2D);

}

void draw() {
  if (cam.available() == true) {
    cam.read();

   
  }
  
  frameRate(60);

  
   down.beginDraw();
    down.image(cam, 0, 0, down.width, down.height);
    down.endDraw();
image(down, 0, 0, width, height);
  //  spout.sendTexture(cam);
    spoutDown.sendTexture(down);
  
  
  

  image(cam, 0, 0, width, height);

  text("fps: " + frameRate,10,10);


  down.beginDraw();
  down.image(cam, 0, 0, down.width, down.height);
  down.endDraw();

 // spout.sendTexture(cam);
  spoutDown.sendTexture(down);
}
//>>>>>>> dc589859e55abddeaa5c1ec52fd89aca867c13a7
