const static int p_mat[]={1,1,1,1,1,1,1,1,1};
const static int p_kernel[]={1,1,1,1,1,1,1,1,1};
int main(){
    int *mat = (unsigned int*)(0x01000000);
    int *kernel = (unsigned int*)(0x01001000);
    unsigned int *end = (unsigned int*)(0x01002000);
    unsigned int *start = (unsigned int*)(0x01003000);
    int *result = (unsigned int*)(0x01004000);
   for (int i = 0; i < 9; i++)
   {
    mat[i] = p_mat[i];
    kernel[i] = p_kernel[i];
   }
    *start = 1;
    while (!(*end));
    int output = *result;

    return output;
}
