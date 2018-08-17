#include "ofApp.h"

//--------------------------------------------------------------
void ofApp::setup() {

	ofBackground(255, 255, 255);
	ofSetVerticalSync(true);

	// SPOUT
	bInitialized = false; // Spout sender initialization
	strcpy(sendername, "OF Spout Webcam Sender"); // Set the sender name
	ofSetWindowTitle(sendername); // show it on the title bar

								  // Webcam setup for a sender
	vidGrabber.setDeviceID(1);
	vidGrabber.setDesiredFrameRate(60); // try to set this frame rate
	vidGrabber.initGrabber(1280, 720); // try to grab at this size. 
	ofSetWindowShape(vidGrabber.getWidth(), vidGrabber.getHeight());
	cout << "Initialized webcam (" << vidGrabber.getWidth() << " x " << vidGrabber.getHeight() << ")" << endl;

}


//--------------------------------------------------------------
void ofApp::update() {
	vidGrabber.update();
}


//--------------------------------------------------------------
void ofApp::draw() {

	char str[256];
	ofSetHexColor(0xFFFFFF);

	vidGrabber.draw(0, 0, ofGetWidth(), ofGetHeight());

	// SPOUT
	if (!bInitialized) {
		// Create a Spout sender the same size as the video
		bInitialized = spoutsender.CreateSender(sendername, vidGrabber.getWidth(), vidGrabber.getHeight());
	}

	if (bInitialized && vidGrabber.isInitialized()) {
		// Send the video texture out for all receivers to use
		spoutsender.SendTexture(vidGrabber.getTextureReference().getTextureData().textureID,
			vidGrabber.getTextureReference().getTextureData().textureTarget,
			vidGrabber.getWidth(), vidGrabber.getHeight(), false);

		// Show what it is sending
		ofSetColor(255);
		sprintf(str, "Sending as : [%s]", sendername);
		ofDrawBitmapString(str, 20, 20);

		// Show fps
		sprintf(str, "fps: %3.3d", (int)ofGetFrameRate());
		ofDrawBitmapString(str, 20, 40);
	}

}

//--------------------------------------------------------------
void ofApp::exit() {
	// SPOUT
	// Release the sender on exit
	spoutsender.ReleaseSender();
}