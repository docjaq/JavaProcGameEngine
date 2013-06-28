package renderer.glmodels;

import static renderer.GLUtilityMethods.exitOnGLError;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import renderer.glprimitives.GLVertex;
import renderer.glshaders.GLShader;

public class GLTilePlane extends GLModel {

	final static float length = 1f;
	final static float zOffset = -0.01f;

	public GLTilePlane(Vector3f modelPos, Vector3f modelAngle, float modelScale, GLShader shader, float[] color) {
		super(modelPos, modelAngle, modelScale, shader, color);

		Vector4f rgba = new Vector4f((float) Math.random(), (float) Math.random(), (float) Math.random(), 1);
		float halfLength = length / 2.0f;

		List<GLVertex> vertexList = new ArrayList<GLVertex>(4);
		vertexList.add(new GLVertex(new Vector4f(-halfLength, -halfLength, zOffset, 1), rgba, new Vector4f(0, 0, 1, 1)));
		vertexList.add(new GLVertex(new Vector4f(-halfLength, halfLength, zOffset, 1), rgba, new Vector4f(0, 0, 1, 1)));
		vertexList.add(new GLVertex(new Vector4f(halfLength, halfLength, zOffset, 1), rgba, new Vector4f(0, 0, 1, 1)));
		vertexList.add(new GLVertex(new Vector4f(halfLength, -halfLength, zOffset, 1), rgba, new Vector4f(0, 0, 1, 1)));

		FloatBuffer verticesFloatBuffer = BufferUtils.createFloatBuffer(vertexList.size() * GLVertex.stride);
		for (GLVertex v : vertexList) {
			verticesFloatBuffer.put(v.getElements());
		}
		verticesFloatBuffer.flip();

		// OpenGL expects to draw vertices in counter clockwise order by default
		int[] indices = { 0, 2, 1, 0, 3, 2 };
		indicesCount = indices.length;
		IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indicesCount);
		indicesBuffer.put(indices);
		indicesBuffer.flip();

		// Create a new Vertex Array Object in memory and select it (bind)
		vaoId = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vaoId);

		// Create a new Vertex Buffer Object in memory and select it (bind)
		vboId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesFloatBuffer, GL15.GL_STATIC_DRAW);

		// Loads all vertex data into a single VBO to save memory on overheads,
		// and to reduce the number of GL calls made
		GL20.glVertexAttribPointer(0, GLVertex.positionElementCount, GL11.GL_FLOAT, false, GLVertex.stride, GLVertex.positionByteOffset);
		GL20.glVertexAttribPointer(1, GLVertex.colorElementCount, GL11.GL_FLOAT, false, GLVertex.stride, GLVertex.colorByteOffset);
		GL20.glVertexAttribPointer(2, GLVertex.normalElementCount, GL11.GL_FLOAT, false, GLVertex.stride, GLVertex.normalByteOffset);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		// Deselect (bind to 0) the VAO
		GL30.glBindVertexArray(0);

		// Create a new VBO for the indices and select it (bind) - INDICES
		vboiId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

		exitOnGLError("setupQuad");

		setRadius();
	}

	/**
	 * Actual tile has no radius, though the things that belong to the tile will
	 **/
	protected void setRadius() {
		radius = 0;

	}

}
