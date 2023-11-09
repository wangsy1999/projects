#include "mbed.h"
DigitalOut led(LED3);
bool toggle_record_mode(bool record_mode,DigitalIn button) {
 
    if (button == 1) {
      
        record_mode = 1;


    }
       
    else{
        record_mode = 0;
        }
    led=record_mode;
    
return record_mode;
}
