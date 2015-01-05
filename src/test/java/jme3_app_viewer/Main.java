package jme3_app_viewer;

import java.util.logging.Level;
import java.util.logging.Logger;

import jme3_ext_pgex.Pgex;
import jme3_ext_remote_editor.AppState4RemoteCommand;
import jme3_ext_spatial_explorer.AppStateSpatialExplorer;
import jme3_ext_spatial_explorer.Helper;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

public class Main {
	public static void main(String[] args) {
		Logger.getLogger("").setLevel(Level.WARNING);

		AppSettings settings = new AppSettings(true);
		settings.setResolution(1280, 720);
		settings.setVSync(true);
		settings.setFullscreen(false);

		SimpleApplication app = new SimpleApplication(){
			@Override
			public void simpleInitApp() {
			}
		};

		app.setSettings(settings);
		app.setShowSettings(false);
		app.setDisplayStatView(true);
		app.setDisplayFps(true);
		// !!!! without .setPauseOnLostFocus(false) server will only send screenshot to blender,... when jme main screen have focus
		app.setPauseOnLostFocus(false);
		app.start();

		//Setup Camera
		app.enqueue(() -> {
			app.getFlyByCamera().setEnabled(true);
			app.getFlyByCamera().setDragToRotate(true);
			//app.getStateManager().detach(app.getStateManager().getState(FlyCamAppState.class));
			app.getInputManager().setCursorVisible(true);
			return null;
		});
		//Setup a default scene (grid + axis)
		app.enqueue(() -> {
			app.getRootNode().attachChild(Helper.makeScene(app));
			return null;
		});
		//Setup SpatialExplorer
		app.enqueue(() -> {
			AppStateSpatialExplorer se = new AppStateSpatialExplorer();
			Helper.registerAction_Refresh(se.spatialExplorer);
			Helper.registerAction_ShowLocalAxis(se.spatialExplorer, app);
			app.getStateManager().attach(se);
			return null;
		});
		//Setup RemoteCommand
		app.enqueue(() -> {
			Pgex pgex = new Pgex(app.getAssetManager());
			app.getStateManager().attach(new AppState4RemoteCommand(4242, pgex));
			return null;
		});
	}
}
