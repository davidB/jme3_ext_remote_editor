package jme3_ext_remote_editor;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;




import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;




import jme3_ext_pgex.LoggerCollector;
import jme3_ext_pgex.Pgex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pgex.Cmds.ChangeAssetFolders;
import pgex.Cmds.Cmd;
import pgex.Cmds.SetEye;
import pgex.Datas.Data;




import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.LightList;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.CameraNode;


/**
 * Handles a server-side channel.
 */
//TODO manage env per connection : remove data from the connection when connection close.
@RequiredArgsConstructor
@Slf4j
public class RemoteHandler {
	final Executor executor = Executors.newSingleThreadExecutor();
	final RemoteCtx remoteCtx = new RemoteCtx();

	public final SimpleApplication app;
	public final Pgex pgex;
	private final Map<String, File> folders = new HashMap<>();

	public void enable() throws Exception {
		app.enqueue(() -> {
			Camera cam0 = app.getCamera();//.clone();
			ViewPort vp = app.getRenderManager().createMainView("remoteHandler_" + System.currentTimeMillis(), cam0);
			vp.setBackgroundColor(ColorRGBA.Gray);
			vp.addProcessor(remoteCtx.view);
			//System.out.printf("Enable camera %s for %s - %s / %s\n", cam0.hashCode(), remoteCtx.view, "", vp);
			//vp.setClearEnabled(true);
			//vp.attachScene(app.getRootNode());
			app.getRootNode().attachChild(remoteCtx.root);
			log.info("connected");
			return null;
		});
	}

	public void disable() throws Exception {
		app.enqueue(() -> {
			remoteCtx.view.getViewPort().removeProcessor(remoteCtx.view);
			//TODO only clean root when no remote client
			app.getRootNode().detachChild(remoteCtx.root);
			remoteCtx.root.detachAllChildren();
			LightList ll = remoteCtx.root.getLocalLightList();
			for(int i = ll.size() - 1; i > -1; i--) ll.remove(i);
			log.info("disconnected");
			return null;
		});
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg0) {
		ByteBuf msg = (ByteBuf)msg0;
		byte k = msg.readByte();
		try {
			switch(k) {
				case Protocol.Kind.askScreenshot : askScreenshot(ctx, msg); break;
				case Protocol.Kind.pgexCmd : pgexCmd(ctx, msg); break;
				default : System.out.println("Unsupported kind of message : " + k);
			}
		} catch(Exception exc) {
			exc.printStackTrace();
		}
	}

	private int lastByteBuf = 0;
	private ByteBuf out = null;
	void askScreenshot(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		int w = msg.readInt();
		int h = msg.readInt();
		msg.release();
		remoteCtx.todos.add((rc)->{
			rc.view.askReshape.set(new SceneProcessorCopyToBGRA.ReshapeInfo(w, h, true));
			//TODO run notify in async (in an executor)
			rc.view.askNotify.set((bytes) -> {
				if (bytes.limit() != (w * h * 4)) {
					log.warn("bad size : {} != {}", bytes.limit(), w*h*4 );
					return false;
				}
				if (lastByteBuf != bytes.hashCode()) {
					if (out != null) out.release();
					out = wrappedBuffer(bytes);  // use a bytes.slice() internally
					lastByteBuf = bytes.hashCode();
				}
				executor.execute(() -> {
					ByteBuf header = ctx.alloc().buffer(4+1);
					header.writeInt(out.readableBytes());
					header.writeByte(Protocol.Kind.rawScreenshot);
					ctx.write(header);
					ctx.writeAndFlush(out.retain());
				});
				return true;
			});
		});
	}

	void pgexCmd(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		try {
			byte[] b = new byte[msg.readableBytes()];
			msg.readBytes(b);
			Cmd cmd0 = Cmd.parseFrom(b, pgex.registry);
			switch(cmd0.getCmdCase()) {
				case SETEYE: setEye(ctx, cmd0.getSetEye()); break;
				case SETDATA: setData(ctx, cmd0.getSetData()); break;
				case CHANGEASSETFOLDERS: changeAssetFolders(ctx, cmd0.getChangeAssetFolders()); break;
				//case : setCamera(ctx, cmd0); break;
				default:
					log.warn("unsupported cmd : {}", cmd0.getCmdCase().name() );
			}
		} catch(Exception exc) {
			exc.printStackTrace();
		}
	}

	void setData(ChannelHandlerContext ctx, Data data) {
		remoteCtx.todos.add((rc)-> {
			LoggerCollector pgexLogger = new LoggerCollector("pgex");
			pgex.merge(data, rc.root, rc.components, pgexLogger);
			pgexLogger.dumpTo(log);
			int errorsCnt = pgexLogger.countOf(LoggerCollector.Level.ERROR);
			if (errorsCnt > 0) {
				log.error("pgex reading, error count : {}", errorsCnt);
			}
			int warnsCnt = pgexLogger.countOf(LoggerCollector.Level.WARN);
			if (warnsCnt > 0) {
				log.warn("pgex reading, warn count : {}", warnsCnt);
			}
		});
	}

	void setEye(ChannelHandlerContext ctx, SetEye cmd) {
		remoteCtx.todos.add((rc)->{
			CameraNode cam = rc.cam;
			Quaternion rot = pgex.cnv(cmd.getRotation(), cam.getLocalRotation());
			cam.setLocalRotation(rot.clone());
			cam.setLocalTranslation(pgex.cnv(cmd.getLocation(), cam.getLocalTranslation()));
			Camera cam0 = rc
					.view
					.getViewPort()
					.getCamera();
			//System.out.printf("setEye camera %s for %s - %s / %s\n", cam0.hashCode(), remoteCtx.view, rc.view, rc.view.getViewPort());
			cam.setCamera(cam0);
			if (cmd.hasNear()) cam0.setFrustumNear(cmd.getNear());
			if (cmd.hasFar()) cam0.setFrustumFar(cmd.getFar());
			if (cmd.hasProjection()) cam0.setProjectionMatrix(pgex.cnv(cmd.getProjection(), new Matrix4f()));
			cam0.update();
			cam.setEnabled(true);
		});
	}

	void changeAssetFolders(ChannelHandlerContext ctx, ChangeAssetFolders cmd) {
		remoteCtx.todos.add((rc)->{
			AssetManager am = app.getAssetManager();
			if (cmd.getUnregisterOther()) {
				for (String p: folders.keySet()) {
					if (!cmd.getPathList().contains(p)) {
						File f = folders.get(p);
						am.unregisterLocator(f.getAbsolutePath(), FileLocator.class);
						log.warn("unregister assets folder : {}", f);
					}
				}
			}
			if (cmd.getRegister()) {
				for (String p: cmd.getPathList()) {
					if (!folders.containsKey(p)) {
						File f = new File(p);
						folders.put(p, f);
						am.registerLocator(f.getAbsolutePath(), FileLocator.class);
						log.warn("register assets folder : {}", f);
					}
				}
			} else {
				for (String p: cmd.getPathList()) {
					if (folders.containsKey(p)) {
						File f = folders.get(p);
						am.unregisterLocator(f.getAbsolutePath(), FileLocator.class);
						log.warn("unregister assets folder : {}", f);
					}
				}
			}
		});
	}
}