
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import Model.Md2Model;
import obj.ObjModel;
import shaders.Md2Shader;
import shaders.StaticShader;
import util.TextureLoader;

import java.awt.image.BufferedImage;
import java.io.IOException;

//import com.sun.org.apache.xerces.internal.dom.DeepNodeListImpl;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main3D {

	// The window handle
	private long window;
	
	float viewAngX = 0;
	float viewAngY = 0;
	float scale = 1.0f;
	
	StaticShader shader;
	

	public void run(String[] args) throws IOException {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		if (args.length > 0) {
			loopMd2(args);
		} else {
			loop();
		}

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	/** Executa o exemplo MD2: java Main3D modelo.md2 [textura.png]. */
	private void loopMd2(String[] args) throws IOException {
		if (args.length > 2) {
			throw new IllegalArgumentException("Uso: Main3D modelo.md2 [textura.png]");
		}

		GL.createCapabilities();
		Md2Model model = new Md2Model(args[0]);
		model.load();
		Md2Shader md2Shader = new Md2Shader();

		String skinPath = args.length == 2 ? args[1] : "texturaGato.jpeg";
		BufferedImage skinImage = TextureLoader.loadImage(skinPath);
		if (skinImage == null) {
			model.cleanUp();
			md2Shader.cleanUp();
			throw new IOException("Nao foi possivel carregar a textura MD2: " + skinPath);
		}
		int skinTexture = TextureLoader.loadTexture(skinImage);

		glEnable(GL_DEPTH_TEST);
		glClearColor(0.1f, 0.1f, 0.15f, 1.0f);

		// Projecao perspectiva em formato column-major, como esperado pelo OpenGL.
		float f = 1.0f / (float) Math.tan(Math.toRadians(45.0f) / 2.0f);
		float aspect = 1500.0f / 1000.0f;
		FloatBuffer projection = BufferUtils.createFloatBuffer(16);
		projection.put(new float[] {
				f / aspect, 0, 0, 0,
				0, f, 0, 0,
				0, 0, -1.002002f, -1,
				0, 0, -0.2002002f, 0
		}).flip();

		FloatBuffer modelMatrix = BufferUtils.createFloatBuffer(16);
		FloatBuffer viewMatrix = BufferUtils.createFloatBuffer(16);
		long previousTime = System.currentTimeMillis();
		try {
			while (!glfwWindowShouldClose(window)) {
				long now = System.currentTimeMillis();
				model.update(now - previousTime);
				previousTime = now;

				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				md2Shader.start();
				md2Shader.loadProjection(projection);
				md2Shader.loadTextureUnit(0);
				glActiveTexture(GL_TEXTURE0);
				glBindTexture(GL_TEXTURE_2D, skinTexture);

				Matrix4f view = new Matrix4f();
				view.setIdentity();
				view.scale(new Vector3f(scale, scale, scale));
				view.rotate(viewAngX * 0.0174532f, new Vector3f(1, 0, 0));
				view.rotate(viewAngY * 0.0174532f, new Vector3f(0, 1, 0));
				view.storeTranspose(viewMatrix);
				viewMatrix.flip();
				md2Shader.loadView(viewMatrix);

				Matrix4f modelTransform = new Matrix4f();
				modelTransform.setIdentity();
				modelTransform.translate(new Vector3f(0, 0, -4.5f));
				modelTransform.scale(new Vector3f(0.025f, 0.025f, 0.025f));
				modelTransform.storeTranspose(modelMatrix);
				modelMatrix.flip();
				md2Shader.loadModel(modelMatrix);
				model.draw(md2Shader);
				md2Shader.stop();

				glfwSwapBuffers(window);
				glfwPollEvents();
			}
		} finally {
			model.cleanUp();
			md2Shader.cleanUp();
			glDeleteTextures(skinTexture);
		}
	}

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(1500, 1000, "Hello World!", NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated
		// or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
		
			if ( key == GLFW_KEY_W) {
				viewAngX+=5;
			}
			if ( key == GLFW_KEY_S) {
				viewAngX-=5;
			}
			if ( key == GLFW_KEY_A) {
				viewAngY+=5;
			}
			if ( key == GLFW_KEY_D) {
				viewAngY-=5;
			}
			if ( key == GLFW_KEY_Z) {
				scale = scale*1.1f;
			}
			if ( key == GLFW_KEY_X) {
				scale = scale*0.9f;
			}
		
		});

		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);
	}

	private void loop() throws IOException {
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.

		GL.createCapabilities();

		
		
		shader = new StaticShader();
		// Neste projeto mostramos apenas o jatinho, sem os objetos de exemplo extras.
		ObjModel jatinho = new ObjModel();
		jatinho.loadObj("x-35_obj.obj");
		jatinho.load();
		System.out.println("Faces do jatinho: " + jatinho.f.size());

		BufferedImage texturaJatinho = TextureLoader.loadImage("x-35_obj.jpg");
		if (texturaJatinho == null) {
			throw new IOException("Nao foi possivel carregar a textura x-35_obj.jpg.");
		}
		int textura = TextureLoader.loadTexture(texturaJatinho);

		int frame = 0;
		long lasttime = System.currentTimeMillis();

		float angle = 0;
		
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(45, 600f / 800f, 0.5f, 100);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		
		FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(16);
		while (!glfwWindowShouldClose(window)) {

			angle+=0.1;
			
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			glEnable(GL_LIGHTING);
			glShadeModel(GL_SMOOTH);

			//float senoang = (float) Math.sin(Math.toRadians(angle));
			//float cosang = (float) Math.cos(Math.toRadians(angle));
			glLoadIdentity();

			float[] lightAmbient = { 0.1f, 0.1f, 0.1f, 0.5f };
			float[] lightDiffuse = { 0.5f, 0.5f, 0.5f, 0.5f };
			float[] lightPosition = { 0.0f, 0.0f, 0.0f, 1.0f };
			//float[] lightPosition = { 0.0f, senoang * 600f, cosang * 600f, 1.0f };

			glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
			glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
			glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);

			float[] lightAmbient2 = { 0.0f, 0.0f, 0.0f, 1.0f };
			float[] lightDiffuse2 = { 1.0f, 1.0f, 1.0f, 1.0f };
			float[] lightPosition2 = { -5f, -5f, 0f, 1.0f };

			shader.start();
			int modellocation = glGetUniformLocation(shader.programID, "model");
			
			glEnable(GL_DEPTH_TEST);
			
	        glActiveTexture(GL_TEXTURE0);
	        glBindTexture(GL_TEXTURE_2D, textura);
	        
	        int loctexture = glGetUniformLocation(shader.programID, "tex");
	        glUniform1i(loctexture, 0);
			
			
			Matrix4f view = new Matrix4f();
			view.setIdentity();
			
			view.scale(new Vector3f(scale,scale,scale));
			view.rotate(viewAngX*0.0174532f, new Vector3f(1,0,0));
			view.rotate(viewAngY*0.0174532f, new Vector3f(0,1,0));
			view.translate(new Vector3f(0,0,0));
			int viewlocation = glGetUniformLocation(shader.programID, "view");
			int corgeralloc = glGetUniformLocation(shader.programID, "corgeral");
			int angpos = glGetUniformLocation(shader.programID, "ang");
			
			glUniform4f(corgeralloc, .5f, .5f, .5f, 1.0f);
			glUniform1f(angpos, angle);
			
			Matrix4f modelm = new Matrix4f();
			modelm.setIdentity();
			
			//System.out.println(""+x+" "+y+" "+z);
			modelm.translate(new Vector3f(0,0,0));
			modelm.scale(new Vector3f(0.01f,0.01f,0.01f));
			//modelm.rotate(ang,new Vector3f(0.0f,1.0f,0.0f));
			
			
			modelm.storeTranspose(matrixBuffer);
			matrixBuffer.flip();
			glUniformMatrix4fv(modellocation, false, matrixBuffer);
			
			view.storeTranspose(matrixBuffer);
			matrixBuffer.flip();
			glUniformMatrix4fv(viewlocation, false, matrixBuffer);
			
			
			//System.out.println("corgeralloc "+corgeralloc);
			glUniform4f(corgeralloc, .5f, 0.8f, 0.5f, 1.0f);
			
			
			jatinho.draw();
			
			
			shader.stop();
			
			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
			frame++;
			long actualTime = System.currentTimeMillis();
			if ((lasttime / 1000) != (actualTime / 1000)) {
				System.out.println("FPS " + frame);
				frame = 0;
				lasttime = actualTime;
			}

		}
	}


	public static void main(String[] args) throws IOException {
		new Main3D().run(args);
	}

	public static void gluPerspective(float fovy, float aspect, float near, float far) {
		float bottom = -near * (float) Math.tan(fovy / 2);
		float top = -bottom;
		float left = aspect * bottom;
		float right = -left;
		glFrustum(left, right, bottom, top, near, far);
	}

}
