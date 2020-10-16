### This project contains two parts:
## 1- Arduino Code for The Esp32 controller (esp32_control):
#### - Make esp32 run as a Bluetooth server 
#### - Recieve the readings from the connected accelerometer and classify them based on certian thresholds 
#### - Based on the classifed event, change the LED output and send short messages to the connected Bluetooth device, ,that is unless one of the classifed events is "shaking", in which case it will tuggle the Bluetooth and LED strip on and off

## 2- Andoid app (rainmaker_app):
#### - Connect to the rainmaker using Bluetooth
#### - Add, edit, remove tasks in a tasks list, while simultaneously send the updates to the rainmaker device over Bluetooth
#### - Recieve messages from the rainmaker that contains infromation about the tasks progess


