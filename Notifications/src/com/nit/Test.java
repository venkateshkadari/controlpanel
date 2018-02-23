package com.nit;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/")
public class Test {
	@GET
	@Path("/studentCancellation/{login}/{code}")
	//@Produces(MediaType.APP)
	public Response studentCancellation(@PathParam("login") String login,@PathParam("code") String code) throws Exception
	{
		String rs  = null;
		try 
		{
			
			}
			catch(Exception e)
			{
				throw e;
			}
			//StringBuffer sb = new StringBuffer();
			/*Gson gson = new Gson();
			System.out.println("hello");
			System.out.println(gson.toJson(feedData));
			feeds = gson.toJson(feedData);
			return Response.status(200).entity(jsonInString).header("Access-Control-Allow-Origin", "*").build();*/
		//String entity_msg = no_insert_records + " records are inserted." ; 

		return Response.status(200).entity(rs).build();
		//return "success";
	}
	@GET
	public void test()throws Exception
	{
	 System.out.println("Test");
	
	}
}
