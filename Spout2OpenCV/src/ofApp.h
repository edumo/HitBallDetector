#pragma once

#include "ofMain.h"
#include "ofxOpenCv.h"
#include "ofxSpout2Sender.h"

class ofApp : public ofBaseApp {
public:
	void setup();
	void update();
	void draw();
	void exit();

	SpoutSender spoutsender;   // A sender object
	char sendername[256];      // Sender name
	bool bInitialized;         // Initialization result

	ofVideoGrabber vidGrabber; // Webcam

};