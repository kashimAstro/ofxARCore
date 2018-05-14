// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#pragma once

#include "ofMain.h"
#include "ofxAndroid.h"
#include "ofxARCore.h"


class ofApp : public ofxAndroidApp{
	
	public:

    void setup();
    void update();
    void draw();
    void drawMessage(string text);

    ofxARCore arcore;
    bool initialized;
    ofMatrix4x4 projectionMatrix;
	ofTrueTypeFont font;

	ofVbo vbo_pointcloud;
	vector<float> point_cloud;
	vector<ofFloatColor> point_color;
};
