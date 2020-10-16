### This project contains two parts:
## 1- Arduino Code for The Esp32 controller:
#### * make esp32 run as a Bluetooth server 
#### * recieve the readings from the connected accelerometer and classify them based on certian thresholds 
#### * based on the classifed event, change the LED output and send short messages to the connected Bluetooth device, ,that is unless one of the classifed events is "shaking", in which case it will tuggle the Bluetooth and LED strip on and off

