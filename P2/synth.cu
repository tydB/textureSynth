#include <windows.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <vector>
//#include <GL/glut.h>
#include "glut.h" // I have copied glut.h into the current directory
#include <cuda.h>

#define WINSIZE 512 // Size of the window
#define YES 1
#define NO 0

/* Define a colour in terms of the r, g, b components*/
typedef struct {
	unsigned char red;
	unsigned char green;
	unsigned char blue; 
} rgb;

/* Create some space to render the image into */
rgb image[WINSIZE*WINSIZE];
rgb mask[WINSIZE*WINSIZE];
rgb* sample;
int sampleWidth;
int sampleHeight;

// Kernel that executes on the CUDA device  
__global__ void render(rgb* image, int width, int height) {
	int x = threadIdx.x;
	int y = blockIdx.x;
	image[y*width+x].red = x % width;
	image[y*width+x].green = y % width;
	image[y*width+x].blue = 0;
}
void loadBMP() {
	printf("\nLoading Image\n");
	unsigned char header[54];
	unsigned int dataStart;
	unsigned int width;
	unsigned int height;
	int imageSize;
	// pixel data
	// unsigned char *data;
	FILE * file = fopen("smallRocks.bmp","rb");
	if (!file) {
		printf("Texture could not be found\n");
		return;
	}
	if (fread(header, 1, 54, file) != 54) { // If not 54 bytes read : problem
		printf("Error with the texture file\n");
		return;
	}
	// get header data
	dataStart = *(int*)&(header[10]);
	imageSize = *(int*)&(header[34]);
	width = *(int*)&(header[18]);
	height = *(int*)&(header[22]);
	int padding = (width*3)%4;

	// get memory for the sampleImage
	sampleWidth = width;
	sampleHeight = height;
	sample = (rgb*)malloc(sizeof(rgb) * sampleWidth * sampleHeight);

	printf("dataStart %d\n", dataStart);
	printf("imageSize %d\n", imageSize);
	printf("width     %d\n", width);
	printf("height    %d\n", height);
	printf("padding   %d\n", padding);

	//get to pixel data

	// create array for pixel data
	unsigned char* data = (unsigned char*)malloc(width * height * 3 * sizeof(unsigned char));
	// get to the pixel data
	fread(data, 1, dataStart - 54, file); // do this better---------------------------------------------------------------------
	// Read the pixel data
	fread(data,1,width * height * 3,file);
	fclose(file);

	// // move the data from the buffer to the pattern
	for (int i = 0; i < width; ++i) {
		for (int j = 0; j < height; ++j) {
			sample[i * width + j].red = (unsigned char)data[(i * width + j) * 3   + 2];
			sample[i * width + j].green = (unsigned char)data[(i * width + j) * 3 + 1];
			sample[i * width + j].blue = (unsigned char)data[(i * width + j) * 3  + 0];
		}
	}
	// // free the buffer
	free(data);
}
void setPixel(int x, int y, rgb color) {
	image[y * WINSIZE + x].red = color.red;
	image[y * WINSIZE + x].green = color.green;
	image[y * WINSIZE + x].blue = color.blue;
	mask[y * WINSIZE + x].red = 255;
	mask[y * WINSIZE + x].green = 255;
	mask[y * WINSIZE + x].blue = 255;
}
rgb getSampleColor(int x, int y) {
	return sample[y * sampleHeight + x];
}
void seed(int seedSize) {
	int x = 60;
	int y = 60;
	for (int sampleI = 0; sampleI < seedSize; sampleI++) {
		for (int sampleJ = 0; sampleJ < seedSize; sampleJ++) {
			setPixel(WINSIZE / 2 + sampleI - 1,
				WINSIZE / 2 + sampleJ - 1,
				getSampleColor(x + sampleI,y + sampleJ));
		}
	}
}
vector<rgb> getNextPixels() {
	return NULL;
}
void synthTexture(int nSize) {

}
//***** OpenGL code
// also contains some CUDA code 
void showimage(void) {
	// This code allocates memory on the graphics card.
	// The amount it allocates is WINSIZE*WINSIZE*sizeof(rgb), which is enough space for every pixel to display
	// rgb * image_Device;
	// cudaMalloc((void **) &image_Device, WINSIZE*WINSIZE*sizeof(rgb));
	// // The number of threads to pass to CUDA will be one for each pixel.
	// int block_size = WINSIZE;  
	// int n_blocks = WINSIZE;
	// // This calls CUDA to run the render function on the graphics card.
	// // CUDA should start n_blocks times block_size threads and pass in the appropriate ID to each thread
	// // Note that the order the threads execute is not known.  A pixel later in the image may be rendered before
	// // a pixel earlier in the image
	// render <<< n_blocks, block_size >>> (image_Device, WINSIZE, WINSIZE);
	// // This copies the generated image from the graphics card back to the CPU RAM where it can be drawn
	// // (somewhat ironic since it is going to be put into OpenGL which sends it back to the graphics card)
	// cudaMemcpy(image, image_Device, WINSIZE*WINSIZE*sizeof(rgb), cudaMemcpyDeviceToHost);
 // 	// Free the memory that has been allocated
	// cudaFree(image_Device);
	// This is OpenGL code that makes a display list that draws the rendered image.
	glNewList(1,GL_COMPILE_AND_EXECUTE);
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	glDrawPixels(WINSIZE,WINSIZE, GL_RGB, GL_UNSIGNED_BYTE, image);
	glDrawPixels(sampleWidth,sampleHeight, GL_RGB, GL_UNSIGNED_BYTE, sample);
	glEndList();
}
// Display call back function clears the screen and draws the rendered image (which at this point is just a picture)
void display(void) {
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);  
	glCallList(1);
	glutSwapBuffers();
}
// This makes the program exit when the user presses 'q'
void keyboard(unsigned char key, int x, int y) {
	switch (key) {
		case 'q': /* user selected quit */
			free(sample);
			exit(0);
			break;
		}
}
// This function animates the four spheres.  It basically
// updates the positions assumming a constant motion and checks for
// intersections with the edges of the world.
// All of this code runs on the CPU (rather than the GPU)
void idle(void) {
	showimage(); // render the image on the graphics card
	glutPostRedisplay(); // request OpenGL show the newly rendered image
}
// main routine that executes on the host  
int main(void) {
	// First print out a little information about the device
	int deviceCount;
	cudaGetDeviceCount(&deviceCount);
	printf("There are %d devices\n", deviceCount);
	cudaDeviceProp deviceProp;
	cudaGetDeviceProperties(&deviceProp, 0); // get the properties from the first devie
	printf("Name: %s\n", deviceProp.name);
	printf("Total Global Memory: %d\n", deviceProp.totalGlobalMem);
	printf("Shared memory per block: %d\n", deviceProp.sharedMemPerBlock);
	printf("Constant Memory: %d\n", deviceProp.totalConstMem);
	printf("CUDA %d.%d\n", deviceProp.major, deviceProp.minor);
	printf("Clock Rate: %d kHz\n", deviceProp.clockRate);
	printf("Warp size: %d\n", deviceProp.warpSize);

	// This code folowing here is OpenGL setup code
	glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGB | GLUT_DEPTH);
	glutCreateWindow("Texture Synthesis");
	glutReshapeWindow(WINSIZE,WINSIZE);

	// get sample image
	loadBMP();
	// seed
	seed(20);
	// synthesis
	synthTexture(5);
	//These callback functions tell OpenGL what to call when there is something to do
	glutDisplayFunc(display); // set the display call back function
	glutKeyboardFunc(keyboard); // set the keyboard call back function
	// glutIdleFunc(idle);  // set the idle call back function
	srand( (unsigned)time( NULL ) );
	showimage(); // Draw the first time
	glutMainLoop(); // pass control over to OpenGL which will call the appropriate call back function when needed
}