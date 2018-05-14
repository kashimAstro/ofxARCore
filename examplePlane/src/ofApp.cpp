#include "ofApp.h"

ofEvent<void> clearDrawingEvent;

ofMesh ofApp::position(ofMesh m, glm::vec3 pos)
{
    ofMesh tm = m;
    for(int i = 0;i < tm.getVertices().size();i++)
        tm.getVertices()[i]+=pos;
    return tm;
}

//--------------------------------------------------------------
void ofApp::setup()
{
    ofBackground(0);
    ofSetOrientation(OF_ORIENTATION_DEFAULT, false);
    font.load("Roboto-Regular.ttf", 40);

    ofAddListener(clearDrawingEvent, this, &ofApp::clearDrawing);
    initialized = false;
    arcore.setup();

    mesh = ofMesh::box(0.15,0.15,0.15);
    image.load("brick.jpg");
}

//--------------------------------------------------------------
void ofApp::update(){
    arcore.update();

    if(!initialized && arcore.isInitialized())
    {
        initialized = true;
        projectionMatrix = arcore.getProjectionMatrix(0.01f,100.0);
    }
}

//--------------------------------------------------------------
void ofApp::draw(){

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

            // anchor in space
            ofEnableDepthTest();
            for(int i = 0; i < copymesh.size(); i++) {
                ofPushStyle();
                    ofSetColor(ofColor::black);
                    copymesh[i].drawFaces();

                    ofSetColor(ofColor::white);
                    ofSetLineWidth(10.f);
                    copymesh[i].drawWireframe();
                ofPopStyle();
            }

            // vertical plane
            std::vector<float> width_vert_plane = arcore.getPlaneVerticalSizeX();
            std::vector<float> height_vert_plane = arcore.getPlaneVerticalSizeZ();
            std::vector<float> x_vert_plane = arcore.getPlaneVerticalX();
            std::vector<float> y_vert_plane = arcore.getPlaneVerticalY();
            std::vector<float> z_vert_plane = arcore.getPlaneVerticalZ();
            for(int i = 0; i < width_vert_plane.size(); i++)
            {
                ofPushStyle();
                image.bind();
                ofDrawBox(x_vert_plane[i],y_vert_plane[i],z_vert_plane[i],0.1,0.1,0.1);
                //ofDrawBox(x_vert_plane[i],y_vert_plane[i],z_vert_plane[i],width_vert_plane[i],height_vert_plane[i],0.002);
                image.unbind();
                ofPopStyle();
            }

            //horizontal plane
            std::vector<float> width_hor_plane = arcore.getPlaneHorizontalSizeX();
            std::vector<float> height_hor_plane = arcore.getPlaneHorizontalSizeZ();
            std::vector<float> x_hor_plane = arcore.getPlaneHorizontalX();
            std::vector<float> y_hor_plane = arcore.getPlaneHorizontalY();
            std::vector<float> z_hor_plane = arcore.getPlaneHorizontalZ();
            for(int i = 0; i < width_hor_plane.size(); i++)
            {
                ofPushStyle();
                image.bind();
                ofDrawBox(x_hor_plane[i],y_hor_plane[i],z_hor_plane[i],width_hor_plane[i],0.002,height_hor_plane[i]);
                image.unbind();
                ofPopStyle();
            }

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


//--------------------------------------------------------------
ofVec3f ofApp::screenToWorld(float x, float y, float z){
    ofVec2f s = ofVec3f(
            (x - ofGetWidth()/2.f)/ofGetWidth(),
            (y - ofGetHeight()/2.f)/ofGetHeight());

    float zFull = z * 100.f;
    float zNearToFar = ofLerp(0.01, 100.0, z);
    float alpha = zFull / zNearToFar;

    auto m  = arcore.getViewMatrix() * projectionMatrix;
    auto p4 = ofVec4f(2.f*s.x,-2.f*s.y,alpha,1.0) * m.getInverse();

    return ofVec3f(p4.x, p4.y, p4.z) / p4.w;
}

//--------------------------------------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_cc_openframeworks_planear_OFActivity_clearDrawing(JNIEnv *env, jclass type) {
    ofNotifyEvent(clearDrawingEvent);
}

//--------------------------------------------------------------
void ofApp::clearDrawing(){
    copymesh.clear();
}

//--------------------------------------------------------------
void ofApp::touchDown(int x, int y, int id){
    _touchDown = true;
    ofVec3f p = screenToWorld(x, y, 0.002);
    copymesh.push_back( position(mesh, glm::vec3(p)) );
}

//------------------------------------o--------------------------
void ofApp::touchMoved(int x, int y, int id){

}

//--------------------------------------------------------------
void ofApp::touchUp(int x, int y, int id){
    _touchDown = false;
}

