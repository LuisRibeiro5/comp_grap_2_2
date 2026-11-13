package obj;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.glu.*;

import shaders.ShaderProgram;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import Model.Model;
import Model.VboCube;

public class Cubo3D extends Object3D {
	//Sphere sphere = new Sphere();
	public Vector3f cor = new Vector3f();
	public Model model = null;
	private static final long DURACAO_EXPLOSAO_MS = 250;
	private final float tamanhoOriginal;
	private long tempoExplosao;
	private boolean explodindo;
	public boolean vivo = true;
	
	FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(16);
	public float rotvel = 0;
	float ang = 0;
	
	public Cubo3D(float x, float y, float z, float r) {
		super(x, y, z);
		raio = r;
		tamanhoOriginal = r;
	}
	
	@Override
	public void DesenhaSe(ShaderProgram shader) {
		if (!vivo) {
			return;
		}
//		glPushMatrix();
//		
//	    glDisable(GL_TEXTURE_2D);
//	    glColor3f(cor.x, cor.y, cor.z);
//	    
//		glTranslatef(x,y,z);
//		
//		sphere.draw(raio, 16, 16);
//		
//		glPopMatrix();
		
		Matrix4f modelm = new Matrix4f();
		modelm.setIdentity();
		
		//System.out.println(""+x+" "+y+" "+z);
		modelm.translate(new Vector3f(x,y,z));
		modelm.scale(new Vector3f(raio,raio,raio));
		modelm.rotate(ang,new Vector3f(0.0f,1.0f,0.0f));
		
		int modellocation = glGetUniformLocation(shader.programID, "model");
		
		modelm.storeTranspose(matrixBuffer);
		matrixBuffer.flip();
		glUniformMatrix4fv(modellocation, false, matrixBuffer);	
		
		model.draw();
	}
	
	@Override
	public void SimulaSe(long diftime) {
		super.SimulaSe(diftime);

		// Expande o cubo durante um curto periodo e depois o marca para remocao.
		if (explodindo) {
			tempoExplosao += diftime;
			if (tempoExplosao >= DURACAO_EXPLOSAO_MS) {
				vivo = false;
				return;
			}
			float progresso = (float) tempoExplosao / DURACAO_EXPLOSAO_MS;
			raio = tamanhoOriginal * (1.0f + 3.0f * progresso);
			return;
		}
		
		x += vx*diftime/1000.0f;
		y += vy*diftime/1000.0f;
		z += vz*diftime/1000.0f;
		
		ang+=rotvel*diftime/1000.0f;
	}

	public void explodir() {
		explodindo = true;
		tempoExplosao = 0;
	}

	/**
	 * Ela pega a diferença entre as coordenadas dos objetos,
	 * calcula a distância entre os centros e compara com a soma dos tamanhos deles.
	 * Se a distância for menor ou igual, considera que houve colisão.
	 */
	public boolean colideCom(Projetil projetil) {
		if (!vivo || explodindo) {
			return false;
		}

		float dx = x - projetil.x;
		float dy = y - projetil.y;
		float dz = z - projetil.z;

		float raioColisao = (Math.abs(raio) + Math.abs(projetil.raio)) * (float) Math.sqrt(3.0);
		return dx * dx + dy * dy + dz * dz <= raioColisao * raioColisao;
	}
}
