#define RELAY_NUMBER 3
#include "./Relay.h"


int counter;
unsigned long check;

Relay relays [] = {
  Relay(2),
  Relay(3),
  Relay(4)
};

void setup() {
  Serial.begin(9600);
  
  for (byte i = 0; i < RELAY_NUMBER; i ++)
    relays[i].setup();
}

void loop() {
 
  for (byte i = 0; i < RELAY_NUMBER; i ++){
    relays[i].countTime();
  } 
     
    relays[0].inflate1();
    relays[2].inflate2();
    relays[1].inflate3();
    //counter ++;
    /*Serial.println(counter);
    if(counter==150000){
      relays[2].on();
      delay(10000);
      relays[2].off();
    }*/

    
  
}    
