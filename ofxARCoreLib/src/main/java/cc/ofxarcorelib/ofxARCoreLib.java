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

package cc.ofxarcorelib;

import android.app.Activity;
import android.content.Context;
import android.view.Surface;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotTrackingException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import cc.openframeworks.OFAndroid;
import cc.openframeworks.OFAndroidObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class ofxARCoreLib extends OFAndroidObject {
	private static final String TAG = "ofxARCoreLib";

	private Config mDefaultConfig;
	private Session mSession;

	private boolean mIsReady = false;
	private int mTexId;

	private FloatBuffer mTextureUV = FloatBuffer.allocate(8);
	private FloatBuffer mTextureUVTransformed = FloatBuffer.allocate(8);
	private boolean mTextureUVDirty;

	private float[] mViewMatrix = new float[16];
	private float[] mProjectionMatrix = new float[16];
	private float[] mAnchorMatrix = new float[16];

	private TrackingState mTrackingState = TrackingState.STOPPED;

	private Pose mPose;
	private ArrayList<Anchor> mAnchors = new ArrayList<>();

	private PointCloud mLastPointCloud = null;


	//        mNumPoints = mLastPointCloud.getPoints().remaining() / FLOATS_PER_POINT;
	//	  mLastPointCloud = cloud;
	//        mLastPointCloud.getPoints()
	//	  mPointCloud.update(frame.getPointCloud());
	//        mPointCloud.draw(frame.getPointCloudPose(), viewmtx, projmtx);


	public void setup(int texId, final int width, final int height){
		Context context = OFAndroid.getContext();

		mTexId = texId;

		((Activity)context).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Context context = OFAndroid.getContext();

				try {
					mSession = new Session((Activity) context);
				} catch (UnavailableArcoreNotInstalledException e) {
					e.printStackTrace();
				} catch (UnavailableApkTooOldException e) {
					e.printStackTrace();
				} catch (UnavailableSdkTooOldException e) {
					e.printStackTrace();
				}

				// Create default config, check is supported, create session from that config.
				mDefaultConfig =  new Config(mSession);
				if (!mSession.isSupported(mDefaultConfig)) {
					Toast.makeText(context, "This device does not support AR", Toast.LENGTH_LONG).show();
					return;
				}
				mSession.configure(mDefaultConfig);

				mSession.setCameraTextureName(mTexId);
				mSession.setDisplayGeometry(Surface.ROTATION_0, width, height);

				// Allocate UV coordinate buffers
				ByteBuffer bbTexCoords = ByteBuffer.allocateDirect(4  * 2 * 4);
				bbTexCoords.order(ByteOrder.nativeOrder());
				mTextureUV = bbTexCoords.asFloatBuffer();
				mTextureUV.put(QUAD_TEXCOORDS);
				mTextureUV.position(0);

				ByteBuffer bbTexCoordsTransformed = ByteBuffer.allocateDirect(4  * 2 * 4);
				bbTexCoordsTransformed.order(ByteOrder.nativeOrder());
				mTextureUVTransformed = bbTexCoordsTransformed.asFloatBuffer();

				try {
					mSession.resume();
				} catch (CameraNotAvailableException e) {
					e.printStackTrace();
				}

			}
		});
	}

	public boolean isInitialized(){
		return mIsReady;
	}

	public boolean isTracking(){
		return mTrackingState == TrackingState.TRACKING;
	}

	public float[] getViewMatrix(){
		return mViewMatrix;
	}

	public float[] getProjectionMatrix(float near, float far) throws CameraNotAvailableException {
		if(mIsReady) mSession.update().getCamera().getProjectionMatrix(mProjectionMatrix, 0, near, far);
		return mProjectionMatrix;
	}

	public float[] getTextureUv(){
		float[] ret = new float[8];
		mTextureUVTransformed.position(0);
		mTextureUVTransformed.get(ret);
		return ret;
	}

	public void addAnchor(){
		if(!mIsReady) return;
		try {
			Anchor a = mSession.createAnchor(mPose);
			mAnchors.add(a);
		} catch (NotTrackingException e) {
			e.printStackTrace();
		}
	}

	public float[] getAnchorPoseMatrix(int index){
		if(mAnchors.size() <= index) return mAnchorMatrix;
		mAnchors.get(index).getPose().toMatrix(mAnchorMatrix, 0);
		return mAnchorMatrix;
	}

	public boolean textureUvDirty(){
		if(mTextureUVDirty){
			mTextureUVDirty = false;
			return true;
		}
		return false;
	}

	public void update(){
		if(mSession == null) return;

		Frame frame = null;
		try {
			frame = mSession.update();
			if (frame.hasDisplayGeometryChanged()) {
				mTextureUVTransformed.position(0);
				frame.transformDisplayUvCoords(mTextureUV, mTextureUVTransformed);
				mTextureUVDirty = true;
			}

			// If not tracking, return
			mTrackingState = frame.getCamera().getTrackingState();
			if (mTrackingState != TrackingState.TRACKING) {
				return;
			}

			mPose = frame.getCamera().getPose();
			frame.getCamera().getViewMatrix(mViewMatrix, 0);

			mIsReady = true;

//			final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private static final float[] QUAD_TEXCOORDS = new float[]{
			0.0f, 1.0f,
			0.0f, 0.0f,
			1.0f, 1.0f,
			1.0f, 0.0f,
	};

	private void updateTexture(){
		mSession.setCameraTextureName(mTexId);
	}

	@Override
	protected void appPause() {
		if(mSession == null) return;
		mSession.pause();
	}

	@Override
	protected void appResume() {
		if(mSession != null)
			try {
				mSession.resume();
			} catch (CameraNotAvailableException e) {
				e.printStackTrace();
			}
	}

	@Override
	protected void appStop() {

	}
}
