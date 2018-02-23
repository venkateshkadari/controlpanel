package com.nit;

	
	import java.beans.PropertyVetoException;
	import java.io.IOException;
	import java.sql.Connection;
	import java.sql.SQLException;
	import com.mchange.v2.c3p0.ComboPooledDataSource;

	public class ConnectionPoolDataSourceZone2 {

	    private static ConnectionPoolDataSourceZone2    datasource;
	    private ComboPooledDataSource cpds;

	    private ConnectionPoolDataSourceZone2() throws IOException, SQLException, PropertyVetoException {
	        cpds = new ComboPooledDataSource();
	        cpds.setDriverClass("oracle.jdbc.driver.OracleDriver");       
	        cpds.setJdbcUrl("jdbc:oracle:thin:@dn1upwsexodb20-scan.pearsontc.com:1521/zone2");  
	        cpds.setUser("bender");
	        cpds.setPassword("vA8apru3");
	        cpds.setMinPoolSize(5);
	        cpds.setAcquireIncrement(5);
	        cpds.setMaxPoolSize(20);
	        cpds.setMaxStatements(180);

	    }

	    public static ConnectionPoolDataSourceZone2 getInstance() throws IOException, SQLException, PropertyVetoException {
	        if (datasource == null) {
	            datasource = new ConnectionPoolDataSourceZone2();
	            return datasource;
	        } else {
	            return datasource;
	        }
	    }

	    public Connection getConnection() throws SQLException {
	        return cpds.getConnection();
	    }

	}


