/*
  The following code will:
  1- make esp32 run as a Bluetooth server
  2- recieve the readings from the connected accelerometer and classify them based on certian thresholds
  3- based on the classifed event, change the LED output and send short messages to the connected Bluetooth device
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
//#define LED_PIN  12 // for testing

// init number of leds
#define NUM_LEDS    10
CRGB leds[NUM_LEDS];


// to store the progress of the tasks
int all_tasks = 0;
int finished_tasks = 0;


// function to update the LED's output for the pending/finished task view
void tasks_leds(int start_led, int end_led, bool pos) {
  if (pos == 0) {


    for (int i = 0; i < start_led; i++) {
      leds[i] = CRGB ( 0, 0, 40);
    }

    for (int l = start_led; l < end_led; l++) {
      leds[l] = CRGB ( 40, 10, 0);
    }

    for (int i = end_led; i <= 9; i++) {
      leds[i] = CRGB ( 0, 0, 0);
    }



  }
  else if (pos == 1) {

    for (int i = 9; i > 9 - start_led; i--) {
      leds[i] = CRGB ( 0, 0, 40);
    }

    for (int l = 9 - start_led; l > 9 - end_led; l--) {
      leds[l] = CRGB ( 40, 10, 0);
    }


    for (int i = (9 - end_led); i >= 0; i--) {
      leds[i] = CRGB ( 0, 0, 0);
    }




  }

  FastLED.show();

}
// hepler function for fade_leds function that will change the LEDs based on the given range
void leds_manage(int i, int start_led, int end_led, bool pos, bool pom) {
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

  if (pom == true) {
    for (int l = 0; l <= 9; l++) {
      leds[l] = CRGB ( i, 0, 0);
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
void fade_leds(int start_led, int end_led, bool pos, bool pom)
{
  if (pom) {
    interval = 100;
  }
  else {
    interval = 60;
  }


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
  leds_manage(brightness, start_led, end_led, pos, pom);
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

  if (pom) {
    if (brightness >= 7 )
    { // reverse the direction of the fading at the ends of the fade:
      brightness = 7;
      fadeAmount = -fadeAmount;
    }
  }
  else {

    if (brightness >= 20 )
    { // reverse the direction of the fading at the ends of the fade:
      brightness = 20;
      fadeAmount = -fadeAmount;
    }
  }


}


// function that will map the given all tasks/finished tasks into fading certian parts of the LED strip
void set_led_tasks(int  finished, int all_tasks, bool vertical_orient, bool pom = false) {

  if (vertical_orient == 0) {
    fade_leds(finished, all_tasks, vertical_orient, pom);
  }
  else if (vertical_orient == 1) {
    fade_leds(9 - finished, 9 - all_tasks, vertical_orient, pom);
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
bool vertical_orient = 0; // 0 for upright, 1 for upsidedown
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


// variables for pomodoro
bool pom_toggle = false;
bool start_work = true;
bool start_break = false;

unsigned long work_pom = 1500000;
unsigned long break_pom = 300000;
unsigned long break_pom_time;
unsigned long work_pom_time;
unsigned long break_start_pom;
unsigned long work_start_pom;
int work_pom_leds;

bool toggle_flip = 0;
bool toogleView = 0; // 0 for tasks, 1 for pom
bool truned_on = true;
unsigned long on_time_start;
unsigned long pom_time_start;
bool pom_time_logged = false;
unsigned long pom_time_end;
bool pom_end_logged = false;
bool finished_time_logged = false;
unsigned long finished_time;
bool finished_sand = false;
int last_work_pom_led;
unsigned long previousMillis_pom = 0;
unsigned long interval_pom = 60;
bool pom_enabled = false;


void loop() {

  sensors_event_t event;
  bno.getEvent(&event);

  if (truned_on) {

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
        Serial.println("finished_task!!");
        if (finished_tasks < all_tasks) {
          finished_tasks++;

          delay(1000);

        }

        if (finished_tasks == all_tasks) {
          Serial.println("finished all!");
          //flipped=1;
          pom_toggle = false;
          toggle_flip = false;
          activated = false;

          tasks_leds(finished_tasks, all_tasks, vertical_orient);
          finished_time = (millis() - on_time_start) / 60000;
          finished_time_logged = true;

        }

        Serial.println ("shaking");
      }
    }
    else {
      shakes = 0;
    }
  }


  if (all_tasks == 0) {
    pom_toggle = false;
    //fade_leds(0, 9,0);
    activated = false;
    toggle_flip = false;
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
      truned_on = true;
      activated = true;
      //on_time_start=millis();
    }

    if (string == "R")
    {
      finished_tasks = 0;
      all_tasks = 0;
      activated = false;

      set_led_tasks(finished_tasks, all_tasks, vertical_orient);
    }

    if (string == "N")
    {
      Serial.println("N");
      volt = analogRead(voltpin);
      level = map(volt, 1700, 2383, 0, 100);
      SerialBT.write(level);
      SerialBT.write(finished_tasks);

      if (pom_end_logged) {
        pom_end_logged = false;
        SerialBT.write('e');
        SerialBT.write(pom_time_end / 60000);
      }
      if (finished_time_logged) {
        finished_time_logged = false;
        SerialBT.write('f');
        SerialBT.write(finished_time);
      }
      SerialBT.write('\n');
    }

    if (string == "TF")
    {
      Serial.println("off is sent!");
      truned_on = false;
      activated = false;
      leds_off();
    }

    if ((string.toInt() > 0) && (string.toInt() <= 11))
    {
      if (all_tasks == 0) {
        flipped = 0;
      }
      if (all_tasks == 0 && finished_tasks == 0 && string.toInt() == 1)  {
        on_time_start = millis();
        Serial.print("on_time_start_millis: ");
        Serial.println(on_time_start);
      }

      all_tasks = string.toInt();
      Serial.println("alltasks");
      Serial.println(all_tasks);
      current = map(event.orientation.y, -90, 90, 0, 180);
      Serial.println( current);

      if (current > 155 and current < 180) {
        vertical_orient == 0;
        //Serial.println("straight");
      }

      if (current > 0 and current < 20) {
        vertical_orient == 1;
        Serial.println("upside down !!");

      }

      if (current > 85 and current < 105) {
        // if in break mode (horizontal)
        //set_led_ratio(break_time, work_time);
        pom_toggle = false;
        //tasks_leds(finished_tasks,all_tasks,vertical_orient);
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
        all_tasks = 0;
        activated = false;
      }
      tasks_leds(finished_tasks, all_tasks, vertical_orient);
    }

  }

  if (truned_on) {
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
          ////
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
          pom_toggle = false;
        }

        if (state == 0) {
          Serial.print("finished tasks");
          Serial.println(finished_tasks);
          Serial.print("all_tasks");
          Serial.println(all_tasks);

          if (flipped) {
            flipped = 0;

            if (toggle_flip == false) {
              toggle_flip = true;
            }
            else if (toggle_flip == true) {
              toggle_flip = false;
            }
          }
          if (toggle_flip and activated) {
            Serial.println("pom on !!!!!!!!!!!!!");
            pom_toggle = true;
            unsigned long currentMillis_pom = millis();

            pom_time_start = (currentMillis_pom);
            pom_enabled = true;
          }
          else {
            pom_toggle = false;
          }
        }
      }

      if (pom_toggle) {
        if (all_tasks == finished_tasks and all_tasks != 0 and finished_tasks != 0) {
          Serial.print("all_tasks: ");
          Serial.println(all_tasks);
          Serial.print("finished: ");
          Serial.println(finished_tasks);
          unsigned long currentMillis_pom = millis();
          pom_time_end = currentMillis_pom - pom_time_start;
          pom_end_logged = true;
        }
        if (work_pom_time < work_pom) {
          start_break = false;
          if (start_work) {
            start_work = false;
            work_start_pom = millis();
          }
          work_pom_time = millis() - work_start_pom;
          work_pom_leds = map(work_pom_time, 0, work_pom, 0, 9);

          showPom(vertical_orient);
        }
        else {
          if (start_break == false) {
            start_break = true;
            break_start_pom = millis();
          }
          break_pom_time = millis() -  break_start_pom;
          // replace with proper flash fade code
          set_led_tasks(finished_tasks, all_tasks, vertical_orient, true);

          if (break_pom_time >= break_pom)
          {
            start_break == false;
            start_work = true;
            work_pom_time = 0;
            break_pom_time = 0;
            work_pom_leds = 0;
          }
        }
      }
      else {
        if (state == 0 or all_tasks <= finished_tasks) {
          start_break == false;
          start_work = true;
          work_pom_time = 0;
          break_pom_time = 0;
          work_pom_leds = 0;

          if (pom_enabled == true) {
            pom_enabled = false;
            unsigned long currentMillis_pom = millis();
            pom_time_end = currentMillis_pom - pom_time_start;
            pom_end_logged = true;
          }

        }

        //set_led_tasks(finished_tasks, all_tasks, vertical_orient, false);

        tasks_leds(finished_tasks, all_tasks, vertical_orient);
      }

    }

    else {
      set_led_tasks(finished_tasks, all_tasks, vertical_orient, false);
    }
  }

}

void showPom(bool pos)
{
  //leds_manage(1,0,work_pom_leds,pos, true);
  if (pos == 0) {
    for (int i = 0; i < work_pom_leds + 1; i++) {
      leds[i] = CRGB ( 1, 0, 0);
    }

    for (int i = work_pom_leds + 1; i <= 9; i++) {
      leds[i] = CRGB ( 0, 0, 0);
    }
  }
  else if (pos == 1) {
    for (int i = 9; i > 9 - (work_pom_leds + 1) ; i--) {
      leds[i] = CRGB ( 1, 0, 0);
    }

    for (int i = 9 - (work_pom_leds + 1); i >= 0; i--) {
      leds[i] = CRGB ( 0, 0, 0);
    }
  }

  FastLED.show();
}


bool showPom_sand(bool pos)
{
  //leds_manage(1,0,work_pom_leds,pos, true);
  if (pos == 0) {

    for (int j = 9; j >= work_pom_leds; j--)
    {
      if (millis() - previousMillis_pom >= interval_pom) {
        leds[j] = CRGB ( 1, 0, 0);

        for (int i = j - 1 ; i >= work_pom_leds; i--) {
          leds[i] = CRGB ( 0, 0, 0);
        }


        FastLED.show();

        if (j == work_pom_leds) {
          return true;
        }
        else {
          return false;
        }
        previousMillis = millis();
      }
    }
  }
  else if (pos == 1) {
  }
}
