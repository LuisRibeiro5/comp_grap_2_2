package Model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

/**
 * Carrega a geometria MD2 e prepara dois frames para interpolacao no vertex shader.
 */
public class Md2Model {
	private final int skinWidth;
	private final int skinHeight;
	private final float[][] frames;
	private final short[][] textureCoordinates;
	private final int[][] triangles;
	private final int[][] triangleTextureCoordinates;
	private final int vertexCount;

	private int positionBufferA;
	private int positionBufferB;
	private int textureBuffer;
	private int uploadedFrameA = -1;
	private int uploadedFrameB = -1;
	private float animationTime;
	private int firstFrame;
	private int nextFrame;
	private float blend;

	public Md2Model(String file) throws IOException {
		ByteBuffer data = ByteBuffer.wrap(Files.readAllBytes(Paths.get(file))).order(ByteOrder.LITTLE_ENDIAN);
		if (data.remaining() < 68) {
			throw new IOException("Arquivo MD2 invalido: cabecalho incompleto.");
		}

		int ident = data.getInt(0);
		int version = data.getInt(4);
		if (ident != 0x32504449 || version != 8) {
			throw new IOException("Arquivo MD2 invalido: esperado IDP2, versao 8.");
		}

		skinWidth = data.getInt(8);
		skinHeight = data.getInt(12);
		int frameSize = data.getInt(16);
		int skinCount = data.getInt(20);
		int vertexCountInFrame = data.getInt(24);
		int textureCoordinateCount = data.getInt(28);
		int triangleCount = data.getInt(32);
		int frameCount = data.getInt(40);
		int textureCoordinateOffset = data.getInt(48);
		int triangleOffset = data.getInt(52);
		int frameOffset = data.getInt(56);
		int endOffset = data.getInt(64);

		if (skinWidth <= 0 || skinHeight <= 0 || vertexCountInFrame <= 0 || textureCoordinateCount <= 0
				|| triangleCount <= 0 || frameCount <= 0 || skinCount < 0
				|| frameSize < 40L + vertexCountInFrame * 4L) {
			throw new IOException("Arquivo MD2 invalido: dimensoes ou contagens fora do esperado.");
		}

		requireRange(data, textureCoordinateOffset, textureCoordinateCount * 4L, "coordenadas de textura");
		requireRange(data, triangleOffset, triangleCount * 12L, "triangulos");
		requireRange(data, frameOffset, frameCount * (long) frameSize, "frames");
		if (endOffset < 68 || endOffset > data.limit()) {
			throw new IOException("Arquivo MD2 invalido: offset final fora do arquivo.");
		}

		this.vertexCount = triangleCount * 3;
		frames = new float[frameCount][vertexCountInFrame * 3];
		textureCoordinates = new short[textureCoordinateCount][2];
		triangles = new int[triangleCount][3];
		triangleTextureCoordinates = new int[triangleCount][3];

		for (int i = 0; i < textureCoordinateCount; i++) {
			int offset = textureCoordinateOffset + i * 4;
			textureCoordinates[i][0] = data.getShort(offset);
			textureCoordinates[i][1] = data.getShort(offset + 2);
		}

		for (int i = 0; i < triangleCount; i++) {
			int offset = triangleOffset + i * 12;
			for (int corner = 0; corner < 3; corner++) {
				triangles[i][corner] = unsignedShort(data.getShort(offset + corner * 2));
				triangleTextureCoordinates[i][corner] = unsignedShort(data.getShort(offset + 6 + corner * 2));
				if (triangles[i][corner] >= vertexCountInFrame
						|| triangleTextureCoordinates[i][corner] >= textureCoordinateCount) {
					throw new IOException("Arquivo MD2 invalido: indice de triangulo fora do limite.");
				}
			}
		}

		for (int frame = 0; frame < frameCount; frame++) {
			int offset = frameOffset + frame * frameSize;
			float scaleX = data.getFloat(offset);
			float scaleY = data.getFloat(offset + 4);
			float scaleZ = data.getFloat(offset + 8);
			float translateX = data.getFloat(offset + 12);
			float translateY = data.getFloat(offset + 16);
			float translateZ = data.getFloat(offset + 20);
			int verticesOffset = offset + 40;
			for (int vertex = 0; vertex < vertexCountInFrame; vertex++) {
				int source = verticesOffset + vertex * 4;
				int destination = vertex * 3;
				float x = (data.get(source) & 0xff) * scaleX + translateX;
				float y = (data.get(source + 1) & 0xff) * scaleY + translateY;
				float z = (data.get(source + 2) & 0xff) * scaleZ + translateZ;

				// O MD2 usa Z para cima; convertemos para o Y para cima usado na cena.
				frames[frame][destination] = x;
				frames[frame][destination + 1] = z;
				frames[frame][destination + 2] = -y;
			}
		}
	}

	private static void requireRange(ByteBuffer data, int offset, long size, String section) throws IOException {
		if (offset < 0 || size < 0 || (long) offset + size > data.limit()) {
			throw new IOException("Arquivo MD2 invalido: secao de " + section + " fora do arquivo.");
		}
	}

	private static int unsignedShort(short value) {
		return value & 0xffff;
	}

	/** Deve ser chamado com o contexto OpenGL ativo. */
	public void load() {
		positionBufferA = glGenBuffers();
		positionBufferB = glGenBuffers();
		textureBuffer = glGenBuffers();

		int positionBytes = vertexCount * 3 * Float.BYTES;
		glBindBuffer(GL_ARRAY_BUFFER, positionBufferA);
		glBufferData(GL_ARRAY_BUFFER, positionBytes, GL_DYNAMIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, positionBufferB);
		glBufferData(GL_ARRAY_BUFFER, positionBytes, GL_DYNAMIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, textureBuffer);
		glBufferData(GL_ARRAY_BUFFER, createTextureBuffer(), GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		uploadFrame(0, positionBufferA);
		uploadFrame(frames.length > 1 ? 1 : 0, positionBufferB);
		uploadedFrameA = 0;
		uploadedFrameB = frames.length > 1 ? 1 : 0;
	}

	/** Avanca a animacao e troca os buffers apenas quando o frame muda. */
	public void update(long elapsedMilliseconds) {
		animationTime += elapsedMilliseconds * 0.01f;
		float framePosition = animationTime;
		firstFrame = ((int) framePosition) % frames.length;
		nextFrame = (firstFrame + 1) % frames.length;
		blend = framePosition - (float) Math.floor(framePosition);

		if (firstFrame != uploadedFrameA) {
			uploadFrame(firstFrame, positionBufferA);
			uploadedFrameA = firstFrame;
		}
		if (nextFrame != uploadedFrameB) {
			uploadFrame(nextFrame, positionBufferB);
			uploadedFrameB = nextFrame;
		}
	}

	public void draw(shaders.Md2Shader shader) {
		shader.loadBlend(blend);

		glBindBuffer(GL_ARRAY_BUFFER, positionBufferA);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);

		glBindBuffer(GL_ARRAY_BUFFER, positionBufferB);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0L);

		glBindBuffer(GL_ARRAY_BUFFER, textureBuffer);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0L);

		glDrawArrays(GL_TRIANGLES, 0, vertexCount);

		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	private FloatBuffer createTextureBuffer() {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexCount * 2);
		for (int triangle = 0; triangle < triangles.length; triangle++) {
			for (int corner = 0; corner < 3; corner++) {
				short[] uv = textureCoordinates[triangleTextureCoordinates[triangle][corner]];
				buffer.put((float) uv[0] / skinWidth);
				buffer.put(1.0f - (float) uv[1] / skinHeight);
			}
		}
		buffer.flip();
		return buffer;
	}

	private void uploadFrame(int frame, int bufferHandle) {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexCount * 3);
		for (int[] triangle : triangles) {
			for (int vertex : triangle) {
				int offset = vertex * 3;
				buffer.put(frames[frame][offset]);
				buffer.put(frames[frame][offset + 1]);
				buffer.put(frames[frame][offset + 2]);
			}
		}
		buffer.flip();

		glBindBuffer(GL_ARRAY_BUFFER, bufferHandle);
		glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	public void cleanUp() {
		glDeleteBuffers(positionBufferA);
		glDeleteBuffers(positionBufferB);
		glDeleteBuffers(textureBuffer);
	}
}
