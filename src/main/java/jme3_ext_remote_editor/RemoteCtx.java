package jme3_ext_remote_editor;

import java.util.Map;
import java.util.TreeMap;

import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;

public class RemoteCtx {
	public final SceneProcessorCopyToBGRA view = new SceneProcessorCopyToBGRA();
	public final Node root = new Node("remoteRootNode");
	public final CameraNode cam = new CameraNode("eye", (Camera) null);
	public final Map<String, Object> components = new TreeMap<>();

	public RemoteCtx() {
		cam.setEnabled(false);
		root.attachChild(cam);
	}
}
