package jme3_ext_animation;

import com.jme3.animation.AnimChannel
import com.jme3.animation.AnimControl
import com.jme3.util.TempVars

public class TrackFactory {

	static def FloatKeyPointsTrack translationX(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = control.getSpatial().getLocalTranslation()
				v.x = value
				s.localTranslation = v
			}
		}
	}
	static def FloatKeyPointsTrack translationY(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = control.getSpatial().getLocalTranslation()
				v.x = value
				s.localTranslation = v
			}
		}
	}
	static def FloatKeyPointsTrack translationZ(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override void apply(float value, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial();
				val v = control.getSpatial().getLocalTranslation();
				v.x = value;
				s.localTranslation = v
			}
		}
	}
	static def FloatKeyPointsTrack rotationX(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = control.getSpatial().getLocalRotation()
				v.set(value, v.getY(), v.getZ(), v.getW())
				s.setLocalRotation(v)
			}
		}
	}
	static def FloatKeyPointsTrack rotationY(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override void apply(float value, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = control.getSpatial().getLocalRotation()
				v.set(v.getX(), value, v.getZ(), v.getW())
				s.setLocalRotation(v)
			}
		}
	}
	static def FloatKeyPointsTrack rotationZ(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override void apply(float value, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = control.getSpatial().getLocalRotation()
				v.set(v.getX(), v.getY(), value, v.getW())
				s.setLocalRotation(v)
			}
		}
	}
	static def FloatKeyPointsTrack rotationW(FloatKeyPoints points) {
		return new FloatKeyPointsTrack(points){
			override apply(float value, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = control.getSpatial().getLocalRotation()
				v.set(v.getX(), v.getY(), v.getZ(), value)
				s.setLocalRotation(v)
			}
		}
	}
	static def FloatKeyPointsTrack scaleX(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = control.getSpatial().getLocalScale()
				v.x = value
				s.setLocalScale(v)
			}
		}
	}
	static def FloatKeyPointsTrack scaleY(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = control.getSpatial().getLocalScale()
				v.y = value
				s.setLocalScale(v)
			}
		}
	}
	static def FloatKeyPointsTrack scaleZ(FloatKeyPoints points) {
		return new FloatKeyPointsTrack(points){
			override apply(float value, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = control.getSpatial().getLocalScale()
				v.z = value
				s.setLocalScale(v)
			}
		}
	}
}
