/* 
The following code will:
1- make esp32 run as a Bluetooth server
2- recieve the readings from the connected accelerometer and classify them based on certian thresholds 
3- based on the classifed event, change the LED output and send short messages to the connected Bluetooth device
   ,that is unless one of the classifed events is "shaking", in which case it will tuggle the Bluetooth and LED strip on and off

*/


// include necessary libraries
#include <FastLED.h>
#include <Arduino.h>
#include <analogWrite.h>
#include <Wire.h>
#include "BluetoothSerial.h"
#include "esp_deep_sleep.h"
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BNO055.h>
#include <utility/imumaths.h>


#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

// init accelerometer
RTC_DATA_ATTR Adafruit_BNO055 bno = Adafruit_BNO055(55);

#define LED_PIN     15  // for huzzah
int voltpin = 35;  // for huzzah

//int voltpin = 32; // for testing
//#define LED_PIN  13 // for testing

// init number of leds 
#define NUM_LEDS    10
CRGB leds[NUM_LEDS];

// hepler function for fade_leds function that will change the LEDs based on the given range  
void leds_manage(int i, int start_led, int end_led, bool pos) {
  if (pos == 0) {
    if (end_led != 0) {
      for (int l = start_led; l < end_led; l++) {
        leds[l] = CRGB ( i, i, 0);
      }
    }

    else {
      for (int l = 0; l <= 9; l++) {
        leds[l] = CRGB ( 0, 0, i);
      }

    }
  }
  else if (pos == 1) {
    if (end_led != 9) {
      for (int l = start_led; l > end_led; l--) {
        leds[l] = CRGB ( i, i, 0);
      }
    }
    else {

      for (int l = 0; l <= 9; l++) {
        leds[l] = CRGB ( 0, 0, i);
      }

    }
  }
}


//  function to turn off the LEDs
void leds_off() {
  for (int i = 0; i <= 9; i++) {
    leds[i] = CRGB ( 0, 0, 0);
  }
  FastLED.show();
}




// variables for LEDs fading fuction
int brightness = 0;    // how bright the LED is
int fadeAmount = 1;    // how many points to fade the LED by
unsigned long previousMillis = 0;
unsigned long interval = 60;


// helper function for set_led_tasks function for fading certian parts of the LED strip based on the given positions
void fade_leds(int start_led, int end_led, bool pos)
{

  if (pos == 0) {

    if (end_led == 0) {

    }
    else {
      for (int i = 0; i < start_led; i++) {
        leds[i] = CRGB ( 30, 30, 0);
      }

      for (int i = end_led; i <= 9; i++) {
        leds[i] = CRGB ( 0, 0, 0);
      }
    }
  }
  else if (pos == 1) {

    if (end_led == 9) {
    }
    else {


      for (int i = 9; i > start_led; i--) {
        leds[i] = CRGB ( 30, 30, 0);

      }

      for (int i = (end_led); i >= 0; i--) {
        leds[i] = CRGB ( 0, 0, 0);
      }
    }
  }

  unsigned long currentMillis = millis(); // grab current time
  leds_manage(brightness, start_led, end_led, pos);
  FastLED.show();   // set the brightness of ledPin:

  if (currentMillis - previousMillis >= interval) {
    brightness = brightness + fadeAmount;     // change the brightness for next time through the loop:
    previousMillis = millis();
  }

  if (brightness <= 0 )
  { // reverse the direction of the fading at the ends of the fade:
    brightness = 0;
    fadeAmount = -fadeAmount;
  }
  if (brightness >= 20 )
  { // reverse the direction of the fading at the ends of the fade:
    brightness = 20;
    fadeAmount = -fadeAmount;
  }

}


// function that will map the given all tasks/finished tasks into fading certian parts of the LED strip
void set_led_tasks(int  finished, int all_tasks, bool vertical_orient) {

  if (vertical_orient == 0) {
    fade_leds(finished, all_tasks, vertical_orient);
  }
  else if (vertical_orient == 1) {
    fade_leds(9 - finished, 9 - all_tasks, vertical_orient);
  }

}

// function that will map the given work/break time into fading certian parts of the LED strip
void set_led_ratio(unsigned long break_time, unsigned long work_time) {
  if (work_time == 0 and break_time == 0) {


    for (int i = 0; i < 5; i++) {
      leds[i] = CRGB ( 0, 0, 40);
    }

    for (int i = 5; i <= 9; i++) {
      leds[i] = CRGB ( 40, 10, 0);
    }

    FastLED.show();

  } else {

    over_all_time = break_time + work_time;

    Serial.print("overall:");
    Serial.println(over_all_time);

    work_leds = (float(work_time ) / float(over_all_time));

    Serial.print("work leds before norm:");
    Serial.println(work_leds);

    work_leds = work_leds * 10;


    Serial.print("work leds:");
    Serial.println(work_leds);
    //break_leds = (break_time / over_all_time) * 10;


    if (work_leds == 10) {
      work_leds = 9;
    }

    if (work_leds == 0) {
      work_leds = 1;
    }

    for (int i = 0; i < int(work_leds); i++) {
      leds[i] = CRGB ( 0, 0, 40);
    }

    for (int i = int(work_leds); i <= 9; i++) {
      leds[i] = CRGB ( 40, 10, 0);
    }

    FastLED.show();

  }
}



BluetoothSerial SerialBT;

void setup() {
  Serial.begin(115200);

  // Start Bluetooth on first bootup
  SerialBT.begin("The_Rainmaker!");
  Serial.println("BT is ON !");

  delay(1000);

  // Assing the leds array to a pin
  FastLED.addLeds<WS2812, LED_PIN, GRB>(leds, NUM_LEDS);

  
  /* Initialise the sensor */
  if (!bno.begin())
  {
    /* There was a problem detecting the BNO055 ... check your connections */
    Serial.print("Ooops, no BNO055 detected ... Check your wiring or I2C ADDR!");
    while (1);

  }

  delay(10);

  bno.setExtCrystalUse(true);
}



//####################
// Define variables that will be used in loop()
//####################


// to handlle messages over Bluetooth
String string; 
char command;
int deletedTask;

// for toggling the sleep mode
bool turend_on = true; 
bool toggle_shake = 0;



// to store the progress of the tasks
int all_tasks = 0;
int finished_tasks = 0;
float  work_leds;
unsigned long over_all_time;


// to store the time durations for work/break time ratio
unsigned long work_time = 0;
unsigned long break_time = 0;
unsigned long work_start = 0;
unsigned long break_start = 0;
unsigned long start_timer = 0;
unsigned long init_timer = 0;



// for classification of the accelerometer readings
int last = 0;
int current = 0;
bool state_changed = 0;
int current_shake = 0;
unsigned long action_time;
bool start_action = 1;
int val_start;
bool state = 0; // 0 for work, 1 for break
bool activated = false;
bool vertical_orient; // 0 for upright, 1 for upsidedown
bool flipped;
int last_val = 0;

//// for shaking classification
int shakes = 0;
bool shake_direct = 0;
bool shaking = 0;
int last_shake = 0;
unsigned long shake_time;


// variables for power managment
float volt = 0;
int level = 0;
int last_volt = 0;



void loop() {

  sensors_event_t event;
  bno.getEvent(&event);

  current_shake = map(event.orientation.y, -90, 90, 0, 180);


  if (current_shake > last_val) {
    if (shake_direct == 0 and abs(current_shake - last_shake) > 8 ) {
      Serial.println (shakes);
      shakes++;
      shake_direct = 1;
      last_shake = current_shake;
    }
  }

  if (current_shake < last_val) {
    if (shake_direct == 1 and abs(current_shake - last_shake ) > 8 ) {
      Serial.println (shakes);
      shakes++;
      shake_direct = 0;
      last_shake = current_shake;
    }
  }

  last_val = current_shake;

  if (shakes == 1) {
    shake_time = millis();
  }


  if  (millis() - shake_time < 500) {

    if (shakes == 4)  {
      delay(500); // very important !!!
      shakes = 0;
      if (toggle_shake == 0 ) {
        toggle_shake = 1;
      }
      else if (toggle_shake == 1 ) {
        toggle_shake = 0;
      }


      Serial.println ("shaking");
    }
  }
  else {
    shakes = 0;
  }


  if (toggle_shake) {


    if (turend_on) {
      turend_on = false;
      leds_off();
      SerialBT.end();
      Serial.println("bt is off!");
      delay(1000);
    
    }

  }
  else {
    if (turend_on == false) {
      turend_on = true;
      

      if (SerialBT.begin("The_Rainmaker!")) {
        Serial.println("bt is ON !!!");
      }
      delay(1000);

    }

    

    if (all_tasks == 0) {
      //fade_leds(0, 9,0);
    }
    else {
      activated = true;
    }





    if (SerialBT.available() > 0) {

      {
        string = "";
      }


      while (SerialBT.available() > 0)
      {
        command = ((byte)SerialBT.read());

        if (command == ':')
        {
          break;
        }

        else
        {
          string += command;
        }

        delay(1);
      }


      if (string == "TO")

      {
        Serial.println("ON is sent!!");
        activated = true;
      }


      if (string == "R")

      {
       

        finished_tasks = 0;

        break_time = 0;
        work_time = 0;
        work_start = 0;
        break_start = 0;


        all_tasks = 0;
        activated = false;

        set_led_tasks(finished_tasks, all_tasks, vertical_orient);

      }

      if (string == "N")
      {
        Serial.println("N");

        volt = analogRead(voltpin);
        Serial.println(volt);

        level = map(volt, 1700, 2383, 0, 100);
        SerialBT.write(level);

   
        SerialBT.write(finished_tasks);
        SerialBT.write('\n');

      }


      if (string == "TF")
      {
        Serial.println("off is sent!");

        for (int i = 0; i <= 9; i++) {
          leds[i] = CRGB ( 0, 0, 0);
        }
        FastLED.show();

        activated = false;
      }

      if ((string.toInt() > 0) && (string.toInt() <= 11))
      {

        if (all_tasks == 0 && finished_tasks == 0 && string.toInt() == 1)  {
          init_timer = millis();
        }

        all_tasks = string.toInt();

        Serial.println("alltasks");
        Serial.println(all_tasks);
        current = map(event.orientation.y, -90, 90, 0, 180);

        Serial.println( current);


        if (current > 155 and current < 180) {
          vertical_orient == 0;
          Serial.println("straigght !!");

          set_led_tasks(finished_tasks, all_tasks, vertical_orient);
        }

        if (current > 0 and current < 20) {
          vertical_orient == 1;

          Serial.println("upside down !!");


          set_led_tasks(finished_tasks, all_tasks, vertical_orient);
        }

        if (current > 85 and current < 105) {

          set_led_ratio(break_time, work_time);

        }

        activated = true;


        delay(10);

      }
      if (string.startsWith("D")) {
        string.remove(0, 1);

        deletedTask = string.toInt() ;

        Serial.println(deletedTask);
        if ((deletedTask + 1) > finished_tasks) {

          all_tasks--;
        }
        delay(10);

        if (all_tasks == 0) {
          finished_tasks = 0;



          break_time = 0;
          work_time = 0;
          work_start = 0;
          break_start = 0;


          all_tasks = 0;
          activated = false;
        }

        set_led_tasks(finished_tasks, all_tasks, vertical_orient);
      }


    }
    if (activated) {
      current = map(event.orientation.y, -90, 90, 0, 180);

      if (abs(current - last) > 5) {
        if (start_action == 1 ) {
          Serial.println("action start!");

          val_start = last;
          start_action = 0;

        }

        last = current;
        action_time = millis();
        Serial.println(current);

      }

      if (millis() - action_time > 700 and start_action == 0) {
        Serial.println("action finish!");
        start_action = 1;

        if (current > 155 and current < 180) {
          if (state == 1)
          { state_changed = 1;

          }

          Serial.println("work");
          if (state == 0) {
            if (vertical_orient == 1) {
              flipped = 1;
            } else {
              flipped = 0;
            }
          }
          state = 0;
          vertical_orient = 0;

        }

        if (current > 0 and current < 20) {

          if (state == 1)
          { state_changed = 1;

          }

          Serial.println("work");

          if (state == 0) {
            if (vertical_orient == 0) {
              flipped = 1;
            } else {
              flipped = 0;
            }
          }
          state = 0;
          vertical_orient = 1;
          
        }

        if (current > 85 and current < 105) {
          if (state == 0)
          { state_changed = 1;

          }

          Serial.println("break");
          state = 1;
        }

        if (state == 0) {

          Serial.print("finished tasks");
          Serial.println(finished_tasks);
          Serial.print("all_tasks");
          Serial.println(all_tasks);


          if (state_changed) {
            state_changed = 0;
            if (finished_tasks < all_tasks) {


              start_timer = (millis() - init_timer) / 1000;

              work_start = start_timer;

              break_time = break_time + (start_timer - break_start);
            }

          }

          Serial.print("Work_time: ");
          Serial.println(work_time);

          Serial.print("Break_time: ");
          Serial.println(break_time);


          if (flipped) {
            flipped = 0;
            Serial.println("finished_task!!");
            if (finished_tasks < all_tasks) {
              finished_tasks++;
            }

            if (finished_tasks == all_tasks) {
              Serial.println("finished all!");
              

            }

            volt = analogRead(voltpin);
            level = map(volt, 1700, 2383, 0, 100);
            //SerialBT.write(level);

            SerialBT.write(level);
            //SerialBT.write('T');
            SerialBT.write(finished_tasks);
            SerialBT.write('\n');

          }

        }

        else if (state == 1) {

          if (state_changed) {

            if (finished_tasks < all_tasks) {

              state_changed = 0;

              start_timer = (millis() - init_timer) / 1000;

              Serial.print("start_tiomer:");
              Serial.println( start_timer);
              break_start = start_timer;


              work_time = work_time + (start_timer - work_start);
            }
          }

          Serial.print("Work_time: ");
          Serial.println(work_time);

          Serial.print("Break_time: ");
          Serial.println(break_time);

          set_led_ratio(break_time, work_time);
          
        }

      }

    }

  }

  if (turend_on) {

    if (all_tasks != 0) {

      if (state == 0) {
        set_led_tasks(finished_tasks, all_tasks, vertical_orient);

      }

    }
    else {
      set_led_tasks(finished_tasks, all_tasks, vertical_orient);

    }

  }


}
