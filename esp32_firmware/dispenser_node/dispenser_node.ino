#include <WiFi.h>
#include <WebServer.h>
#include <HX711.h>
#include <ESP32Servo.h>

// --- WIFI CONFIG (SoftAP) ---
const char* ssid = "NextGen_Dispenser_1";
const char* password = "kiosk_admin_123";

// --- PINS & HARDWARE ---
// HX711 Pins
const int LOADCELL_DOUT_PIN = 16;
const int LOADCELL_SCK_PIN = 4;
// Servo Pin
const int SERVO_PIN = 14;

// --- CALIBRATION ---
// Custom calibration factor provided: 440.88
const float CALIBRATION_FACTOR = 440.88; 

// --- SERVO STATES (For sliding flap over 2.5cm hole) ---
const int FLAP_CLOSED = 0;   // 0 degrees - fully covers hole
const int FLAP_PARTIAL = 45; // 45 degrees - flutter mode (limits flow)
const int FLAP_OPEN = 90;    // 90 degrees - fully unblocks hole

// --- INSTANCES ---
WebServer server(80);
HX711 scale;
Servo flapServo;

// --- STATE VARIABLES ---
enum DispenseState { IDLE, DISPENSING, PAUSED };
DispenseState currentState = IDLE;

float targetWeightGrams = 0.0;
float currentWeightGrams = 0.0;

// ==========================================
// SETUP
// ==========================================
void setup() {
  Serial.begin(115200);
  delay(100);

  Serial.println("\nNextGen-PDS Dispenser Node Booting...");

  // 1. Initialize Servo
  flapServo.attach(SERVO_PIN);
  flapServo.write(FLAP_CLOSED); 
  Serial.println("Servo attached and flap closed.");

  // 2. Initialize Load Cell (HX711)
  scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);
  scale.set_scale(CALIBRATION_FACTOR);
  scale.tare(); // Zero out on boot
  Serial.println("Load cell initialized and tared.");

  // 3. Setup WiFi Access Point
  WiFi.softAP(ssid, password);
  IPAddress IP = WiFi.softAPIP();
  Serial.print("SoftAP IP Address: ");
  Serial.println(IP); // Often 192.168.4.1

  // 4. Setup API Routes
  server.on("/status", HTTP_GET, handleStatus);
  server.on("/tare", HTTP_POST, handleTare);
  server.on("/dispense", HTTP_POST, handleDispense);
  server.on("/pause", HTTP_POST, handlePause);
  server.on("/resume", HTTP_POST, handleResume);
  server.on("/stop", HTTP_POST, handleStop);

  server.begin();
  Serial.println("HTTP server started.");
}

// ==========================================
// MAIN LOOP
// ==========================================
void loop() {
  server.handleClient(); // Listen for incoming REST requests

  // Update Scale Reading
  if (scale.is_ready()) {
    currentWeightGrams = scale.get_units(5); // Average over 5 readings for stability
    if(currentWeightGrams < 0) currentWeightGrams = 0.0; // Prevent negative display
  }

  // Handle Dispensing Logic Hook
  if (currentState == DISPENSING) {
    if (currentWeightGrams >= targetWeightGrams) {
      // 1. We reached the goal! Cut off instantly.
      flapServo.write(FLAP_CLOSED);
      currentState = IDLE;
      Serial.println("Target reached. Valve closed.");
    } else if (targetWeightGrams - currentWeightGrams <= 200.0) {
      // 2. We are within 200g of the target. Throttle the flap to partial to prevent over-spill
      flapServo.write(FLAP_PARTIAL);
    } else {
      // 3. Far from goal, keep it wide open
      flapServo.write(FLAP_OPEN);
    }
  }

  delay(50); // Small loop delay for stability
}

// ==========================================
// ROUTE HANDLERS
// ==========================================

// GET /status
// Returns active weight and state JSON
void handleStatus() {
  String stateStr = "idle";
  if (currentState == DISPENSING) stateStr = "dispensing";
  if (currentState == PAUSED) stateStr = "paused";

  String json = "{";
  json += "\"status\":\"" + stateStr + "\",";
  json += "\"current_weight_g\":" + String(currentWeightGrams, 2) + ",";
  json += "\"target_weight_g\":" + String(targetWeightGrams, 2);
  json += "}";

  server.send(200, "application/json", json);
}

// POST /tare
// Zeros out the scale explicitly
void handleTare() {
  if (currentState != DISPENSING) {
     scale.tare();
     server.send(200, "application/json", "{\"message\":\"Scale Tared Successfully\"}");
  } else {
     server.send(400, "application/json", "{\"error\":\"Cannot tare while dispensing\"}");
  }
}

// POST /dispense 
// Requires ?target=5000 (Grams) flag in URL
void handleDispense() {
  if (server.hasArg("target")) {
    targetWeightGrams = server.arg("target").toFloat();
    if (targetWeightGrams > 0) {
      currentState = DISPENSING;
      server.send(200, "application/json", "{\"message\":\"Dispensing Initiated\", \"target\":" + String(targetWeightGrams) + "}");
    } else {
      server.send(400, "application/json", "{\"error\":\"Target weight must be > 0\"}");
    }
  } else {
    server.send(400, "application/json", "{\"error\":\"Missing target argument\"}");
  }
}

// POST /pause
void handlePause() {
  if (currentState == DISPENSING) {
    flapServo.write(FLAP_CLOSED); // Shut valve immediately
    currentState = PAUSED;
    server.send(200, "application/json", "{\"message\":\"Dispensing Paused\"}");
  } else {
    server.send(400, "application/json", "{\"error\":\"Not dispensing currently\"}");
  }
}

// POST /resume
void handleResume() {
  if (currentState == PAUSED) {
    currentState = DISPENSING; // Valve will reopen in main loop
    server.send(200, "application/json", "{\"message\":\"Dispensing Resumed\"}");
  } else {
     server.send(400, "application/json", "{\"error\":\"Not paused currently\"}");
  }
}

// POST /stop
void handleStop() {
  flapServo.write(FLAP_CLOSED);
  currentState = IDLE;
  targetWeightGrams = 0.0;
  server.send(200, "application/json", "{\"message\":\"Dispensing Cancelled\"}");
}
