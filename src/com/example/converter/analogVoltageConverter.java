package com.example.converter;

public class analogVoltageConverter {

	public static float fsrToVoltage(int fsrFromArduino){
    	/*
    	 * The value returned from Arduino is 0 to 1023, and the range of voltage
    	 * is 0 to 5 V. So in this method, convert the returned value to the range
    	 * of volts by the equation below.
    	 * 			value / range_of_value * range_of_volts
    	 */
    	double valRange = 1024.0;
    	double maxVoltage = 5.0;
    	/*
    	 * Value returned from arduino is volt value, so we need to convert it to force value
    	 * or pressure value. The next equation is derived from a circuit and equation 
    	 * between force value and resistance value. 1inch = 25.4mm
    	 * 			Force/square-half-inch = 40 / [50/Volt - 10] 
    	 */
    	float Ans = (float)(fsrFromArduino / valRange * maxVoltage);

    	return Ans;
	}
}
