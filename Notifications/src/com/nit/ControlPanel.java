package com.nit;


import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import com.google.gson.Gson;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/notification")
public class ControlPanel {	
	
	
    @POST
	@Path("/forceSyncFromXls/{zone}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response forceSyncDataFromXls(@FormDataParam("file") InputStream uploadedInputStream,@FormDataParam("file") FormDataContentDisposition fileDetail ,@PathParam("zone") String zone)   throws  BiffException,IOException
	{
		   
		    String uploadedFileLocation = "c://upload/" + fileDetail.getFileName();
		 
			writeToFile(uploadedInputStream, uploadedFileLocation);
			
			//return Response.status(200).entity(output).build();
		 
			String filePath =uploadedFileLocation ;
		    FileInputStream fs = new FileInputStream(filePath);
			Workbook wb;			
			String sbMessage="";
		    wb = Workbook.getWorkbook(fs);		
			Sheet sh = wb.getSheet(0);
			int totalNoOfRows =sh.getRows();
			int totalNoOfCols = sh.getColumns();
			System.out.println("totalNoOfRows---"+totalNoOfRows+"totalNoOfCols-------------"+totalNoOfCols);
			Connection connection = null;
			String entity_msg=null;
			try{
				    connection = getConnection(zone);
		        	String sql = "insert into SSDS_NSE_FORCE_SYNC_QUEUE (sync_type, sync_id) values (?, ?)";				
				    PreparedStatement ps = connection.prepareStatement(sql);				
				    for (int row = 0; row < totalNoOfRows; row++) {

					    for (int col = 0; col < totalNoOfCols; col++) {					
						ps.setString(1, "CONTRACT");
						ps.setString(2,sh.getCell(col, row).getContents());						
						ps.addBatch();
					}					
				}

				 ps.executeBatch();
				 ps.close();		
				 fs.close();
File xlsfile=new File(filePath);
Boolean fileDeleted=xlsfile.delete();

							 String[] myList = {"C:\\forcesyncconfig\\"};				 			
				 ForceSyncUtility fsu = new ForceSyncUtility();
				 sbMessage=fsu.contolPanelSync(myList);		
			
			     entity_msg = "{\"sbMessage\":\""+sbMessage+"\"}" ;     
		      
		        }
		        catch(Exception e){
		        	e.printStackTrace();		        	
		        }
	
			return Response.status(200).entity(sbMessage).build(); 

	}
	
	@GET
	@Path("/forceSync")	
	public Response forcSync(){
		
		String[] myList = {"C:\\forcesyncconfig\\"};
		String sbMessage="";

		try
		{	
		    ForceSyncUtility fsu = new ForceSyncUtility();
		 sbMessage=fsu.contolPanelSync(myList);		
	
		}
		catch(Exception e){			
	    	e.printStackTrace();
		}	

		String entity_msg = "{\"sbMessage\":\""+sbMessage+"\"}" ;
		return Response.status(200).entity(sbMessage).build();

	
	}
	
	@GET
	@Path("/getEmployees/centerID/{centerID}/{zone}")
	@Produces(MediaType.APPLICATION_JSON)
	public List getEmployees(@PathParam("centerID") String centerID,@PathParam("zone") String zone){
	   
		 
		 Connection connection = null;
			List empList =new ArrayList();
		try
		{
					
			connection = getConnection(zone);
		
			String sqlQuery="select du.id,du.login,du.password,ece.category_ref from dps_user du, ewsi_center_employee ece  where du.id=ece.id and ece.is_active=1 and ece.is_ssds_user=1 and ece.center_ref=?";
			 PreparedStatement stmt=connection.prepareStatement(sqlQuery);  
			 stmt.setString(1,centerID);
			 ResultSet rs=stmt.executeQuery(); 			 
			 while(rs.next()){  
				 System.out.println(rs.getString("login")+" "+rs.getString("password")); 
				 EmployeesBean eb= new EmployeesBean();
				 eb.setId(rs.getString("id"));
				 eb.setLogin(rs.getString("login"));
				 eb.setPassword(rs.getString("password"));
				 eb.setCategoryRef(rs.getString("category_ref"));
				 empList.add(eb);
				 }  
			 System.out.println("Successfully retrived Data");	
			
		}
		catch(Exception e){			
		e.printStackTrace();
		}
		finally{
			if(connection!=null)
			{				try {
					connection.close();
				} catch (SQLException e) {				
					e.printStackTrace();
				}
			}			
		}
		return empList;	

		//return Response.status(200).entity("{\"status\":\"Successfully retrived Data\"}").build();

	
	}
	@GET
	public void fixKeyNotFoundIssue1(){
		
	System.out.println("hello");
	}
	@GET
	@Path("/updateKeyNotFound/{zone}")
	public Response fixKeyNotFoundIssue(@PathParam("zone") String zone){
	   
		 
		 Connection connection = null;
		
		try
		{
					
			connection = getConnection(zone);
			 CallableStatement stmt=connection.prepareCall("{call SDS_KEY_NOT_FOUND}");  		  
			 stmt.execute();  
			 System.out.println("Successfully updated current Level values");
			
		}
		catch(Exception e){			
		
		}
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
				
					e.printStackTrace();
				}
			}
			
		}
			String entity_msg = "Successfully updated current Level values" ; 

		return Response.status(200).entity("{\"status\":\"Successfully updated current Level values\"}").build();

	
	}
	
	
	@GET
	@Path("/cancelStudent/{login}/activityCode/{activityCode}/{zone}")
	public Response cancelStudent(@PathParam("login") String login,@PathParam("activityCode") String activityCode,@PathParam("zone") String zone){
	   
		 
		System.out.println("inside1 cancel student");
		 Connection connection = null;
		 String status=null;		
		try
		{
					
			connection = getConnection(zone);
			 CallableStatement stmt=connection.prepareCall("{call STUDENT_CANCELLATION(?,?,?)}"); 
			 stmt.setString(1,login);
			 stmt.setString(2,activityCode);
		stmt.registerOutParameter(3, Types.VARCHAR);	
			    boolean hadResults = stmt.execute();			 
			 status=stmt.getString(3);
		
			
		}
		catch(Exception e){			
		
		}
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
				
					e.printStackTrace();
				}
			}
			
		}		

		return Response.status(200).entity("{\"status\":\""+status+"\"}").build();

	
	}
	
	

	public Connection getConnection(String zone) throws ClassNotFoundException,
			SQLException {
		
		Connection con=null;
		try {
			if(zone.equals("z1")){
			con= ConnectionPoolDataSourceZone1.getInstance().getConnection();
			}
			else
			{
				con= ConnectionPoolDataSourceZone2.getInstance().getConnection();
			}
		} catch (IOException e) {
		
			e.printStackTrace();
		} catch (PropertyVetoException e) {
	
			e.printStackTrace();
		}
//		Connection connection;
//		Class.forName("oracle.jdbc.driver.OracleDriver");
//		System.out.println("----------zone---------"+zone);
//		//connection = DriverManager.getConnection("jdbc:oracle:thin:@dn1udwsexodb10-scan.pearsontc.com:1521/RACZ1", "bender", "vA8apru3");
//		if(zone.equals("z1")){
//		connection = DriverManager.getConnection("jdbc:oracle:thin:@dn1upwsexodb10-scan.pearsontc.com:1521/zone1", "bender", "vA8apru3");
//		}
//		else{
//		connection = DriverManager.getConnection("jdbc:oracle:thin:@dn1upwsexodb20-scan.pearsontc.com:1521/zone2", "bender", "vA8apru3");
//		}
	return con;
	}

	@GET
	@Path("/insert/{message}/{startDate}/{endDate}/{centerID}/{masterID}/{zone}")
	public Response insertMessage(@PathParam("message") String message,@PathParam("startDate") String startDate,@PathParam("endDate") String endDate,@PathParam("zone") String zone) {
	
		
		 java.sql.Date sqlStartDate =null;
		 java.sql.Date sqlEndDate=null; 		 
		 SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");    
		 int no_insert_records=0;
		 Connection connection = null;
	        try {
	        	
			 sqlStartDate = new Date(formatter.parse(startDate).getTime());
			 sqlEndDate = new Date(formatter.parse(endDate).getTime()); 
		
			} catch (ParseException e1) {				
				e1.printStackTrace();
			}
		
		try
		{
					
				        connection = getConnection(zone);
				
				        UUID idOne = UUID.randomUUID();
				        System.out.println("bet");
				    //    SourceConnection = DriverManager.getConnection(sourcedbURL, sourceUser, sourcePassword);
				        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");				      			        
				        PreparedStatement ps = connection.prepareStatement("INSERT INTO SDS_MESSAGE_PANEL (MESSAGEID,MESSAGE,STARTDATE,ENDDATE,CREATED_DATE)"
				        		+ " VALUES ('"+idOne.toString().substring(0, 18)+"','"+message+"','"+sdf.format(sqlStartDate)+"','"+sdf.format(sqlEndDate)+"','"+sdf.format(new java.sql.Date(Calendar.getInstance().getTimeInMillis()))+"')");
						
//						ps.setString(1,message);
//						ps.setDate(2,'01-Jul-2017');
//						ps.setDate(3,sqlEndDate);
//						ps.setDate(4,sqlEndDate);//						
						//ps.setString(6,centerID);
						//ps.setString(7,territoryID);
				    
				      no_insert_records= ps.executeUpdate();
		}
		catch(Exception e){			
		  e.printStackTrace();
		    no_insert_records = 0 ;
		}
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
			String entity_msg = no_insert_records + " records are inserted." ; 

		return Response.status(200).entity(entity_msg).build();

	}
	
	
	
	String getIDByLogin(String studentLogin,String zone){
		String idValue="";
		Connection con=null;
		try {
			 con=getConnection(zone);			
			ResultSet rs=con.createStatement().executeQuery("select id from dps_user where login='"+studentLogin+"'");			
			if(rs.next())
			{
				
				idValue=rs.getString("id");
			}			
			
		} catch ( Exception e) {
		
			e.printStackTrace();
		}
		finally{
			if(con!=null)
			{
				try {
					con.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		return idValue;
		
	}
	@GET
	@Path("/studentprofilesync/{login}/{zone}")
	public Response insertMessage(@PathParam("login") String login,@PathParam("zone") String zone) {
	
		 Connection connection = null;
		 String sbMessage="";
		 String studentID;
			String[] myList = {"C:\\forcesyncconfig\\"};
		try
		{
					
			connection = getConnection(zone);				
			 SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");				      			        
			 PreparedStatement ps = connection.prepareStatement("INSERT INTO SSDS_NSE_FORCE_SYNC_QUEUE(sync_type,sync_id) values (?,?)");
				ps.setString(1,"STUDENTPROFILE");
				ps.setString(2,getIDByLogin(login,zone));
			int i=ps.executeUpdate();
			System.out.println("Record(s) inserted----"+i);
			ForceSyncUtility fu= new ForceSyncUtility();
			 sbMessage=fu.contolPanelSync(myList);		
			
		}
		catch(Exception e){			
		  e.printStackTrace();
		  
		}
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		return Response.status(200).entity(sbMessage).build();

	}
	
	
	@GET
	@Path("/reassignConsultant/{login}/{zone}")
	public Response reassignConsultant(@PathParam("login") String login,@PathParam("zone") String zone) {
	
		 Connection connection = null;
		 String sbMessage="";
		 String studentID;
			String[] myList = {"C:\\forcesyncconfig\\"};
		try
		{
					
			connection = getConnection(zone);				
            System.out.println("--login----"+login);
			String sqlQuery="select id from sds_contract where consultant_ref in (select id from dps_user where login=?)";
			System.out.println("-----sqlQuery------"+sqlQuery);
			PreparedStatement ps1 = connection.prepareStatement("INSERT INTO SSDS_NSE_FORCE_SYNC_QUEUE(sync_type,sync_id) values (?,?)");
			 PreparedStatement stmt=connection.prepareStatement(sqlQuery);  
			 stmt.setString(1,login);
			 ResultSet rs=stmt.executeQuery(); 			 
			
			
				while(rs.next())
				{			
					       ps1.setString(1,"CONTRACT");
							ps1.setString(2,rs.getString("id"));	
							System.out.println("-----------rs.getString(id)---------"+rs.getString("id"));
						ps1.addBatch();
				}
			
				int[] i=ps1.executeBatch();
				System.out.println(i);
				connection.commit();
			System.out.println("Record(s) inserted----"+i);
			ForceSyncUtility fu= new ForceSyncUtility();
			 sbMessage=fu.contolPanelSync(myList);	
			 System.out.println("----------------------"+sbMessage);
			
		}
		catch(Exception e){			
		  e.printStackTrace();
		  
		}
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		return Response.status(200).entity(sbMessage).build();

	}
	
	
	@GET
	@Path("/reassignPT/{login}/{zone}")
	public Response reassignPT(@PathParam("login") String login,@PathParam("zone") String zone) {
	
		 Connection connection = null;
		 String sbMessage="";
		 String studentID;
			String[] myList = {"C:\\forcesyncconfig\\"};
		try
		{
					
			connection = getConnection(zone);				
            System.out.println("--login----"+login);
			String sqlQuery="select id from sds_contract where lab_teacher_ref in (select id from dps_user where login=?)";
			System.out.println("-----sqlQuery------"+sqlQuery);
			PreparedStatement ps1 = connection.prepareStatement("INSERT INTO SSDS_NSE_FORCE_SYNC_QUEUE(sync_type,sync_id) values (?,?)");
			 PreparedStatement stmt=connection.prepareStatement(sqlQuery);  
			 stmt.setString(1,login);
			 ResultSet rs=stmt.executeQuery(); 			 
			
			
				while(rs.next())
				{			
					       ps1.setString(1,"CONTRACT");
							ps1.setString(2,rs.getString("id"));	
							System.out.println("-----------rs.getString(id)---------"+rs.getString("id"));
						ps1.addBatch();
				}
			
				int[] i=ps1.executeBatch();
				System.out.println(i);
				connection.commit();
			System.out.println("Record(s) inserted----"+i);
			ForceSyncUtility fu= new ForceSyncUtility();
			 sbMessage=fu.contolPanelSync(myList);	
			 System.out.println("----------------------"+sbMessage);
			
		}
		catch(Exception e){			
		  e.printStackTrace();
		  
		}
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		return Response.status(200).entity(sbMessage).build();

	}
	
	
	
	@GET
	@Path("/studentcontractsync/{login}/{zone}")
	public Response insertContractMessage(@PathParam("login") String login,@PathParam("zone") String zone) {
		
	
		 Connection connection =null;
	
		 String sbMessage="";
		 String studentID=getIDByLogin(login,zone);
			String[] myList = {"C:\\forcesyncconfig\\"};
	
			 PreparedStatement ps;
			try {
				try {
					connection= getConnection(zone);
				
				} catch (ClassNotFoundException e1) {
			
					e1.printStackTrace();
				}
			
				ps = connection.prepareStatement("select id from sds_contract where student_ref=?");
				connection.setAutoCommit(false);
				ps.setString(1,studentID);
			
				ResultSet rs=ps.executeQuery();
				PreparedStatement ps1 = null;
			
				 ps1 = connection.prepareStatement("INSERT INTO SSDS_NSE_FORCE_SYNC_QUEUE(sync_type,sync_id) values (?,?)");
				while(rs.next())
				{			
			
					ps1.setString(1,"CONTRACT");
							ps1.setString(2,rs.getString("id"));
				
						ps1.addBatch();
						System.out.println("true");
					
					
				}
			
				int[] i=ps1.executeBatch();
				connection.commit();

			} catch (SQLException e1) {
				try {
					connection.rollback();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				e1.printStackTrace();
			}
			finally{
				if(connection!=null)
				{
					try {
						connection.close();
					} catch (SQLException e) {
					
						e.printStackTrace();
					}
				}
				
			}		

			
		
		
			ForceSyncUtility fu= new ForceSyncUtility();
			 sbMessage=fu.contolPanelSync(myList);	
	
	
		
		return Response.status(200).entity(sbMessage).build();

	}
	
	
	@GET
	@Path("/get/messageID/{messageID}/{zone}")
	public Response getMessage(@PathParam("messageID") String messageID,@PathParam("zone") String zone) {
		
		Connection connection = null;
		MessageNotification mn=null;
	
		try{
        connection = getConnection(zone);
        PreparedStatement ps = connection.prepareStatement("select * from sds_message_panel where messageid='"+messageID+"'");
      ResultSet rs=ps.executeQuery();
      
      
      while(rs.next()){
    	  
    	 mn= new MessageNotification();
    	 mn.setMessageID(rs.getString("messageid"));
    	  mn.setMessage(rs.getString("message"));
    	  mn.setStartDate(rs.getString("startdate"));
      mn.setEndDate(rs.getString("enddate"));
      }
		
        }
        catch(Exception e){
        	
        }
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
				
					e.printStackTrace();
				}
			}
			
		}		

        System.out.println(mn);
        Gson gson=new Gson();
        String jsonInString = gson.toJson(mn);
        
        return Response.status(200).entity(jsonInString).build();
	 
		 

	}
	
	
	
	@GET
	@Path("/getDetailsByLogin/{login}/{zone}")	
	public Response getDetailsByLogin(@PathParam("login") String login,@PathParam("zone") String zone) {
		
		Connection connection = null;
		StudentDetails sd=null;
		StudentContract sd1=null;
		
		try{
        connection = getConnection(zone);
        PreparedStatement ps = connection.prepareStatement("select du.id id,du.login as login,du.password as password,email,first_name,last_name, es.center_ref as center_ref, nvl(es.valid_contract_ref,'NOT VALID') as Status from dps_user du,ewsi_student es where du.login='"+login+"' and du.id=es.id");
        ResultSet rs=ps.executeQuery();
      
      
      while(rs.next()){
    	  
    	 sd= new StudentDetails();
    	 sd.setId(rs.getString("id"));
    	 sd.setLogin(rs.getString("login"));
    	 sd.setPassword(rs.getString("password"));
    	 sd.setCenterID(rs.getString("center_ref"));
    	 sd.setStatus(rs.getString("Status"));
    	 sd.setEmail(rs.getString("email"));
    	 sd.setFirstname(rs.getString("first_name"));
    	 sd.setLastname(rs.getString("last_name"));
      }
   //   PreparedStatement ps1 = connection.prepareStatement("select sc.id,contract_end_type_ref,is_valid_contract from sds_contract sc,dps_user du where sc.student_ref=du.id and du.login='"+login+"'");
      PreparedStatement ps1 = connection.prepareStatement("select sc.id,sc.center_ref,sc.start_level,sc.end_level,sc.product_type,sc.start_date,sc.end_date,sc.extension_date,sc.real_end_date,contract_end_type_ref,is_valid_contract from sds_contract sc,dps_user du where sc.student_ref=du.id and du.login='"+login+"'");
      ResultSet rs1=ps1.executeQuery();
     
      
      while(rs1.next()){
    	  
    	 sd1= new StudentContract();
    	 sd1.setId(rs1.getString("id"));
    	 sd1.setContractEndType(rs1.getString("contract_end_type_ref"));
    	 sd1.setIsValidContract(rs1.getString("is_valid_contract"));
      	 sd1.setCenterID(rs1.getString("center_ref"));
    	 sd1.setStartLevel(rs1.getString("start_level"));
    	 sd1.setEndLevel(rs1.getString("end_level"));
    	 sd1.setProductType(rs1.getString("product_type"));
    	 sd1.setStartDate(rs1.getDate("start_date"));
    	 sd1.setEndDate(rs1.getDate("end_date"));
    	 sd1.setRealEndDate(rs1.getDate("real_end_date"));
    	 sd1.setExtensionDate(rs1.getDate("extension_date"));
    	 sd.studentContracts.add(sd1);

      }
      PreparedStatement ps2 = connection.prepareStatement("select sc.student_ref,sc.center_from_ref,sc.center_to_ref,sc.student_transfer_date,sc.status_transfer,sc.request_date  from sds_student_transfer sc,dps_user du where sc.student_ref=du.id and du.login='"+login+"'");
      ResultSet rs2=ps2.executeQuery();
      while(rs2.next()){
    	  StudentTransfer st= new StudentTransfer();    
    	st.setCenterTo(rs2.getString("center_to_ref"));
    	st.setCenterFrom(rs2.getString("center_from_ref"));
    	st.setTransferStatus(rs2.getString("status_transfer"));
    	st.setTransferDate(rs2.getDate("student_transfer_date").UTC(rs2.getDate("student_transfer_date").getYear(), rs2.getDate("student_transfer_date").getMonth(), rs2.getDate("student_transfer_date").getDate(), 0, 0, 0));
    	st.setRequestDate(rs2.getDate("request_date").UTC(rs2.getDate("request_date").getYear(), rs2.getDate("request_date").getMonth(), rs2.getDate("request_date").getDate(), 0, 0, 0));
    	 sd.studentTransfer.add(st);
       }
      
      PreparedStatement ps4 = connection.prepareStatement("select sc.student_ref,sc.center_from_ref,sc.center_to_ref,sc.student_transfer_date,sc.status_transfer,sc.request_date  from sds_student_transfer sc,dps_user du where sc.student_ref=du.id and du.login='"+login+"'");
      ResultSet rs4=ps4.executeQuery();
      
      while(rs4.next()){
    	  StudentTransfer st= new StudentTransfer();    
    	st.setCenterTo(rs2.getString("center_to_ref"));
    	st.setCenterFrom(rs2.getString("center_from_ref"));
    	st.setTransferStatus(rs2.getString("status_transfer"));
    	st.setTransferDate(rs2.getDate("student_transfer_date").UTC(rs2.getDate("student_transfer_date").getYear(), rs2.getDate("student_transfer_date").getMonth(), rs2.getDate("student_transfer_date").getDate(), 0, 0, 0));
    	st.setRequestDate(rs2.getDate("request_date").UTC(rs2.getDate("request_date").getYear(), rs2.getDate("request_date").getMonth(), rs2.getDate("request_date").getDate(), 0, 0, 0));
    	 sd.studentTransfer.add(st);
       }
      


        }
        catch(Exception e){
        	
        }
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
				
					e.printStackTrace();
				}
			}
			
		}		

      
        Gson gson=new Gson();
        String jsonInString = gson.toJson(sd);
        
        return Response.status(200).entity(jsonInString).build();

	 
		 
	}
	
	@GET
	@Path("/getContractDetailsByLogin/{login}/{zone}")
	@Produces(MediaType.APPLICATION_JSON)
	public List getContractDetailsByLogin(@PathParam("login") String login,@PathParam("zone") String zone) {
		
		Connection connection = null;
		StudentContract sd=null;
		ArrayList scList= new ArrayList();
	
		try{
        connection = getConnection(zone);
        PreparedStatement ps = connection.prepareStatement("select sc.id,sc.center_ref,sc.start_level,sc.end_level,sc.product_type,sc.start_date,sc.end_date,sc.extension_date,sc.real_end_date,contract_end_type_ref,is_valid_contract from sds_contract sc,dps_user du where sc.student_ref=du.id and du.login='"+login+"'");
      ResultSet rs=ps.executeQuery();
      
      
      while(rs.next()){
    	  
    	 sd= new StudentContract();
    	 sd.setId(rs.getString("id"));
    	 sd.setContractEndType(rs.getString("contract_end_type_ref"));
    	 sd.setIsValidContract(rs.getString("is_valid_contract"));
    	 sd.setCenterID(rs.getString("center_ref"));
    	 sd.setStartLevel(rs.getString("start_level"));
    	 sd.setEndLevel(rs.getString("end_level"));
    	 sd.setProductType(rs.getString("product_type"));
    	 sd.setStartDate(rs.getDate("start_date"));
    	 sd.setEndDate(rs.getDate("end_date"));
    	 sd.setRealEndDate(rs.getDate("real_end_date"));
    	 sd.setExtensionDate(rs.getDate("extension_date"));
    	 scList.add(sd);

      }
		
        }
        catch(Exception e){
        	
        }
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
				
					e.printStackTrace();
				}
			}
			
		}		

return scList;
		 
	}
	
	@GET
	@Path("/activateORContract/{login}/{aycl}/{sdNO}/{zone}")
	public Response activateORContract(@PathParam("login") String login,@PathParam("aycl") String aycl, @PathParam("sdNO") String sdNO,@PathParam("zone") String zone){
		   
		 
			 Connection connection = null;
			 String status=null;
			
			try
			{
						
				 connection = getConnection(zone);
				 CallableStatement stmt=connection.prepareCall("{call BENDER.ACTIVATE_OR_CONTRACT(?,?,?,?)}");  	
				 stmt.setString(1,login);
				 stmt.setString(2,aycl);
				 stmt.setString(3,sdNO);
				 stmt.registerOutParameter(4, Types.VARCHAR);					
			  stmt.execute();			 
			     status=stmt.getString(4);
			
		
				
			}
			catch(Exception e){			
			e.printStackTrace();
			}
			finally{
				if(connection!=null)
				{
					try {
						connection.close();
					} catch (SQLException e) {
					
						e.printStackTrace();
					}
				}
				
			}
				String entity_msg = "{\"status1\":\""+status+"\"}" ; 

			//return Response.status(200).entity("{\"status\":\"Successfully updated current Level values\"}").build();
			return Response.status(200).entity(entity_msg).build();

		
		}
	
	
	@GET
	@Path("/centerslist/{zone}")
	public List getCenters(@PathParam("zone") String zone){
		List cl= new ArrayList();
		Connection connection=null;
		try{
		connection=getConnection(zone);
		String sqlQuery="select id,name from ewsi_center";
		 PreparedStatement stmt=connection.prepareStatement(sqlQuery);  
		
		 ResultSet rs=stmt.executeQuery(); 			 
		 while(rs.next()){  
			
			 CenterData eb= new CenterData();
			 eb.setId(rs.getString("id"));
			 eb.setName(rs.getString("name"));
			 cl.add(eb);
			 }  
		 System.out.println("Successfully retrived Data");	
		
	}
	catch(Exception e){			
	e.printStackTrace();
	}
	finally{
		if(connection!=null)
		{				try {
				connection.close();
			} catch (SQLException e) {				
				e.printStackTrace();
			}
		}			
	}
	return cl;	
		 

	}
	@GET
	@Path("/delete/{messageID}/{zone}")
	public Response deleteMessage(@PathParam("messageID") String messageID,@PathParam("zone") String zone){
		Connection connection = null;
		MessageNotification mn=null;
		int no_insert_records=0;

		try{
        connection = getConnection(zone);
     
        PreparedStatement ps = connection.prepareStatement("delete from sds_message_panel where messageid='"+messageID+"'");
        no_insert_records=ps.executeUpdate();
        
       
      
        }
        catch(Exception e){
        	e.printStackTrace();
        	
        }
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
				
					e.printStackTrace();
				}
			}
			
		}		

	      
	      String entity_msg = no_insert_records + " records are deleted." ; 
        
        return Response.status(200).entity(entity_msg).build();
	 
		 

	}
	
	@GET
	@Path("/update/{messageID}/{startDate}/{endDate}/{message}/{zone}")
	public Response updateMessage(@PathParam("messageID") String messageID,@PathParam("message") String message,@PathParam("startDate") String startDate,@PathParam("endDate") String endDate,@PathParam("zone") String zone) {
		
		Connection connection = null;
		MessageNotification mn=null;
		int no_insert_records=0;
		 java.sql.Date sqlStartDate =null;
		 java.sql.Date sqlEndDate=null; 		 
		 SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy"); 
		  SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");	
		   try {
	        	
				 sqlStartDate = new Date(formatter.parse(startDate).getTime());
				 sqlEndDate = new Date(formatter.parse(endDate).getTime()); 		
			
				} catch (ParseException e1) {				
					e1.printStackTrace();
				}

		try{
			
			System.out.println("testing 123456");
        connection = getConnection(zone);
        System.out.println("testing 678");
        String sqlString1="update sds_message_panel set startdate='"+sdf.format(sqlStartDate)+"', enddate='"+sdf.format(sqlEndDate)+"', message='"+message+"' where messageid='"+messageID+"'";
        PreparedStatement ps = connection.prepareStatement(sqlString1);
        
        System.out.println(sqlString1);
        System.out.println("testing 8910");
        no_insert_records=ps.executeUpdate();
        

      
        }
        catch(Exception e){
        	e.printStackTrace();
        	
        }
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
				
					e.printStackTrace();
				}
			}
			
		}		

	      
	      String entity_msg = no_insert_records + " records are updated." ; 
        
        return Response.status(200).entity(entity_msg).build();
	 
		 

	}
	
	
	// save uploaded file to new location
		private void writeToFile(InputStream uploadedInputStream,
			String uploadedFileLocation) {

			try {
				OutputStream out = new FileOutputStream(new File(
						uploadedFileLocation));
				int read = 0;
				byte[] bytes = new byte[1024];

				out = new FileOutputStream(new File(uploadedFileLocation));
				while ((read = uploadedInputStream.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				out.flush();
				out.close();
			} catch (IOException e) {

				e.printStackTrace();
			}

		}
	@GET
	@Path("/messages/{zone}")
	public Response getAllMessages(@PathParam("zone") String zone) {
		
		Connection connection = null;
		MessageNotification mn=null;
		 ArrayList<MessageNotification> msgNotifications= new ArrayList<MessageNotification>();
	
		try{
			
        connection = getConnection(zone);
        PreparedStatement ps = connection.prepareStatement("select * from sds_message_panel");
        ResultSet rs=ps.executeQuery();     
      
      while(rs.next()){
    	  
    	  mn= new MessageNotification();
    	  mn.setMessageID(rs.getString("messageid"));
    	  mn.setMessage(rs.getString("message"));
    	  mn.setStartDate(rs.getString("startdate"));
          mn.setEndDate(rs.getString("enddate"));
          msgNotifications.add(mn);
      }
		
        }
        catch(Exception e){
        	
        }
		finally{
			if(connection!=null)
			{
				try {
					connection.close();
				} catch (SQLException e) {
				
					e.printStackTrace();
				}
			}
			
		}		

        Gson gson=new Gson();
        String jsonInString = gson.toJson(msgNotifications);
        
        return Response.status(200).entity(jsonInString).header("Access-Control-Allow-Origin", "*").build();
       // return Response.ok(output).header("Access-Control-Allow-Origin", "*").build();
	}

	static class MessageNotification{
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public String getStartDate() {
			return startDate;
		}
		public void setStartDate(String startDate) {
			this.startDate = startDate;
		}
		public String getEndDate() {
			return endDate;
		}
		public void setEndDate(String endDate) {
			this.endDate = endDate;
		}
		String message;
		String messageID;
		public String getMessageID() {
			return messageID;
		}
		public void setMessageID(String messageID) {
			this.messageID = messageID;
		}
		String startDate;
		String endDate;
	}
	
	static public class StudentDetails{
	
		String id;
		String status;
		String email;
		String firstname;
		
		ArrayList studentTransfer =new ArrayList();
		ArrayList studentContracts=new ArrayList();
	
			public String getFirstname() {
			return firstname;
		}
		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}
		public String getLastname() {
			return lastname;
		}
		public void setLastname(String lastname) {
			this.lastname = lastname;
		}
		String lastname;
		public String getEmail() {
			return email;
		}
		public void setEmail(String email) {
			this.email = email;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getLogin() {
			return login;
		}
		public void setLogin(String login) {
			this.login = login;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public String getCenterID() {
			return centerID;
		}
		public void setCenterID(String centerID) {
			this.centerID = centerID;
		}
		String login;
		String password;
		String centerID;
	}
	
	
	public static class StudentContract{
		
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getContractEndType() {
			return contractEndType;
		}
		public void setContractEndType(String contractEndType) {
			this.contractEndType = contractEndType;
		}
		public String getIsValidContract() {
			return isValidContract;
		}
		public void setIsValidContract(String isValidContract) {
			this.isValidContract = isValidContract;
		}
		String id;
		String contractEndType;
		String isValidContract;
			public String getCenterID() {
			return centerID;
		}
		public void setCenterID(String centerID) {
			this.centerID = centerID;
		}
		public String getStartLevel() {
			return startLevel;
		}
		public void setStartLevel(String startLevel) {
			this.startLevel = startLevel;
		}
		public String getEndLevel() {
			return endLevel;
		}
		public void setEndLevel(String endLevel) {
			this.endLevel = endLevel;
		}
		public Date getStartDate() {
			return startDate;
		}
		public void setStartDate(Date startDate) {
			this.startDate = startDate;
		}
		public Date getEndDate() {
			return endDate;
		}
		public void setEndDate(Date endDate) {
			this.endDate = endDate;
		}
		public Date getExtensionDate() {
			return extensionDate;
		}
		public void setExtensionDate(Date extensionDate) {
			this.extensionDate = extensionDate;
		}
		public Date getRealEndDate() {
			return realEndDate;
		}
		public void setRealEndDate(Date realEndDate) {
			this.realEndDate = realEndDate;
		}
		public String getProductType() {
			return productType;
		}
		public void setProductType(String productType) {
			this.productType = productType;
		}
			String centerID;
			String startLevel;
			String endLevel;
			Date startDate;
			Date endDate;
			Date extensionDate;
			Date realEndDate;
			String productType;
	
	}
	
	
	public static class StudentEncounters{	
		
		String id;
		
		
	}
	
	public static class StudentActivities{		
		
		String activityDate;
		
		String studentID;
		String activityCode;
		String activityID;
		String  cancelledByStudent;
		String  cancelledByCenter;
		String wasAttended;
	
	}
	
	public static class StudentTransfer{		
	
		String centerFrom;
		public String getCenterFrom() {
			return centerFrom;
		}
		public void setCenterFrom(String centerFrom) {
			this.centerFrom = centerFrom;
		}
		public String getCenterTo() {
			return centerTo;
		}
		public void setCenterTo(String centerTo) {
			this.centerTo = centerTo;
		}
		public long getTransferDate() {
			return transferDate;
		}
		public void setTransferDate(long transferDate) {
			this.transferDate = transferDate;
		}
		public String getTransferStatus() {
			return transferStatus;
		}
		public void setTransferStatus(String transferStatus) {
			this.transferStatus = transferStatus;
		}
		public long getRequestDate() {
			return requestDate;
		}
		public void setRequestDate(long requestDate) {
			this.requestDate = requestDate;
		}
		public String getStudentID() {
			return studentID;
		}
		public void setStudentID(String studentID) {
			this.studentID = studentID;
		}
		String centerTo;
		long transferDate;
		String transferStatus;
		long requestDate;
		String studentID;
	
	}
	
	static class ReleaseNotification{
	
		String id ;
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		String description;
	}
	
	static class EmployeesBean{
		
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getLogin() {
			return login;
		}
		public void setLogin(String login) {
			this.login = login;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		String id ;
	String login;
	String password;
	String categoryRef;
	public String getCategoryRef() {
		return categoryRef;
	}
	public void setCategoryRef(String categoryRef) {
		this.categoryRef = categoryRef;
	}
	
	}
static class CenterData{
	
		
		public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
		String id ;
	String name;

	
	
	}

}





