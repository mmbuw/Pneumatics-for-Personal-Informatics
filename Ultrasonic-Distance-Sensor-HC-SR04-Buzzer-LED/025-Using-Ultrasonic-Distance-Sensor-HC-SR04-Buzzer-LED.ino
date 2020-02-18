// defines pins numbers
const int trigPin = 9;
const int echoPin = 10;
int fsrAnalogPin = 6;
const int ledPin = 13;

int fsrReading;  
int LEDbrightness;
// defines variables
long duration;
int distance;
int safetyDistance;


void setup() {
pinMode(trigPin, OUTPUT); // Sets the trigPin as an Output
pinMode(echoPin, INPUT); // Sets the echoPin as an Input

pinMode(ledPin, OUTPUT);
Serial.begin(9600); // Starts the serial communication
}


void loop() {
// Clears the trigPin

fsrReading = analogRead(fsrAnalogPin);
Serial.println(fsrReading);
LEDbrightness = map(fsrReading, 0, 1023, 0, 255);
analogWrite(ledPin, LEDbrightness);


digitalWrite(trigPin, LOW);
delayMicroseconds(2);

// Sets the trigPin on HIGH state for 10 micro seconds
digitalWrite(trigPin, HIGH);
delayMicroseconds(10);
digitalWrite(trigPin, LOW);

//// Reads the echoPin, returns the sound wave travel time in microseconds
duration = pulseIn(echoPin, HIGH);

// Calculating the distance
distance= duration*0.034/2;

safetyDistance = distance;
if (safetyDistance <= 30){
  
  digitalWrite(ledPin, HIGH);
}
else{
  
  digitalWrite(ledPin, LOW);
}

// Prints the distance on the Serial Monitor
//Serial.print("Distance: ");
//Serial.println(distance);
}
