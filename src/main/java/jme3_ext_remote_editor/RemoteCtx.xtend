package jme3_ext_remote_editor;

import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;

public class RemoteCtx {
	public val root = new Node("remoteRootNode");
	public val cam = new CameraNode("eye", null as Camera)
	public val components = new TreeMap<String, Object>() as Map<String, Object>
	public val todos = new ConcurrentLinkedQueue<Consumer<RemoteCtx>>() as Queue<Consumer<RemoteCtx>>
	public val view = new SceneProcessorCaptureToBGRA(){
		override  preFrame(float tpf) {
			var Consumer<RemoteCtx> job;
			while ( (job = todos.poll()) != null) {
				try {
					job.accept(RemoteCtx.this);
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
