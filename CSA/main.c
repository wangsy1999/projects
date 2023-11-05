#include <stdio.h>
#include"header1.h"
int main()
{
  setupGPIO();
while(1)
{
A:if(checkBot(TST)&&!checkBot(FLA)) 
{
    setLED(GREEN,ON);
      setLED(YELLOW,ON);
      setLED(RED,ON);  
      delay(DELAY);
      setLED(YELLOW,OFF);
      setLED(RED,OFF);  
      delay(DELAY);
        continue;
}    
B:if(!checkBot(TST))
   { 
    
    if(checkBot(TST)&&!checkBot(FLA)) goto A;
    if(checkBot(FLA))
     {
   
        
      if(!checkBot(FLA)&&!checkBot(OLA)) goto B;
      if(checkBot(OLA)) goto E;       
      if(checkBot(ACK)&&!checkBot(OLA))
      {    
         while(1)
         {
         D:if(!checkBot(FLA)) goto C;
         setLED(YELLOW,ON);
         setLED(RED,OFF);
        setLED(GREEN,OFF);
         if(checkBot(OLA)) goto E;

         


         }
      }
      if(checkBot(OLA))
      {

      if(checkBot(ACK))
      {
         while(1)
         {
        F:setLED(RED,ON);
        setLED(GREEN,OFF);
        setLED(YELLOW,OFF);
        if(!checkBot(OLA)) goto D;
        }
         
      }
      if(!checkBot(OLA))

      {
        goto D;
      }
      E: if(checkBot(ACK)) goto F;
      setLED(RED,ON);
        setLED(GREEN,OFF);
        setLED(YELLOW,OFF);
     delay(DELAY);
      setLED(RED,OFF);
      delay(DELAY);
     
        continue;
      
      }
      else if (!checkBot(FLA)&&!checkBot(OLA)) goto C;
      C:setLED(RED,OFF);
        setLED(GREEN,OFF);
        setLED(YELLOW,ON);
     delay(DELAY);
      setLED(YELLOW,OFF);
      delay(DELAY);
        continue;
     }
     setLED(GREEN,ON);
    setLED(YELLOW,OFF);
      setLED(RED,OFF);
        continue;
   }
}
return 0;
}