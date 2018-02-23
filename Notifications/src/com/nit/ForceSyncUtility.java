package com.nit;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.codehaus.jackson.map.ObjectMapper;

public class ForceSyncUtility {
	static String zone;
	static String opType;
	static String sbURL;
	static String jdbcURL;
	static Properties config;
	final static String CONTRACT="CONTRACT";
	final static String STAFFPROFILE="STAFFPROFILE";
	final static String BOOKMARK="BOOKMARK";
	final static String STUDENTPROFILE="STUDENTPROFILE";
	final static String ACTIVITY="ACTIVITY";
	final static String PROSPECT="PROSPECT";
	
	static StringBuilder stringb= null;
	public String contolPanelSync(String[] args)
	{
	
		try {
			main(args);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return stringb.toString();
	}
	public  static final List<String> NSE_LEVELS = Arrays.asList("L1", "L2",
			"L3", "L4", "L5", "L6", "L7", "L8", "L9", "L10", "L11", "L12",
			"L13", "L14", "L15", "L16", "L17", "L18", "L19", "L20");
	static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	static List zonelist=new ArrayList();
	static Map  objMap =new HashMap();
	static {
		zonelist.add("z1");
		zonelist.add("z2");
		objMap.put(CONTRACT,new ContractSync());		
		objMap.put(STAFFPROFILE,new StaffSync());
		objMap.put(BOOKMARK,new BookmarkSync());
		objMap.put(STUDENTPROFILE,new StudentSync());
		objMap.put(ACTIVITY,new ActivitySync());
		objMap.put(PROSPECT,new ProspectSync());
	}
	static String path="C:\\forcesyncconfig";

	
	public static void main(String[] args) throws IOException {
		if(args==null || args.length==0) {
			stringb.append("\r\nFirst argument can be the "
					+ "path of Config/SQL files(default value of argument is C:\\forcesyncconfig\\)");
			
		}
		if(args.length==1) {
			path =args[0];
		}
		stringb=	new StringBuilder();
		
	//	stringb.append("Selected path of config is  "+path);
		config=new Properties();
		config.load(new FileInputStream(path+"/forcesynconfig.properties"));
		jdbcURL=config.getProperty("z1");
		zone="z1";
		sbURL= config.getProperty("sbURL");
		startSync();
		zone="z2";
		jdbcURL=config.getProperty("z2");
		startSync();
	}
	public static void startSync() {
		long l1=System.currentTimeMillis();
		java.sql.Connection conn = null;
		
		try {
	  		String DBURL ="jdbc:oracle:thin:@"+jdbcURL; 
	        //properties for creating connection to Oracle database
	        Properties props = new Properties();
	        props.setProperty("user", config.getProperty("user"));
	        props.setProperty("password",config.getProperty("password"));
	        conn = DriverManager.getConnection(DBURL,props);
	        process(conn);
     	}
		catch(Exception w) {
			w.printStackTrace();
			
		}
	 	finally {
			if(conn!=null) {
				try {
				//	stringb.append("\r\nClosing DB connection for "+zone);
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
		}

        long l2=System.currentTimeMillis();
       //	stringb.append("Zone "+zone +" sync completed in "+(l2-l1)/(1000) +" seconds");
     }
	 public static void process(java.sql.Connection conn) {
		 PreparedStatement ps=null;
		 ResultSet rs=null;
		 try {
			ps=conn.prepareStatement(config.getProperty("deleteSyncedRecordQuery"));
			String id=null;
			int i=0;
			rs=conn.createStatement().executeQuery(config.getProperty("getSyncRecordQuery"));
			List sbObjList=null;
			Map objListMap=new HashMap();
			while(rs.next()) {
				id=rs.getString(1);
				String type=rs.getString(2)==null?"":rs.getString(2);
				type=type.toUpperCase().trim();
				if(!objMap.containsKey(type) || id==null) {
					stringb.append("Invalid Sync Type record.Ignoring the record "+id + " "+type);
					continue;
				}
			    Sync s=(Sync)objMap.get(type);
			    sbObjList =(List)objListMap.get(s.getTopicName());
			    if(sbObjList==null) {
			    	sbObjList=new ArrayList();
			    	objListMap.put(s.getTopicName(),sbObjList);
			    }	
			    sbObjList.add(s.getData(conn,id));
			    ps.setString(1,id);
			    ps.addBatch();
			    i++;
			}
			sendToSB(objListMap);
			ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(rs!=null) {
				try {
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			}
			if(ps!=null) {
				try {
					ps.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			}
			
		}
		 
	 }
	 public static String toJson(Object input) throws Exception {
		 ObjectMapper mapper = new ObjectMapper();
		 try {
			 String message = mapper.writeValueAsString(input);
			 return message;
		 } catch (Exception e) {
			 e.printStackTrace();
		 } 
		 return "";
	 }

	 public static void sendToSB(Map objMap) throws Exception {
		Iterator keyItr =objMap.keySet().iterator();
		int finalCount=0;
		while(keyItr.hasNext()) {
			finalCount++;
			String topicName =(String)keyItr.next();
			ServiceBus s=new ServiceBus();
		    s.init(sbURL);
		    List sbObjList=(List)objMap.get(topicName);
		    Iterator objItr=sbObjList.iterator();
		    int i=1;
		    s.open(topicName);
			while(objItr.hasNext()) {
				 SBObject sb=(SBObject)objItr.next();
				 if(sb==null) {
					 continue;
				 }
				 sb.modifyData();
				 stringb.append("\r\n("+i+")------"+sb.masterRef+": \r\n"+toJson(sb));
				 s.send(toJson(sb),zone,sb.masterRef);
				 Thread.sleep(1000);
				 i++;
				 
			}
		    s.close();
	  	    stringb.append("\r\nZone "+zone+":"+ (i-1)+ " message(s) sent to SB "+topicName);
		}	
		if(finalCount==0) {
			stringb.append("\r\n==================================================================================================================="
					+ "=================================\r\nNo record(s) to sync in zone "+zone);
		}	
	 }
	 
}	 



final class ServiceBus{
	private static ConnectionFactory CF;
	public void init(String url){
		if(url == null){
			return;
		}
		CF = new JmsConnectionFactory(url);
	}
	private Connection conn;
	private MessageProducer mp;
	private Session session;
	private String endPoint;
	public ServiceBus open(String topic) throws JMSException{
		conn = CF.createConnection();
		session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Topic t = session.createTopic("migration");
		mp = session.createProducer(t);
		this.endPoint = topic;
		return this;
	}
	public void send(String msg,String zone,String masterRef) throws JMSException{
		BytesMessage m = session.createBytesMessage();
		m.setStringProperty("event", endPoint);
		m.setStringProperty("PublisherName","ssds");
		m.setStringProperty("MasterRef",masterRef);
		m.setStringProperty("zone",zone);//TODO: change for z2
		
		m.writeBytes(msg.getBytes());
		mp.send(m);
	}
	public void close() throws JMSException{
		if(mp!=null) mp.close();
		if(session!=null ) session.close();
		if(conn!=null) conn.close();
	}
}

abstract class Sync {

	final public SBObject getData(java.sql.Connection conn,String id)
				throws Exception {
	    String sql="";
		ResultSet rs=null;
		PreparedStatement ps=null;
		try {
			ps=conn.prepareStatement(getSQL());
			ps.setString(1, id);
			rs=ps.executeQuery();		
			return processResult(rs);
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(rs!=null) rs.close();
			if(ps!=null) ps.close();
			
		}
		return null;
	}

	abstract String getTopicName();
	
	 
	abstract public SBObject processResult(ResultSet rs) throws SQLException;
	
	public  String nvl(String val) {
		 if(val==null) return ""; else return val; 
    }
	abstract String getSQL() throws Exception; 
	
	protected  String getSQL(String fileName) throws Exception  {
		String txt = "";
		InputStream in = new FileInputStream(ForceSyncUtility.path+"/"+fileName);
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		String l;
		try {
			while((l = r.readLine())!= null){
				txt += l+" ";
			}
		} catch (IOException e) {
		}
		return txt;
	}
	
	
}
class StaffSync extends Sync {
	public String getSQL() throws Exception{
		return super.getSQL("staffprofile.sql");
	}
	public String getTopicName() {
		return "user-profile";
	}
	
	protected User populateUser(ResultSet rs) throws SQLException {
		User u = null;
		if(rs.next()){
			u = new User();
			u.id = rs.getString("id");
			u.firstName = rs.getString("first_name");
			u.lastName = rs.getString("last_name");
			u.email = nvl(rs.getString("email"));
			u.emailVerified = rs.getBoolean("email_verified");
			Date d = rs.getDate("date_of_birth");
			u.dateOfBirth = d==null? "": ForceSyncUtility.dateFormat.format(d);
			u.login = rs.getString("login");
			u.password = rs.getString("password");
			u.mobilePhone = nvl(rs.getString("mobile_phone"));
			u.gender = rs.getInt("gender") == 2 ? 1 : 2;
			u.workPhone = nvl(rs.getString("work_phone"));
			u.homePhone = nvl(rs.getString("home_phone"));
			u.centerId = rs.getString("center_ref");
			u.masterRef=rs.getString("master_ref");
		}	
		return u;
	
	}
	
	public User processResult(ResultSet rs) throws SQLException {
			User u=populateUser(rs);
			if(u==null) return null;
			int userType=rs.getInt("user_type");
			if(userType!=2) {
				return null;
			}
			u.userType = "staff";
			u.staffActive = "1".equals(rs.getString("is_active"))?true:false;
			u.staffRole = rs.getString("category_ref");			
			return u;
	}
	
	
}
/*{"id":"1f23b025-da7d-4115-80ee-677249d8b69a","firstName":"testing staging 2nd",
 * "lastName":"testing staging 2nd","login":"venkatesh.kadari@pearson.com",
 * "password":"password@123","email":"venkatesh.kadari@pearson.com","centerId":"I187","masterId":"I1"}
 */

class ProspectSync extends Sync {
	public String getSQL() throws Exception{
		return super.getSQL("prospectprofile.sql");
	}
	public String getTopicName() {
		return "user-profile";
	}
	
	protected ProspectUser populateUser(ResultSet rs) throws SQLException {
		ProspectUser u = null;
		if(rs.next()){
			u = new ProspectUser();
			u.id = rs.getString("id");
			u.firstName = rs.getString("first_name");
			u.lastName = rs.getString("last_name");
			u.email = nvl(rs.getString("email"));
			u.login = rs.getString("login");
			u.password = rs.getString("password");
			u.centerId = rs.getString("center_ref");
			u.masterRef=rs.getString("master_ref");
			u.masterId=rs.getString("master_ref");
		}	
		return u;
	
	}
	
	public ProspectUser processResult(ResultSet rs) throws SQLException {
			ProspectUser u=populateUser(rs);
			if(u==null) return null;
			return u;
	}
	
	
}


class StudentSync extends StaffSync {
	public String getSQL() throws Exception{
		return super.getSQL("studentprofile.sql");
	}
	public User processResult(ResultSet rs) throws SQLException {
		User u=populateUser(rs);
		if(u==null) return null;
		int userType=rs.getInt("user_type");
		if(userType!=4) {
			return null;
		}
		u.userType = "student";
		return u;
	}
	
}

class BookmarkSync extends Sync {
	public String getSQL() throws Exception{
		return super.getSQL("bookmark.sql");
	}
	public String getTopicName() {
		return "bookmark";
	}	
	
	 public Bookmark processResult(ResultSet rs) throws SQLException {
			Bookmark l = null;
			if(rs.next()){
				 l = new Bookmark(rs.getString("student_ref"),
						rs.getString("current_level"),rs.getString("next_unit"));
				 l.masterRef=rs.getString("master_ref");
			}
			return l;
		}


	
}

class ContractSync extends Sync {
	
	public  String getSQL() throws Exception{
		return super.getSQL("contractdata.sql");
	}
	public String getTopicName() {
		return "contract-info";
	}	
	public ContractSB processResult(ResultSet rs) throws SQLException{
		
		ContractSB c = null;
		String [] peplevel =new String[3];
		List pepList=new ArrayList();
		int i=0;
		while(rs.next()) {
			String pepLevel=rs.getString("pep_level");
			if(pepLevel!=null && pepLevel.startsWith("PE")) {
				pepList.add(pepLevel);
			}
			if(i>=1) {
				continue;
			}
			i++;
			c = new ContractSB();
			c.contractID = rs.getString("contract_id");
			c.userID = rs.getString("student_ref");
			c.startDate = rs.getString("start_date");
			c.endDate = rs.getString("end_date");
			c.status = rs.getString("status");
			c.center = c.new Center(
					rs.getString("centerid"), 
					rs.getString("id_on_sdsint"),
					rs.getString("center_name"));
			c.masterRef=rs.getString("master_ref");
			c.labTeacher = c.new LabTeacher(
					rs.getString("labteacherID"),
					rs.getString("ltfirst_name"),
					rs.getString("ltlast_name"),
					rs.getString("ltuser_type"));
			c.consultant = c.new Consultant(
					rs.getString("consultantID"),
					rs.getString("ctfirst_name"),
					rs.getString("ctlast_name"),
					rs.getString("ctuser_type"));

			ContractSB.ContractAdditional ca = c.new ContractAdditional();
			ca.AYCL = String.valueOf(rs.getBoolean("AYCL"));
			ca.B2B = String.valueOf(rs.getBoolean("B2B"));
			ca.B2BCourseType = rs.getString("b2b_course_type");
			ca.OnlineClassAccess = String.valueOf(rs
					.getBoolean("HAS_ONLN_CLASS_ACCESS"));
			ca.AllowSelfBooking = String.valueOf(rs
					.getBoolean("IS_BOOKING_ALLOWED"));
			ca.DigitalBookAccess = String.valueOf(rs
					.getBoolean("HAS_DIG_BOOK_ACCESS"));
			ca.EnglishAnytime = String.valueOf(rs
					.getBoolean("HAS_ENGLISH_ANYTIME"));
			c.contractAdditionalDetail = ca;

			String startLevel = rs.getString("start_level");
			String endLevel = rs.getString("end_level");
			int startIndex = ForceSyncUtility.NSE_LEVELS.indexOf(startLevel);
			int endIndex = ForceSyncUtility.NSE_LEVELS.indexOf(endLevel);
			String[] levels = ForceSyncUtility.NSE_LEVELS.subList(startIndex,
					endIndex + 1).toArray(new String[0]);
			c.levels = c.new Levels(levels);
			
			}
			
		    if(!pepList.isEmpty()) {
		    	c.levels.PE=(String[])pepList.toArray(new String[pepList.size()]);
		    }
		return c;
	}
	
	
}
abstract class SBObject  {
	
	
	public void modifyData() {
	}
	public transient String masterRef;
}
	

class ContractSB extends SBObject {		
	public String contractID;
	public String userID;
	public String startDate;
	public String endDate;
	public String status;
	public Center center;
	public LabTeacher labTeacher;
	public Consultant consultant;
	public ContractAdditional contractAdditionalDetail;		
	public Levels levels;	
	
	 class Levels {		
			
		 public Levels(String[] NSELevels){
				this.NSE = NSELevels;
			}
		 
		 
			public String[] NSE;
			public String[] PE=new String[0];
							
			
		}
	
	 class Center {		
		 
		 public Center(String id, String code, String name){
				this.id = id;
				this.code = code;
				this.name = name;
			
			}
			
			public String id;
			public String code;
			public String name;
			
		}
	
	 class LabTeacher {		
		
		 public LabTeacher(String id, String firstName, String lastName, String userType){
				if(id==null) return; 
			 	this.id = id;
				this.firstName = firstName;
				this.lastName = lastName;
				this.userType = userType;
			}
		 
			public String id="";
			public String firstName="";
			public String lastName="";
			public String userType="";				
			
		}
		
	 class ContractAdditional {		
		public ContractAdditional(){
			
		}
		 public ContractAdditional(String AYCL, String B2B, String B2BCourseType, String OnlineClassAccess, String AllowSelfBooking, String DigitalBookAccess, String EnglishAnytime){
				this.AYCL = AYCL;
				this.B2B = B2B;
				this.B2BCourseType = B2BCourseType;
				this.OnlineClassAccess = OnlineClassAccess;
				this.AllowSelfBooking = AllowSelfBooking;
				this.DigitalBookAccess = DigitalBookAccess;
				this.EnglishAnytime = EnglishAnytime;
			}
		 
			public String AYCL;
			public String B2B;	
			public String B2BCourseType = "";
			public String OnlineClassAccess;
			public String AllowSelfBooking;
			public String DigitalBookAccess;
			public String EnglishAnytime;
			
		}
	 
	 class Consultant {		
		 
		 public Consultant(String id, String firstName, String lastName, String userType){
			 	if(id==null) return;
			 	this.id = id;
				this.firstName = firstName;
				this.lastName = lastName;
				this.userType = userType;
			}
			
			public String id="";
			public String firstName="";
			public String lastName="";
			public String userType="";				
			
		}
	
}
class User  extends SBObject {
	public String id;
	public String firstName;
	public String lastName;
	public String email;
	public boolean emailVerified;
	public String dateOfBirth;
	public String login;
	public String password;
	public String homePhone;
	public String mobilePhone;
	public int gender;
	public String workPhone;
	public String centerId;
	public String userType;
	public String staffRole;
	public boolean staffActive;
	
}
class ProspectUser  extends SBObject {
	public String id;
	public String firstName;
	public String lastName;
	public String email;
	public String login;
	public String password;
	public String centerId;
	public String masterId;
}


class Bookmark extends SBObject {
	public Bookmark(String studentId, String currentLevel,String currentUnit) {
		this.studentId = studentId;
		this.currentLevel = currentLevel;
		this.currentUnit = currentUnit;
	}

	public String studentId;
	public String currentLevel;
	public String currentUnit;

}
class ActivityUpdates  extends SBObject  {		
	
		public String centerId;
		public String activityId;
		public String activityType;
		public String activityCode;			
		public String datetimeStart;
		public int duration;
		public String teacherId;
		public Integer activityUnit;
		public boolean isOnline;
		public MaxStudents maxStudents;
		public String description;
		public String[] levels;
		public boolean isClosed;
		public boolean  isCancelled;
		public boolean  B2B;
}
class MaxStudents {		
		public int max;
		public int booked;
}
class MaxStudentsEN extends MaxStudents {
	public int standby;
}


class ActivitySync extends Sync {

	@Override
	String getTopicName() {
		// TODO Auto-generated method stub
		return "center-schedule";
	}

	@Override
	public SBObject processResult(ResultSet rs) throws SQLException {
		ActivityUpdates au = null;
		au=new ActivityUpdates();
		String  activityLevels="";
		int i=0;
		while(rs.next()) {
			String levelGroup=rs.getString("LEVEL_GROUP_REF");
			if(levelGroup!=null) {
				activityLevels+=levelGroup+" ";
			}		  	
			if(i>0) {
				continue;
			}
			i++;
		  	au.activityId=rs.getString("ID");          	
		  	au.activityType=rs.getString("ACTIVITY_TYPE_REF");
		  	au.description=rs.getString("SOCIAL_CLUB_DESCRIPTION");
		  	au.activityCode=rs.getString("ACTIVITY_CODE");
		  	au.isClosed=rs.getInt("IS_CLOSED")==1?true:false;
		  	au.isCancelled=rs.getInt("IS_CANCELLED")==1?true:false;
		    if(au.description!=null && au.description.equalsIgnoreCase("OE") )  {
				    au.isOnline=true;
		    } else {
		         au.isOnline=false;
		    }
		  	
		    if(au.activityType.equalsIgnoreCase("AS") || au.activityType.equalsIgnoreCase("SE")|| 
		    		au.activityType.equalsIgnoreCase("RE"))  {
	  			int slotStart=rs.getInt("SLOT_START");
	  		    int slotEnd=rs.getInt("SLOT_END");
	  		    if(slotEnd-slotStart>1)	{
	  		    	au.duration=60;
	  		    } else {
	  		    	au.duration=30;
	  		    }
	  	        	
	  		} else {
	  		 au.duration=60;
	  		}	  	 	          
	  	
		  	au.centerId=rs.getString("CENTER_REF");
		  	au.masterRef=rs.getString("MASTER_REF");
		  	if("EN".equals(au.activityType)) {
		  		au.maxStudents=new MaxStudentsEN();
		  		((MaxStudentsEN)au.maxStudents).standby=rs.getInt("STANDBY");
		  	} else {
		  		au.maxStudents=new MaxStudents();
		  	}	
		  	au.maxStudents.max=rs.getInt("MAX_PARTICIPANTS");
		  	au.maxStudents.booked=rs.getInt("PARTICIPANTS");
		  	if(rs.getString("UNIT_REF")!=null) {
	  		au.activityUnit=Integer.parseInt(rs.getString("UNIT_REF"))-104;
		  	}
		  	SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		  	au.datetimeStart= DATE_FORMAT.format(rs.getTimestamp("DATE_UTC"));
		  	if(rs.getString("EMPLOYEE_REF")!=null) {
		  		au.teacherId=rs.getString("EMPLOYEE_REF");
		  	}
		  	au.B2B=rs.getInt("B2B")==0?false:true;
		  	
	  	
		}
		//stringb.append(activityLevels);
		if(activityLevels!=null && activityLevels.length()>0) {
			au.levels=activityLevels.split("\\s+");
		}	
		return au;
	}

	@Override
	String getSQL() throws Exception {
			// TODO Auto-generated method stub
		return  super.getSQL("activity.sql");
	}
	
	
}
