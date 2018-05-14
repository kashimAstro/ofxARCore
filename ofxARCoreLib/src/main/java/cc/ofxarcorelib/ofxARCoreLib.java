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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

	/* point cloud and plane */
	// point cloud
	private PointCloud pointcloud_data = null;
	private FloatBuffer pcloud_buffer;
	private float[] pcloud_array;

	// plane
	private float[] plane_matrix = new float[16];
	private FloatBuffer plane_poly;
	private float[][] plane_array_poly;
	private float[] x_hor_plane;
	private float[] y_hor_plane;
	private float[] z_hor_plane;
	private float[] plane_extent_hor_X;
	private float[] plane_extent_hor_Z;

	private float[] x_ver_plane;
	private float[] y_ver_plane;
	private float[] z_ver_plane;
	private float[] plane_extent_ver_X;
	private float[] plane_extent_ver_Z;

	// light color correction
	final float[] color_correction_rgba = new float[4];
	/* point cloud and plane */

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

				ByteBuffer bbTexCoords = ByteBuffer.allocateDirect(4 * 2 * 4);
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

	/* point cloud and plane @kashimAstro */
	public float[] getPointCloud()
    	{
        	return pcloud_array;
	}                   /* (incomplete) */
	public float[] getColorCorrectionRGBA()
	{
		return color_correction_rgba;
	} /* (incomplete) */
	public float[] getPlaneMatrix(){ return plane_matrix; }   				  /* (incomplete) */
	/**/
	public float[][] getPlaneShape(){ return plane_array_poly; }
	public float[] getPlaneHorizontalSizeX(){ return plane_extent_hor_X; }
	public float[] getPlaneHorizontalSizeZ(){ return plane_extent_hor_Z; }
	public float[] getPlaneHorizontalX(){ return x_hor_plane; }
	public float[] getPlaneHorizontalY(){ return y_hor_plane; }
	public float[] getPlaneHorizontalZ(){ return z_hor_plane; }

	public float[] getPlaneVerticalSizeX(){ return plane_extent_ver_X; }
	public float[] getPlaneVerticalSizeZ(){ return plane_extent_ver_Z; }
	public float[] getPlaneVerticalX(){ return x_ver_plane; }
	public float[] getPlaneVerticalY(){ return y_ver_plane; }
	public float[] getPlaneVerticalZ(){ return z_ver_plane; }
	/* point cloud and plane @kashimAstro */

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

	/* plane utility */
	static class SortablePlane {
		final float distance;
		final Plane plane;

		SortablePlane(float distance, Plane plane) {
			this.distance = distance;
			this.plane = plane;
		}
	}
	/* plane utility */

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

			/* plane collection @kashimAstro */
			Collection<Plane> allPlanes = mSession.getAllTrackables(Plane.class);
			// horizontal
			x_hor_plane = new float[allPlanes.size()];
			y_hor_plane = new float[allPlanes.size()];
			z_hor_plane = new float[allPlanes.size()];
			plane_extent_hor_X = new float[allPlanes.size()];
			plane_extent_hor_Z = new float[allPlanes.size()];
			// vertical
			x_ver_plane = new float[allPlanes.size()];
			y_ver_plane = new float[allPlanes.size()];
			z_ver_plane = new float[allPlanes.size()];
			plane_extent_ver_X = new float[allPlanes.size()];
			plane_extent_ver_Z = new float[allPlanes.size()];
			//
			plane_array_poly = new float[allPlanes.size()][];

			int count_vert = 0;
			int count_hori = 0;
			int count_all  = 0;
			for (Plane plane : allPlanes) {

				if (plane.getTrackingState() != TrackingState.TRACKING || plane.getSubsumedBy() != null) {
					continue;
				}
				if ( plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING) {
					x_hor_plane[count_hori] = plane.getCenterPose().tx();
					y_hor_plane[count_hori] = plane.getCenterPose().ty();
					z_hor_plane[count_hori] = plane.getCenterPose().tz();
					plane_extent_hor_X[count_hori] = plane.getExtentX();
					plane_extent_hor_Z[count_hori] = plane.getExtentZ();
					count_hori++;
				}
				else if ( plane.getType() == Plane.Type.VERTICAL ) {
					x_ver_plane[count_vert] = plane.getCenterPose().tx();
					y_ver_plane[count_vert] = plane.getCenterPose().ty();
					z_ver_plane[count_vert] = plane.getCenterPose().tz();
					plane_extent_ver_X[count_vert] = plane.getExtentX();
					plane_extent_ver_Z[count_vert] = plane.getExtentZ();
					count_vert++;
				}

				plane_array_poly[count_all] = new float[plane.getPolygon().array().length];
				plane_array_poly[count_all] = plane.getPolygon().array();
				count_all++;
			}

			/* color correction @kashimAstro */
			frame.getLightEstimate().getColorCorrection(color_correction_rgba, 0);

			/* point cloud @kashimAstro */
			pointcloud_data = frame.acquirePointCloud();
			pcloud_buffer   = pointcloud_data.getPoints();
			pcloud_array 	=  new float[pcloud_buffer.limit()];
			pcloud_buffer.get(pcloud_array);

			mIsReady = true;
			/*float lightIntensity = frame.getLightEstimate().getPixelIntensity();*/
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
