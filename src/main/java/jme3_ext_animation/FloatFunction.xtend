package jme3_ext_animation

import com.jme3.export.Savable
import com.jme3.export.JmeImporter
import java.io.IOException
import com.jme3.export.JmeExporter

interface FloatFunction extends Savable{
	def float apply(float v)
}

abstract class FloatFunction0 implements FloatFunction {
	override read(JmeImporter im) throws IOException {
	}

	override write(JmeExporter ex) throws IOException {
	}
}

class Identity extends FloatFunction0 {
	override apply(float v) {
		v
	}
}

class OneMinus extends FloatFunction0 {
	override apply(float v) {
		return 1.0f - v;
	}
}

class Compose2 extends FloatFunction0 {

	FloatFunction op2
	FloatFunction op1

	new() {
	}

	new(FloatFunction op1, FloatFunction op2) {
		this.op1 = op1
		this.op2 = op2
	}

	override apply(float v) {
		return op2.apply(op1.apply(v));
	}

	override read(JmeImporter im) throws IOException {
		val ic = im.getCapsule(this);
		op1 = ic.readSavable("op1", null) as FloatFunction
		op2 = ic.readSavable("op2", null) as FloatFunction
	}

	override write(JmeExporter ex) throws IOException {
		val oc = ex.getCapsule(this)
		oc.write(op1, "op1", null)
		oc.write(op2, "op2", null)
	}
}
