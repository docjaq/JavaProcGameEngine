package renderer;

import static maths.LinearAlgebra.degreesToRadians;
import static renderer.GLUtilityMethods.exitOnGLError;
import static renderer.GLUtilityMethods.loadShader;
import static renderer.GLUtilityMethods.setupOpenGL;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import renderer.glmodels.GLTexturedQuad;
import exception.RendererException;

public class Renderer_3_2 {
	// Entry point for the application
	public static void main(String[] args) {
		try {
			new Renderer_3_2();
		} catch (RendererException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	// Setup variables
	private final String WINDOW_TITLE = "Footsnip";
	private final int WIDTH = 800;
	private final int HEIGHT = 600;

	// Shader variables
	private int vsId = 0; // vertex shader ID
	private int fsId = 0; // fragment/pixel shader ID
	private int pId = 0; // program ID

	// TODO: For now, have a data-structure here of all our entities, etc
	private GLTexturedQuad texturedQuad;

	/*
	 * TODO:
	 * 
	 * - Create a datastructure of all the models - Figure out some way to
	 * notify all the models in some list and update their positions, angles,
	 * scales, etc
	 * 
	 * - I think these shaders simply allow texturing and transforms, so they're
	 * 'pipeline' shaders. I imagine each model could also have shaders (or
	 * share shaders in some static pool)
	 */

	private GLWorld glWorld;

	public Renderer_3_2() throws RendererException {
		// Initialize OpenGL (Display)
		setupOpenGL(WIDTH, HEIGHT, WINDOW_TITLE);

		// The vector3f defines the starting camera position. This is not
		// actually modified I don't think...
		glWorld = new GLWorld(WIDTH, HEIGHT, new Vector3f(0, 0, 0));

		// game threads
		// update entity stuff

		// start renderer while loop
		Vector3f modelPos = new Vector3f(0, 0, 0);
		Vector3f modelAngle = new Vector3f(0, 0, 0);
		Vector3f modelScale = new Vector3f(0.2f, 0.2f, 0.2f);
		texturedQuad = new GLTexturedQuad(modelPos, modelAngle, modelScale);

		this.setupShaders();
		// this.setupTextures();

		texturedQuad.setupTextures();

		while (!Display.isCloseRequested()) {
			// Do a single loop (logic/render)
			this.loopCycle();

			// Force a maximum FPS of about 60
			Display.sync(0);
			// Let the CPU synchronize with the GPU if GPU is tagging behind
			Display.update();
		}

		// Destroy OpenGL (Display)
		this.destroyOpenGL();
	}

	private void setupShaders() throws RendererException {
		// Load the vertex shader
		vsId = loadShader("resources/shaders/moving/vertex.glsl", GL20.GL_VERTEX_SHADER);
		// Load the fragment shader
		fsId = loadShader("resources/shaders/moving/fragment.glsl", GL20.GL_FRAGMENT_SHADER);

		// Create a new shader program that links both shaders
		pId = GL20.glCreateProgram();
		GL20.glAttachShader(pId, vsId);
		GL20.glAttachShader(pId, fsId);
		GL20.glLinkProgram(pId);

		// Position information will be attribute 0
		GL20.glBindAttribLocation(pId, 0, "in_Position");
		// Color information will be attribute 1
		GL20.glBindAttribLocation(pId, 1, "in_Color");
		// Texture information will be attribute 2
		GL20.glBindAttribLocation(pId, 2, "in_TextureCoord");

		// Get matrices uniform locations
		glWorld.projectionMatrixLocation = GL20.glGetUniformLocation(pId, "projectionMatrix");
		glWorld.viewMatrixLocation = GL20.glGetUniformLocation(pId, "viewMatrix");
		glWorld.modelMatrixLocation = GL20.glGetUniformLocation(pId, "modelMatrix");

		GL20.glValidateProgram(pId);

		exitOnGLError("setupShaders");
	}

	// TODO: Sort this method out so we can apply transforms and shit to our
	// 'data strcure' of models. Currently it's pretty hard-coded to our
	// texturedQuad model
	private void logicCycle() {
		// -- Input processing
		float rotationDelta = 0.05f;
		float scaleDelta = 0.001f;
		float posDelta = 0.001f;
		Vector3f scaleAddResolution = new Vector3f(scaleDelta, scaleDelta, scaleDelta);
		Vector3f scaleMinusResolution = new Vector3f(-scaleDelta, -scaleDelta, -scaleDelta);

		// Allows you to hold the key down
		Keyboard.enableRepeatEvents(true);

		/*
		 * if (Keyboard.isKeyDown(Keyboard.KEY_1) && !Keyboard.isRepeatEvent())
		 * textureSelector = 0; if (Keyboard.isKeyDown(Keyboard.KEY_2) &&
		 * !Keyboard.isRepeatEvent()) textureSelector = 1;
		 */

		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT))
			texturedQuad.modelPos.x -= posDelta;
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT))
			texturedQuad.modelPos.x += posDelta;
		if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))
			texturedQuad.modelPos.y -= posDelta;
		if (Keyboard.isKeyDown(Keyboard.KEY_UP))
			texturedQuad.modelPos.y += posDelta;

		if (Keyboard.isKeyDown(Keyboard.KEY_PERIOD))
			Vector3f.add(texturedQuad.modelScale, scaleAddResolution, texturedQuad.modelScale);
		if (Keyboard.isKeyDown(Keyboard.KEY_COMMA))
			Vector3f.add(texturedQuad.modelScale, scaleMinusResolution, texturedQuad.modelScale);

		// Just set up a standard rotation for testing
		texturedQuad.modelAngle.z += rotationDelta;

		// -- Update matrices
		// Reset view and model matrices
		glWorld.viewMatrix = new Matrix4f();
		glWorld.modelMatrix = new Matrix4f();

		// Translate camera
		Matrix4f.translate(glWorld.cameraPos, glWorld.viewMatrix, glWorld.viewMatrix);

		// Scale, translate and rotate model
		Matrix4f.scale(texturedQuad.modelScale, glWorld.modelMatrix, glWorld.modelMatrix);
		Matrix4f.translate(texturedQuad.modelPos, glWorld.modelMatrix, glWorld.modelMatrix);
		Matrix4f.rotate(degreesToRadians(texturedQuad.modelAngle.z), GLWorld.BASIS_Z, glWorld.modelMatrix, glWorld.modelMatrix);
		Matrix4f.rotate(degreesToRadians(texturedQuad.modelAngle.y), GLWorld.BASIS_Y, glWorld.modelMatrix, glWorld.modelMatrix);
		Matrix4f.rotate(degreesToRadians(texturedQuad.modelAngle.x), GLWorld.BASIS_X, glWorld.modelMatrix, glWorld.modelMatrix);

		// Upload matrices to the uniform variables
		GL20.glUseProgram(pId);

		// Clean up

		glWorld.projectionMatrix.store(glWorld.matrix44Buffer);
		glWorld.matrix44Buffer.flip();
		GL20.glUniformMatrix4(glWorld.projectionMatrixLocation, false, glWorld.matrix44Buffer);
		glWorld.viewMatrix.store(glWorld.matrix44Buffer);
		glWorld.matrix44Buffer.flip();
		GL20.glUniformMatrix4(glWorld.viewMatrixLocation, false, glWorld.matrix44Buffer);
		glWorld.modelMatrix.store(glWorld.matrix44Buffer);
		glWorld.matrix44Buffer.flip();
		GL20.glUniformMatrix4(glWorld.modelMatrixLocation, false, glWorld.matrix44Buffer);

		GL20.glUseProgram(0);

		exitOnGLError("logicCycle");
	}

	// I don't understand the buffers and Array objects yet. Decide what needs
	// to be in a model and what is shared between models. I have a feeling that
	// each model has a VBA, and a VBO contains lots of VBAs. But I'm not sure,
	// so for simplicity for now, they are all in the GLTexturedQuad class.
	private void renderCycle() {
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		// This seems to activate the shaders
		GL20.glUseProgram(pId);

		texturedQuad.draw();

		// Deactivates the shaders
		GL20.glUseProgram(0);

		exitOnGLError("renderCycle");
	}

	private void loopCycle() {
		// Update logic
		this.logicCycle();
		// Update rendered frame
		this.renderCycle();

		exitOnGLError("loopCycle");
	}

	private void destroyOpenGL() {

		// Delete the core shaders.
		GL20.glUseProgram(0);
		GL20.glDetachShader(pId, vsId);
		GL20.glDetachShader(pId, fsId);

		GL20.glDeleteShader(vsId);
		GL20.glDeleteShader(fsId);
		GL20.glDeleteProgram(pId);

		// Clean up all of our models
		texturedQuad.cleanUp();

		Display.destroy();
	}

}