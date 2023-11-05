#define DELAY 200
#define ON 0x01
#define OFF 0x00
#define NUM_LEDS 0x03
#define RED 0x00800
#define YELLOW 0x01000
#define GREEN 0x02000
#define TST 0x40000
#define FLA 0x800000
#define OLA 0x00001
#define ACK 0x00002

void setupGPIO();
int setLED(int color, int state);
void delay(int milliseconds);
int checkBot(int PIN);