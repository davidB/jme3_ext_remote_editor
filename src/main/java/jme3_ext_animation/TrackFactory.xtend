package jme3_ext_animation;

import com.jme3.animation.AnimChannel
import com.jme3.animation.AnimControl
import com.jme3.util.TempVars

public class TrackFactory {

	static def FloatKeyPointsTrack translationX(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float delta, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = s.getLocalTranslation()
				v.x += delta
				s.localTranslation = v
			}
		}
	}
	static def FloatKeyPointsTrack translationY(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float delta, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = s.getLocalTranslation()
				v.y += delta
				s.localTranslation = v
			}
		}
	}
	static def FloatKeyPointsTrack translationZ(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float delta, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = s.getLocalTranslation()
				v.z += delta
				s.localTranslation = v
			}
		}
	}
	static def FloatKeyPointsTrack rotationX(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float delta, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = s.getLocalRotation()
				v.set(v.getY() + delta, v.getY(), v.getZ(), v.getW())
				s.localRotation = v
			}
		}
	}
	static def FloatKeyPointsTrack rotationY(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float delta, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = s.getLocalRotation()
				v.set(v.getY(), v.getY() + delta, v.getZ(), v.getW())
				s.localRotation = v
			}
		}
	}
	static def FloatKeyPointsTrack rotationZ(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float delta, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = s.getLocalRotation()
				v.set(v.getY(), v.getY(), v.getZ() + delta, v.getW())
				s.localRotation = v
			}
		}
	}
	static def FloatKeyPointsTrack rotationW(FloatKeyPoints points) {
		return new FloatKeyPointsTrack(points){
			override apply(float value, float delta, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = s.getLocalRotation()
				v.set(v.getY(), v.getY(), v.getZ(), v.getW() + delta)
				s.localRotation = v
			}
		}
	}
	static def FloatKeyPointsTrack scaleX(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float delta, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = s.getLocalScale()
				v.x += delta
				s.localScale = v
			}
		}
	}
	static def FloatKeyPointsTrack scaleY(FloatKeyPoints points) {
		new FloatKeyPointsTrack(points){
			override apply(float value, float delta, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = s.getLocalScale()
				v.y += delta
				s.localScale = v
			}
		}
	}
	static def FloatKeyPointsTrack scaleZ(FloatKeyPoints points) {
		return new FloatKeyPointsTrack(points){
			override apply(float value, float delta, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
				val s = control.getSpatial()
				val v = s.getLocalScale()
				v.z += delta
				s.localScale = v
			}
		}
	}
}
