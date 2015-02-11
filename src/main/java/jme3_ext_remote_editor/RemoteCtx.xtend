package jme3_ext_remote_editor;

import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;

public class RemoteCtx {
	public val root = new Node("remoteRootNode");
	public val cam = new CameraNode("eye", null as Camera)
	public val components = new TreeMap<String, Object>() as Map<String, Object>
	public val todos = new ConcurrentLinkedQueue<Procedures.Procedure1<RemoteCtx>>() as Queue<Procedures.Procedure1<RemoteCtx>>
	public val view = new SceneProcessorCaptureToBGRA(){
		override  preFrame(float tpf) {
			var Procedures.Procedure1<RemoteCtx> job;
			while ( (job = todos.poll()) != null) {
				try {
					job.apply(RemoteCtx.this);
				} catch(Exception exc) {
					exc.printStackTrace();
				}
			}
		}
	}

	new() {
		cam.setEnabled(false);
		root.attachChild(cam);
	}
}
