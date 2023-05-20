static int *p_mat = (unsigned int *)(0x01000000);
static int *p_kernel = (unsigned int *)(0x01001000);
static unsigned int *end = (unsigned int *)(0x01002000);
static unsigned int *start = (unsigned int *)(0x01003000);
static int *result = (unsigned int *)(0x01004000);
static int ready = 0;
__attribute__ ((interrupt ("machine"))) void _trap_entry (void) {
    *end = 0;
    ready = 1;
}
const int mx[9] = { -1,0,1,-2,0,2,-1,0,1 };
const int my[9] = { -1,-2,-1,0,0,0,1,2,1 };
int convolution_cpu(int *image, int w, int h, const int *kernel, int row, int col)
{
    int i, j, sum = 0;
	for (i = 0; i < 3; i++) {
		for (j = 0; j < 3; j++) {
            sum += image[(i + row) * w + (j + col)] * kernel[i * 3 + j];
        }
	}
	return sum;
}
int convolution(int *image, int w, int h, const int *kernel, int row, int col)
{
    int i, j, sum = 0;
	for (i = 0; i < 3; i++) {
		for (j = 0; j < 3; j++) {
            p_mat[i*3+j] = image[(i + row) * w + (j + col)];
            p_kernel[i*3+j] = kernel[i * 3 + j];
        }
	}
    *start = 1;
    while (!ready);
    ready = 0;
    sum = *result;
	return sum;
}
int abs(int x){
    if (x<0)
    {
        return -x;
    }else{
        return x;
    }
    
}

void sobel_edge_detector(unsigned int* image, int w , int h , unsigned int* out_image) {
	int i, j, gx, gy;
	
	for (i = 1; i < h - 2; i++) {
		for (j = 1; j < w - 2; j++) {
			gx = convolution_cpu(image,w,h, mx, i, j);
			gy = convolution_cpu(image,w,h, my, i, j);
			out_image[i*w+j] = abs(gx) + abs(gy);
		}
	}
	
}
void gray(int w, int h, int* matrix) {
	int i, j;

	for(i = 0; i < h; i++) {
		for(j = 0; j < w; j++) {
            unsigned int col = matrix[i * w + j];
            unsigned int alpha = col & 0xFF000000;
            unsigned int red = (col & 0x00FF0000) >> 16;
            unsigned int green = (col & 0x0000FF00) >> 8;
            unsigned int blue = (col & 0x000000FF);
            unsigned int gray = (red + green + blue) / 3;
            unsigned int newColor = gray;
            matrix[i * w + j] = newColor;
        }
	} 
}
int main(){
    int w=512, h=399;
    unsigned int *frame_buffer = (unsigned int*)(0x84000000);
    // gray(w,h,frame_buffer);
    sobel_edge_detector(frame_buffer, w, h,frame_buffer);

    return 0;
}