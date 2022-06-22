# LoRa Bridge Debug Communication
## Introduction
For easier connection testing and remote bridge configuration a set of useful debugging features were added to the lora bridge source code.
These can be run remotely by sending specific messages to the lora bridge. In the following the offered tools together with there usecases are explained.

## How to use
The debug features are controlled remotely by sending specific lora messages to the bridge. All message have the following common structure:  
| 8 bit control flag | 8 bit command id | values... |  
The 8 bit long control flag tells the bridge that the sent message contains a command.
The 8 bit long command id specifies the command to be executed.
After that there follows a command specific amount of bits containing parameters for the command.  
The default control flag is 0b10011001 or 153 in decimal notation.

## SF_SET
The spreading factor set (SF_SET) feature allows to set the bridges spreading factor remotely by sending the following message.  
| 10011001 | 10000001 | 8 bit spread_fact (byte) |  
Thereby the 8 bit long spread_fact is the desired spread facor binary representation. Allowed are values between ? and ?.

## TX_SET
The transmission power set (TX_SET) feature allows to set the bridges LoRa signal strength remotely by sending the following message.  
| 10011001 | 10010011 | 8 bit sig_strength (byte) |  
Thereby the 8 bit long sig_strength is the desired transmit power in binary representation. Allowed are values between 0 dBm and 20 dBm.

## BW_SET
The bandwidth set (BW_SET) feature allows to set the bridges bandwidth remotely by sending the following message.  
| 10011001 | 10100101 | 32 bit bandwidth (int) |  
Thereby the 32 bit long bandwidth is the desired bandwidth in binary representation. Allowed are values between ? and ?.

## STAT_REQUEST
Stat request allows a node to request the bridge to send back the rssi and snr values from the request packackage back to the requesting node.
A stat request has the following structure:  
| 10011001 | 10110111 | 64 bit request_id (long) |
The 64 bit long request_id is used to identify to wich request the answer belongs. It's recomended to use the current time on the sending node as the request_id as its unique and further allows response time measurements. The response to a stat request is the STAT_SEND package.
The response is sent via the interface that received the request.

## STAT_SEND
A STAT_SEND package is sent by the pridge as a response to a stat request. It contains the request_id used in the request followed by 32 bits representing the rssi and further 32 bits representing the snr.  
| 10011001 | 11001001 | 64 bit request_id (long) | 32 bit rssi (int) | 32 bit snr (float) |
A received response is forwarded to every interface excepte the one that received the response.