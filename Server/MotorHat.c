/*
 * Original code by "garagebrewer" 
 * https://www.raspberrypi.org/forums/viewtopic.php?f=44&t=112415
 *
 * Modified by Albin Hellqvist
 */

#include "MotorHat.h"

void setAllPWM(uShort i2c, uShort on, uShort off) {
	wiringPiI2CWriteReg8(i2c, PWM_ALL_LED_ON_L, on & 0xFF);
	wiringPiI2CWriteReg8(i2c, PWM_ALL_LED_ON_H, on >> 8);
	wiringPiI2CWriteReg8(i2c, PWM_ALL_LED_OFF_L, off & 0xFF);
	wiringPiI2CWriteReg8(i2c, PWM_ALL_LED_OFF_H, off >> 8);
}

void setPWM(uShort i2c, uShort pin, uShort on, uShort off) {
	wiringPiI2CWriteReg8(i2c, PWM_LED0_ON_L + 4 * pin, on & 0xFF);
	wiringPiI2CWriteReg8(i2c, PWM_LED0_ON_H + 4 * pin, on >> 8);
	wiringPiI2CWriteReg8(i2c, PWM_LED0_OFF_L + 4 * pin, off & 0xFF);
	wiringPiI2CWriteReg8(i2c, PWM_LED0_OFF_H + 4 * pin, off >> 8);
}

void setPin(uShort i2c, uShort pin, uShort value) {
	if (pin < 0 || pin > 15) {
		printf("PWM pin must be between 0 and 15 inclusive. Received '%d'\n", pin);
		return;
	}

	switch(value) {
		case 0:
			setPWM(i2c, pin, 0, 4096);
			break;
		case 1:
			setPWM(i2c, pin, 4096, 0);
			break;
		default:
			printf("PWM pin value must be 0 or 1. Received '%d'\n", pin);
			return;
	}
}

void runMotor(uShort i2c, uShort motor, uShort command) {
	uShort in1, in2;

	switch(motor) {
		case 1:
			in1 = PWM_M1_IN1;
			in2 = PWM_M1_IN2;
			break;
		case 2:
			in1 = PWM_M2_IN1;
			in2 = PWM_M2_IN2;
			break;
		case 3:
			in1 = PWM_M3_IN1;
			in2 = PWM_M3_IN2;
			break;
		case 4:
			in1 = PWM_M4_IN1;
			in2 = PWM_M4_IN2;
			break;
		default:
			printf("Invalid motor number '%d'\n", motor);
			return;
	}

	switch(command) {
		case MOTOR_FORWARD:
			setPin(i2c, in2, 0);
			setPin(i2c, in1, 1);
			break;
		case MOTOR_BACK:
			setPin(i2c, in1, 0);
			setPin(i2c, in2, 1);
			break;
		case MOTOR_RELEASE:
			setPin(i2c, in1, 0);
			setPin(i2c, in2, 0);
			break;
		default:
			printf("Unsupported command '%d'\n", command);
			return;
	}
}

void setSpeed(uShort i2c, uShort motor, uShort speed) {
	if (speed < 0 || speed > 255) {
		printf("Speed must be between 0 and 255 inclusive. Received '%d'\n", speed);
		return;
	}

	uShort pwm;
	
	switch(motor) {
		case 1:
			pwm = PWM_M1_PWM;
			break;
		case 2:
			pwm = PWM_M2_PWM;
			break;
		case 3:
			pwm = PWM_M3_PWM;
			break;
		case 4:
			pwm = PWM_M4_PWM;
			break;
		default:
			printf("Unsupported motor '%i'\n", motor);
			break;
	}

	setPWM(i2c, pwm, 0, speed * 16);
}

uShort init() {
	//Setup I2C
	uShort i2c = wiringPiI2CSetup(ADAFRUIT_MOTOR_HAT);

	//Setup PWM
	setAllPWM(i2c, 0, 0);
	wiringPiI2CWriteReg8(i2c, PWM_MODE2, PWM_OUTDRV);
	wiringPiI2CWriteReg8(i2c, PWM_MODE1, PWM_ALLCALL);
	delay(5);
	
	uShort mode1 = wiringPiI2CReadReg8(i2c, PWM_MODE1) & ~PWM_SLEEP;
	wiringPiI2CWriteReg8(i2c, PWM_MODE1, mode1);
	delay(5);

	//Set PWM frequency
	uShort prescale = (int)(25000000.0 / 4096.0 / PWM_FREQUENCY - 1.0);
	uShort oldmode = wiringPiI2CReadReg8(i2c, PWM_MODE1);
	uShort newmode = (oldmode & 0x7F) | 0x10;
	
	wiringPiI2CWriteReg8(i2c, PWM_MODE1, newmode);
	wiringPiI2CWriteReg8(i2c, PWM_PRESCALE, prescale);
	wiringPiI2CWriteReg8(i2c, PWM_MODE1, oldmode);
	delay(5);
	wiringPiI2CWriteReg8(i2c, PWM_MODE1, oldmode | 0x80);

	return i2c;
}
