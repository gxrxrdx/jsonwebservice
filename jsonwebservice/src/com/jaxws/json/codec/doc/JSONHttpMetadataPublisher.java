package com.jaxws.json.codec.doc;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jaxws.json.codec.JSONCodec;
import com.jaxws.json.codec.decode.WSJSONPopulator;
import com.jaxws.json.codec.encode.WSJSONWriter;
import com.sun.istack.NotNull;
import com.sun.xml.bind.v2.runtime.JAXBContextImpl;
import com.sun.xml.ws.api.model.wsdl.WSDLPart;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.HttpMetadataPublisher;
import com.sun.xml.ws.transport.http.WSHTTPConnection;
import com.sun.xml.ws.util.ServiceFinder;

/**
 * @author Sundaramurthi
 * @version 0.1
 * @mail sundaramurthis@gmail.com
 */
public class JSONHttpMetadataPublisher extends HttpMetadataPublisher {
	/**
	 * Template cache
	 */
	
	/**
	 * meta data model cache.
	 */
	@NotNull 
	private JSONCodec codec;

	/**
	 * @param endPoint
	 * @param codec
	 */
	public JSONHttpMetadataPublisher(JSONCodec codec) {
		this.codec		= codec;
	}

	@Override
	public boolean handleMetadataRequest(HttpAdapter adapter,
			WSHTTPConnection connection) throws IOException {
		String 	queryString 	= connection.getQueryString();
		
		// If query handled by document provider, handle it.
		for (HttpMetadataProvider metadataProvider : ServiceFinder.find(HttpMetadataProvider.class)) {
			if(metadataProvider.canHandle(queryString)){
				metadataProvider.setJSONCodec(this.codec);
				metadataProvider.setHttpAdapter(adapter);
				connection.setStatus(HttpURLConnection.HTTP_OK);
				connection.setContentTypeResponseHeader(metadataProvider.getContentType());
				metadataProvider.doResponse(connection);
				return true;
			}
		}
		// Call http get operationn. 
		adapter.invokeAsync(connection);
		int i = 0;
		while(!connection.isClosed()){
			// Wait untill response complete
			try {
				Thread.sleep(200);// 0.1 milliseconds
				
			} catch (InterruptedException e) {
				
			}
			if(i++ > 90000){
				connection.close();
				break;
				//5 * 60 * 60 * 1000 / 200 maximum of 5 minitus to respond
				
			}
		}
		return true;
	}
	
	/**
	 * Private utility to conver parameter list to JSON DOC
	 * @param parameters
	 * @return
	 */
	public static String getJSONAsString(Map<String,WSDLPart> parts , JAXBContextImpl context,JSONCodec codec){
		try{
			// RPC and DOCUMENT
			return WSJSONWriter.writeMetadata(getJSONAsMap(parts, context), codec.getCustomSerializer());
		}catch(Throwable e){
			// IGNORE
			return "{\"ERROR_IN_DOC\":\""+ e.getMessage() +"\"}";
		}
	}
	
	/**
	 * Private utility to conver parameter list to JSON DOC
	 * @param parameters
	 * @return
	 */
	public static HashMap<String,Object> getJSONAsMap(Map<String,WSDLPart> parts, JAXBContextImpl context){
		HashMap<String,Object> parameterMap = new HashMap<String, Object>();
		try{
			for(Entry<String, WSDLPart> part : parts.entrySet()){
				Class<?> clazz = context.getGlobalType(part.getValue().getDescriptor().name()).jaxbType;
				if(WSJSONPopulator.isJSONPrimitive(clazz)){
					parameterMap.put(part.getKey(), clazz.getSimpleName());
				} else if(clazz.isEnum()){
					parameterMap.put(part.getKey(), clazz.getEnumConstants()[0]);
				}else{
					parameterMap.put(part.getKey(), clazz.newInstance());
				}
			}
		}catch(Throwable e){
			parameterMap.put("ERROR_IN_DOC",e.getMessage());
		}
		return parameterMap;
	}
}
