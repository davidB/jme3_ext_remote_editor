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
	public final Node root = new Node("remoteRootNode");
	public final CameraNode cam = new CameraNode("eye", (Camera) null);
	public final Map<String, Object> components = new TreeMap<>();
	public final Queue<Consumer<RemoteCtx>> todos = new ConcurrentLinkedQueue<>();
	public final SceneProcessorCopyToBGRA view = new SceneProcessorCopyToBGRA(){
		@Override
		public void preFrame(float tpf) {
			Consumer<RemoteCtx> job;
			while ( (job = todos.poll()) != null) {
				job.accept(RemoteCtx.this);
			}
		}
	};

	public RemoteCtx() {
		cam.setEnabled(false);
		root.attachChild(cam);
	}
}
