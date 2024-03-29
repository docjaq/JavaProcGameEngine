package assets.world;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import renderer.GLPosition;
import renderer.glmodels.GLMesh;
import renderer.glprimitives.GLTriangle;
import renderer.glprimitives.GLVertex;
import terraingen.MapGenerationUtilities;
import terraingen.MeshGenerationUtilities;
import terraingen.simplex.SimplexNoise;
import assets.world.datastructures.DataStructureKey2D;
import assets.world.datastructures.TileDataStructure2D;

public class PolygonHeightmapTileFactory {

	private final static float Z_OFFSET = 0f;
	private final static double WATER_CHANCE = 1;
	private final static int COLOR_MAP_SIZE = 32;

	// private List<GLVertex> factoryVertices;
	// private List<GLTriangle> factoryTriangles;
	// private GLModel openGLReferenceMesh;

	private int tileComplexity;
	private TileDataStructure2D tileDataStructure;

	private SimplexNoise simplexNoise;

	// TODO: As this is only created once, should really only set it once in the
	// shader...
	private FloatBuffer colorMapBuffer;

	public GLMesh getOpenGLReferenceMesh() {
		return openGLReferenceMesh;
	}

	private GLMesh openGLReferenceMesh;

	public PolygonHeightmapTileFactory(int tileComplexity, TileDataStructure2D tileDataStructure) {
		this.tileComplexity = tileComplexity;
		this.tileDataStructure = tileDataStructure;

		// MeshGenerationUtilities.generatePlanarMesh(this.factoryVertices,
		// this.factoryTriangles);

		// Function of number of octaves (noise complexity)
		// Type of terrain (low = flat. High = rocky)
		// Random seed
		simplexNoise = new SimplexNoise(320, 0.5, 5000);

		// adjustMeshToHeightmap(this.factoryVertices,
		// simplexNoise.getSection(tileComplexity, 0, 0));

		List<GLVertex> vertices = new ArrayList<GLVertex>(tileComplexity * tileComplexity);
		List<GLTriangle> triangles = new ArrayList<GLTriangle>((tileComplexity - 1) * (tileComplexity - 1) * 2);
		System.out.println("Tile complexity from PolygonHeightmap = " + tileComplexity);
		MeshGenerationUtilities.generatePlanarMesh(vertices, triangles, tileComplexity);
		openGLReferenceMesh = new GLMesh(triangles, vertices);
		openGLReferenceMesh.pushToGPU();

		colorMapBuffer = MapGenerationUtilities.generateColorMap(COLOR_MAP_SIZE);

	}

	public AbstractTile create(DataStructureKey2D key, GLPosition position) {

		// System.out.println("Creating new tile");

		// TODO: I think it just passes this for the GPU... maybe clean this up
		AbstractTile tile = new PolygonHeightmapTile(key, openGLReferenceMesh, position);
		if (key == null)
			key = new DataStructureKey2D(0, 0);

		ByteBuffer heightmapBuff = simplexNoise.getSectionAsByteBuffer(tileComplexity, key.x, key.y, 0.4f, 0.8f);

		if (PolygonHeightmapTile.class.isInstance(tile)) {
			PolygonHeightmapTile polygonTile = ((PolygonHeightmapTile) tile);

			polygonTile.setHeightmapBuf(heightmapBuff);

			polygonTile.setHeightmapSize(tileComplexity);

			polygonTile.setColorMap(colorMapBuffer);
			polygonTile.setColorMapSize(COLOR_MAP_SIZE);

			// Water stuff
			polygonTile.setWater((Math.random() < WATER_CHANCE) ? true : false);
			polygonTile.setWaterHeight((float) ((0.05 - 0.025) + 0.45));
		}

		// System.out.print("\n\n");

		return tile;
	}

	// TODO: Replace this method by generating the simplex output as a buffer
	// directly
	// private ByteBuffer convertArrayToBuffer(float[][] array) {
	// int tWidth = array.length;
	// int tHeight = array.length;
	//
	// // Change this to
	// //
	// http://stackoverflow.com/questions/7070576/get-one-dimensionial-array-from-a-mutlidimensional-array-in-java
	// // Float is four bytes
	// ByteBuffer buf = ByteBuffer.allocateDirect(tWidth * tHeight *
	// 4).order(ByteOrder.nativeOrder());
	//
	// // verticesByteBuffer = BufferUtils.createByteBuffer(vertexList.size() *
	// // GLVertex.stride).order(ByteOrder.nativeOrder());
	//
	// System.out.println("Converted array pre-buffer");
	// FloatBuffer fBuf = buf.asFloatBuffer();
	// for (int y = 0; y < tHeight; y++) {
	// for (int x = 0; x < tWidth; x++) {
	// // Scale it like this to make sure the value is always positive
	// // Due to texture format (unsigned int)
	// // (but over the same range. Invert in shader)
	//
	// System.out.print((array[x][y]) + " ");
	// fBuf.put((array[x][y] + 1f) / 2f);// (x +1f) / 2f;
	// }
	// }
	// System.out.println();
	//
	// fBuf.flip();
	//
	// int numItems = tWidth * tWidth;
	//
	// System.out.println("Converted Array");
	// for (int i = 0; i < numItems; i++) {
	// System.out.print(fBuf.get() + " ");
	// }
	// fBuf.rewind();
	// System.out.println();
	//
	// return buf;
	// }
}
