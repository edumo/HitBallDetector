# HitBallDetector

[![Watch the video]](https://github.com/edumo/HitBallDetector/blob/master/2018-12-14%2021-09-08.flv?raw=true)

Computer vision app to detect balls hitting a Wall with one IR camera and two lights sources. Development of this project was supported by nexcommunity.com.

TODO license

## The problem

We need to detect ball hits in a wall with only one ir camera ad two light sources. The balls has different sizes and colors, can throw many balls at same time. Two app instances will run on the same windows 10 machine.

## Architecture

The video sources will be shared throw spout 1280x720, the Spout2OpenCV project is an openframeworks.cc project using spot 2.0 and opencv compiled with vs2017.

The HitDetector project is a P5 (processing.org) project using eclipse as IDE, detects and identify blobs in a scene over time. 

We need a HitDetector instance for each camera, also a wekinator instance running in different ports for each. Could be used only one instance, the running is not dependent.

## Compilation

The project uses maven. Import the project in your favorite IDE, only tested with Eclipse.

## Solutions

We will solve the problem with a classical aproch with Blob Tracking and path anlysis. After that, will test some modern techniques as research.

### OPENCV

We will track the blobs generated by balls and analyze the path generated. Rates detecting hits are correct for the porpouse of these project. 

### Machine Learning test

- Wekinator Image Classifiers
- Tests DWT with wekinator
- Tests HOG descriptor generation with dlib
- Tests DNN dlib 

### WEKINATOR CLASSIFIERS

This is the picked solution, after testing the options explained below using object detection. 

The app will send to Wekinator a downsampled matrix with finded blobs, wekinator will classify each matrix image with two classes Hit and Ball. The problem is reduced a classification problem using the blob tracking data from opencv.

The dataset used to train is the same used to train dlib. In the eclipse project there are facilities to save the image balls matrix in a jpeg, and sending this to Wekinator, from the dataset created with imgLab.

### DWT

Dwt is nice to detect gestures, if only one ball is on scene this method will work smoothly with blobs positions. Shadow and  ball draws a nice pattern of each blob. Detecting hits using the identification from opencv could be a nice solution but you need as many wekinator instances as the maximun of the balls that could be detected, and group the blobs by ball ( ball and shadow)

### HOG DLIB

From Dlib docs "This is a technique for detecting semi-rigid objects". The light distribution and the movement changes the features of object detected ...

- Example used to train

http://dlib.net/train_object_detector.cpp.html

- Dataset

https://github.com/edumo/HitBallDetectorDataSet/tree/master/hits

### DNN DLIB

The project has to run on windows, and after getting everything compiling with vs2017 + CUDA + OPENCV, the trainning is using gpu, but gpu is not used running the demos. Using dlib is hardest on windows than linux or OSX. Tests done with linux works simplier. The dataset is shared ... 

#### Object detection

Before multiclass is trainned we've tested a simplier demo without any changes in net or code 

- Example used to train
http://dlib.net/dnn_mmod_ex.cpp.html

- Dataset
https://github.com/edumo/HitBallDetectorDataSet/tree/master/hits

The results are better but are slow ( no GPU) and we've to code the interface with the rest of sw, trying with ofx and opencv ...

#### Multiclassifier

- Example used to train
http://dlib.net/dnn_mmod_find_cars2_ex.cpp.html

- Dataset
https://github.com/edumo/HitBallDetectorDataSet/tree/master/hitsAndBalls

After 48 hours the model is failing ... something we've done wrong




