/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3_ext_pgex;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;

import java.io.IOException;

/**
 * Apply current spatial's Transform to the light, following pgex rules (!= jme's LightControl).
 */
public class PgexLightControl extends AbstractControl {

	public Light light;

	public PgexLightControl() {
	}

	// fields used, when inversing ControlDirection:
	@Override
	protected void controlUpdate(float tpf) {
		if (spatial != null && light != null) {
			if (light instanceof PointLight) {
				((PointLight) light).setPosition(spatial.getWorldTranslation());
			}
			TempVars vars = TempVars.get();

			if (light instanceof DirectionalLight) {
				((DirectionalLight) light).setDirection(spatial.getWorldRotation().multLocal(vars.vect1.set(Vector3f.UNIT_Z)));
			}

			if (light instanceof SpotLight) {
				((SpotLight) light).setPosition(spatial.getWorldTranslation());
				((SpotLight) light).setDirection(spatial.getWorldRotation().multLocal(vars.vect1.set(Vector3f.UNIT_Z)));
			}
			vars.release();
		}
	}

	@Override
	protected void controlRender(RenderManager rm, ViewPort vp) {
	}

	@Override
	public Control cloneForSpatial(Spatial newSpatial) {
		PgexLightControl control = new PgexLightControl();
		control.light = light;
		control.setSpatial(newSpatial);
		control.setEnabled(isEnabled());
		return control;
	}

	private static final String LIGHT_NAME = "light";

	@Override
	public void read(JmeImporter im) throws IOException {
		super.read(im);
		InputCapsule ic = im.getCapsule(this);
		light = (Light)ic.readSavable(LIGHT_NAME, null);
	}

	@Override
	public void write(JmeExporter ex) throws IOException {
		super.write(ex);
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(light, LIGHT_NAME, null);
	}
}