package com.jaxws.json.codec.doc;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.namespace.QName;

import com.googlecode.jsonplugin.JSONPopulator;
import com.jaxws.json.JaxWsJSONPopulator;
import com.jaxws.json.codec.JSONCodec;
import com.sun.xml.bind.v2.runtime.JAXBContextImpl;
import com.sun.xml.bind.v2.runtime.JaxBeanInfo;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.server.PortAddressResolver;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.model.ParameterImpl;
import com.sun.xml.ws.model.WrapperParameter;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.HttpMetadataPublisher;
import com.sun.xml.ws.transport.http.WSHTTPConnection;

/**
 * @author Sundaramurthi
 * @version 0.1
 * @mail sundaramurthis@gmail.com
 */
public class JSONHttpMetadataPublisher extends HttpMetadataPublisher {
	private static final Properties  templates = new Properties();
	MetaDataModelServer	metaDataModelServer = null;
	JsClientServer jsClientServer = null;
	static{
		try {
			templates.load(JSONHttpMetadataPublisher.class.getResourceAsStream("codec.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private WSEndpoint<?> endPoint;

	public JSONHttpMetadataPublisher(WSEndpoint<?> endPoint) {
		this.endPoint = endPoint;
	}

	@Override
	public boolean handleMetadataRequest(HttpAdapter adapter,
			WSHTTPConnection connection) throws IOException {
		String queryString = connection.getQueryString();
		if ((queryString == null || queryString.equals("")) && endPoint != null){
			
			JAXBContextImpl context = (JAXBContextImpl)endPoint.getSEIModel().getJAXBContext();
			connection.setStatus(HttpURLConnection.HTTP_OK);
			connection.setContentTypeResponseHeader("text/html;charset=\"utf-8\"");
	        
			OutputStream out = connection.getOutput();
			
			String templateMain = templates.getProperty("template.main", "<html><body>My Bad...</body></html>");
			
			templateMain		= templateMain.replaceAll("#SERIVICE_NAME#", endPoint.getServiceName().getLocalPart());

	        PortAddressResolver portAddressResolver = adapter.owner.createPortAddressResolver(connection.getBaseAddress());
	        String address = portAddressResolver.getAddressFor(endPoint.getServiceName(), endPoint.getPortName().getLocalPart());
	        if(address != null)
	        	templateMain = templateMain.replaceAll("#END_POINT_URL#", address);

	        StringBuffer 	methods = new StringBuffer();
	        
	        int count = 0;
			for(JavaMethod method:endPoint.getSEIModel().getJavaMethods()){
				String methodTemplate 	= templates.getProperty("template.method", "");
				methodTemplate			= methodTemplate.replaceAll("#METHOD_NAME#", method.getOperationName());
				methodTemplate			= methodTemplate.replaceAll("#TR_CLASS#",(count % 2) == 1 ? "odd" : "even");

				QName methodQName = method.getRequestPayloadName();
				JaxBeanInfo type = context.getGlobalType(methodQName);
				StringBuffer jsonIn = new StringBuffer();
				StringBuffer jsonOut = new StringBuffer();
				if(type == null && method instanceof JavaMethodImpl){
					JavaMethodImpl methodImpl = (JavaMethodImpl) method;
					jsonIn.append("{\""+methodImpl.getOperationName()+"\":{");
					int countParam = 0;
					for(ParameterImpl param:methodImpl.getRequestParameters()){
						if(param instanceof WrapperParameter){
							WrapperParameter wparam =(WrapperParameter)param;
							for(ParameterImpl paramChild :wparam.getWrapperChildren()){
								if(countParam >0){
									jsonIn.append(",");
								}
								jsonIn.append("\""+paramChild.getName().getLocalPart()+"\":");
								serializeBean((Class<?>) paramChild.getTypeReference().type,jsonIn,new ArrayList<Class<?>>(),false);
								countParam ++;
							}
						}else{
							jsonIn.append("\""+param.getName().getLocalPart()+"\":");
							serializeBean((Class<?>) param.getTypeReference().type,jsonIn,new ArrayList<Class<?>>(),false);
							countParam ++;
						}
					}
					jsonIn.append("}}");
					//Out
					jsonOut.append("{");
					countParam = 0;
					for(ParameterImpl param:methodImpl.getResponseParameters()){
						if(param instanceof WrapperParameter){
							WrapperParameter wparam =(WrapperParameter)param;
							for(ParameterImpl paramChild :wparam.getWrapperChildren()){
								if(countParam >0){
									jsonOut.append(",");
								}
								jsonOut.append("\""+paramChild.getName().getLocalPart()+"\":");
								serializeBean((Class<?>) paramChild.getTypeReference().type,jsonOut,new ArrayList<Class<?>>(),false);
								countParam ++;
							}
						}else{
							jsonOut.append("\""+param.getName().getLocalPart()+"\":");
							serializeBean((Class<?>) param.getTypeReference().type,jsonOut,new ArrayList<Class<?>>(),false);
							countParam ++;
						}
					}
					jsonOut.append("}");
				}else{
					Class<?> bean = type.jaxbType;
					jsonIn.append("{\""+method.getOperationName()+"\":");
					serializeBean(bean,jsonIn,new ArrayList<Class<?>>(),false);
					jsonIn.append("}");
					//
					if(!method.getMEP().isOneWay()){
						bean = context.getGlobalType(method.getResponsePayloadName()).jaxbType;
						serializeBean(bean,jsonOut, new ArrayList<Class<?>>(),false);
					}else{
						jsonOut.append("ONE-WAY");
					}
				}
				methodTemplate = methodTemplate.replaceAll("#INPUT_JSON#", jsonIn.toString());
				try{
					methodTemplate = methodTemplate.replaceAll("#OUTPUT_JSON#", jsonOut.toString().replaceAll("$", ""));
				}catch(Throwable th){
					th.printStackTrace();
				}
				
				methods.append(methodTemplate);
				count++;
			}
			templateMain		= templateMain.replace("#METHODS#", methods.toString());
			
			out.write(templateMain.getBytes());
			return true;
		}else if(queryString.startsWith("model")){
			//if(metaDataModelServer == null )
				metaDataModelServer = new MetaDataModelServer(endPoint,queryString.indexOf("&all") > -1,true);
			
			connection.setStatus(HttpURLConnection.HTTP_OK);
			connection.setContentTypeResponseHeader("text/javascript;charset=\"utf-8\"");
			metaDataModelServer.doResponse(connection.getOutput());
			return true;
		}else if(queryString.startsWith("jsonmodel")){
			//if(metaDataModelServer == null )
			metaDataModelServer = new MetaDataModelServer(endPoint,queryString.indexOf("&all") > -1,false);
		
			connection.setStatus(HttpURLConnection.HTTP_OK);
			connection.setContentTypeResponseHeader("application/json;charset=\"utf-8\"");
			metaDataModelServer.doResponse(connection.getOutput());
			return true;
		}else if(queryString.startsWith("client")){
			if(jsClientServer == null)
				jsClientServer	= new JsClientServer(endPoint);
			
			StringBuffer model = new StringBuffer();
			connection.setStatus(HttpURLConnection.HTTP_OK);
			connection.setContentTypeResponseHeader("text/javascript;charset=\"utf-8\"");
			jsClientServer.doResponse(connection.getOutput());
			return true;
			
			/*model.append("{\""+endPoint.getServiceName().getLocalPart()+"\":{");
			
			model.append("\""+endPoint.getPortName().getLocalPart()+"\":{");
			byte count 	= 0;
			for(JavaMethod method:endPoint.getSEIModel().getJavaMethods()){
				JavaMethodImpl methodImpl = (JavaMethodImpl) method;
				if(count != 0){
					model.append(",");
				}
				count++;
				model.append("\""+methodImpl.getOperationName()+"\":{");
				
				model.append("\"input\":{");
				int countParam = 0;
				for(ParameterImpl param:methodImpl.getRequestParameters()){
					if(param instanceof WrapperParameter){
						WrapperParameter wparam =(WrapperParameter)param;
						for(ParameterImpl paramChild :wparam.getWrapperChildren()){
							if(countParam >0){
								model.append(",");
							}
							model.append("\""+paramChild.getName().getLocalPart()+"\":");
							serializeBean((Class<?>) paramChild.getTypeReference().type,model,new ArrayList<Class<?>>(),queryString.indexOf("autoBind") > 0);
							countParam ++;
						}
					}else{
						model.append("\""+param.getName().getLocalPart()+"\":");
						serializeBean((Class<?>) param.getTypeReference().type,model,new ArrayList<Class<?>>(),queryString.indexOf("autoBind") > 0);
						countParam ++;
					}
				}
				model.append("}");
				
				//
				model.append(",\"output\":\"TODO?? MAy be place holder for result\"");
				//Type metadata
				//Method close
				model.append("}");
			}
			model.append("}"); // Port end	
			model.append("}");// Service end
			model.append("}");
			//serializeBean(endPoint.getImplementationClass(),form,new ArrayList<Class<?>>());
			return true;*/
		}
		return false;
	}
	
	private void serializeBean(Class<?> bean,StringBuffer out,List<Class<?>> stack,boolean autoBind) throws IOException{
		for(Class<?> stackBean : stack ){
			if(stackBean.equals(bean)){//Recursion deducted
				out.append("null");
				return;
			}
		}
		if(bean.isEnum()){
			out.append("\""+bean.getSimpleName()+"\"");
			return;
		}
		if(JaxWsJSONPopulator.isJSONPrimitive(bean)){
			out.append("\""+bean.getSimpleName()+"\"");
			return;
		}
		try{
			if(bean != null){
				int count =0;
				nextField:
				for(Field field:bean.getDeclaredFields()){
					field.getAnnotations();
					for(Pattern patten:JSONCodec.excludeProperties){
						if(patten.matcher(field.getName()).matches()){
							continue nextField;
						}
					}
					if(field.getDeclaringClass().getName().equals(bean.getName())){
						if(count != 0 ){
							out.append(",");
						}else{
							out.append("{");
						}
						try{
							Class.forName("com.jaxws.json.JaxWsJSONPopulator");
						}catch(Throwable th){
							th.printStackTrace();
						}
						if(field.getType() instanceof Class && !JaxWsJSONPopulator.isJSONPrimitive(field.getType())){
							out.append("\""+escapeString(field.getName())+"\":");
							if(field.getType().getName().equals(JAXBElement.class.getName())){
								//TODO serialize element
								//serializeBean(field.getType(), out);
								out.append("null");
							}else{
								stack.add(bean);
								serializeBean(field.getType(), out,stack,autoBind);
								stack.remove(bean);
							}
						}else{
							XmlElement xmlElemnt = field.getAnnotation(XmlElement.class);
							if(autoBind){
								//out.append("\""+escapeString(field.getName())+"\":function(){try{return $(\""+escapeString(field.getName())+"\").getValue().toJSON();}catch(e){}}");
								out.append("\""+escapeString(field.getName())+"\":function(){try{return JSON_BIND_ACTIVE_FORM."+escapeString(field.getName())+".value}catch(e){return null;}}");
							}else{
								out.append("\""+escapeString(field.getName())+"\":\""+(xmlElemnt != null?xmlElemnt.defaultValue() != null ?xmlElemnt.defaultValue().trim():"":"") +"\"");
							}
						}
					}
					count++;
				}
				if(count != 0 ){
					out.append("}");
				}else if(bean.isPrimitive()){
					bean.getAnnotations();
					bean.getDeclaredAnnotations();
					out.append("\""+(new JSONPopulator().convert(bean, null, null, null))+"\"");
				}else{
					out.append("null");
				}
			}
		}catch(Throwable th){
			th.printStackTrace();
			out.append(th.getMessage());
		}
	}
	
	private void serializeMetaDtata(Class<?> bean,StringBuffer out,List<Class<?>> stack) throws IOException{
		for(Class<?> stackBean : stack ){
			if(stackBean.equals(bean)){//Recursion deducted
				out.append("null");
				return;
			}
		}
		if(bean.isEnum()){
			out.append("[");
			int size = bean.getEnumConstants().length;
			for(Object cnst:bean.getEnumConstants()){
				out.append("\""+cnst.toString()+"\""+(size-- == 0?"":","));
			}
			out.append("]");
			return;
		}
		if(JaxWsJSONPopulator.isJSONPrimitive(bean)){
			//out.append("\"\"");
			out.append("{");
			out.append("\"type\":\""+bean.getSimpleName()+"\",");
			out.append("\"nillable\":\"false\",");
			out.append("\"required\":\"true\"");
			out.append("}");
			return;
		}
		try{
			if(bean != null){
				int count =0;
				nextField:
				for(Field field:bean.getDeclaredFields()){
					field.getAnnotations();
					for(Pattern patten:JSONCodec.excludeProperties){
						if(patten.matcher(field.getName()).matches()){
							continue nextField;
						}
					}
					if(field.getDeclaringClass().getName().equals(bean.getName())){
						if(count != 0 ){
							out.append(",");
						}else{
							out.append("{");
						}
						try{
							Class.forName("com.jaxws.json.JaxWsJSONPopulator");
						}catch(Throwable th){
							th.printStackTrace();
						}
						if(field.getType() instanceof Class && !JaxWsJSONPopulator.isJSONPrimitive(field.getType())){
							out.append("\""+escapeString(field.getName())+"\":");
							if(field.getType().getName().equals(JAXBElement.class.getName())){
								//TODO serialize element
								//serializeBean(field.getType(), out);
								out.append("null");
							}else{
								stack.add(bean);
								serializeMetaDtata(field.getType(), out,stack);
								stack.remove(bean);
							}
						}else{
							XmlElement xmlElemnt = field.getAnnotation(XmlElement.class);
							out.append("\""+escapeString(field.getName())+"\":{");
							// Generate object meta data
							if(xmlElemnt != null){
								out.append("\"defaultValue\":");
								string(xmlElemnt.defaultValue(),out);
								out.append(", \"nillable\":\""+xmlElemnt.nillable()+"\"");
								out.append(", \"required\":\""+xmlElemnt.required()+"\"");// TODO difrence betwin nillable and required
								out.append(", \"type\":\""+field.getType().getSimpleName()+"\"");
								out.append(", \"restriction\":{");
									out.append("\"minLength\":0");
									out.append(",");
									out.append("\"maxLength\":255");
									out.append(",\"pattern\":\"\"");
								out.append("}");
								
							}
							out.append("}");
							
							//		"\""+(xmlElemnt != null?xmlElemnt.defaultValue() != null ?xmlElemnt.defaultValue().trim():"":"") +"\"");
						}
					}
					count++;
				}
				if(count != 0 ){
					out.append("}");
				}else if(bean.isPrimitive()){
					bean.getAnnotations();
					bean.getDeclaredAnnotations();
					out.append("\""+(new JSONPopulator().convert(bean, null, null, null))+"\"");
				}else{
					out.append("null");
				}
			}
		}catch(Throwable th){
			th.printStackTrace();
			out.append(th.getMessage());
		}
	}
	private String escapeString(String input){
		try{
			return input.replaceAll("\\$", "");
		}catch(NullPointerException exp){
			return "null";
		}catch(Throwable th){
			return input;
		}
	}
	
	private void string(Object obj, StringBuffer out) {
		out.append('"');
		CharacterIterator it = new StringCharacterIterator(obj.toString());
		for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
			if (c == '"') {
				out.append("\\\"");
			} else if (c == '\\') {
				out.append("\\\\");
			} else if (c == '/') {
				out.append("\\/");
			} else if (c == '\b') {
				out.append("\\b");
			} else if (c == '\f') {
				out.append("\\f");
			} else if (c == '\n') {
				out.append("\\n");
			} else if (c == '\r') {
				out.append("\\r");
			} else if (c == '\t') {
				out.append("\\t");
			} else if (Character.isISOControl(c)) {
				// this.unicode(c);
			} else {
				out.append(c);
			}
		}
		out.append('"');
	}
}
