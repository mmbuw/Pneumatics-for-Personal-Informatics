
class Relay {

  private:
    
    bool active = false;
    unsigned long startTime, endTime;
    int inputs [3] = {A0,A1,A2};
    int counter;
    
  
  public:
    byte pin;
    
    Relay (byte _pin) {pin = _pin;}

    
    
    void setup () {
      pinMode(pin, OUTPUT);
      off();
      
      for(byte i = 0; i < 3; i ++)
      pinMode(inputs[i], INPUT);
      
    }

    void countTime() {
      if (active && millis() > startTime) {
        active = false;
        off();
      }
    }

    void inflate1 (int duration = 2) {
      if(digitalRead(inputs[0]) == HIGH && counter == 0){
        on();
        startTime = duration * 1000;
        delay(startTime);
        off();
        counter ++;
        //Serial.println(counter);
      }
    }
    
      void inflate2 (int duration = 1.5) {
      if(digitalRead(inputs[1]) == HIGH && counter == 0){
        on();
        startTime = duration * 1000;
        delay(startTime);
        off();
        counter ++;
      }
    }

      void inflate3 (int duration = 2.3) {
      if(digitalRead(inputs[2]) == HIGH && counter == 0){
        on();
        startTime = duration * 1000;
        delay(startTime);
        off();
        counter ++;
        //Serial.println(counter);
      }
    }
    
      
      
      
  
    void on () {
      digitalWrite(pin, LOW);
    }

    void off () {
      digitalWrite(pin, HIGH);
    }
   
};

