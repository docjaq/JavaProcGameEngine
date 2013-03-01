package renderer.glmodels;

import static maths.LinearAlgebra.degreesToRadians;
import static renderer.GLUtilityMethods.exitOnGLError;
import static renderer.GLUtilityMethods.loadPNGTexture;
import geometry.BoundingBox;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import renderer.GLWorld;

public class GLModel {

	// Quad Position variables
	public Vector3f modelPos;
	public Vector3f modelAngle;
	public Vector3f modelScale;

	// Quad variables
	public int vaoId = 0;
	public int vboId = 0;
	public int vboiId = 0;
	public int indicesCount = 0;

	// Textures
	private List<Integer> texIds;

	// Model Matrix
	public Matrix4f modelMatrix;

	// public FloatBuffer matrix44Buffer = null;

	public GLModel(Vector3f modelPos, Vector3f modelAngle, Vector3f modelScale) {
		// Set the default quad rotation, scale and position values
		this.modelPos = modelPos;
		this.modelAngle = modelAngle;
		this.modelScale = modelScale;

		texIds = new ArrayList<Integer>();

		modelMatrix = new Matrix4f();

		// matrix44Buffer = BufferUtils.createFloatBuffer(16);
	}

	// TODO: Check to see if there is a texture before rendering. Either that,
	// or subclass this into textured and non-textured objects
	// TODO:Presumably bind multiple textures, or have a choice which texture to
	// bind... or whatevs, like
	public void draw() {
		// Bind the texture
		// This is the texture unit
		GL13.glActiveTexture(GL13.GL_TEXTURE0);

		// This is the index to the specific texture to use
		// Currently, hard coded to the first texture id
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texIds.get(0));

		// Bind to the VAO that has all the information about the vertices
		GL30.glBindVertexArray(vaoId);

		// Currently, our GLModel, therefore, only supports 3 arrays; xyz, rgb,
		// and st. If we load them in the current manner, they MUST all be
		// present
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL20.glEnableVertexAttribArray(2);

		// Bind to the index VBO that has all the information about the order of
		// the vertices
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId);

		// Draw the vertices
		// Currently, our GLModel, therefore, can only consist of triangles
		GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0);

		// Put everything back to default (deselect)
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL20.glDisableVertexAttribArray(2);
		GL30.glBindVertexArray(0);
	}

	// TODO: This method should accept a string, the
	// texture name, and then it should read it into a list of texture ids (not
	// a fixed length array)
	public void setupTextures() {
		// Reads a PNG, creates an texture, binds it to memory within OPENGL,
		// sets it up (parameterisation-wise) and returns an int 'pointer' which
		// can be used to reference it in OpenGL

		texIds.add(loadPNGTexture("resources/images/stGrid1.png", GL13.GL_TEXTURE0));
		// texIds[1] = loadPNGTexture("resources/images/stGrid2.png",
		// GL13.GL_TEXTURE0); modelMatrix = new Matrix4f();

		exitOnGLError("setupTexture");
	}

	// TODO: Check if there is a texture, as with draw()
	public void cleanUp() {
		// Delete textures
		for (int i : texIds) {
			GL11.glDeleteTextures(i);
		}
		// Clean up geometry

		// Select the VAO
		GL30.glBindVertexArray(vaoId);

		// Disable the VBO index from the VAO attributes list
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		// This last one was missing in the original code, but seems important
		// to clear the data for the st texture coords
		GL20.glDisableVertexAttribArray(2);

		// Delete the vertex VBO
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(vboId);
		// modelMatrix = new Matrix4f();

		// Delete the index VBO
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(vboiId);

		// Delete the VAO
		GL30.glBindVertexArray(0);
		GL30.glDeleteVertexArrays(vaoId);

		exitOnGLError("destroyOpenGL");
	}

	// TODO: Implement this method
	public BoundingBox getBoundingBox() {
		return new BoundingBox();
	}

	public void transform() {
		// This order is important for building the matrix
		clearModelMatrix();
		Matrix4f.scale(modelScale, modelMatrix, modelMatrix);
		Matrix4f.translate(modelPos, modelMatrix, modelMatrix);
		Matrix4f.rotate(degreesToRadians(modelAngle.z), GLWorld.BASIS_Z, modelMatrix, modelMatrix);
		Matrix4f.rotate(degreesToRadians(modelAngle.y), GLWorld.BASIS_Y, modelMatrix, modelMatrix);
		Matrix4f.rotate(degreesToRadians(modelAngle.x), GLWorld.BASIS_X, modelMatrix, modelMatrix);
	}

	// ATTENTION: Before, had a local Floatbuffer in the class, but as it's just
	// used to copy stuff to the shader, seems a waste. Maybe a better way than
	// sending the reference around though?
	public void copyModelMatrixToShader(int modelMatrixLocation, FloatBuffer matrix44Buffer) {
		modelMatrix.store(matrix44Buffer);
		matrix44Buffer.flip();
		GL20.glUniformMatrix4(modelMatrixLocation, false, matrix44Buffer);
	}

	// public void matrixCleanup() {
	// modelMatrix.store(matrix44Buffer);
	// matrix44Buffer.flip();
	// GL20.glUniformMatrix4(modelMatrixLocation, false, matrix44Buffer);
	// }

	// ATTENTION:Setting it to the identity seems more efficient than creating a
	// new one?
	private void clearModelMatrix() {
		// modelMatrix = new Matrix4f();
		modelMatrix.setIdentity();
	}
}
