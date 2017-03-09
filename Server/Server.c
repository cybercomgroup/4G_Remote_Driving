/*
 * Written by Albin Hellqvist
 */

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <wiringPi.h>
#include <wiringPiI2C.h>
#include <arpa/inet.h>
#include <sys/time.h>
#include "MotorHat.h"

#define DEBUG

#ifdef DEBUG
	#define debug_printf(...) printf(__VA_ARGS__)
#else
	#define debug_printf(...) 
#endif

#define BUFFER_SIZE			1024
#define DEFAULT_IP_ADRESS	"0.0.0.0"
#define DEFAULT_PORT		3000
#define ENGINE_RESET_TIME	1000
#define MOTOR_LEFT_IO		1
#define MOTOR_RIGHT_IO		3
#define SOCKET_TIMEOUT_TIME	5
#define STRENGTH_OFFSET		13
#define TIMESTAMP_SIZE		13

void setMotors(uShort i2c, int degree, int strength);
long long get_ms();

int main(int argc, char **argv) {
	// All variables for the socket functionality 
	char buffer[BUFFER_SIZE];
	int welcomeSocket, newSocket;
	int port = DEFAULT_PORT;
	struct sockaddr_in serverAddr;
	struct sockaddr_storage serverStorage;
	socklen_t addr_size;
	long long time;

	// Changes port if program was executed with an argument
	if (argv[1] != NULL) {
		port = atoi(argv[1]);
	}

	// Initializes the motor hat and restores engines to default
	uShort i2c = init();
	setMotors(i2c, 0, 0);

	// Set server address family = Internet
	serverAddr.sin_family = AF_INET;
	// Set server port number
	serverAddr.sin_port = htons(port);
	// Set IP address to localhost
	serverAddr.sin_addr.s_addr = inet_addr(DEFAULT_IP_ADRESS);
	// Set all bits of the padding field to 0
	memset(serverAddr.sin_zero, '\0', sizeof(serverAddr.sin_zero));

	// Sets the socket timeout to the struct
	struct timeval tv;
	tv.tv_sec	= SOCKET_TIMEOUT_TIME;
	tv.tv_usec	= 0;

	// Creates socket
	// 1) Internet domain 2) Stream socket 3) Default protocol (TCP in this case)
	welcomeSocket = socket(PF_INET, SOCK_STREAM, 0);

	// Bind the address struct to the socket
	bind(welcomeSocket, (struct sockaddr *) &serverAddr, sizeof(serverAddr));

	// Loop of the listening socket
	while (TRUE) {
		// Listen on the socket, with 1 max connection requests queued
		if (listen(welcomeSocket, 1) == 0) {
			printf("SERVER: Listening to port: %i\n", port);
		}
		else {
			printf("SERVER ERROR: BAD CONNECTION\n");
			break;
		}

		// Accept call creates a new socket for the incoming connection
		addr_size = sizeof(serverStorage);
		newSocket = accept(welcomeSocket, (struct sockaddr *) &serverStorage, &addr_size);

		printf("SERVER: Client accepted\n");

		// Sets the timeout value to the socket
		setsockopt(newSocket, SOL_SOCKET, SO_RCVTIMEO, (const char*)&tv,sizeof(struct timeval));

		// Sets the current time in ms to the time variable
		time = get_ms();

		// Loop of the client socket
		while (TRUE) {
			// Restores engines to default if there is no joystick response 
			if (time + (long long)ENGINE_RESET_TIME < get_ms()) {
				setMotors(i2c, 0, 0);
			}
			
			// Clears buffer and reads message from client
			bzero(buffer, BUFFER_SIZE);
			if (read(newSocket, buffer, BUFFER_SIZE) < 0) {
				printf("SERVER ERROR: ERROR READING FROM SOCKET OR SOCKET TIMEOUT\n");
				printf("SERVER ERROR: CLOSING SOCKET\n");
				break;
			}
			
			// Checks for the zero message bug
			if (strlen(buffer) < 4) {
				printf("SERVER ERROR: ERROR READING FROM SOCKET OR SOCKET TIMEOUT\n");
				printf("SERVER ERROR: CLOSING SOCKET\n");
				break;
			}
			
			printf("CLIENT: %s\n", buffer);

			// Checks if the received message came from the joystick
			if (strstr(buffer, "(") != NULL) {
				// Sets a new base time for the engine timeout
				time = get_ms();
				
				// Calculates sizes of degree and strength (Joystick signals)
				int sizeOfDegree	= strstr(buffer, ";") - strstr(buffer, "(") - 1;
				int sizeOfStrength	= strstr(buffer, ")") - strstr(buffer, ";") - 1;

				// Makes place for these in char arrays
				char degreeString[sizeOfDegree + 1];
				char strengthString[sizeOfStrength + 1];

				// Copies degree and strength from buffer to char arrays
				strncpy(degreeString, strstr(buffer, "(") + 1, sizeOfDegree);
				strncpy(strengthString, strstr(buffer, "(") + sizeOfDegree + 2, sizeOfStrength);

				// Fixes the degree and strength strings correctly
				degreeString[sizeOfDegree]		= '\0';
				strengthString[sizeOfStrength]	= '\0';

				debug_printf("DEBUG: Degree: %s\n",   degreeString);
				debug_printf("DEBUG: Strength: %s\n", strengthString);

				// Sets the DC engines (based on the joystick signals)
				setMotors(i2c, (int)atoi(degreeString), (int)atoi(strengthString));
			}
			// Checks if the client sent a "leave" message
			if (strstr(buffer, "q") != NULL) {
				printf("SERVER: Client left\n");
				break;
			}
			// Checks if the client sent a ping
			else if (strstr(buffer, "p") != NULL) {
				char timeToSend[TIMESTAMP_SIZE+2];
				
				// Copies received ms to "timeToSend"
				strncpy(timeToSend, strstr(buffer, "p") + 5, TIMESTAMP_SIZE);
				timeToSend[TIMESTAMP_SIZE]		= '\n';
				timeToSend[TIMESTAMP_SIZE+1]	= '\0';
				
				// Send back the received time
				if (write(newSocket, timeToSend, strlen(timeToSend)) < 0) {
					printf("SERVER ERROR: ERROR WRITING TO SOCKET\n");
					printf("SERVER ERROR: CLOSING SOCKET\n");
					break;
				}
			}
		}
		
		// Restores the engines to default
		setMotors(i2c, 0, 0);
		
		// Closes client socket
		shutdown(newSocket, 0);
		close(newSocket);
	}

	// Closes listening socket
	shutdown(welcomeSocket, 0);
	close(welcomeSocket);

	// Shuts down the server
	printf("SERVER: Shutting down\n");
	return 0;
}

void setMotors(uShort i2c, int degree, int strength) {
	// Variables for the speed and direction of the motors
	int motorLeftSpeed		= strength;
	int motorRightSpeed		= strength;
	int motorLeftDirection	= MOTOR_FORWARD;
	int motorRightDirection	= MOTOR_FORWARD;

	// If strength and degree are 0 then release engine
	if (strength == 0) {
		motorLeftDirection	= MOTOR_RELEASE;
		motorRightDirection	= MOTOR_RELEASE;
	}
	// Different cases dependent on the degree
	else if (degree >= 350 || degree <= 10) {
		motorRightDirection	= MOTOR_BACK;
	}
	else if (degree >= 170 && degree <= 190) {
		motorLeftDirection	= MOTOR_BACK;
	}
	else if (degree >= 260 && degree <= 280) {
		motorLeftDirection	= MOTOR_BACK;
		motorRightDirection	= MOTOR_BACK;
	}
	else if (degree > 10 && degree < 80) {
		motorRightSpeed		= (degree * strength) / 90 + STRENGTH_OFFSET;
	}
	else if (degree > 100 && degree < 170) {
		motorLeftSpeed		= ((180 - degree) * strength) / 90 + STRENGTH_OFFSET;
	}
	else if (degree > 190 && degree < 260) {
		motorLeftSpeed		= ((degree - 180) * strength) / 90 + STRENGTH_OFFSET;
		motorLeftDirection	= MOTOR_BACK;
		motorRightDirection	= MOTOR_BACK;
	}
	else if (degree > 280 && degree < 350) {
		motorRightSpeed		= ((360 - degree) * strength) / 90 + STRENGTH_OFFSET;
		motorLeftDirection	= MOTOR_BACK;
		motorRightDirection	= MOTOR_BACK;
	}

	debug_printf("DEBUG: Left  speed #1: %i\n", motorLeftSpeed);
	debug_printf("DEBUG: Right speed #1: %i\n", motorRightSpeed);

	// Changes the speed signals to 0-255 instead of 0-100
	motorLeftSpeed	= (motorLeftSpeed  * 255) / 100;
	motorRightSpeed	= (motorRightSpeed * 255) / 100;

	debug_printf("DEBUG: Left  speed #2: %i\n", motorLeftSpeed);
	debug_printf("DEBUG: Right speed #2: %i\n", motorRightSpeed);

	// Sets the speed of the engines
	setSpeed(i2c, (uShort)MOTOR_LEFT_IO,  motorLeftSpeed);
	setSpeed(i2c, (uShort)MOTOR_RIGHT_IO, motorRightSpeed);

	if (motorLeftDirection != MOTOR_RELEASE) {
		// Always reverses the current direction of the left engine (It is a car you know)
		motorLeftDirection = 1 - motorLeftDirection;
	}

	// Sets the direction of the engines
	runMotor(i2c, (uShort)MOTOR_LEFT_IO,  motorLeftDirection);
	runMotor(i2c, (uShort)MOTOR_RIGHT_IO, motorRightDirection);
}

long long get_ms() {
    struct timeval te; 
    gettimeofday(&te, NULL);
    return (long long)(te.tv_sec*1000LL + te.tv_usec/1000);
}
