## Customizing response encoding ##

# Introduction #

JSON web service support changing/customizing your response content and encoding.

Use case: Post JSON request and result is in HTML, PDF, or other response, but not default JSON. Response content produced using result of your implementation operation.


NOT USED FOR:

1) If you have use case: customize only specific Object in json response, This is NOT correct way. Look at Custom serializer.
2) If you have use case: diffident content/type produced by other service and like to send to json web service, client consider using attachment.


How it different from JSON/SOAP attachment?

1) Content Type:

ATTACHMENT: Attachment always produce multipart/mime response with your main part as JSON and attachments are following part with relevant content type.
CUSTOM ENCODER: Custom encoder give control to developer to specify main content type. Encoder responsible for response header/body.

2) Resource Access:

ATTACHMENT: In attachment, Attachment done inside implementation method. Developer have control/access to all resource or operations.
CUSTOM ENCODER: Custom encoder called only after operation completed. Encoder can access only result of operation.


Here is steps to create and register custom Encoder.

1) Create your wsdl with mime content output.
```
<operation name="encodedResponse">
			<json:operation soapAction="http://soap.sitestat.nl/encodedResponse" />
			<input>
				<json:body use="literal" />
			</input>
			<output>
				<mime:content type="text/html,application/pdf,text/*" part="response"/>
			</output>
		</operation>
```

2) Implement interface com.jaxws.json.packet.handler.Encoder.

```
public class ResponseEncoder implements Encoder {
	/**
	 * @return any valid MIME type. mime content response matched with wsdl mime:content value.
	 * If wsdl mime match with encoder mime then this encoder instance used for out put rendering.
	 * 
	 * If your like to customize JSON content type look at JSONObjectCustomizer.
	 */
	public String mimeContent() {
		return "text/html,application/pdf,text/*";
	}
	
	/**
	 * Static content type returned by encoder. If null content type returned by encoder implementation,
	 * response content buffered and  "encode" method returned content type used as content type header.
	 * @param packet
	 * @return
	 */
	public ContentType getStaticContentType(Packet packet){
HttpServletResponse response = (HttpServletResponse)packet.get(MessageContext.SERVLET_RESPONSE);
		response.addHeader("Content-disposition", "attachment; filename=\""+exportFile+"\"");
		response.setCharacterEncoding("utf-8");
		return htmlContentType;
}
	
	/**
	 * Called when user request accept content type with is supported by MIME type returned in contentType call.
	 * 
	 * @param packet
	 * @param output (Most case direct servlet output stream)
	 * @return content type of response.
	 * @throws IOException
	 * 
	 * WARN: returned content type ignored in current version, due to content not buffered.
	 */
	public ContentType encode(Packet packet, OutputStream output) throws IOException{
output.write("<html><body><p>Hello</p></body></html>".getBytes());
return htmlContentType;
}


public static final ContentType htmlContentType = new ContentType(){

		@Override
		public String getAcceptHeader() {
			return null;
		}

		@Override
		public String getContentType() {
			return "text/html";
		}

		@Override
		public String getSOAPActionHeader() {
			return null;
		}
	};
}

```

3) Create service registry file under META-INF/services/com.jaxws.json.packet.handler.Encoder

4) add your implementation encoder to registry.


Note mimeContent is the one match your implementation with operation response. In e.g case it is text/html,application/pdf,text/**. But it can be any string.**

Test!