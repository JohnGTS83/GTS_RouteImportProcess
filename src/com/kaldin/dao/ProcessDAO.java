package com.kaldin.dao;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.kaldin.dto.OperatorDTO;
import com.kaldin.dto.RunUpdateStatusDTO;
import com.kaldin.dto.UserDTO;
import com.s5.common.db.QueryHelper;
import com.s5.util.DateTimeUtil;

import io.swagger.client.model.BusStopDetails;
import io.swagger.client.model.GPSRouteVehicle;
import io.swagger.client.model.Operator;
import io.swagger.client.model.Position;
import io.swagger.client.model.Route;
import io.swagger.client.model.Run;
import io.swagger.client.model.RunPoint;
import io.swagger.client.model.WayPoint;

public class ProcessDAO {

	public boolean deleteOldRuns(String supportedOperators,String runidsUpdated) {
		boolean returnValue = true;
		QueryHelper helper = new QueryHelper();
		try {
			if(StringUtils.isNotEmpty(runidsUpdated)) {
				String sql = "update tdsb_i_run_points set Active = 0, RetireDate = getdate() where route_fk_id in (select id from tdsb_i_route where operator_id in ("+ supportedOperators +") and localroute = 0) and Active = 1 and id not in ("+ runidsUpdated +")";
				System.out.println(sql);
				helper.runQuery(sql);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			returnValue = false;
		} finally {
			helper.closeConnection();
		}
		return returnValue;
	}
	
	public boolean logProcess(int iRecords,boolean bImportStatus,String sErrorMsg,int iVendorId,int iOprEnum) {
		QueryHelper helper = new QueryHelper();
		boolean done = true;
		try {
			String sql = "INSERT INTO tdsb_data_import (created_date,imported_date,record,isImporeted,error_msg,vendor_id,oprEnum) VALUES (getdate(),getdate(),?,?,?,?,?)";
			helper.addParam(iRecords);
			helper.addParam(bImportStatus==true?1:0);
			helper.addParam(sErrorMsg);
			helper.addParam(iVendorId);
			helper.addParam(iOprEnum);
			helper.runQuery(sql);
		} catch (Exception e) {
			e.printStackTrace();
			done = false;
		} finally {
			helper.closeConnection();
		}
		return done;
	}

	public ArrayList<UserDTO> getAllActiveUsers() {
		ArrayList<UserDTO> userList = new ArrayList<UserDTO>();
		QueryHelper helper = new QueryHelper();
		try {
			
			ResultSet rs =  helper.runQueryStreamResults("select id,provider,user_name,user_pass,user_url from tdsb_api where is_active = 1 order by order_number");
			while(rs.next()) {
				UserDTO user = new UserDTO();
				user.setId(rs.getInt("id"));
				user.setUrl(StringUtils.trimToEmpty(rs.getString("user_url")));
				user.setUserName(StringUtils.trimToEmpty(rs.getString("user_name")));
				user.setPassword(StringUtils.trimToEmpty(rs.getString("user_pass")));
				user.setProvider(StringUtils.trimToEmpty(rs.getString("provider")));
				userList.add(user);
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			helper.closeConnection();
		}
		return userList;	
	}
	
	public boolean saveOperator(List<Operator> list, String provider) {
		boolean isInseted = true;
		QueryHelper helper = new QueryHelper();
		int id = 0;
		try {
			ResultSet rs = null;
			String sqlInsert = " INSERT INTO busplanner_operator (name,email,addr1,addr2,operator_id,phone,provider,operatorGuid,displayName) VALUES (?,?,?,?,?,?,?,?,?) ";
			String sqlUpdate = "update busplanner_operator set operatorGuid = ? ,displayName = ? where id = ?";
			for (Operator list1 : list) {
				System.out.println(provider + ": " + list1.toString());
				rs =  helper.runQueryStreamResults("select id,name from busplanner_operator WITH(NOLOCK) WHERE operator_id = '" + list1.getOperatorId() + "' and provider ='" + provider + "'");
				if(rs.next()) {
					id = rs.getInt("id");
				}else
					id = 0;
				rs.close();
				if(id == 0) {
					helper.addParam(StringUtils.trimToEmpty(list1.getName()));
					helper.addParam(StringUtils.trimToEmpty(list1.getEmail()));
					helper.addParam(StringUtils.trimToEmpty(list1.getMailingAddress1()));
					helper.addParam(StringUtils.trimToEmpty(list1.getMailingAddress2()));
					helper.addParam(StringUtils.trimToEmpty(list1.getOperatorId()));
					helper.addParam(StringUtils.trimToEmpty(list1.getPhone()));
					helper.addParam(provider);
					helper.addParam(StringUtils.trimToEmpty(list1.getOperatorGuid()));
					helper.addParam(StringUtils.trimToEmpty(list1.getDisplayName()));
					helper.runQuery(sqlInsert);
				} else {
					helper.addParam(StringUtils.trimToEmpty(list1.getOperatorGuid()));
					helper.addParam(StringUtils.trimToEmpty(list1.getDisplayName()));
					helper.addParam(id);
					helper.runQuery(sqlUpdate);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			isInseted = false;
		} finally {
			helper.closeConnection();
		}
		return isInseted;
	}
	
	public int populateRoute(Route route) {
		QueryHelper helper = new QueryHelper();
		int id = 0;
		boolean isAready = false;
		Date dateTimeLocal = DateTimeUtil.getDefaultDate();
		Date dateTimeModifed =null;
		Date dateTimeCreated = DateTimeUtil.getDefaultDate();
		try {
			
//			2020-11-16 : Mon Nov 16 15:48:47 EST 2020 : 2020-11-16T15:48:47Z
			
			Date dtLocal = DateTimeUtil.getDateFromOffsetDateTime(route.getModifiedDate());
			if(dateTimeLocal.before(dtLocal))
				dateTimeModifed = dtLocal;
			else
				dateTimeModifed = DateTimeUtil.getDefaultDate();
			
			String sql = " INSERT INTO tdsb_i_route (route_name,unit_id,create_date,modified_date,operator_id,description,vehicle_key,route_key) values (?,?,?,?,?,?,?,?) ";
			ResultSet rs =  helper.runQueryStreamResults("SELECT id,modified_date from tdsb_i_route WITH(NOLOCK) WHERE route_key = '" + route.getRouteGuid() + "'");
				if(rs.next()) {
					
					Date dt = new java.util.Date(rs.getTimestamp("modified_date").getTime());
//					System.out.println(dt + " : " + dateTimeModifed + " : " + route.getModifiedDate());
					int diff = dt.compareTo(dateTimeModifed);
					if(diff<0) {
//					if(dateTimeModifed.after(dt)) {
						id = rs.getInt("id");
//						System.out.println("Updated : " + id);
					}
					isAready = true;
				}else {
					id = 0;
					isAready = false;
				}
				rs.close();
				if (isAready == false){ 
					if(dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(route.getCreatedDate())))
						 dateTimeCreated = DateTimeUtil.getDateFromOffsetDateTime(route.getCreatedDate());
					helper.clearParams();
					helper.addParam(route.getRouteID());
					helper.addParam(route.getVehicleID());
					helper.addParam(DateTimeUtil.getDefaultDateString(dateTimeCreated));
					helper.addParam(DateTimeUtil.getDefaultDateString(dateTimeModifed));
					helper.addParam(route.getOperatorID());
					helper.addParam(processText(route.getDescription()));
					helper.addParam(route.getVehicleGuid());
					helper.addParam(route.getRouteGuid());
					long _id = helper.runQueryKey(sql);
					id = (int)_id;
                }else {
                	if(id>0) {
                		sql = " UPDATE tdsb_i_route SET is_delete = 0, route_name= '"+ route.getRouteID() +"',  modified_date = '"+ DateTimeUtil.getDefaultDateString(dateTimeModifed)+"', operator_id = '"+ route.getOperatorID() +"' where id = "+id;
                		helper.clearParams();
                		helper.runQuery(sql);
                	}
                }
		} catch (Exception e) {
			e.printStackTrace();
			id = 0;
		} finally {
			helper.closeConnection();
		}
		return id;
	}
	
	public boolean populateBusStops(List<BusStopDetails> stops,String userName,ArrayList<OperatorDTO> operators,boolean syslog) {
        boolean success = true;
        String sql = "";
        Date dateTimeLocal = DateTimeUtil.getDefaultDate();
        Date dateTimeStart = DateTimeUtil.getDefaultDate();
        Date dateTimeEnd = DateTimeUtil.getDefaultDate();
        Date dateTimeDStart = DateTimeUtil.getDefaultDate();
        Date dateTimeCreated = DateTimeUtil.getDefaultDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
       
        Map<String,Integer> map = new HashMap<String, Integer>();
       
        int modified = 0;
        for(BusStopDetails stop : stops) {
        	if(syslog) {
        		System.out.println(stop.toString());
        	}
//        	if(stop.getBasicInformation().getBusStopID().equalsIgnoreCase("MIDA_014_PU") ) {
//        		System.out.println("UMESH AAWTE");	
//        		System.out.println(stop.toString());
//        	}
        	QueryHelper helper = new QueryHelper();
        	 try {
        		 ResultSet rs = null;
        	boolean saveFlag = false;
        	
			for (OperatorDTO operator : operators) {
				if(stop.getOperatorGuid().equalsIgnoreCase(operator.getOperatorGuid())) {
					saveFlag = true;
		
				}
			}
			if(saveFlag) {
                modified = 0;
                helper.clearParams();
                String frequencyValue = stop.getBasicInformation().getFrequency().getFrequencyString();
				if(map.containsKey(frequencyValue )) {
                	modified = map.get(stop.getBasicInformation().getFrequency().getFrequencyString() );
                } else {
	                sql = "select id,FrequencyString from tdsb_i_Frequency WITH(NOLOCK) where FrequencyString = '" + stop.getBasicInformation().getFrequency().getFrequencyString() + "'";
	                rs = helper.runQueryStreamResults(sql);
	                if (rs.next())
	                {
	                    modified = rs.getInt("id");
	                    map.put(rs.getString("FrequencyString"), modified);
	                } else {
	                    sql = "insert into tdsb_i_Frequency (FrequencyString,FrequencyValue) values ('" + stop.getBasicInformation().getFrequency().getFrequencyString() + "','" + stop.getBasicInformation().getFrequency().getFrequencyString() + "');";
	                    modified = (int) helper.runQueryKey(sql);
	                    map.put(stop.getBasicInformation().getFrequency().getFrequencyString(), modified);
	                }
	                rs.close();
                }
                if (dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(stop.getBasicInformation().getEffectiveDate())))
                {
                    dateTimeStart = DateTimeUtil.getDateFromOffsetDateTime(stop.getBasicInformation().getEffectiveDate());
                }
                else
                    dateTimeStart = dateTimeLocal;

                if (dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime((stop.getBasicInformation().getRetireDate()))))
                {
                    dateTimeEnd = DateTimeUtil.getDateFromOffsetDateTime(stop.getBasicInformation().getRetireDate());
                }
                else
                    dateTimeEnd = dateTimeLocal;

                helper.clearParams();
                sql = "select bsk.id,bsk.RunID,bsk.StartTime,det.ModifiedDate,bsk.EfectiveDate from tdsb_i_BusStopBasic bsk WITH(NOLOCK),tdsb_i_BusStopDetails det WITH(NOLOCK),tdsb_i_Frequency f WITH(NOLOCK) where f.FrequencyString = '"+ stop.getBasicInformation().getFrequency().getFrequencyString() +"' and f.id = bsk.Frequency and bsk.id = det.BusStopBasicId and bsk.busStopGuid = ? ";
                sql += " and  bsk.runGuid = ? and bsk.routeGuid = ? ";

                helper.addParam(stop.getBasicInformation().getBusStopGuid());
                helper.addParam(stop.getBasicInformation().getRunGuid());
                helper.addParam(stop.getRouteGuid());
                
                ResultSet rs1 = helper.runQueryStreamResults(sql);
                int stopId = 0;
                Date modiledDate = null;
                Date efectiveDate = null;
                String dbStartTime = null;
                
                if (rs1.next())
                {
                    stopId = rs1.getInt("id");
                    modiledDate = rs1.getTimestamp("ModifiedDate");
                    efectiveDate = rs1.getTimestamp("EfectiveDate");
                    
                    dbStartTime = DateTimeUtil.parseDateTimeNew(rs1.getTimestamp("StartTime"),"HH:mm");
                    
                    
                }
                rs1.close();
                
                if (dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(stop.getModifiedDate())))
                {
                    dateTimeDStart = DateTimeUtil.getDateFromOffsetDateTime(stop.getModifiedDate());
                }
                else
                    dateTimeDStart = dateTimeLocal;
                helper.clearParams();
                if (stopId != 0 ) {
	                boolean ismodifed = false;
                	if(modiledDate != null && modiledDate.before(dateTimeDStart)) {
	                	ismodifed = true;
//                		System.out.println(modiledDate+" Stop Updated: " + stop.toString());
	                } else if(efectiveDate != null && efectiveDate.before(dateTimeStart)) {
	                	ismodifed = true;
//	                	System.out.println(efectiveDate + " Stop Effective date Updated: " + stop.toString());
	                } else {
	                	String startTime = DateTimeUtil.parseDateTimeNew(DateTimeUtil.getDateFromOffsetDateTime(stop.getBasicInformation().getTime()),"HH:mm");
	                	if(!startTime.equalsIgnoreCase(dbStartTime)) {
	                		ismodifed = true;
//		                    System.out.println("Modified Stop Time: " + startTime + " : " + dbStartTime + " : " +  stop.toString());
	                	}
	                }
                    if (ismodifed) {

                    	sql = "INSERT INTO tdsb_i_BusStopBasic_log " + 
                    			" (BusStopBasicId,BusStopID,Description,Position,RunID,StartTime,OrderNumber,StopType,RHSPickup,SpecialEd,Active,EfectiveDate,RetireDate,Frequency,route_fk_id,busStopGuid,runGuid,routeGuid) " + 
                    			" SELECT * FROM tdsb_i_BusStopBasic WHERE id =" + stopId;
                        helper.runQuery(sql);

                        sql = "INSERT INTO tdsb_i_BusStopDetails_log " + 
                    			" (BusStopBasicId,RouteID,OperatorName,ModifiedDate,CreatedDate,latitude,longitude) " + 
                    			" SELECT BusStopBasicId,RouteID,OperatorName,ModifiedDate,CreatedDate,latitude,longitude FROM tdsb_i_BusStopDetails WHERE BusStopBasicId = " + stopId;
                        helper.runQuery(sql);

//	                    System.out.println("Modified: " + modiledDate + " : " + dateTimeDStart + " : " + stop.getBasicInformation().getBusStopID() + " : " + stop.getRouteID());
                        sql = "update tdsb_i_BusStopBasic WITH (ROWLOCK) set routeGuid='"+ stop.getRouteGuid() +"' ,runGuid='"+ stop.getBasicInformation().getRunGuid()  +"',busStopGuid= '"+ stop.getBasicInformation().getBusStopGuid() +"', RetireDate = '" + sdf.format(dateTimeEnd) + "',Active= " + (stop.getBasicInformation().isActive() == true ? 1 : 0) + ",RunID='" + processText(stop.getBasicInformation().getRunID()) + "',StartTime='" + sdf.format(DateTimeUtil.getDateFromOffsetDateTime(stop.getBasicInformation().getTime())) 
                        + "',Description='"+ stop.getBasicInformation().getDescription().replace("'", "\"")  +"',Position='"+  stop.getBasicInformation().getPosition().replace("'", "\"") +"',Frequency = "+ modified 
                        + ",EfectiveDate=  '" + sdf.format(dateTimeStart) + "'" 
                        +"  where id = " + stopId;
                        helper.runQuery(sql);

                        sql = "update tdsb_i_BusStopDetails WITH (ROWLOCK) set ModifiedDate = '" + sdf.format(dateTimeDStart) + "',latitude = " + stop.getLatLong().getLatitude() + " , longitude = "+stop.getLatLong().getLongitude() +"   where BusStopBasicId = " + stopId;
                        helper.runQuery(sql);

                    } else {
//	                    	sql = "update tdsb_i_BusStopBasic WITH (ROWLOCK) set routeGuid='"+ stop.getRouteGuid() +"' ,runGuid='"+ stop.getBasicInformation().getRunGuid()  +"',busStopGuid= '"+ stop.getBasicInformation().getBusStopGuid() +"', RetireDate = '" + sdf.format(dateTimeEnd) + "',Active= " + (stop.getBasicInformation().isActive() == true ? 1 : 0) + ",RunID='" + processText(stop.getBasicInformation().getRunID()) + "',StartTime='" + sdf.format(DateTimeUtil.getDateFromOffsetDateTime(stop.getBasicInformation().getTime())) 
//	                        + "',Description='"+ stop.getBasicInformation().getDescription().replace("'", "\"")  +"',Position='"+  stop.getBasicInformation().getPosition().replace("'", "\"") +"',Frequency = "+ modified
//	                        + ",EfectiveDate=  '" + sdf.format(dateTimeStart) + "' " 
//	                        +"  where id = " + stopId;
//	                        helper.runQuery(sql);
//	                        System.out.println(sql);
                    }
                } else {
//                	System.out.println("Stop Added: " + stop.toString());
                    sql = "INSERT INTO tdsb_i_BusStopBasic (BusStopID,Description,Position,RunID,StartTime,OrderNumber,StopType,RHSPickup"
                                    + ",SpecialEd,Active,EfectiveDate,RetireDate,Frequency,route_fk_id,routeGuid,runGuid,busStopGuid) VALUES "
                                    + "('" + stop.getBasicInformation().getBusStopID().replace("'", "\"")  + "','" + stop.getBasicInformation().getDescription().replace("'", "\"") + "','" + stop.getBasicInformation().getPosition().replace("'", "\"") + "','" + processText(stop.getBasicInformation().getRunID()) + "','" + sdf.format(DateTimeUtil.getDateFromOffsetDateTime(stop.getBasicInformation().getTime())) + "'," + stop.getBasicInformation().getOrder()
                                    + ",'" + stop.getBasicInformation().getStopType() + "'," + (stop.getBasicInformation().isRhSPickup() == true ? 1 : 0) + "," + (stop.getBasicInformation().isSpecialEd() == true ? 1 : 0) + "," + (stop.getBasicInformation().isActive() == true ? 1 : 0) + ",'" + sdf.format(dateTimeStart) + "'"
                                    + ",'" + sdf.format(dateTimeEnd) + "','" + modified + "',0,'"+ stop.getRouteGuid() +"','"+ stop.getBasicInformation().getRunGuid() +"','"+ stop.getBasicInformation().getBusStopGuid() +"')";

                    long key = helper.runQueryKey(sql);
                    if (key != 0)
                        modified = (int) key;

                    if (dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(stop.getCreatedDate()))) {
                        dateTimeCreated = DateTimeUtil.getDateFromOffsetDateTime(stop.getCreatedDate());
                    }
                    sql = "INSERT INTO tdsb_i_BusStopDetails (BusStopBasicId, RouteID, OperatorName, ModifiedDate, CreatedDate, latitude, longitude) VALUES("
                                     + modified + ", '" + stop.getRouteID() + "', '" + stop.getOperatorName().replace("'", "\"") + "','" + sdf.format(dateTimeDStart)
                                     + "','" + sdf.format(dateTimeCreated) + "'," + stop.getLatLong().getLatitude() + "," + stop.getLatLong().getLongitude() + ");";
                    helper.runQuery(sql);
                }
			}
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        } finally {
        	helper.releaseConnection();
        }
            }
        return success;
    }
	
	public boolean saveGPSRouteVehicle(GPSRouteVehicle r, String provider) {
		boolean isInseted = true;
		QueryHelper helper = new QueryHelper();
		try {
			SimpleDateFormat formatter = new  SimpleDateFormat("yyyy-MM-dd"); //,"yyyy-MM-dd"
			  String cmdStr = "SELECT i.sn_imei_id  FROM tracker_info i WITH(NOLOCK) INNER JOIN user_tracker_relation r WITH(NOLOCK) ON r.sn_imei_id = i.sn_imei_id and i.tracker_name = '" + r.getVehicleID() + "' "
	                    + ", busplanner_operator bp, user_operator_relation uor where provider = '" + provider + "' and bp.id = uor.operator_id and r.user_id = uor.user_id";
			  
			  ResultSet rs = helper.runQueryStreamResults(cmdStr);
			  String imei = null;
			  if(rs.next()) {
				  imei = rs.getString("sn_imei_id");
			  } else {
				  imei = r.getVehicleID();
			  }
			  rs.close();
	            Date dateTimeLocal = DateTimeUtil.getDefaultDate();
	            Date dateTimeEffectiveDate = DateTimeUtil.getDefaultDate();
	            Date dateTimeRetireDate = DateTimeUtil.getDefaultDate();
	            Date dateTimeRetireDateFuture = DateTimeUtil.getDefaultFutureDate(2026);//new DateTime(2025, 12, 31, 23, 0, 0);

	            Date effectiveDate = DateTimeUtil.getDateFromOffsetDateTime(r.getEffectiveDate());
                if (dateTimeLocal.before(effectiveDate))
                    dateTimeEffectiveDate = effectiveDate;
                else
                    dateTimeEffectiveDate = dateTimeLocal;

                Date retireDate = DateTimeUtil.getDateFromOffsetDateTime(r.getRetireDate());
                if (dateTimeLocal.before(retireDate))
                    dateTimeRetireDate = retireDate;
                else
                    dateTimeRetireDate = dateTimeRetireDateFuture;
	            
			  boolean isAready = false;
			  int recordid = 0;
			  rs = helper.runQueryStreamResults("select id from tdsb_i_GPSRouteVehicle WITH(NOLOCK) where RouteGuid = '" + r.getRouteGuid() + "' and ( VehicleId = '" + r.getVehicleID() + "'  or VehicleName = '" + r.getVehicleID() + "') and EffectiveDate <= getdate() and CONVERT(DATE, RetireDate) >= '" + formatter.format(dateTimeRetireDate) + "' ");
              if (rs.next()) {
            	  recordid = rs.getInt("id");
                  isAready = true;
              } else {
                  isAready = false;
              }
              rs.close();

              if (!isAready) {
//            	  System.out.println(provider+ " : Not added");
            	  cmdStr = "INSERT INTO tdsb_i_GPSRouteVehicle_test " 
                           + "(EffectiveDate ,Platform ,Provider,RetireDate,RouteGuid,RouteID,RouteVehicleGuid,VehicleGuid,VehicleName,fromProvider,VehicleID) VALUES "
                           + "('" + DateTimeUtil.getDefaultDateString(dateTimeEffectiveDate) + "','" + processText(r.getPlatform())  + "','" + r.getProvider() + "','" + DateTimeUtil.getDefaultDateString(dateTimeRetireDate) + "','" + r.getRouteGuid() + "','" + r.getRouteID()
                           + "','" + r.getRouteVehicleGuid() + "','" + r.getVehicleGuid() + "','" + r.getVehicleID() + "','" + provider + "','" + imei + "')";
              } else {
//            	  System.out.println("Update");
            	  cmdStr = "update tdsb_i_GPSRouteVehicle_test " 
                           + " set EffectiveDate = '" + DateTimeUtil.getDefaultDateString(dateTimeEffectiveDate) + "', RetireDate = '" + DateTimeUtil.getDefaultDateString(dateTimeRetireDate) + "'"
                           + ",fromProvider = '" + provider + "' ,VehicleName = '" + r.getVehicleID()  + "',VehicleID='" + imei + "' where id = " + recordid;
              }
              helper.runQuery(cmdStr);
		} catch (Exception e) {
			e.printStackTrace();
			isInseted = false;
		} finally {
			helper.closeConnection();
		}
		return isInseted;
	}
	
	public void processRouteWayPoints(List<WayPoint> points, String routeID, int routeFKId) {
		QueryHelper helper = new QueryHelper();
		try {
			helper.runQuery("DELETE FROM tdsb_i_route_waypoints WHERE route_fk_id = " + routeFKId);
			String sql = " INSERT INTO tdsb_i_route_waypoints (Actions,BusStopID,Distance,EndTime,Heading,Latitude,Longitude," + 
						 " Municipality,OrderNumber,RouteID,RunID,StartTime,Street,X,Y,route_fk_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
			for (WayPoint wayPoint : points ) {
				helper.clearParams();
				helper.addParam(wayPoint.getAction());
				helper.addParam(wayPoint.getBusStopID().replace("'", "\""));
				helper.addParam(wayPoint.getDistance());
				helper.addParam(wayPoint.getDistance());
				helper.addParam(wayPoint.getHeading());
				helper.addParam(wayPoint.getLatitude());
				helper.addParam(wayPoint.getLongitude());
				helper.addParam(wayPoint.getMunicipality());
				helper.addParam(wayPoint.getOrder());
				helper.addParam(wayPoint.getRouteID());
				helper.addParam(wayPoint.getRunID());
				helper.addParam(wayPoint.getStartTime());
				helper.addParam(wayPoint.getStreet());
				helper.addParam(wayPoint.getX());
				helper.addParam(wayPoint.getY());
				helper.addParam(routeFKId);
				helper.runQuery(sql);
            }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			helper.closeConnection();
		}
	}
	
	public RunUpdateStatusDTO populateRunPoint(Run run, String userName,String supportedOperators) {
        boolean ismodifed = false;	
        Date dateTimeLocal = DateTimeUtil.getDefaultDate();
 		Date modifiedDate = DateTimeUtil.getDefaultDate();
 		Date createdDate = DateTimeUtil.getDefaultDate();
 		Date retireDate = DateTimeUtil.getDefaultDate();
 		Date effectiveDate = DateTimeUtil.getDefaultDate();
 		int id = 0;
 		int runid = 0;
 		QueryHelper helper = new QueryHelper();
 		RunUpdateStatusDTO status = null;
		try {
			ResultSet rs = null;
			rs = helper.runQueryStreamResults(" SELECT id FROM tdsb_i_route WITH(NOLOCK) WHERE route_key = '"+ run.getRouteGuid()  + "' AND  operator_id IN ("+ supportedOperators +")");
			if(rs.next())
				id = rs.getInt("id");
			else
				id = 0;
			rs.close();
//			System.out.println("Route ID: "+ id);
			if(id != 0 ) {
				if(dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(run.getModifiedDate())))
					modifiedDate = DateTimeUtil.getDateFromOffsetDateTime(run.getModifiedDate());
				else
					modifiedDate = dateTimeLocal;
				if(dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(run.getCreatedDate())))
					createdDate = DateTimeUtil.getDateFromOffsetDateTime(run.getCreatedDate());
				 else
					 createdDate = dateTimeLocal;
				if(dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(run.getEffectiveDate())))
					effectiveDate = DateTimeUtil.getDateFromOffsetDateTime(run.getEffectiveDate());
				else
					effectiveDate = dateTimeLocal;
				if(dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(run.getRetireDate())))
					retireDate = DateTimeUtil.getDateFromOffsetDateTime(run.getRetireDate());
				 else
					retireDate = dateTimeLocal;
				
				rs = helper.runQueryStreamResults(" SELECT id,Active,ModifiedDate FROM tdsb_i_run_points WITH(NOLOCK) WHERE route_fk_id = " + id + " AND RunId = '" + processText(run.getRunID()) + "' AND  RunGuid = '"+ run.getRunGuid() + "' and ComponentVariation = '"+ run.getComponentVariation() +"' order by id desc");
				//,RetireDate
				Date modiledDate = null;
//				Date localRetireDate = null;
				runid = 0;
				int count = 0;
				boolean isActive = false;
				while(rs.next()) {
					if(count == 0) {
						modiledDate = new Date(rs.getTimestamp("ModifiedDate").getTime());
//						localRetireDate = new Date(rs.getTimestamp("RetireDate").getTime());
						runid = rs.getInt("id");
						isActive = rs.getBoolean("Active");
					}
					
					count = count + 1;
				}
				rs.close();
				if(runid == 0) {
					String sql = "INSERT INTO tdsb_i_run_points (RunId,Description,Instruction,RouteID,ComponentID,RunOrder,StartTime,EndTime,ModifiedDate,CreatedDate,ComponentVariation,RunVariation,RunGuid,SIFGuid,"  
							 + "Type,Regular,SpecialEd,Active,Protected,EffectiveDate,RetireDate,RunVGuid,Distance,route_fk_id,routeGuid) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
					helper.clearParams();
					helper.addParam(processText(run.getRunID()));
					helper.addParam(run.getDescription());
					helper.addParam(run.getInstruction());
					helper.addParam(run.getRouteID());
					helper.addParam(run.getComponentID());
					helper.addParam(run.getRunOrder());
					helper.addParam(run.getStartTime());
					helper.addParam(run.getEndTime());
					helper.addParam(DateTimeUtil.getDefaultDateString(modifiedDate));
					helper.addParam(DateTimeUtil.getDefaultDateString(createdDate));
					helper.addParam(run.getComponentVariation());
					helper.addParam(run.getRunVariation());
					helper.addParam(run.getRunGuid());
					helper.addParam(run.getSiFGuid());
					helper.addParam(run.getType());
					helper.addParam((run.isRegular() == true ? 1 : 0));
					helper.addParam((run.isSpecialEd() == true ? 1 : 0));
					helper.addParam((run.isActive() == true ? 1 : 0));
					helper.addParam((run.isProtected() == true ? 1 : 0));
					helper.addParam(DateTimeUtil.getDefaultDateString(effectiveDate));
					helper.addParam(DateTimeUtil.getDefaultDateString(retireDate));
					helper.addParam(run.getRunVGuid());
					helper.addParam(run.getDistance());
					helper.addParam(id);
					helper.addParam(run.getRouteGuid());
					long ids = helper.runQueryKey(sql);
					runid = (int)ids;
					status = new RunUpdateStatusDTO();
					status.setRunid(runid);
					status.setNewStatus(true);
					status.setStopsUpdated(true);
					status.setOldUpdate(false);
					status.setComponentVariation(run.getComponentVariation());
				} else {
					if((modiledDate != null && modiledDate.before(modifiedDate))) {
						ismodifed = true;
//						System.out.println("Modified : " + runid + " " + modiledDate + "  :  "+modifiedDate + " : " + run.getRunID() + " : " + run.getRouteID() );
					} else if (isActive != run.isActive()) {
						ismodifed = true;
//						System.out.println("Modified Active : " + runid + " " +  modiledDate + "  :  "+modifiedDate + " : " + run.getRunID() + " : " + run.getRouteID() );
//					} else {
//						System.out.println("Not Modified : " + modiledDate + "  :  "+modifiedDate + " : " + run.getRunID() + " : " + run.getRouteID() );
					}
					 if (ismodifed){
						status = new RunUpdateStatusDTO();
						status.setRunid(runid);
						status.setNewStatus(false);
						status.setStopsUpdated(true);
						status.setOldUpdate(false);
						status.setComponentVariation(run.getComponentVariation());
						String sql = "update tdsb_i_run_points WITH (ROWLOCK) set Description =  '" + processText(run.getDescription())
                         + "', Instruction = '" + processText(run.getInstruction()) + "', ComponentID='" + run.getComponentID()
                         + "',RunOrder=" + run.getRunOrder() + ",StartTime='" + run.getStartTime() + "',EndTime='" + run.getEndTime() + "',ModifiedDate='" + DateTimeUtil.getDefaultDateString(modifiedDate) + "',CreatedDate='" + DateTimeUtil.getDefaultDateString(createdDate) + "',"
                         + "ComponentVariation='" + run.getComponentVariation() + "',RunVariation='" + run.getRunVariation() + "',RunGuid='" + run.getRunGuid() + "',SIFGuid='" + run.getSiFGuid()
                         + "',Type=" + run.getType()
                         + ",Regular=" + (run.isRegular() == true ? 1 : 0) + ",SpecialEd=" + (run.isSpecialEd() == true ? 1 : 0) + ",Active=" + (run.isActive() == true ? 1 : 0)
                         + ",Protected=" + (run.isProtected() == true ? 1 : 0) + ",EffectiveDate='" + DateTimeUtil.getDefaultDateString(effectiveDate) + "',RetireDate='" + DateTimeUtil.getDefaultDateString(retireDate)
                         + "',RunVGuid='" + run.getRunVGuid() + "',Distance=" + run.getDistance() + ",route_fk_id=" + id
                         + " ,routeGuid = '"+ run.getRouteGuid()+"' "
                         + " where id = " + runid;
						helper.clearParams();
						helper.runQuery(sql);
					 } else {
						 /*
						 if((localRetireDate != null && !retireDate.equals(localRetireDate))) {
							 String sql = "update tdsb_i_run_points WITH (ROWLOCK) set RetireDate='" + DateTimeUtil.getDefaultDateString(retireDate) + "' where id = " + runid;
								helper.clearParams();
								helper.runQuery(sql);
								System.out.println("RetireDate Modified : " + runid + " " + modiledDate + "  :  "+modifiedDate + " : " + run.getRunID() + " : " + run.getRouteID() );
						 }
						*/
						status = new RunUpdateStatusDTO();
						status.setRunid(runid);
						status.setNewStatus(false);
						status.setStopsUpdated(false);
						status.setOldUpdate(false);
						status.setComponentVariation(run.getComponentVariation());
					 }
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			status = null;
		} finally {
			helper.closeConnection();
		}
		return status;
	}
	
	public boolean populateOnlyRunPoint(Run run, String userName,String supportedOperators,RunUpdateStatusDTO runStatus) {
 		QueryHelper helper = new QueryHelper();
		try {
			ResultSet rs = null;
			String sql = "";
			if (run.getRunPoints() != null && run.getRunPoints().size() > 0) {
				 int locationOrderNumber = 0;
				 boolean hasBatch = false;
				 if(runStatus.isNewStatus()) {
					 	int orderNumber = 1;
						for (RunPoint runPoint : run.getRunPoints() ) {
								sql = " INSERT INTO tdsb_i_Runpoints_position (route_point_id,BusstopId,Latitude,Longitude,active,orderNumber,startDate,busStopGuid) VALUES (?,?,?,?,1,?,getdate(),?) ";
								helper.clearParams();
								helper.addParam(runStatus.getRunid());
								helper.addParam(runPoint.getBusStopID());
								helper.addParam(runPoint.getLatLong().getLatitude());
								helper.addParam(runPoint.getLatLong().getLongitude());
								if(StringUtils.isNotEmpty(runPoint.getBusStopID())) {
									helper.addParam(orderNumber);
									orderNumber = orderNumber + 1;
								} else {
									helper.addParam(0);
								}
								helper.addParam(runPoint.getBusStopGuid());
								helper.runQuery(sql);
								
						}
						if (run.getLatLongs() != null && run.getLatLongs().size() > 0) {
							for(Position pos : run.getLatLongs()) {
								helper.addBatch("insert into tdsb_i_run_position (order_number,runid,Latitude,Longitude) values ("+locationOrderNumber+","+ runStatus.getRunid() +","+  pos.getLatitude() +"," +  pos.getLongitude() +")");
								locationOrderNumber = locationOrderNumber +1;
								hasBatch = true;
							}
						}
						if(hasBatch) {
							helper.runBatch();
						}
			} else {
					 if(runStatus.isStopsUpdated()) {
						 if (run.getRunPoints() != null && run.getRunPoints().size() > 0) {
							int orderNumber = 1;
			                int stopid = 0;
			                String sql_query = "select id,BusstopId from tdsb_i_Runpoints_position WITH(NOLOCK) where route_point_id = " + runStatus.getRunid() + "  and (BusStopId is not null and BusStopId != '')";
							helper.clearParams();
							rs = helper.runQueryStreamResults(sql_query);
							Map<String,Integer> stopMap = new HashMap<String, Integer>();
							while(rs.next()) {
								stopMap.put(rs.getString("BusstopId"),rs.getInt("id"));
							}
							rs.close();
							helper.clearParams();
							String stopIdsIn = "";
							for (RunPoint r : run.getRunPoints() ) {
								 stopid = 0;
								 if (!r.getBusStopID().isEmpty()) {
									if(stopMap.containsKey(r.getBusStopID())) {
										stopid = stopMap.get(r.getBusStopID()); 
									}
									if (stopid != 0) {
										sql_query = "update tdsb_i_Runpoints_position WITH (ROWLOCK) set Latitude="+ r.getLatLong().getLatitude() +",Longitude="+ r.getLatLong().getLongitude() +",  orderNumber = " + orderNumber + " , active = 1,endDate=NULL,busStopGuid='"+ r.getBusStopGuid() +"' where id = " + stopid;
										hasBatch = true;
									} else {
										sql_query = "INSERT INTO tdsb_i_Runpoints_position (route_point_id,BusstopId,Latitude,Longitude,active,orderNumber,startDate,busStopGuid) VALUES (" + runStatus.getRunid() + ",'" + r.getBusStopID() + "'," + r.getLatLong().getLatitude() + "," + r.getLatLong().getLongitude() + ",1," + orderNumber + ",getdate(),'"+ r.getBusStopGuid() +"')";
										hasBatch = true;
									}
									stopIdsIn = stopIdsIn + (StringUtils.isEmpty(stopIdsIn)?"'"+stopid+"'":",'"+stopid+"'");
									helper.addBatch(sql_query);
									orderNumber = orderNumber + 1;
								}
			                } 
							if(hasBatch) {
								int[] result = helper.runBatch();
								if(result.length>0) {
									if(!StringUtils.isEmpty(stopIdsIn)) {
										sql = "update tdsb_i_Runpoints_position WITH (ROWLOCK) set active = 0,endDate=getDate() where active = 1 and (cast(startDate as date) != CAST(getdate() as date) or startDate is null)  and route_point_id = " + runStatus.getRunid() + " and id not in (" + stopIdsIn + ")";
										helper.runQuery(sql);
									}	 
								 }
							}
						 }
						 hasBatch = false;
						 if (run.getLatLongs() != null && run.getLatLongs().size() > 0) {
							 String _sql = "delete from tdsb_i_run_position where runid = " + runStatus.getRunid();
							 helper.clearParams();
							 helper.runQuery(_sql);
							 for(Position pos : run.getLatLongs()) {
								helper.addBatch("insert into tdsb_i_run_position (order_number,runid,Latitude,Longitude) values ("+locationOrderNumber+","+ runStatus.getRunid() +","+  pos.getLatitude() +"," +  pos.getLongitude() +")");
								locationOrderNumber = locationOrderNumber +1;
								hasBatch = true;
							 }
							 if(hasBatch) {
									helper.runBatch();
							}
						 }
					 } else if(runStatus.isOldUpdate()){
						 
						 if (run.getRunPoints() != null && run.getRunPoints().size() > 0) {
								int orderNumber = 1;
				                int stopid = 0;
				                String sql_query = "select id,BusstopId from tdsb_i_Runpoints_position WITH(NOLOCK) where route_point_id = " + runStatus.getRunid() + "  and (BusStopId is not null and BusStopId != '')";
//				                System.out.println(sql_query);
								helper.clearParams();
								rs = helper.runQueryStreamResults(sql_query);
								Map<String,Integer> stopMap = new HashMap<String, Integer>();
								while(rs.next()) {
									stopMap.put(rs.getString("BusstopId"),rs.getInt("id"));
								}
								rs.close();
								helper.clearParams();
								for (RunPoint r : run.getRunPoints() ) {
									 stopid = 0;
									 if (!r.getBusStopID().isEmpty()) {
										if(stopMap.containsKey(r.getBusStopID())) {
											stopid = stopMap.get(r.getBusStopID()); 
										}
										if (stopid != 0) {
											sql_query = "update tdsb_i_Runpoints_position WITH (ROWLOCK) set orderNumber = " + orderNumber + " , busStopGuid='"+ r.getBusStopGuid() +"' where id = " + stopid;
											hasBatch = true;
										}
										helper.addBatch(sql_query);
										orderNumber = orderNumber + 1;
									}
				                } 
								if(hasBatch) {
									helper.runBatch();
								}
							 }
					 }
				 }
			 }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			helper.closeConnection();
		}
		return true;
	}
	
	public void populateRunPoints(List<Run> runs, String userName,String supportedOperators) {
		
		int modified = 0;
        boolean ismodifed = false;	

        Date dateTimeLocal = DateTimeUtil.getDefaultDate();
 		Date modifiedDate = DateTimeUtil.getDefaultDate();
 		Date createdDate = DateTimeUtil.getDefaultDate();
 		Date retireDate = DateTimeUtil.getDefaultDate();
 		Date effectiveDate = DateTimeUtil.getDefaultDate();
 		int id = 0;
 		int runid = 0;
 		QueryHelper helper = new QueryHelper();
		for (Run run : runs) {
			try {
				ResultSet rs = null;
			rs = helper.runQueryStreamResults(" SELECT id FROM tdsb_i_route WITH(NOLOCK) WHERE route_key = '"+ run.getRouteGuid()  + "' AND  operator_id IN ("+ supportedOperators +")");
			if(rs.next())
				id = rs.getInt("id");
			else
				id = 0;
			rs.close();
			if(id != 0 ) {
				if(dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(run.getModifiedDate())))
					modifiedDate = DateTimeUtil.getDateFromOffsetDateTime(run.getModifiedDate());
				else
					modifiedDate = dateTimeLocal;
				if(dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(run.getCreatedDate())))
					createdDate = DateTimeUtil.getDateFromOffsetDateTime(run.getCreatedDate());
				 else
					 createdDate = dateTimeLocal;
				if(dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(run.getEffectiveDate())))
					effectiveDate = DateTimeUtil.getDateFromOffsetDateTime(run.getEffectiveDate());
				else
					effectiveDate = dateTimeLocal;
				if(dateTimeLocal.before(DateTimeUtil.getDateFromOffsetDateTime(run.getRetireDate())))
					retireDate = DateTimeUtil.getDateFromOffsetDateTime(run.getRetireDate());
				 else
					retireDate = dateTimeLocal;
				
				rs = helper.runQueryStreamResults(" SELECT id,Active,ModifiedDate FROM tdsb_i_run_points WITH(NOLOCK) WHERE route_fk_id = " + id + " AND RunGuid = '"+ run.getRunGuid() + "'");
				Date modiledDate = null;
				if(rs.next()) {
					modiledDate = new Date(rs.getTimestamp("ModifiedDate").getTime());
					runid = rs.getInt("id");
				}else
					runid = 0;
				rs.close();
				if(runid == 0) {
					String sql = " INSERT INTO tdsb_i_run_points (RunId,Description,Instruction,RouteID,ComponentID,RunOrder,StartTime,EndTime,ModifiedDate, "  
							 + "CreatedDate,ComponentVariation,RunVariation,RunGuid,SIFGuid,Type,Regular,SpecialEd,Active,Protected,EffectiveDate,RetireDate,"
	                         + "RunVGuid,Distance,route_fk_id,routeGuid) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
					helper.clearParams();
					helper.addParam(run.getRunID());
					helper.addParam(run.getDescription());
					helper.addParam(run.getInstruction());
					helper.addParam(run.getRouteID());
					helper.addParam(run.getComponentID());
					helper.addParam(run.getRunOrder());
					helper.addParam(run.getStartTime());
					helper.addParam(run.getEndTime());
					helper.addParam(DateTimeUtil.getDefaultDateString(modifiedDate));
					helper.addParam(DateTimeUtil.getDefaultDateString(createdDate));
					helper.addParam(run.getComponentVariation());
					helper.addParam(run.getRunVariation());
					helper.addParam(run.getRunGuid());
					helper.addParam(run.getSiFGuid());
					helper.addParam(run.getType());
					helper.addParam((run.isRegular() == true ? 1 : 0));
					helper.addParam((run.isSpecialEd() == true ? 1 : 0));
					helper.addParam((run.isActive() == true ? 1 : 0));
					helper.addParam((run.isProtected() == true ? 1 : 0));
					helper.addParam(DateTimeUtil.getDefaultDateString(effectiveDate));
					helper.addParam(DateTimeUtil.getDefaultDateString(retireDate));
					helper.addParam(run.getRunVGuid());
					helper.addParam(run.getDistance());
					helper.addParam(id);
					helper.addParam(run.getRouteGuid());
					long ids = helper.runQueryKey(sql);
					modified = (int)ids;
					int locationOrderNumber = 0;
					boolean hasBatch = false;
					
					for (RunPoint runPoint : run.getRunPoints() ) {
						sql = " INSERT INTO tdsb_i_Runpoints_position (route_point_id,BusstopId,Latitude,Longitude) VALUES (?,?,?,?) ";
						helper.clearParams();
						helper.addParam(modified);
						helper.addParam(runPoint.getBusStopID());
						helper.addParam(runPoint.getLatLong().getLatitude());
						helper.addParam(runPoint.getLatLong().getLongitude());
						helper.runQuery(sql);
						
						helper.addBatch("insert into tdsb_i_run_position (order_number,runid,Latitude,Longitude) values ("+locationOrderNumber+","+ modified +","+  runPoint.getLatLong().getLatitude() +"," +  runPoint.getLatLong().getLongitude() +")");
						locationOrderNumber = locationOrderNumber +1;
						hasBatch = true;
					}
					if(hasBatch) {
						helper.runBatch();
					}
				}else {
					if(modiledDate != null && modiledDate.before(modifiedDate)) {
						ismodifed = true;
//						System.out.println("Modified : " + modiledDate + "  :  "+modifiedDate + " : " + run.getRunID() + " : " + run.getRouteID() );
					}
					 if (ismodifed){
						String sql = "update tdsb_i_run_points WITH (ROWLOCK) set Description =  '" + processText(run.getDescription())
                         + "', Instruction = '" + processText(run.getInstruction()) + "', ComponentID='" + run.getComponentID()
                         + "',RunOrder=" + run.getRunOrder() + ",StartTime='" + run.getStartTime() + "',EndTime='" + run.getEndTime() + "',ModifiedDate='" + DateTimeUtil.getDefaultDateString(modifiedDate) + "',CreatedDate='" + DateTimeUtil.getDefaultDateString(createdDate) + "',"
                         + "ComponentVariation='" + run.getComponentVariation() + "',RunVariation='" + run.getRunVariation() + "',RunGuid='" + run.getRunGuid() + "',SIFGuid='" + run.getSiFGuid()
                         + "',Type=" + run.getType()
                         + ",Regular=" + (run.isRegular() == true ? 1 : 0) + ",SpecialEd=" + (run.isSpecialEd() == true ? 1 : 0) + ",Active=" + (run.isActive() == true ? 1 : 0)
                         + ",Protected=" + (run.isProtected() == true ? 1 : 0) + ",EffectiveDate='" + DateTimeUtil.getDefaultDateString(effectiveDate) + "',RetireDate='" + DateTimeUtil.getDefaultDateString(retireDate)
                         + "',RunVGuid='" + run.getRunVGuid() + "',Distance=" + run.getDistance() + ",route_fk_id=" + id
                         + " ,routeGuid = '"+ run.getRouteGuid()+"' "
                         + " where id = " + runid;
						helper.clearParams();
						helper.runQuery(sql);

						 if (run.getRunPoints() != null && run.getRunPoints().size() > 0) {
							 sql = "update tdsb_i_Runpoints_position WITH (ROWLOCK) set orderNumber = 0 , active = 0,endDate=getDate() where route_point_id = " + runid;
							 helper.runQuery(sql);
							 int orderNumber = 1;
	                         int stopid = 0;
	                         
	                        String sql_query = "select id,BusstopId from tdsb_i_Runpoints_position WITH(NOLOCK) where route_point_id = " + runid + "  and (BusStopId is not null and BusStopId != '')";
							helper.clearParams();
							rs = helper.runQueryStreamResults(sql_query);
							Map<String,Integer> stopMap = new HashMap<String, Integer>();
							while(rs.next()) {
								stopMap.put(rs.getString("BusstopId"),rs.getInt("id"));
							}
							rs.close();
							helper.clearParams();
							boolean hasBatch = false;
							String _sql = "delete from tdsb_i_run_position where runid = " + runid;
							helper.runQuery(_sql);
							int locationOrderNumber = 0;
							for (RunPoint r : run.getRunPoints() ) {
								 stopid = 0;
								 if (!r.getBusStopID().isEmpty()) {
									if(stopMap.containsKey(r.getBusStopID())) {
										stopid = stopMap.get(r.getBusStopID()); 
									}
									if (stopid != 0) {
										sql_query = "update tdsb_i_Runpoints_position WITH (ROWLOCK) set Latitude="+ r.getLatLong().getLatitude() +",Longitude="+ r.getLatLong().getLongitude() +",  orderNumber = " + orderNumber + " , active = 1,endDate=NULL,busStopGuid='"+ r.getBusStopGuid() +"' where id = " + stopid;
										hasBatch = true;
									} else {
										sql_query = "INSERT INTO tdsb_i_Runpoints_position (route_point_id,BusstopId,Latitude,Longitude,active,orderNumber,startDate,busStopGuid) VALUES (" + runid + ",'" + r.getBusStopID() + "'," + r.getLatLong().getLatitude() + "," + r.getLatLong().getLongitude() + ",1," + orderNumber + ",getdate(),'"+ r.getBusStopGuid() +"')";
										hasBatch = true;
									}
									helper.addBatch(sql_query);
									orderNumber = orderNumber + 1;
								}
								 hasBatch = true;
								 helper.addBatch("insert into tdsb_i_run_position (order_number,runid,Latitude,Longitude) values ("+locationOrderNumber+","+ runid +","+  r.getLatLong().getLatitude() +"," +  r.getLatLong().getLongitude() +")");
								 locationOrderNumber = locationOrderNumber +1;
	                        } 
							if(hasBatch) {
								helper.runBatch();
							}
						 }
						 /*
						 if (run.getLatLongs() != null && run.getLatLongs().size() > 0) {
							String _sql = "delete from  tdsb_i_position where runid = " + runid;
						 	helper.runQuery(_sql);
							for (Position r : run.getLatLongs()){
								_sql = "INSERT INTO tdsb_i_position (runid,Latitude,Longitude) VALUES (" + runid + "," + r.getLatitude() + "," + r.getLongitude() + ")";
								helper.runQuery(_sql);
							}	 
						 }
						 */
					 } else {
						 /*
						 if (run.getRunPoints() != null && run.getRunPoints().size() > 0) {
							 if(runid == 13294) {
									System.out.println(run.toString());
								}
							boolean hasBatch = false;
							String _sql = "delete from tdsb_i_run_position where runid = " + runid;
							helper.runQuery(_sql);
							int locationOrderNumber = 0;
							for (RunPoint r : run.getRunPoints() ) {
								 helper.addBatch("insert into tdsb_i_run_position (order_number,runid,Latitude,Longitude) values ("+locationOrderNumber+","+ runid +","+  r.getLatLong().getLatitude() +"," +  r.getLatLong().getLongitude() +")");
								 hasBatch = true;
								 locationOrderNumber = locationOrderNumber +1;
							}
							if(hasBatch) {
								helper.runBatch();
							}
						 }
						 */
						 /*
						 String sql = "update tdsb_i_run_points set Description =  '" + processText(run.getDescription())
                         + "', Instruction = '" + processText(run.getInstruction()) + "', ComponentID='" + run.getComponentID()
                         + "',RunOrder=" + run.getRunOrder() + ",StartTime='" + run.getStartTime() + "',EndTime='" + run.getEndTime() + "',ModifiedDate='" + DateTimeUtil.getDefaultDateString(modifiedDate) + "',CreatedDate='" + DateTimeUtil.getDefaultDateString(createdDate) + "',"
                         + "ComponentVariation='" + run.getComponentVariation() + "',RunVariation='" + run.getRunVariation() + "',RunGuid='" + run.getRunGuid() + "',SIFGuid='" + run.getSiFGuid()
                         + "',Type=" + run.getType()
                         + ",Regular=" + (run.isRegular() == true ? 1 : 0) + ",SpecialEd=" + (run.isSpecialEd() == true ? 1 : 0) + ",Active=" + (run.isActive() == true ? 1 : 0)
                         + ",Protected=" + (run.isProtected() == true ? 1 : 0) + ",EffectiveDate='" + DateTimeUtil.getDefaultDateString(effectiveDate) + "',RetireDate='" + DateTimeUtil.getDefaultDateString(retireDate)
                         + "',RunVGuid='" + run.getRunVGuid() + "',Distance=" + run.getDistance() + ",route_fk_id=" + id
                         + " ,routeGuid = '"+ run.getRouteGuid()+"' "
                         + " where id = " + runid;
						helper.clearParams();
						helper.runQuery(sql);
						*/
						 /*
						 String sql_query = "select id,BusstopId from tdsb_i_Runpoints_position WITH(NOLOCK) where route_point_id = " + runid + "  and (BusStopId is not null and BusStopId != '')";
							helper.clearParams();
							ResultSet rs1 = helper.runQueryStreamResults(sql_query);
							Map<String,Integer> stopMap = new HashMap<String, Integer>();
							while(rs1.next()) {
								stopMap.put(rs1.getString("BusstopId"),rs1.getInt("id"));
							}
							rs1.close();
							helper.clearParams();
							boolean hasBatch = false;
							  int stopid = 0;
							for (RunPoint r : run.getRunPoints() ) {
								 stopid = 0;
								 if (!r.getBusStopID().isEmpty()) {
									if(stopMap.containsKey(r.getBusStopID())) {
										stopid = stopMap.get(r.getBusStopID()); 
									}
									if (stopid != 0) {
										sql_query = "update tdsb_i_Runpoints_position WITH (ROWLOCK) set busStopGuid='"+ r.getBusStopGuid() +"' where id = " + stopid;
										hasBatch = true;
										helper.addBatch(sql_query);
									}
								}
	                        } 
							if(hasBatch) {
								helper.runBatch();
							}
						 */
					 }
				}
			} else {
//					System.out.println(userName +" : NOT SUPPORTED " + run.getRunID() + " : " + run.getRouteGuid());
			}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				helper.closeConnection();
			}
		}
		
	}
	
	public ArrayList<OperatorDTO> getOperatorGuids(String provider) {
		String cmdStr = "SELECT bp.operatorGuid,bp.name,bp.operator_id  FROM  busplanner_operator bp WITH(NOLOCK) , user_operator_relation uor WITH(NOLOCK)  where provider = '" + provider + "' and bp.id = uor.operator_id";
		ArrayList<OperatorDTO> list = new ArrayList<OperatorDTO>();
		QueryHelper helper = new QueryHelper();
		try {
			ResultSet rs = helper.runQueryStreamResults(cmdStr);
			String operatorGuid;
			while(rs.next()) {
				operatorGuid = rs.getString("operatorGuid");
				if(StringUtils.isNotEmpty(operatorGuid) ) {
					OperatorDTO operator = new OperatorDTO();
					operator.setOperatorGuid(operatorGuid);
					operator.setOperatorId(rs.getString("operator_id"));
					list.add(operator);
				}
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			helper.closeConnection();
		}
//		OperatorDTO operator = new OperatorDTO();
//		operator.setOperatorGuid("");
//		operator.setOperatorId("CSB");
//		list.add(operator);
		return list;
	}
 	
	 public String processText(String text)
     {
         return text.replaceAll( "'", "''");
     }
	
}
