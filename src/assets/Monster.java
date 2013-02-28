package assets;

import org.lwjgl.util.vector.Vector3f;

import renderer.glmodels.GLModel;

//This should probably be a class NPC, which Monster then extends, but decided
//to simplify it
public class Monster extends Character {

	private int level;
	private float[] color;

	private float rotationDelta = 0.01f;
	private float scaleDelta = 0.001f;
	private float posDelta = 0.001f;
	private Vector3f scaleAddResolution = new Vector3f(scaleDelta, scaleDelta, scaleDelta);
	private Vector3f scaleMinusResolution = new Vector3f(-scaleDelta, -scaleDelta, -scaleDelta);

	public Monster(GLModel model, String name, int level, float[] color) {
		super(model, name);
		this.level = level;
		this.color = color;
	}

	public int getLevel() {
		return level;
	}

	public void moveLeft() {
		model.modelPos.x -= posDelta;
	}

	public void moveRight() {
		model.modelPos.x += posDelta;
	}

	public void moveUp() {
		model.modelPos.y += posDelta;
	}

	public void moveDown() {
		model.modelPos.y -= posDelta;
	}

	public void increaseScale() {
		Vector3f.add(model.modelScale, scaleAddResolution, model.modelScale);
	}

	public void decreaseScale() {
		Vector3f.add(model.modelScale, scaleMinusResolution, model.modelScale);
	}

	public void rotate() {
		model.modelAngle.z += rotationDelta;
	}
}
