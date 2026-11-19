/*
  Aqua Core – Arduino Uno water level sensor -> USB serial

  Wiring (analog water level sensor, 3 pins):
    sensor "S"  ->  A0
    sensor "+"  ->  D7   (sensor is powered ONLY while measuring: electrodes corrode much slower)
    sensor "-"  ->  GND

  Upload this sketch, then connect the Uno to the phone with a USB OTG cable.
  The app reads one line per second:  {"raw":312.4}
  The value is the real, averaged ADC reading (0-1023). Calibrate EMPTY / FULL in the app.
*/

const int SENSOR_PIN = A0;
const int POWER_PIN = 7;
const int SAMPLES = 10;

void setup() {
  Serial.begin(9600);
  pinMode(POWER_PIN, OUTPUT);
  digitalWrite(POWER_PIN, LOW);
}

void loop() {
  digitalWrite(POWER_PIN, HIGH);
  delay(20);                       // let the reading settle
  long sum = 0;
  for (int i = 0; i < SAMPLES; i++) {
    sum += analogRead(SENSOR_PIN);
    delay(5);
  }
  digitalWrite(POWER_PIN, LOW);

  Serial.print("{\"raw\":");
  Serial.print(sum / (float)SAMPLES, 1);
  Serial.println("}");

  delay(900);
}
