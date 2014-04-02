package renderer.glshaders;

import static org.lwjgl.opengl.GL20.glUniform1f;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

public class GLGaussianTessellationShader extends GLGaussianShader {

	// Tessellation Control shader
	protected int tessLevelInner;
	protected int tesLevelOuter;

	public GLGaussianTessellationShader(int projectionBlockIndex) {
		super(projectionBlockIndex);
	}

	protected int heightMapUniform;
	// Not really sure that this is the best way to set the texture
	private int heightmapLocation = -1;
	// Shader binding subset
	private int heightmapTexUnit = 0;
	// Opengl Shader binding
	// private int glHeightmapUnit = GL13.GL_TEXTURE0;

	// protected int normalmapUniform;
	// Not really sure that this is the best way to set the texture
	// private int normalmapLocation = -1;
	// Shader binding subset
	// private int normalmapTexUnit = 1;
	// Opengl Shader binding
	// private int glNormalmapUnit = GL13.GL_TEXTURE0;

	protected int colorMapUniform;
	// Not really sure that this is the best way to set the texture
	private int colorMapLocation = -1;
	// Shader binding subset
	private int colorMapTexUnit = 1;

	// Binding of texture as sampler
	private int sampler;

	@Override
	public void setupShaderVariables() {

		// Vertex shader uniforms
		modelToCameraMatrixUniform = GL20.glGetUniformLocation(programID, "modelToCameraMatrix");
		normalModelToCameraMatrixUniform = GL20.glGetUniformLocation(programID, "normalModelToCameraMatrix");
		System.err.println("modelToCameraMatrixUniform " + modelToCameraMatrixUniform);
		System.err.println("normalModelToCameraMatrixUniform " + normalModelToCameraMatrixUniform);

		// Tessellation Control shader uniforms
		tessLevelInner = GL20.glGetUniformLocation(programID, "tessLevelInner");
		tesLevelOuter = GL20.glGetUniformLocation(programID, "tessLevelOuter");
		System.err.println("tessLevelInner " + tessLevelInner);
		System.err.println("tessLevelOuter " + tesLevelOuter);

		// Tessellation Evaluation shader uniforms
		heightMapUniform = GL20.glGetUniformLocation(programID, "heightMap");
		// normalmapUniform = GL20.glGetUniformLocation(programID, "normalMap");
		colorMapUniform = GL20.glGetUniformLocation(programID, "testColorMap");
		System.err.println("height map " + heightMapUniform);
		System.err.println("color map " + colorMapUniform);

		// Fragment shader uniforms
		lightIntensityUniform = GL20.glGetUniformLocation(programID, "lightIntensity");
		ambientIntensityUniform = GL20.glGetUniformLocation(programID, "ambientIntensity");
		cameraSpaceLightPositionUniform = GL20.glGetUniformLocation(programID, "cameraSpaceLightPos");
		lightAttenuationUniform = GL20.glGetUniformLocation(programID, "lightAttenuation");
		shininessFactorUniform = GL20.glGetUniformLocation(programID, "shininessFactor");
		System.err.println("lightIntensityUniform " + lightIntensityUniform);
		System.err.println("ambientIntensityUniform " + ambientIntensityUniform);
		System.err.println("cameraSpaceLightPositionUniform " + cameraSpaceLightPositionUniform);
		System.err.println("lightAttenuationUniform " + lightAttenuationUniform);
		System.err.println("shininessFactorUniform " + shininessFactorUniform);

		int projectionBlock = GL31.glGetUniformBlockIndex(programID, "Projection");
		GL31.glUniformBlockBinding(programID, projectionBlock, projectionBlockIndex);

		setupSamplerUBO();
		// bindSamplerUnit();
	}

	@Override
	public void copyShaderSpecificUniformsToShaderInit() {
		glUniform1f(tessLevelInner, 2);
		glUniform1f(tesLevelOuter, 2);
	}

	private void setupSamplerUBO() {
		// This is an unusual one. Seems it only needs to be bound once, but
		// needs the actual shader bound when doing it. How odd.
		bindShader();
		GL20.glUniform1i(heightMapUniform, heightmapTexUnit);
		// GL20.glUniform1i(normalmapUniform, normalmapTexUnit);
		GL20.glUniform1i(colorMapUniform, colorMapTexUnit);
		unbindShader();
	}

	// This actually stops the interpolation of values, and means that you get
	// step functions rather than smooth values when sampling the heightmap
	public void bindSamplerUnit() {
		// Not sure whether this needs to happen after the OpenGL texture unit
		// has been created
		sampler = GL33.glGenSamplers();
		GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

		// This clamps the range of the sampler access. If enabled, it will be
		// strictly more accurate, though will result in gaps
		GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
	}

	public void bindTextures() {
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + heightmapTexUnit);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, heightmapLocation);
		GL20.glUniform1i(heightMapUniform, heightmapTexUnit);// May be wrong
		// GL33.glBindSampler(heightmapTexUnit, sampler);

		GL13.glActiveTexture(GL13.GL_TEXTURE0 + colorMapTexUnit);
		GL11.glBindTexture(GL11.GL_TEXTURE_1D, colorMapLocation);
		GL20.glUniform1i(colorMapUniform, colorMapTexUnit);
	}

	public void unbindTextures() {
		GL33.glBindSampler(heightmapTexUnit, 0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

		GL33.glBindSampler(colorMapTexUnit, 0);
		GL11.glBindTexture(GL11.GL_TEXTURE_1D, 0);
	}

	// public void bindNormalmap() {
	// GL13.glActiveTexture(GL13.GL_TEXTURE0 + normalmapTexUnit);
	// GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalmapLocation);
	// GL20.glUniform1i(normalmapUniform, normalmapTexUnit); // May be wrong
	// // GL33.glBindSampler(normalmapTexUnit, sampler);
	//
	// }
	//
	// public void unbindNormalmap() {
	// GL33.glBindSampler(normalmapTexUnit, 0);
	// GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	// }

	public void setHeightmapLocation(int heightmapLocation) {
		this.heightmapLocation = heightmapLocation;
	}

	public int getHeightmapLocation() {
		return heightmapLocation;
	}

	public void setColorMapLocation(int colorMapLocation) {
		this.colorMapLocation = colorMapLocation;
	}

	public int getColorMapLocation() {
		return colorMapLocation;
	}

	// public int getNormalmapTexUnit() {
	// return normalmapTexUnit;
	// }
	//
	// public void setNormalmapLocation(int normalmapLocation) {
	// this.normalmapLocation = normalmapLocation;
	// }
	//
	// public int getNormalmapLocation() {
	// return normalmapLocation;
	// }

}
