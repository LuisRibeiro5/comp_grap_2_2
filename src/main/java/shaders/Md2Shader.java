package shaders;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

public class Md2Shader extends ShaderProgram {
	private static final String VERTEX_FILE = "./shaders/md2VertexShader.txt";
	private static final String FRAGMENT_FILE = "./shaders/md2FragmentShader.txt";

	private final int modelLocation;
	private final int viewLocation;
	private final int projectionLocation;
	private final int blendLocation;
	private final int textureLocation;

	public Md2Shader() {
		super(VERTEX_FILE, FRAGMENT_FILE);
		modelLocation = glGetUniformLocation(programID, "model");
		viewLocation = glGetUniformLocation(programID, "view");
		projectionLocation = glGetUniformLocation(programID, "projection");
		blendLocation = glGetUniformLocation(programID, "blend");
		textureLocation = glGetUniformLocation(programID, "skin");
	}

	@Override
	protected void bindAttributes() {
		bindAttribute(0, "positionA");
		bindAttribute(1, "positionB");
		bindAttribute(2, "textuv");
	}

	public void loadModel(FloatBuffer matrix) {
		glUniformMatrix4fv(modelLocation, false, matrix);
	}

	public void loadView(FloatBuffer matrix) {
		glUniformMatrix4fv(viewLocation, false, matrix);
	}

	public void loadProjection(FloatBuffer matrix) {
		glUniformMatrix4fv(projectionLocation, false, matrix);
	}

	public void loadBlend(float value) {
		glUniform1f(blendLocation, value);
	}

	public void loadTextureUnit(int unit) {
		glUniform1i(textureLocation, unit);
	}
}
