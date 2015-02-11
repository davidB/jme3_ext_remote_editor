package jme3_ext_remote_editor

import com.jme3.app.SimpleApplication
import com.jme3.asset.plugins.FileLocator
import com.jme3.math.ColorRGBA
import com.jme3.math.Matrix4f
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import java.io.File
import java.util.HashMap
import java.util.concurrent.Executors
import jme3_ext_pgex.LoggerCollector
import jme3_ext_pgex.Pgex
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.slf4j.LoggerFactory
import pgex.Cmds
import pgex.Cmds.Cmd
import pgex.Cmds.SetEye.ProjMode
import pgex.Datas

import static io.netty.buffer.Unpooled.wrappedBuffer

@FinalFieldsConstructor
class ReqHandler {
	val executor = Executors.newSingleThreadExecutor();
	val remoteCtx = new RemoteCtx();
	val log = LoggerFactory.getLogger(this.getClass)

	val SimpleApplication app;
	val Pgex pgex;
	private val folders = new HashMap<String, File>();

	def enable() {
		app.enqueue[|
			val cam0 = app.getCamera();//.clone();
			val vp = app.getRenderManager().createMainView("remoteHandler_" + System.currentTimeMillis(), cam0)
			vp.setBackgroundColor(ColorRGBA.Gray)
			vp.addProcessor(remoteCtx.view)
			//System.out.printf("Enable camera %s for %s - %s / %s\n", cam0.hashCode(), remoteCtx.view, "", vp);
			//vp.setClearEnabled(true);
			//vp.attachScene(app.getRootNode());
			app.getRootNode().attachChild(remoteCtx.root)
			log.info("connected")
			null
		]
	}

	def disable() {
		app.enqueue[|
			remoteCtx.view.getViewPort().removeProcessor(remoteCtx.view)
			//TODO only clean root when no remote client
			app.getRootNode().detachChild(remoteCtx.root)
			remoteCtx.root.detachAllChildren()
			remoteCtx.root.getLocalLightList().clear()
			log.info("disconnected")
			null
		]
	}

	def channelRead(ChannelHandlerContext ctx, Object msg0) {
		val msg = msg0 as ByteBuf;
		val k = msg.readByte();
		try {
			switch(k) {
				case Protocol.Kind.askScreenshot : askScreenshot(ctx, msg)
				case Protocol.Kind.pgexCmd : pgexCmd(ctx, msg)
				default : System.out.println("Unsupported kind of message : " + k)
			}
		} catch(Exception exc) {
			exc.printStackTrace();
		}
	}

	def askScreenshot(ChannelHandlerContext ctx, ByteBuf msg) {
		val w = msg.readInt()
		val h = msg.readInt()
		msg.release()
		remoteCtx.todos.add [rc |
			rc.view.askReshape.set(new SceneProcessorCaptureToBGRA.ReshapeInfo(w, h, true))
			//TODO run notify in async (in an executor)
			rc.view.askNotify.set [bytes |
				if (bytes.limit() != (w * h * 4)) {
					log.warn("bad size : {} != {}", bytes.limit(), w*h*4 )
					return false
				}
				val out = wrappedBuffer(bytes)  // use a bytes.slice() internally
				out.resetReaderIndex()
				executor.execute [
					val header = ctx.alloc().buffer(4+1)
					header.writeInt(out.readableBytes())
					header.writeByte(Protocol.Kind.rawScreenshot)
					ctx.write(header)
					ctx.writeAndFlush(out)
				]
				return true;
			]
//			for (ViewPort vp : rc.view.getRenderManager().getMainViews()) {
//				for(Spatial scene : vp.getScenes()) {
//					scene.breadthFirstTraversal(new SceneGraphVisitorAdapter(){
//						public void visit(Geometry geom) {
//							System.out.printf("G: " + geom);
//							if (!geom.checkCulling(vp.getCamera())) {
//								System.out.printf("vp(%s) hide : %s\n",vp.getName(), geom);
//							}
//						}
//					});
//				}
//			}
		]
	}

	def pgexCmd(ChannelHandlerContext ctx, ByteBuf msg) {
		try {
			val b = newByteArrayOfSize(msg.readableBytes())
			msg.readBytes(b);
			val cmd0 = Cmd.parseFrom(b, pgex.registry);
			switch(cmd0.getCmdCase()) {
				case SETEYE: setEye(ctx, cmd0.getSetEye())
				case SETDATA: setData(ctx, cmd0.getSetData())
				case CHANGEASSETFOLDERS: changeAssetFolders(ctx, cmd0.getChangeAssetFolders())
				//case : setCamera(ctx, cmd0); break;
				default:
					log.warn("unsupported cmd : {}", cmd0.getCmdCase().name() )
			}
		} catch(Exception exc) {
			exc.printStackTrace();
		}
	}

	def setData(ChannelHandlerContext ctx, Datas.Data data) {
		remoteCtx.todos.add [rc |
			val pgexLogger = new LoggerCollector("pgex");
			pgex.merge(data, rc.root, rc.components, pgexLogger);
			pgexLogger.dumpTo(log);
			val errorsCnt = pgexLogger.countOf(LoggerCollector.Level.ERROR);
			if (errorsCnt > 0) {
				log.error("pgex reading, error count : {}", errorsCnt);
			}
			val warnsCnt = pgexLogger.countOf(LoggerCollector.Level.WARN);
			if (warnsCnt > 0) {
				log.warn("pgex reading, warn count : {}", warnsCnt);
			}
		]
	}

	def setEye(ChannelHandlerContext ctx, Cmds.SetEye cmd) {
		remoteCtx.todos.add [rc |
			val cam = rc.cam;
			val rot = pgex.cnv(cmd.getRotation(), cam.getLocalRotation());
			cam.setLocalRotation(rot.clone());
			cam.setLocalTranslation(pgex.cnv(cmd.getLocation(), cam.getLocalTranslation()));
			val cam0 = rc
					.view
					.getViewPort()
					.getCamera();
			//System.out.printf("setEye camera %s for %s - %s / %s\n", cam0.hashCode(), remoteCtx.view, rc.view, rc.view.getViewPort());
			cam.setCamera(cam0);
			if (cmd.hasNear()) cam0.setFrustumNear(cmd.getNear());
			if (cmd.hasFar()) cam0.setFrustumFar(cmd.getFar());
			if (cmd.hasProjection()) {
				val proj = pgex.cnv(cmd.getProjection(), new Matrix4f());
				cam0.setParallelProjection(cmd.getProjMode() == ProjMode.orthographic);
				cam0.setProjectionMatrix(proj);
			}

			cam0.update();
			cam.setEnabled(true);
		]
	}

	def changeAssetFolders(ChannelHandlerContext ctx, Cmds.ChangeAssetFolders cmd) {
		remoteCtx.todos.add [rc |
			val am = app.getAssetManager();
			if (cmd.getUnregisterOther()) {
				for (String p: folders.keySet()) {
					if (!cmd.getPathList().contains(p)) {
						val f = folders.get(p);
						am.unregisterLocator(f.getAbsolutePath(), typeof(FileLocator))
						log.warn("unregister assets folder : {}", f);
					}
				}
			}
			if (cmd.getRegister()) {
				for (String p: cmd.getPathList()) {
					if (!folders.containsKey(p)) {
						val f = new File(p)
						folders.put(p, f);
						am.registerLocator(f.getAbsolutePath(), typeof(FileLocator))
						log.warn("register assets folder : {}", f);
					}
				}
			} else {
				for (String p: cmd.getPathList()) {
					if (folders.containsKey(p)) {
						val f = folders.get(p)
						am.unregisterLocator(f.getAbsolutePath(), typeof(FileLocator))
						log.warn("unregister assets folder : {}", f);
					}
				}
			}
		]
	}
}