package com.example.converter;

// Convert Integer and Float data to Byte data
public class ByteConverter {

	public static void printInteger(byte[] data,int offset,int value) throws IllegalArgumentException{
		int size=Integer.SIZE/Byte.SIZE;
		if(data==null||data.length<size||offset<0||data.length-size<offset)
			throw new IllegalArgumentException("Bat Param");
		else{
			for(int i=0;i<size;i++)
				data[offset+i]=Integer.valueOf(value>>(Byte.SIZE*(size-1-i))).byteValue();
		}
	}
	
	public static int getInteger(byte[] data,int offset) throws IllegalArgumentException{
		int result=0;
		int size=Integer.SIZE/Byte.SIZE;
		if(data==null||data.length<size||offset<0||data.length-size<offset)
			throw new IllegalArgumentException("Bat Param");
		else{
			for(int i=0;i<size;i++)
				result|=Integer.valueOf(data[offset+i]&0xff).intValue()<<(Byte.SIZE*(size-1-i));
		}
		return result;
	}
	
	public static void printFloat(byte[] data,int offset,float value) throws IllegalArgumentException{
		printInteger(data, offset, Float.floatToIntBits(value));
	}
	
	public static float getFloat(byte[] data,int offset) throws IllegalArgumentException{
		return Float.intBitsToFloat(getInteger(data, offset));
	}

	public static int composeInt(byte hi, byte lo) {
		int val = (int)( hi & 0xff );
		val *= 256;
		val += (int)( lo & 0xff );
		return val;
	}

}
