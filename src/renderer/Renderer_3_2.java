package renderer;

import static renderer.GLUtilityMethods.destroyOpenGL;
import static renderer.GLUtilityMethods.exitOnGLError;
import static renderer.GLUtilityMethods.setupOpenGL;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.Main;
import mesh.Ply;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import renderer.glmodels.GLDefaultProjectileFactory;
import renderer.glmodels.GLMesh;
import renderer.glmodels.GLModel;
import renderer.glmodels.GLTileFactory;
import renderer.glmodels.GLTileMidpointDisplacementFactory;
import renderer.glshaders.GLPhongShader;
import renderer.glshaders.GLShader;
import thread.RendererThread;
import util.Utils;
import assets.AssetContainer;
import assets.entities.Entity;
import assets.entities.Monster;
import assets.entities.MonsterFactory;
import assets.entities.Player;
import assets.entities.PolygonalScenery;
import assets.entities.PolygonalSceneryFactory;
import assets.entities.Projectile;
import assets.world.AbstractTile;
import assets.world.PolygonHeightmapTile;
import assets.world.datastructures.TileDataStructure2D;
import exception.RendererException;

public class Renderer_3_2 extends RendererThread {

	private final String[] DEFAULT_SHADER_LOCATION = { "resources/shaders/lighting/phong_vert.glsl",
			"resources/shaders/lighting/phong_frag.glsl" };

	// Setup variables
	private final String WINDOW_TITLE = "Footsnip";
	private final int WIDTH = 1024;
	private final int HEIGHT = 768;

	private final int MAX_FPS = 200;

	private Map<Class<?>, GLShader> shaderMap;

	private Class<GLPhongShader> defaultShaderClass = GLPhongShader.class;

	/**
	 * The time of the last frame, to calculate the time delta for rotating
	 * monsters.
	 */
	private long lastFrameTime;

	private GLWorld glWorld;

	/** The time we started counting frames. */
	private long lastFPSUpdate;

	/** The number of frames rendered since lastFPSUpdate. */
	private int framesThisSecond;

	public Renderer_3_2(AssetContainer assContainer, Main mainApplication) {
		super(assContainer, mainApplication);

		// Initialise FPS calculation fields.
		framesThisSecond = 0;
		lastFPSUpdate = Utils.getTime();
		getFrameTimeDelta();
	}

	protected void beforeLoop() throws RendererException {
		float[] backgroundColor = { 1f, 1f, 1f, 1.0f };
		setupOpenGL(WIDTH, HEIGHT, WINDOW_TITLE, backgroundColor);

		// Camera is actually static at this stage
		glWorld = new GLWorld(WIDTH, HEIGHT, new Vector3f(0, 0, 0));

		shaderMap = new HashMap<Class<?>, GLShader>();

		GLShader currentShader = new GLPhongShader(glWorld);
		currentShader.create(DEFAULT_SHADER_LOCATION);
		shaderMap.put(defaultShaderClass, currentShader);

		createEntities(currentShader);
		createWorld(currentShader);
		createScenery(currentShader);
	}

	protected void afterLoop() {
		// TODO: Modify this to accept the whole entity data-structure
		destroyOpenGL(glWorld, assContainer.getPlayer());
	}

	// TODO: Sort this method out so we can apply transforms and shit to our
	// 'data structure' of models. Currently it's pretty hard-coded to our
	private void logicCycle() {

		// Reset view and model matrices
		glWorld.clearViewMatrix();
		glWorld.setCameraPos(assContainer.getPlayer().getModel().modelPos);

		// Translate camera
		glWorld.transformCamera();

		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

		GLShader currentShader = shaderMap.get(defaultShaderClass);
		currentShader.bindShader();
		currentShader.copyCameraMatricesToShader();

		renderScenery(assContainer.getPolygonalSceneries(), currentShader);
		renderTiles(assContainer.getTileDataStructure(), currentShader);
		renderMonsters(assContainer.getMonsters(), currentShader);
		renderPlayer(assContainer.getPlayer(), currentShader);
		renderProjectiles(assContainer.getProjectiles(), currentShader);

		currentShader.unbindShader();

		exitOnGLError("logicCycle");
	}

	private void createWorld(GLShader shader) throws RendererException {
		Vector3f tilePos = new Vector3f(0, 0, 0);
		Vector3f tileAngle = new Vector3f(0, 0, 0);
		float tileScale = 1f;

		PolygonHeightmapTile initialTile = new PolygonHeightmapTile(null, null, tilePos);
		GLTileFactory glTileFactory = new GLTileMidpointDisplacementFactory(65, assContainer.getTileDataStructure());
		GLModel model = glTileFactory.create(initialTile, tilePos, tileAngle, tileScale, AbstractTile.SIZE);
		initialTile.setModel(model);

		assContainer.getTileDataStructure().init(glTileFactory, initialTile);
	}

	private void createScenery(GLShader shader) {
		// Hardcoded because Dave's mother is a prostitute

		assContainer.setPolygonalSceneries(new ArrayList<PolygonalScenery>());
		Vector3f sceneryPos = new Vector3f(0.05f, 0.05f, 0);
		assContainer.addPolygonalScenery(PolygonalSceneryFactory.create(shader, sceneryPos));
	}

	private void createEntities(GLShader shader) throws RendererException {

		Vector3f playerPos = new Vector3f(0, 0, 0);
		Vector3f playerAngle = new Vector3f(0, 0, 0);
		float playerScale = 1f;
		Vector4f playerColor = new Vector4f(0.9f, 0.9f, 0.9f, 1.0f);

		Ply playerMesh = new Ply();
		playerMesh.read(new File("resources/meshes/SpaceFighter_small.ply"), playerColor);
		GLModel playerModel = new GLMesh(playerMesh.getTriangles(), playerMesh.getVertices(), playerPos, playerAngle, playerScale);

		assContainer.setPlayer(new Player(playerModel, "Dave", 0, new float[] { 1.0f, 0.0f, 0.0f }));
		assContainer.setMonsters(new ArrayList<Monster>());

		Vector4f monsterColor = new Vector4f(0.3f, 0.3f, 0.05f, 1.0f);
		Ply monsterMesh = new Ply();
		monsterMesh.read(new File("resources/meshes/SmoothBlob_small.ply"), monsterColor);

		String script = "resources/lua/monsters.lua";
		LuaValue _G = JsePlatform.standardGlobals();
		_G.get("dofile").call(LuaValue.valueOf(script));
		LuaValue getRotationDelta = _G.get("getRotationDelta");

		float spread = 2;
		for (int i = 0; i < 10; i++) {
			Vector3f monsterPos = new Vector3f((float) (Math.random() - 0.5f) * spread, (float) (Math.random() - 0.5f) * spread, 0);
			// Vector3f monsterPos = new Vector3f(-0.45f, -0.45f, 0);

			float rotationDelta = getRotationDelta.call(LuaValue.valueOf(i)).tofloat();
			assContainer.addMonster(MonsterFactory.create(monsterMesh, shader, monsterPos, rotationDelta));
		}

		// Initialise projectile factory
		assContainer.setProjectileFactory(new GLDefaultProjectileFactory());
	}

	private void renderTiles(TileDataStructure2D dataStructure, GLShader shader) {
		dataStructure.draw(shader);

		/*
		 * TileDataStructure2D tiles = assContainer.getTileDataStructure(); for
		 * (AbstractTile tile : tiles.getTilesAsList()) { //
		 * System.out.println("Number of entities: " + //
		 * tile.getContainedEntities().size()); for (Map.Entry<Integer, Entity>
		 * entry : tile.getContainedEntities().entrySet()) { if
		 * (entry.getValue() instanceof Projectile) {
		 * System.out.println("Found a projectile!"); } } }
		 */

	}

	private void renderScenery(List<PolygonalScenery> scenery, GLShader shader) {
		for (PolygonalScenery s : scenery) {
			s.getModel().draw(shader);
		}
	}

	private void renderPlayer(Player player, GLShader shader) {
		player.getModel().draw(shader);
	}

	private void renderMonsters(List<Monster> monsters, GLShader shader) {

		List<Entity> toRemove = new ArrayList<Entity>();
		for (Monster m : monsters) {
			if (m.isDestroyable()) {
				toRemove.add(m);
			}
		}
		monsters.removeAll(toRemove);
		for (Monster m : monsters) {
			m.getModel().draw(shader);
		}
	}

	private void renderProjectiles(List<Projectile> projectiles, GLShader shader) {

		List<Entity> toRemove = new ArrayList<Entity>();

		if (projectiles.size() > 0) {
			for (Projectile p : projectiles) {
				if (p.isDestroyable()) {
					toRemove.add(p);
				} else if (p.getModel() == null) {
					p.createModel(assContainer.getProjectileFactory());
				}
			}
			projectiles.removeAll(toRemove);

			for (Projectile p : projectiles) {
				try {
					p.getModel().draw(shader);
				} catch (NullPointerException e) {
					System.out.println("Exception: GLModel does not exist");
				}

			}
		}
	}

	public void gameLoop() {
		// Update logic and render
		logicCycle();

		exitOnGLError("loopCycle");

		// Force a maximum FPS.
		Display.sync(MAX_FPS);

		// Let the CPU synchronize with the GPU if GPU is tagging behind
		Display.update();

		updateFPS();

		// TODO: Should this really be in the renderer?
		if (Display.isCloseRequested()) {
			mainApplication.quitGame();
		}
	}

	/**
	 * Calculate the frames per second, by counting the number of frames every
	 * second, and resetting that count at the end of each second.
	 * 
	 * TODO: Just displaying it in the title bar for now.
	 */
	private void updateFPS() {
		if (Utils.getTime() - lastFPSUpdate > 1000) {
			Display.setTitle("FPS: " + framesThisSecond);
			framesThisSecond = 0;
			lastFPSUpdate += 1000;
		}

		framesThisSecond++;
	}

	/**
	 * Calculate the time delta between now and the previous frame.
	 * 
	 * @return Milliseconds since the last frame.
	 */
	protected int getFrameTimeDelta() {
		long time = Utils.getTime();
		int delta = (int) (time - lastFrameTime);
		lastFrameTime = time;

		return delta;
	}
}
