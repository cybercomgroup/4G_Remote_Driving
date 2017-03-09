/*
 * Original code by "garagebrewer" 
 * https://www.raspberrypi.org/forums/viewtopic.php?f=44&t=112415
 *
 * Modified by Albin Hellqvist
 */

#ifndef MOTORHAT_H_INCLUDED
#define MOTORHAT_H_INCLUDED

#define uShort				unsigned short
#define ADAFRUIT_MOTOR_HAT	0x60

#define PWM_M1_PWM			8
#define PWM_M1_IN1			10
#define PWM_M1_IN2			9
#define PWM_M2_PWM			13
#define PWM_M2_IN1			11
#define PWM_M2_IN2			12
#define PWM_M3_PWM			2
#define PWM_M3_IN1			4
#define PWM_M3_IN2			3
#define PWM_M4_PWM			7
#define PWM_M4_IN1			5
#define PWM_M4_IN2			6

#define PWM_FREQUENCY		1600.0
#define PWM_PRESCALE		0xFE

#define PWM_MODE1			0x00
#define PWM_MODE2			0x01
#define PWM_LED0_ON_L		0x06
#define PWM_LED0_ON_H		0x07
#define PWM_LED0_OFF_L		0x08
#define PWM_LED0_OFF_H		0x09

#define PWM_RESTART			0x80
#define PWM_SLEEP			0x10
#define PWM_ALLCALL			0x01
#define PWM_INVRT			0x10
#define PWM_OUTDRV			0x04

#define PWM_ALL_LED_ON_L	0xFA
#define PWM_ALL_LED_ON_H	0xFB
#define PWM_ALL_LED_OFF_L	0xFC
#define PWM_ALL_LED_OFF_H	0xFD

#define MOTOR_FORWARD		0
#define MOTOR_BACK			1
#define MOTOR_RELEASE		2

void setAllPWM(uShort i2c, uShort on, uShort off);
void setPWM(uShort i2c, uShort pin, uShort on, uShort off);
void setPin(uShort i2c, uShort pin, uShort value);
void runMotor(uShort i2c, uShort motor, uShort command);
void setSpeed(uShort i2c, uShort motor, uShort speed);
uShort init();

#endif
