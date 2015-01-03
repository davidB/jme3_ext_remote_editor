package jme3_ext_remote_editor;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;

//http://hub.jmonkeyengine.org/forum/topic/offscreen-rendering-problem/
public class SceneProcessorCopyToBGRA implements SceneProcessor {

	@Getter private RenderManager renderManager;
	@Getter private ViewPort viewPort;

	private TransfertImage timage;

	public final AtomicReference<ReshapeInfo> askReshape = new AtomicReference<>();

	private Function<ByteBuffer, Boolean> notify;
	public final AtomicReference<Function<ByteBuffer, Boolean>> askNotify = new AtomicReference<>();

	@RequiredArgsConstructor
	static class ReshapeInfo {
		public final int with;
		public final int height;
		public final boolean fixAspect;
	}

	@Override
	public void initialize(RenderManager rm, ViewPort vp) {
		this.renderManager = rm;
		this.viewPort = vp;
	}

	private TransfertImage reshapeInThread(int width0, int height0, boolean fixAspect) {
		TransfertImage ti = new TransfertImage(width0, height0);
		viewPort.getCamera().resize(width0, height0, fixAspect);
		renderManager.getRenderer().setMainFrameBufferOverride(ti.fb);
		renderManager.notifyReshape(ti.width, ti.height);

		//		for (ViewPort vp : viewPorts){
		//			vp.getCamera().resize(ti.width, ti.height, fixAspect);
		//
		//			// NOTE: Hack alert. This is done ONLY for custom framebuffers.
		//			// Main framebuffer should use RenderManager.notifyReshape().
		//			for (SceneProcessor sp : vp.getProcessors()){
		//				sp.reshape(vp, ti.width, ti.height);
		//			}
		//		}
		return ti;
	}

	@Override
	public boolean isInitialized() {
		return timage != null;
	}

	@Override
	public void preFrame(float tpf) {
	}

	@Override
	public void postQueue(RenderQueue rq) {
	}

	@Override
	public void postFrame(FrameBuffer out) {
		if (timage != null && notify != null) {
			//		if (out != timage.fb){
			//			throw new IllegalStateException("Why did you change the output framebuffer? " + out + " != " + timage.fb);
			//		}
			if (timage.copyFrameBufferToBGRA(renderManager, notify)) {
				notify = null;
			};
		}
		// for the next frame
		ReshapeInfo askR = askReshape.getAndSet(null);
		if (askR != null){
			timage = reshapeInThread(Math.max(1, askR.with), Math.max(1, askR.height), askR.fixAspect);
			//TODO dispose previous timage ASAP (when no longer used in JavafFX thread)
		}
		Function<ByteBuffer, Boolean> askN = askNotify.getAndSet(null);
		if (askN != null) {
			notify = askN;
		}
	}

	@Override
	public void cleanup() {
		if (timage != null) {
			timage.dispose();
			timage = null;
		}
	}

	@Override
	public void reshape(ViewPort vp, int w, int h) {
	}

	static class TransfertImage {
		public final int width;
		public final int height;
		public final FrameBuffer fb;
		public final ByteBuffer byteBuf;

		//static final int BGRA_size = 8 * 4; // format of image returned by  readFrameBuffer (ignoring format in framebuffer.color
		static final int BGRA_size = 4; // format of image returned by  readFrameBuffer (ignoring format in framebuffer.color

		TransfertImage(int width, int height) {
			this.width = width;
			this.height = height;
			fb = new FrameBuffer(width, height, 1);
			fb.setDepthBuffer(Format.Depth);
			fb.setColorBuffer(Format.ABGR8);
			byteBuf = BufferUtils.createByteBuffer(width * height * BGRA_size);
		}

		/** SHOULD run in JME'Display thread */
		Boolean copyFrameBufferToBGRA(RenderManager rm, Function<ByteBuffer, Boolean> notify) {
			synchronized (byteBuf) {
				byteBuf.clear();
				rm.getRenderer().readFrameBuffer(fb, byteBuf);
			}
			return notify.apply(byteBuf);
		}

		void dispose() {
			fb.dispose();
		}
	}
}