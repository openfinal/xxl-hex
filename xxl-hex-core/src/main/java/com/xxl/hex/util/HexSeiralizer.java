package com.xxl.hex.util;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xxl.hex.HexEnum;
import com.xxl.hex.annotation.FieldDef;
import com.xxl.hex.codec.IRequest;
import com.xxl.hex.codec.IResponse;

public class HexSeiralizer {
	private static transient Logger logger = LoggerFactory.getLogger(HexSeiralizer.class);
	
	// ------------------------------------ obj byte --------------------------------
	private static byte[] obj2byte(Object obj){
		ByteWriteFactory writer = new ByteWriteFactory();
		try {
			Field[] allFields = obj.getClass().getDeclaredFields();
			for (Field fieldInfo : allFields) {
				Class<?> fieldClazz = fieldInfo.getType();
				fieldInfo.setAccessible(true);
				if (fieldClazz == String.class) {
					FieldDef fieldDef = fieldInfo.getAnnotation(FieldDef.class);
					String value = (String) fieldInfo.get(obj);
					writer.writeString(value, fieldDef.fieldLength());
				} else if (fieldClazz == Integer.TYPE) {
					int value = fieldInfo.getInt(obj);
					writer.writeInt(value);
				}
			}
		} catch (Exception e) {
			logger.error("", e);
		}
		byte[] bytes = writer.getBytes();
		return bytes;
	}
	
	private static Object byte2obj(ByteReadFactory reader, Class<?> clazz){
		try {
			Object msgInfo = clazz.newInstance();
			Field[] allFields = clazz.getDeclaredFields();
			for (Field fieldInfo : allFields) {
				Class<?> fieldClazz = fieldInfo.getType();
				fieldInfo.setAccessible(true);
				if (fieldClazz == String.class) {
					FieldDef fieldDef = fieldInfo.getAnnotation(FieldDef.class);
					String sValue = reader.readString(fieldDef.fieldLength());
					fieldInfo.set(msgInfo, sValue);
				} else if (fieldClazz == Integer.TYPE) {				
					int iValue = reader.readInt();
					fieldInfo.set(msgInfo, iValue);
				}								
			}
			return msgInfo;
		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}
	
	// --------------------------------- request serialize/deserialize -------------------
	public static String serializeRequest(IRequest request){
		
		ByteWriteFactory requestData = new ByteWriteFactory();
		requestData.writeInt(request.getMsgType());
		requestData.write(obj2byte(request));
		
		return requestData.getHex();
	}
	
	public static Object deserializeRequest(String request_hex){
		ByteReadFactory reader = new ByteReadFactory();
		boolean readOver = reader.readRequestHex(request_hex);
		if (!readOver) {
			return null;
		}
		
		int msgType = reader.readInt();
		
		Class<?> msgClazz = HexEnum.get(msgType).getRequestClazz();
		Object obj = byte2obj(reader, msgClazz);
		
		IRequest request = (IRequest) obj;
		request.setMsgType(msgType);
		
		return request;
	}
	
	// --------------------------------- response serialize/deserialize -------------------
	public static String serializeResponse(IResponse response){
		
		ByteWriteFactory writer = new ByteWriteFactory();
		writer.writeInt(response.getCode());
		writer.writeString(response.getMsg(), 64);
		writer.write(obj2byte(response));
		
		return writer.getHex();
	}
	
	public static Object deserializeResponse(String response_hex, int msgType){
		ByteReadFactory reader = new ByteReadFactory();
		boolean readOver = reader.readRequestHex(response_hex);
		if (!readOver) {
			return null;
		}
		
		int code = reader.readInt();
		String msg = reader.readString(64);
		
		Class<? extends IResponse> responseClazz = HexEnum.get(msgType).getResponseClazz();
		Object obj = byte2obj(reader, responseClazz);
		
		IResponse response = (IResponse) obj;
		response.setCode(code);
		response.setMsg(msg);
				
		return response;
	}
	
}
