Any issue,question or suggestion post in [JSON Group](http://groups.google.com/group/jsonwebservice).


## Getting started with JSON Web Service ##

1) Overview
> JSON web service is focused towards developing web service with JSON as input and output format. Also existing web service can be accessed using JSON.

2) Dependency
  1. JAX-WS also called as metro (https://jax-ws.dev.java.net/)

> Other default development tools (Ant java and tomcat)

3) Installation
  1. This JSON web service plug-in can be installed by copying the jsonwebservice-ri-**.jar into your application's /WEB-INF/lib directory.
> 2) JAX-WS/Metro installation,
> > Download jax-ws from https://jax-ws.dev.java.net/ and copy all**.jars located from EXTRACT\_ROOT/lib into your application's /WEB-INF/lib directory. (optionaly you can ignore **-tools**.jar,**-extra**.jar)


> Note : installation of Java 1.5 ant and tomcat refer in relevant sites.

4) Creating hello world
  1. Decide your workspace root directory, and create following directory structure.
> > (For eclipse webtools users dynamic webproject create it for you)
```
       workspace
          |_helloWorld
          		 |_src
          		 |
               |_WebContent
                    |_WEB-INF
                    |    |_lib
                    |    |_web.xml
                    |    |_sun-jaxws.xml
               			|_index.html
```


> 2) Now follow up installation procedure described in previous step 3. Its simply copying jar files into your WEB-INF/lib folder.
> > In case of if your using metro, all jax-ws jars named as webservices-**.jar.
> > In case if your using Java webservice developer pack your jar name looks like jax-**.jar, saaj**.jar, etc**


> After installation procedure your file structure looks something like,
> ![http://jsonwebservice.googlecode.com/svn/wiki/helloWorldStruct.jpg](http://jsonwebservice.googlecode.com/svn/wiki/helloWorldStruct.jpg)

> 3) web.xml update.
> > Now open your helloWorld/WebContent/WEB-INF/web.xml and jax-ws specific following servlet entry.
```
        <?xml version="1.0" encoding="UTF-8"?>
		<web-app version="2.4" xmlns="http://java.sun.com/xml/ns/j2ee"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd">
		
			<listener>
				<listener-class>com.sun.xml.ws.transport.http.servlet.WSServletContextListener</listener-class>
			</listener>
		
			<servlet>
				<servlet-name>JAX-WS-Service</servlet-name>
				<servlet-class>com.sun.xml.ws.transport.http.servlet.WSServlet</servlet-class>
				<load-on-startup>1</load-on-startup>
			</servlet>
		
			<servlet-mapping>
				<servlet-name>JAX-WS-Service</servlet-name>
				<url-pattern>/json/*</url-pattern>
			</servlet-mapping>
			
			<!-- OPTIONAL IF YOU ALSO LIKE TO USE SOAP  -->
			<servlet-mapping>
				<servlet-name>JAX-WS-Service</servlet-name>
				<url-pattern>/soap/*</url-pattern>
			</servlet-mapping>
		
		</web-app>
```

> 4) sun-jaxws.xml update.
> > Now open your helloWorld/WebContent/WEB-INF/sun-jaxws.xml and hello world specific implementation entry.
```
		<?xml version="1.0" encoding="UTF-8"?>
		<endpoints version="2.0" 
			xmlns="http://java.sun.com/xml/ns/jax-ws/ri/runtime" >
		  <endpoint 
		  		name="HelloService" 
		  		implementation="com.mycompany.HelloWorldImpl" 
		  		url-pattern="/json/hello" 
		  		binding="http://jsonplugin.googlecode.com/json/"/>
		  
		   <!-- OPTIONAL IF YOU ALSO LIKE TO USE SOAP  -->
  			
			  <endpoint name="HelloServiceSOAP"
					implementation="com.mycompany.HelloWorldImpl"
					binding="http://schemas.xmlsoap.org/wsdl/soap/http" url-pattern="/soap/hello" />		
		</endpoints>
```

> 5) Creating HelloWorldImpl.
> > Now create folders 	com/mycompany under src directory. And create HelloWorldImpl.java file.
> > In HelloWorldImpl.java add your methods,
```
	    
	    package com.mycompany;

			import javax.jws.WebMethod;
			import javax.jws.WebParam;
			import javax.jws.WebResult;
			import javax.jws.WebService;
			import javax.jws.soap.SOAPBinding;
			
			@SOAPBinding(style = SOAPBinding.Style.RPC)
			@WebService (name="HelloService", targetNamespace="http://album.jsonplugin.com/json/")
			public class HelloWorldImpl {
			
				@WebMethod (operationName = "sayHello")
				public @WebResult(name="message") String sayHello(
						@WebParam(name="name") String name){
					return "Hello "+name;
				}
			}
			
```



> NOTE: @SOAPBinding(style = SOAPBinding.Style.RPC) annotation is required, Document binding NOT tested with this JSON codec.

> 6) Build
> > To build war creates ant 	build.xml inside workspace/helloWorld folder.


> Following are the content of build.xml
```
	   	<?xml version="1.0" encoding="UTF-8"?>
			<!-- ====================================================================== 
			     Dec 21, 2010                                                        
			
			     helloWorld    
			     demo
			     ====================================================================== -->
			<project name="helloWorld" default="war">
			  
				<path id ="classpath">
					<fileset dir="./WebContent/WEB-INF/lib" includes="*.jar"/>
					<pathelement location="./classes"/>
				</path>
			
			    <!-- ================================= 
			          target: war              
			         ================================= -->
			    <target name="war" depends="build" description="description">
			    	<war destfile="${ant.project.name}.war" webxml="./WebContent/WEB-INF/web.xml">
			      	<fileset dir="./WebContent/"/>
			      	<classes dir="./classes"/>
			      </war>
			    </target>
			
			    <!-- ================================= 
			          target: build                      
			         =================================  -->
			    <target name="build">
			    	<mkdir dir="classes"/>
			    	<javac classpathref="classpath" srcdir="./src"
			    	    				destdir="./classes" debug="off" source="1.5"/>
			    </target>
			
			</project>
```
> 7) Run the ant build.xml . You may down load pre build war from here http://jsonwebservice.googlecode.com/svn/trunk/demos/helloWorld/helloWorld.war

> 8) Now you can see the helloWorld.war inside   workspace/helloWorld.

> 9) Now copy the way file into tomcat webapps folder or use tomcat manager console to deploy it.
    1. ) After success full deployment look at automated document for json endpoint "http://localhost:8080/helloWorld/json/hello" or look at index page.
> > ![http://jsonwebservice.googlecode.com/svn/wiki/helloHome.jpg](http://jsonwebservice.googlecode.com/svn/wiki/helloHome.jpg)
> > ![http://jsonwebservice.googlecode.com/svn/wiki/endPoint.jpg](http://jsonwebservice.googlecode.com/svn/wiki/endPoint.jpg)
> > ![http://jsonwebservice.googlecode.com/svn/wiki/methodForm.jpg](http://jsonwebservice.googlecode.com/svn/wiki/methodForm.jpg)

  1. ) Once you got doc/test page you're done!!!


