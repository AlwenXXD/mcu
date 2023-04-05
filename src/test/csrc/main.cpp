#include <cstdio>
#include <cstdlib>
#include <cstdint>
#include <getopt.h>
#include <SDL.h>
#include <SDL_image.h>
#include "VSimTop.h"
#include "verilated.h"
#include "verilated_vcd_c.h"

#define ANSI_COLOR_RED "\x1b[31m"
#define ANSI_COLOR_GREEN "\x1b[32m"
#define ANSI_COLOR_YELLOW "\x1b[33m"
#define ANSI_COLOR_BLUE "\x1b[34m"
#define ANSI_COLOR_MAGENTA "\x1b[35m"
#define ANSI_COLOR_CYAN "\x1b[36m"
#define ANSI_COLOR_RESET "\x1b[0m"


#define RAM_SIZE (1024 * 1024 * 16)
static uint64_t ram[RAM_SIZE];
//h8800_0000
#define FB_BASE (RAM_SIZE/2)

extern "C" uint64_t ram_read_helper(uint8_t en, uint64_t rIdx)
{
	rIdx %= RAM_SIZE;
	uint64_t rdata = (en) ? ram[rIdx] : 0;
	return rdata;
}

extern "C" void ram_write_helper(uint64_t wIdx, uint64_t wdata, uint64_t wmask, uint8_t wen)
{
	if (wen)
	{
		if (wIdx >= RAM_SIZE)
		{
			printf("ERROR: ram wIdx = 0x%lx out of bound!\n", wIdx);
			return;
		}
		ram[wIdx] = (ram[wIdx] & ~wmask) | (wdata & wmask);
	}
}

static char *img_file = NULL;
static bool trace_on = false;
static int parse_args(int argc, char *argv[])
{
	const struct option table[] = {
		{"help", no_argument, NULL, 'h'},
		{"trace", no_argument, NULL, 't'},
		{0, 0, NULL, 0},
	};
	int o;
	while ((o = getopt_long(argc, argv, "-ht", table, NULL)) != -1)
	{
		switch (o)
		{
		case 't':
			trace_on = true;
			break;
		case 1:
			img_file = optarg;
			return 0;
		default:
			printf("Usage: %s  IMAGE_PATH\n\n", argv[0]);
			printf("\n");
			exit(0);
		}
	}
	return 0;
}

int main(int argc, char **argv, char **env)
{
	VerilatedContext *contextp = new VerilatedContext;
	contextp->commandArgs(argc, argv);
	VSimTop *dut = new VSimTop;
	VerilatedVcdC *tfp = NULL;
	int state = 0;

	parse_args(argc, argv);

	if (trace_on)
	{
		Verilated::traceEverOn(true);
		tfp = new VerilatedVcdC;
		dut->trace(tfp, 99);
		tfp->open("wave.vcd");
	}

	FILE *fp = fopen(img_file, "rb");
	if (fp == NULL)
	{
		printf("Can not open '%s'\n", img_file);
		assert(0);
	}
	fseek(fp, 0, SEEK_END);
	auto img_size = ftell(fp);
	if (img_size > RAM_SIZE)
	{
		img_size = RAM_SIZE;
	}
	fseek(fp, 0, SEEK_SET);
	auto ret = fread(ram, img_size, 1, fp);
	assert(ret == 1);
	fclose(fp);

	bool quit = false;
    SDL_Event event;
 
    SDL_Init(SDL_INIT_VIDEO);
    IMG_Init(IMG_INIT_WEBP);
 
    SDL_Window * window = SDL_CreateWindow("image",
        SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, 640, 480, 0);
    SDL_Renderer * renderer = SDL_CreateRenderer(window, -1, 0);
    SDL_Surface * image = SDL_LoadBMP("apple1.bmp");
    SDL_Texture * texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STATIC, image->w, image->h);
    uint32_t *pixels = (uint32_t *) image->pixels;
	uint32_t *frame_buffer = (uint32_t *) &ram[FB_BASE];
	uint32_t size = image->w * image->h;
	for (size_t i = 0; i < size; i++)
	{
		frame_buffer[i] = pixels[i];
	}
	image->pixels = frame_buffer;

	// start simulation
	for (int i = 0; i < 100; i++)
	{
		dut->reset = 1;
		dut->clock = 0;
		dut->eval();
		dut->clock = 1;
		dut->eval();
	}
	dut->reset = 0;
	uint64_t cycles = 0;
	int update_fb = 0;
	while (!contextp->gotFinish())
	{
		update_fb++;
		dut->clock = 0;
		contextp->timeInc(1);
		dut->eval();
		if (trace_on)
			tfp->dump(contextp->time());
		dut->clock = 1;
		contextp->timeInc(1);
		dut->eval();
		if (trace_on)
			tfp->dump(contextp->time());
		cycles += 1;
		if (dut->io_is_halt)
		{
			if (dut->io_val_a0 == 0)
			{
				printf(ANSI_COLOR_GREEN "Test Pass!\n" ANSI_COLOR_RESET);
			}
			else
			{
				printf(ANSI_COLOR_RED "Test Fali!\n" ANSI_COLOR_RESET);
				state = 1;
			}
			break;
		}
		if (dut->io_is_error)
		{
			printf(ANSI_COLOR_BLUE "Illegal Instruction!\n" ANSI_COLOR_RESET);
			state = 1;
			break;
		}
		if (update_fb >= 500)
		{
			SDL_UpdateTexture(texture, NULL, image->pixels, image->w * sizeof(Uint32));
			SDL_RenderCopy(renderer, texture, NULL, NULL);
			SDL_RenderPresent(renderer);
			update_fb = 0;
		}
	}

	if (trace_on)
		tfp->close();
	delete dut;
	delete contextp;

	SDL_DestroyTexture(texture);
    SDL_FreeSurface(image);
    SDL_DestroyRenderer(renderer);
    SDL_DestroyWindow(window);

    IMG_Quit();
    SDL_Quit();
	return state;
}
