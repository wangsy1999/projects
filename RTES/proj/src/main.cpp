#include "mbed.h"
#include "drivers/LCD_DISCO_F429ZI.h"
#include <arm_math.h>
#define FOREGROUND 0
#define WINDOW_SIZE 7 //set window size of the filter
#define AS 50 //set array size. As the sample time is 80 ms, the longest pattern is 4 second


//Instruction: at first time of recording, this will be the sequence you need to do when unlocking. Push and hold the button when recording. The second, third...of the recording are for verifying the sequence.
//If your movement is correct, the screen will be green. If not, it will be red. If you want to change the sequence, please use reset button.  

bool toggle_record_mode(bool record_mode, DigitalIn button);//define the function when we press the button
float* gyro();//define gyro function to get data from gyro, which is in gyro.cpp
float* REC; // define the pointer to save the address of return value of gyro()


LCD_DISCO_F429ZI lcd;
DigitalIn record_button(USER_BUTTON);   //detect if the button is pressed
DigitalOut led2(LED4);  // set the LED we use when recording in progress


//defined veriable, will use later
float32_t X[AS];
float32_t Y[AS];
float32_t Z[AS];
float32_t Xa[AS];
float32_t Ya[AS];
float32_t Za[AS];
float32_t Xf[AS];
float32_t Yf[AS];
float32_t Zf[AS];
float32_t Xaf[AS];
float32_t Yaf[AS];
float32_t Zaf[AS];
float32_t squaredSumX;
float32_t squaredSumY;
float32_t squaredSumZ;
float32_t squaredSum;
float32_t vectorLength;
float32_t x;
float32_t y;
float32_t z;
float32_t sum;  
float32_t outputSignal[AS];
bool R = 0;


//set a screen before we record the pattern
void black(){

    lcd.SelectLayer(FOREGROUND);
    lcd.Clear(LCD_COLOR_BLACK);
    //lcd.SetFont(&Font24);
    lcd.DisplayStringAt(0, LINE(9), (uint8_t *)"Press Blue button", CENTER_MODE);
    lcd.DisplayStringAt(0, LINE(10), (uint8_t *)"to record pattern", CENTER_MODE);

}
//set a screen before we unlock the pattern
void black_3(){

    lcd.SelectLayer(FOREGROUND);
    lcd.Clear(LCD_COLOR_BLACK);
    lcd.SetFont(&Font16);
    lcd.DisplayStringAt(0, LINE(9), (uint8_t *)"Press Blue button", CENTER_MODE);
    lcd.DisplayStringAt(0, LINE(10), (uint8_t *)"to unlock pattern", CENTER_MODE);

}

//set a screen when the pattern is recording
void black_1(){

    lcd.SelectLayer(FOREGROUND);
    lcd.Clear(LCD_COLOR_BLACK);
    lcd.SetFont(&Font24);
    lcd.DisplayStringAt(0, LINE(6), (uint8_t *)"Recording", CENTER_MODE);

}

//set a screen when the pattern is recorded
void black_2(){

    lcd.SelectLayer(FOREGROUND);
    lcd.Clear(LCD_COLOR_BLACK);
    lcd.SetFont(&Font24);
    lcd.DisplayStringAt(0, LINE(6), (uint8_t *)"Recorded", CENTER_MODE);

}

//right: green screen, unlock successful
void right(){
   
  lcd.SelectLayer(FOREGROUND);
  lcd.Clear(LCD_COLOR_GREEN);
  lcd.SetFont(&Font20);
  lcd.DisplayStringAt(0, LINE(6), (uint8_t *)"Unlock Successful", CENTER_MODE);
}
//wrong: red screen, unlock failed
void wrong(){
   
  lcd.SelectLayer(FOREGROUND);
  lcd.Clear(LCD_COLOR_RED);
  lcd.SetFont(&Font24);
  lcd.DisplayStringAt(0, LINE(6), (uint8_t *)"Unlock Failed", CENTER_MODE);
}

//get the number of none empty elements to know the length of data
uint32_t count_non_empty_elements(float* vector1, uint32_t length) {
    
    uint32_t count = 0;
    
    for (uint32_t i = 0; i < length; i++) {
        if (vector1[i] != 0.0) {
            count++;
        
        } 
    }
return count;
}

//get length of vector
void vectorlength(float32_t* m1,uint32_t G, float32_t* O){

    float32_t squaredSumX;
    arm_power_f32(m1,AS, &squaredSumX);           
    arm_sqrt_f32(squaredSumX, O);
}


//filter the data use moving Average Filter
void movingAverageFilter(float32_t *input, float32_t *output, int size) {
  
    for (int i=0;i<size;i++) {
         int G = WINDOW_SIZE;
         sum=0.0; 
        
        for (int j=0;j<WINDOW_SIZE-1;j++) {
            if(i+j<size){
                sum+=input[i+j];
            }
            else{
                sum+=0;
                G-=1;
            }
        }
        output[i]=sum/G;
        
    }
}

//calculate the cos() of two vectors
float32_t COS(float32_t* x1, float32_t* x2, uint32_t length){
    float32_t X;
            arm_dot_prod_f32(x1,x2,length,&X);
            float32_t vectorLengthx1;
            vectorlength(x1,AS,&vectorLengthx1);
            float32_t vectorLengthx2;
            vectorlength(x2,AS,&vectorLengthx2);
            X=X/(vectorLengthx1*vectorLengthx2);
            return X;

}
int main()
{

    int a=0; 
    int i=0;
    int c=0;

    black();
//initialize the start screen
    while(1)
    {

        if(a==0){
            
            memset(X, 0, sizeof(Xa));//empty the array
            memset(Y, 0, sizeof(Ya));
            memset(Z, 0, sizeof(Za));

            i=0;
            R=toggle_record_mode(R,record_button); //check if the button is pressed

            while(R)     
            {
                
                black_1();
                REC=gyro();
                X[i]=REC[0];
                Y[i]=REC[1];
                Z[i]=REC[2];
                printf("R%f\n",X[i]);

                i+=1;
                thread_sleep_for(80);
                a=1;//set a flag of record pattern
                R=toggle_record_mode(R,record_button);  //check if the button is released
            }
            
            if(a==1){
                //set screen
                black_2();
                thread_sleep_for(1500);
                //process the data
                movingAverageFilter(X,Xf,AS);
                movingAverageFilter(Y,Yf,AS);
                movingAverageFilter(Z,Zf,AS);
                //set screen
                black_3();        
            }
        }
        else{
            //empty the array
            memset(Xa, 0, sizeof(Xa));
            memset(Ya, 0, sizeof(Ya));
            memset(Za, 0, sizeof(Za));
            i=0;
            R=toggle_record_mode(R,record_button);//check if the button is pressed
             
            while(R)     
            {
                black_1(); 
                //set a flag of unlock pattern
                c=1;
                REC=gyro();
                Xa[i]=REC[0];
                Ya[i]=REC[1];
                Za[i]=REC[2];
                printf("UN %f\n",REC[1]);

                i+=1;
                thread_sleep_for(80);
                //detect the button
                R=toggle_record_mode(R,record_button); //check if the button is released
            }
            if(c==1){
                //process data
                movingAverageFilter(Xa,Xaf,AS);
                movingAverageFilter(Ya,Yaf,AS);
                movingAverageFilter(Za,Zaf,AS); 
            }
            }

        if(a==1&&c==1){
            //calculate the distiance of 2 vectors: D=||x,y,z||=(x^2+y^2+z^2)^1/2
            float Dx[AS];
            for(i=0;i<AS;i++){
                Dx[i]=Xf[i]-Xaf[i];
            }
            float Dy[AS];
            for(i=0;i<AS;i++){
                Dy[i]=Yf[i]-Yaf[i];
            }
            float Dz[AS];
            for(i=0;i<AS;i++){
                Dz[i]=Zf[i]-Zaf[i];
            }

            int m1=count_non_empty_elements(X,AS);   
            int m2=count_non_empty_elements(Xa,AS);   
            int m=m2-m1;  


            arm_power_f32(Dx,AS, &squaredSumX);         
            arm_power_f32(Dy,AS, &squaredSumY);
            arm_power_f32(Dz,AS, &squaredSumZ);

            squaredSum=squaredSumX+squaredSumY+squaredSumZ;          
            arm_sqrt_f32(squaredSum, &vectorLength);
            printf("%f\n %d\n",vectorLength,m);

           //calculate cos() between two arrays in the same dimention
            x=COS(Xf,Xaf,AS);
            y=COS(Yf,Yaf,AS);
            z=COS(Zf,Zaf,AS);


            printf("x %f\n y %f\n z %f\n",x,y,z);

            //the condition to say if two sequence are the same
            if((vectorLength<=2.5&&m<=6&&m>=-6&&x>=0.5&&y>=0.5&&z>=0.5)||(x>=0.75&&y>=0.75&&z>=0.75&&m<=6&&m>=-6)){
                    right();
                    thread_sleep_for(1500);
                    black_3();

            }

                else{
                    //show the wrong screen if is not unlock
                    wrong();
                    thread_sleep_for(1500);
                    black_3();

            }
            //clear the unlock flag so that next unlock recording can start
            c=0;

        }
    }
}