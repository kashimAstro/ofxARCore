#include "ofApp.h"

//--------------------------------------------------------------
void ofApp::setup()
{
    ofBackground(0);
    ofSetOrientation(OF_ORIENTATION_DEFAULT, false);
    font.load("Roboto-Regular.ttf", 40);
    arcore.setup();

    for(int i = 0; i < point_cloud.size(); i++)
        point_color.push_back(ofColor::red);
    vbo_pointcloud.setVertexData(&point_cloud[0], 3, 1000, GL_DYNAMIC_DRAW);
    vbo_pointcloud.setColorData(&point_color[0], 1000, GL_DYNAMIC_DRAW);
    point_color.clear();
}

//--------------------------------------------------------------
void ofApp::update(){
    arcore.update();
    if(!initialized && arcore.isInitialized())
    {
        initialized = true;
        projectionMatrix = arcore.getProjectionMatrix(0.01f,100.0);
        point_cloud = arcore.getPointCloud();
    }
    if(arcore.isInitialized()) {
        if(arcore.isTracking())
        {
            ofFloatColor color = arcore.getColorCorrection();
            point_cloud = arcore.getPointCloud();
            if (point_cloud.size() > 1) {
                for (int i = 0; i < point_cloud.size(); i++)
                    point_color.push_back(ofColor::red);
                vbo_pointcloud.updateVertexData(&point_cloud[0], (int) point_cloud.size() * 2);
                vbo_pointcloud.updateColorData(&point_color[0], (int) point_color.size() * 2);
            }
        }
    }
}

//--------------------------------------------------------------
void ofApp::draw()
{
    ofSetColor(255);
    if(initialized)
    {
        arcore.draw();
        if(arcore.isTracking())
        {
            ofSetMatrixMode(OF_MATRIX_PROJECTION);
            ofLoadMatrix(projectionMatrix);
            ofSetMatrixMode(OF_MATRIX_MODELVIEW);
            ofLoadMatrix(arcore.getViewMatrix());

            ofEnableDepthTest();
                glPointSize(10);
                vbo_pointcloud.draw(GL_POINTS, 0, point_cloud.size()*2);
            ofDisableDepthTest();
        }
        else {
            drawMessage("Not tracking");
        }
    } else {
        drawMessage("Initializing ARCore");
    }
}

//--------------------------------------------------------------
void ofApp::drawMessage(string text){
    ofPushStyle();
    ofSetColor(0,0,0, 100);
    ofDrawRectangle(0, ofGetHeight() - 200, ofGetWidth(), 300);

    ofSetColor(255);
    float w = font.stringWidth(text);
    font.drawString(text, ofGetWidth()/2-w/2, ofGetHeight() - 110);
    ofPopStyle();
}

