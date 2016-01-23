package com.fedorkzsoft.speedometer;

public class Util {
	/**
	 * Returns only lower integer part of long
	 * @param l
	 * @return lower part
	 */
	public static int getLongLow(long l){
		return (int)(l & 0xFFFFFFFF);
	}
	
	/**
	 * Counts progress
	 * @param val - current value 
	 * @param start - start value
	 * @param end - end value
	 * @return
	 */
	public static float countPrct(float val, float start, float end){
		return (val - start)/(end - start);
	}
	
}
