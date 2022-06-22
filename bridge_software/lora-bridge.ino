/*
  tinySSB LoRa-to-WiFiUDP/BT/USB bridge

  2022-04-02 <christian.tschudin@unibas.ch>
*/

// config:
#define AP_SSID   "bridge"
#define AP_PW     "tiny-ssb-1"
#define UDP_PORT   5001

#define BTname    "tinyssb-bridge-1"

#define LORA_FREQ  867500000L
// #define LORA_FREQ  868000000L

#define CONTROL_ID 0x99 //0b10011001
#define SF_SET 0x81 //0b10000001
#define TX_SET 0x93 //0b10010011
#define BW_SET 0xA5 //0b10100101
#define STAT_REQUEST 0xB7 //0b10110111
#define STAT_SEND 0xC9 //0b11001001

#include "heltec.h" 
#include "tinyssb-logo.h"
#include "WiFi.h"
#include "WiFiAP.h"
#include "BluetoothSerial.h"

#include <lwip/sockets.h>
#include <cstring>
#include <string.h>

// ------------------------------------------------------------------------------------

BluetoothSerial BT;
IPAddress myIP;
int udp_sock = -1;
struct sockaddr_in udp_addr; // wifi peer
unsigned int udp_addr_len;
short rssi, ap_client_cnt, err_cnt;
short lora_cnt, udp_cnt, bt_cnt, serial_cnt;
char wheel[] = "/-\\|";

// -------------------------------------------------------------------------------------

struct kiss_buf {
  char esc;
  unsigned char buf[256];
  short len;
};

struct kiss_buf serial_kiss, bt_kiss;

#define KISS_FEND   0xc0
#define KISS_FESC   0xdb
#define KISS_TFEND  0xdc
#define KISS_TFESC  0xdd

void kiss_write(Stream &s, unsigned char *buf, short len) {
  s.write(KISS_FEND);
  for (int i = 0; i < len; i++, buf++) {
    if (*buf == KISS_FESC) {
      s.write(KISS_FESC); s.write(KISS_TFESC);
    } else if (*buf == KISS_FEND) {
      s.write(KISS_FESC); s.write(KISS_TFEND);
    } else
      s.write(*buf);
  }
  s.write(KISS_FEND);
}

int kiss_read(Stream &s, struct kiss_buf *kp) {
  while (s.available()) {
    short c = s.read();
    if (c == KISS_FEND) {
      kp->esc = 0;
      short sz = 0;
      if (kp->len != 0) {
        // Serial.printf("KISS packet, %d bytes\n", kp->len);
        sz = kp->len;
        kp->len = 0;
      }
      return sz;
    }
    if (c == KISS_FESC) {
      kp->esc = 1;
    } else if (kp->esc) {
      if (c == KISS_TFESC || c == KISS_TFEND) {
        if (kp->len < sizeof(kp->buf))
          kp->buf[kp->len++] = c == KISS_TFESC ? KISS_FESC : KISS_FEND;
      }
      kp->esc = 0;
    } else if (kp->len < sizeof(kp->buf))
      kp->buf[kp->len++] = c;
  }
  return 0;
}

// -------------------------------------------------------------------------------------

void ShowIP() {
  Heltec.display->setColor(BLACK); 
  Heltec.display->fillRect(0, 42, DISPLAY_WIDTH, DISPLAY_HEIGHT-42);
  Heltec.display->setColor(WHITE); 
  
  Heltec.display->drawString(0, 43, "AP=" + myIP.toString() + "/" + String(UDP_PORT));
  String str = IPAddress(udp_addr.sin_addr.s_addr).toString() + "/" + String(ntohs(udp_addr.sin_port));
  Heltec.display->drawString(0, 53, str);
}

void ShowWheels() {
  Heltec.display->setColor(BLACK); 
  Heltec.display->fillRect(DISPLAY_WIDTH-10, DISPLAY_HEIGHT-40, 10, 40);
  Heltec.display->setColor(WHITE);
  String str = " ";
  str[0] = wheel[lora_cnt % 4];   Heltec.display->drawString(DISPLAY_WIDTH-10, 22, str);
  str[0] = wheel[udp_cnt % 4];    Heltec.display->drawString(DISPLAY_WIDTH-10, 32, str);
  str[0] = wheel[bt_cnt % 4];     Heltec.display->drawString(DISPLAY_WIDTH-10, 42, str);
  str[0] = wheel[serial_cnt % 4]; Heltec.display->drawString(DISPLAY_WIDTH-10, 52, str);
}

void ShowCounters(){
  String str;
  Heltec.display->setColor(BLACK); 
  Heltec.display->fillRect(38, 20, DISPLAY_WIDTH-38, 22);
  Heltec.display->setColor(WHITE); 

  str = String(lora_cnt, DEC);   Heltec.display->drawString(38, 20, "L."+ str);
  str = String(udp_cnt, DEC);    Heltec.display->drawString(38, 30, "U."+ str);
  str = String(bt_cnt, DEC);     Heltec.display->drawString(82, 20, "B."+ str);
  str = String(serial_cnt, DEC); Heltec.display->drawString(82, 30, "S." + str);
  // rssi
  // str = String(err_cnt, DEC);  Heltec.display->drawString(90, 30, "e:");
  //                             Heltec.display->drawString(99, 30, str);
}

void send_udp(unsigned char *buf, short len) {
  if (udp_sock >= 0 && udp_addr_len > 0) {
    if (lwip_sendto(udp_sock, buf, len, 0,
                  (sockaddr*)&udp_addr, udp_addr_len) < 0)
        err_cnt += 1;
    }
}

void send_bt(unsigned char *buf, short len) {
  if (BT.connected())
    kiss_write(BT, buf, len);
}

void send_serial(unsigned char *buf, short len) {
  kiss_write(Serial, buf, len);
}

void send_lora(unsigned char *buf, short len) {
  LoRa.beginPacket();
  LoRa.write(buf, len);
  LoRa.endPacket();
}

void send_serial_int(int i) {
    String str = String(i);
    char cStr[str.length()];
    sprintf(cStr, "%d", i);
    send_serial(reinterpret_cast<unsigned char*> (cStr), str.length());
}

void send_bt_int(int i) {
    if (BT.connected()) {
      
      String str = String(i);
      char cStr[str.length()];
      sprintf(cStr, "%d", i);
      kiss_write(BT, reinterpret_cast<unsigned char*> (cStr), str.length());
    }
}

// ---------------------------------------------------------------------------

int int_from_bytes(unsigned char upper, unsigned char lower) {
  return (upper << 8) + lower;
}

// output signal strength in dBm
void set_TX_power(int output_signal_strength) {
  LoRa.setTxPower(output_signal_strength,RF_PACONFIG_PASELECT_PABOOST);
}

void set_spread_factor(int spread_factor) {
  LoRa.setSpreadingFactor(spread_factor);
}

void set_bandwith(int bandwidth) {
  LoRa.setSignalBandwidth(bandwidth);
}

void send_transmission_info(unsigned char *buf, short buf_len) {
  int rssi = LoRa.packetRssi();
  float snr = LoRa.packetSnr();

  short len = buf_len + 8;
  unsigned char message[len];

  message[0] = CONTROL_ID;
  message[1] = STAT_SEND;

  // decode timestamp to bytes
  int i = 0;
  for (int j = 2; j < len; j++) {
    message[i++] = buf[j];
  }

  // decode rssi to bytes
  message[i++] = (rssi >> 24) & 0xFF;
  message[i++] = (rssi >> 16) & 0xFF;
  message[i++] = (rssi >> 8) & 0xFF;
  message[i++] = rssi & 0xFF;

  // decode snr to bytes
  unsigned char* f_char_p = reinterpret_cast<unsigned char*>(&snr);
  int j;
  for (j = 0; j < sizeof(float); j++) {
    message[i++] = f_char_p[j];
  }
  
  send_lora(message, len);
}

void forward_transmission_info(unsigned char *buf, short buf_len, short interface) {
  int rssi = LoRa.packetRssi();
  float snr = LoRa.packetSnr();

  short len = buf_len + 8;
  unsigned char message[len];

  // copy received info to new message
  int i;
  for (i = 0; i < buf_len; i++) {
    message[i] = buf[i];
  }

  // append own transmission info
  // decode rssi to bytes
  message[i++] = (rssi >> 24) & 0xFF;
  message[i++] = (rssi >> 16) & 0xFF;
  message[i++] = (rssi >> 8) & 0xFF;
  message[i] = rssi & 0xFF;

  // decode snr to bytes
  int j;
  unsigned char* f_char_p = reinterpret_cast<unsigned char*>(&snr); 
  for (j = 0; j < sizeof(float); j++) {
    message[i++] = f_char_p[j];
  }

  // send status to all except receiving interface
  if (interface != 0) send_lora(message, len); //order
  if (interface != 1) send_serial(message, len);
  if (interface != 2) send_bt(message, len);
  if (interface != 3) send_udp(message, len);
}

// ---------------------------------------------------------------------------
int is_control_msg(unsigned char *buf, short len) {
  return (*buf) == CONTROL_ID && (len > 3);
}

bool parse_command(unsigned char *buf, short len, short interface) {
  switch(buf[1]) {
    case SF_SET:
      set_spread_factor(buf[2]);
      return true;

    case TX_SET:
      set_TX_power(buf[2]);
      return true;

    case BW_SET:
      {
        int bandwidth = int_from_bytes(buf[2], buf[3]);
        set_bandwith(bandwidth);
        return true;
      }

    case STAT_REQUEST:
      // not from LoRa -> send request
      if (interface != 0) {
        return false;
      }
      // from LoRa -> answer request
      send_transmission_info(buf, len);
      return true;

    case STAT_SEND:
      // not from LoRa -> forward normaly
      if (interface != 0) {
        return false;
      }
      // from LoRa -> complete info and forward
      forward_transmission_info(buf, len, interface);
      return true;
  }
}

// ---------------------------------------------------------------------------

void setup() { 
  Serial.begin(115200);
  
  Heltec.begin(true /*DisplayEnable Enable*/,
               true /*Heltec.Heltec.Heltec.LoRa Disable*/,
               true /*Serial Enable*/,
               true /*PABOOST Enable*/,
               LORA_FREQ /*long BAND*/);
  LoRa.setSignalBandwidth(250000);
  LoRa.setSpreadingFactor(7);
  LoRa.setCodingRate4(7);
  LoRa.setTxPower(14,RF_PACONFIG_PASELECT_PABOOST);
  LoRa.receive();

  Heltec.display->init();
  Heltec.display->flipScreenVertically();  
  Heltec.display->setFont(ArialMT_Plain_10);
  Heltec.display->clear();
  Heltec.display->drawXbm(0, 5, tinyssb_logo_width,tinyssb_logo_height,
                          (unsigned char*)tinyssb_logo_bits);
  Heltec.display->display();
  Heltec.display->setTextAlignment(TEXT_ALIGN_LEFT);
  Heltec.display->setFont(ArialMT_Plain_10);
  delay(2000);

  BT.begin(BTname);
  BT.setPin("0000");
  BT.write(KISS_FEND);

  WiFi.disconnect(true);
  delay(500);
  WiFi.mode(WIFI_AP);
  WiFi.softAP(AP_SSID, AP_PW, 7, 0, 2); // limit to two clients, only one will be served
  myIP = WiFi.softAPIP();
  delay(500);

  {
        struct sockaddr_in serv_addr;
        unsigned int serv_addr_len = sizeof(serv_addr);
 
        udp_sock = lwip_socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
        if (udp_sock >= 0) {
          memset(&serv_addr, 0, sizeof(serv_addr));
          serv_addr.sin_family = AF_INET;
          serv_addr.sin_port = htons(5001);
          serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
          if (lwip_bind(udp_sock, (const sockaddr*) &serv_addr, sizeof(serv_addr)) < 0)
              err_cnt += 1;

          int flags = fcntl(udp_sock, F_GETFL, 0);
          if (fcntl(udp_sock, F_SETFL, flags | O_NONBLOCK) < 0)
              err_cnt += 1;
      }
  }

  ShowIP();
  ShowCounters();
  Heltec.display->display();
}

// --------------------------------------------------------------------------

void loop() {
  uint8_t pkt_buf[250];
  int pkt_len;
  short change = 0;
  
  pkt_len = LoRa.parsePacket();
  if (pkt_len > 0) {
    change = 1;
    lora_cnt += 1;
    if (pkt_len > sizeof(pkt_buf))
      pkt_len = sizeof(pkt_buf);
    LoRa.readBytes(pkt_buf, pkt_len);
    bool no_forward = false;
    if (is_control_msg(pkt_buf, pkt_len)) {
      no_forward |= parse_command(pkt_buf, pkt_len, 0);
    }
    if (!no_forward) {
      send_udp(pkt_buf, pkt_len);  // order?
      send_bt(pkt_buf, pkt_len);
      send_serial(pkt_buf, pkt_len);
    }
  }

  pkt_len = kiss_read(Serial, &serial_kiss);
  if (pkt_len > 0) {
    change = 1;
    serial_cnt += 1;
    bool no_forward = false;
    if (is_control_msg(serial_kiss.buf, pkt_len)) {
      no_forward |= parse_command(pkt_buf, pkt_len, 1);
    }
    if (!no_forward) {
      send_lora(serial_kiss.buf, pkt_len);  // order?
      send_udp(serial_kiss.buf, pkt_len);
      send_bt(serial_kiss.buf, pkt_len);
    }
  }

  pkt_len = kiss_read(BT, &bt_kiss);
  if (pkt_len > 0) {
    change = 1;
    bt_cnt += 1;
    bool no_forward = false;
    if(is_control_msg(bt_kiss.buf, pkt_len)) {
      no_forward |= parse_command(pkt_buf, pkt_len, 2);
    }
    if (!no_forward) {
      send_lora(bt_kiss.buf, pkt_len);   // order?
      send_udp(bt_kiss.buf, pkt_len);
      send_serial(bt_kiss.buf, pkt_len);
    }
  }

  if (udp_sock >= 0) {
    struct sockaddr_in addr;
    unsigned int addr_len = sizeof(addr);
    pkt_len = lwip_recvfrom(udp_sock, pkt_buf, sizeof(pkt_buf), 0,
                             (struct sockaddr *)&addr, &addr_len);
    if (pkt_len > 0) {
      change = 1;
      udp_cnt += 1;
      memcpy(&udp_addr, &addr, addr_len);
      udp_addr_len = addr_len;
      bool no_forward = false;
      if(is_control_msg(pkt_buf, pkt_len)) {
        no_forward |= parse_command(pkt_buf, pkt_len, 3);
      }
      if (!no_forward) {
        send_lora(pkt_buf, pkt_len);  // order?
        send_serial(pkt_buf, pkt_len);
        send_bt(pkt_buf, pkt_len);
        ShowIP();
      }
    }
  }

  if (change) {
    ShowCounters();
    ShowWheels();
  }

  int n = WiFi.softAPgetStationNum();
  if (n != ap_client_cnt) {
    if (n == 0) {
      memset(&udp_addr, 0, udp_addr_len);
      udp_addr_len = 0;
    }
    ap_client_cnt = n;
    change = 1;
    ShowIP();
  }

  if (change)
    Heltec.display->display();
}

// eof
