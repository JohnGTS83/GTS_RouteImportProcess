package com.kaldin;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.threeten.bp.ZoneId;

import com.kaldin.dao.ProcessDAO;
import com.kaldin.dto.OperatorDTO;
import com.kaldin.dto.RunUpdateStatusDTO;
import com.kaldin.dto.UserDTO;
import com.s5.util.DateTimeUtil;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.BusStopApi;
import io.swagger.client.api.GpsRouteVehicleApi;
import io.swagger.client.api.OperatorApi;
import io.swagger.client.api.RouteApi;
import io.swagger.client.api.RunApi;
import io.swagger.client.api.StudentCompleteApi;
import io.swagger.client.api.WayPointApi;
import io.swagger.client.model.BusStopDetails;
import io.swagger.client.model.GPSRouteVehicle;
import io.swagger.client.model.Operator;
import io.swagger.client.model.Route;
import io.swagger.client.model.Run;
import io.swagger.client.model.StudentComplete;
import io.swagger.client.model.WayPoint;

public class ImportJobByAPI implements Job {

	public ImportJobByAPI() {
		dao = new ProcessDAO();
		defaultApiCleint = new ApiClient();
	}

	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		try {
			boolean oneTime = true;
			if(jobExecutionContext.getMergedJobDataMap().containsKey("oneTime")) {	
				oneTime = (boolean) jobExecutionContext.getMergedJobDataMap().get("oneTime");
			}
			
			PrintStream o = new PrintStream(new File("tdsb_logs"+ File.separator +"TDSB_IMPORT_LOG_"+ DateTimeUtil.parseDateTimeNew(Calendar.getInstance().getTime(),"yyyy_MM_dd") +".txt"));
		    System.setOut(o); 
		    String operation = jobExecutionContext.getJobDetail().getKey().getGroup();
		    userList = dao.getAllActiveUsers();
			
			org.threeten.bp.format.DateTimeFormatter df1 =  new org.threeten.bp.format.DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss").toFormatter();
			df1  = df1.withZone(ZoneId.of("GMT"));
			df1 = df1.withResolverStyle(org.threeten.bp.format.ResolverStyle.SMART);
			defaultApiCleint.setOffsetDateTimeFormat(df1);
			defaultApiCleint.setLocalDateFormat(df1);
//			defaultApiCleint.setConnectTimeout(ProcessMain.TIMEOUT_IN_SEC*1000);
//			defaultApiCleint.setReadTimeout(ProcessMain.TIMEOUT_IN_SEC*1000);
//			defaultApiCleint.setWriteTimeout(ProcessMain.TIMEOUT_IN_SEC*1000);
			int iUserCount = 0;
			do {
				UserDTO userDTO = userList.get(iUserCount);
				System.out.println(userDTO.toString());
				defaultApiCleint.setBasePath(userDTO.getUrl());
				this.generatTime = null;
				if(operation.equalsIgnoreCase("OPERATOR")) {
					System.out.println(userDTO.getProvider()+ " OPERATOR Start: " + new Date());
					if(this.processOperator(userDTO)) {
						System.out.println(userDTO.getProvider()+ " Operator DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " Operator ERROR");
					}
					System.out.println(userDTO.getProvider()+ " OPERATOR END: " + new Date());
				} else if(operation.equalsIgnoreCase("ROUTE")) {
					System.out.println(userDTO.getProvider()+ " RoutesWithWayPoints Start: " + new Date());
					if(this.processRoutesWithWayPoints(userDTO)) {
						System.out.println(userDTO.getProvider()+ " RoutesWithWayPoints DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " RoutesWithWayPoints ERROR");
					}
					System.out.println(userDTO.getProvider()+ " RoutesWithWayPoints END: " + new Date());
				} else if(operation.equalsIgnoreCase("STOP")) {
					System.out.println(userDTO.getProvider()+ " Home stop Start: " + new Date());
					if(this.processBusStops(userDTO,OPERATION.HOME_STOP)) {
						System.out.println(userDTO.getProvider()+ " Home Stop DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " Home Stop ERROR");	
					}
					System.out.println(userDTO.getProvider()+ " Home stop End: " + new Date());
					System.out.println(userDTO.getProvider()+ " School stop Start: " + new Date());
					if(this.processBusStops(userDTO,OPERATION.SCHOOL_STOP)) {
						System.out.println(userDTO.getProvider()+ " School Stop DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " School Stop ERROR");	
					}
					System.out.println(userDTO.getProvider()+ " School stop End: " + new Date());
				} else if(operation.equalsIgnoreCase("RUN")) {
					System.out.println(userDTO.getProvider()+ " RunPoints Start: " + new Date());
					if(this.processRunPoints(userDTO,false)) {
						System.out.println(userDTO.getProvider()+ " RunPoints DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " RunPoints ERROR");
					}
					System.out.println(userDTO.getProvider()+ " RunPoints End: " + new Date());
				} else if(operation.equalsIgnoreCase("ASSIGNMENTS")) {
					System.out.println(userDTO.getProvider()+ " RouteVehical Start: " + new Date());
					if(this.processRouteVehical(userDTO)) {
						System.out.println(userDTO.getProvider()+ " RouteVehical DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " RouteVehical ERROR");
					}
					System.out.println(userDTO.getProvider()+ " RouteVehical End: " + new Date());
				} else if(operation.equalsIgnoreCase("STUDENT")) {
					System.out.println(userDTO.getProvider()+ " STUDENT Start: " + new Date());
					if(this.processStudent(userDTO)) {
						System.out.println(userDTO.getProvider()+ " STUDENT DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " STUDENT ERROR");
					}
					System.out.println(userDTO.getProvider()+ " STUDENT End: " + new Date());
				} else if (operation.equalsIgnoreCase("ALL")) {
					System.out.println(userDTO.getProvider()+ " RoutesWithWayPoints Start: " + new Date());
					if(this.processRoutesWithWayPoints(userDTO)) {
						System.out.println(userDTO.getProvider()+ " RoutesWithWayPoints DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " RoutesWithWayPoints ERROR");
					}
					System.out.println(userDTO.getProvider()+ " RoutesWithWayPoints END: " + new Date());
	
					System.out.println(userDTO.getProvider()+ " Home stop Start: " + new Date());
					if(this.processBusStops(userDTO,OPERATION.HOME_STOP)) {
						System.out.println(userDTO.getProvider()+ " Home Stop DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " Home Stop ERROR");	
					}
					System.out.println(userDTO.getProvider()+ " Home stop End: " + new Date());
					System.out.println(userDTO.getProvider()+ " School stop Start: " + new Date());
					if(this.processBusStops(userDTO,OPERATION.SCHOOL_STOP)) {
						System.out.println(userDTO.getProvider()+ " School Stop DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " School Stop ERROR");	
					}
					System.out.println(userDTO.getProvider()+ " School stop End: " + new Date());
					System.out.println(userDTO.getProvider()+ " RunPoints Start: " + new Date());
					boolean localeOneTime = true;
					if(oneTime) { 
						localeOneTime = false;
					}
					if(this.processRunPoints(userDTO,localeOneTime)) {
						System.out.println(userDTO.getProvider()+ " RunPoints DONE");
					} else {
						System.out.println(userDTO.getProvider()+ " RunPoints ERROR");
					}
					System.out.println(userDTO.getProvider()+ " RunPoints End: " + new Date());
				}
				iUserCount++;
			} while(iUserCount<userList.size());
			o.close();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		System.gc();
	}

	public boolean processOperator(UserDTO user) {
		boolean flag = true;
		String operatorGuid = null;
		String databaseGuid = null;
		try {
			if(this.login(user)) {
				OperatorApi api = new OperatorApi();
				api.setApiClient(this.defaultApiCleint);
				List<Operator> list = api.operatorGet(operatorGuid, databaseGuid);
				if(list.size() > 0) {
					this.dao.saveOperator(list,user.getProvider());
					this.dao.logProcess(list.size(), true, "OPERATOR import done", user.getId(),OPERATION.OPERAOTR.getValue());
				}
			}
		} catch (ApiException e) {
			flag = false;
			System.out.println(user.getProvider());
			sysoutError(e);
			this.dao.logProcess(0, false, "ERROR: " + e.getResponseBody(), user.getId(),OPERATION.OPERAOTR.getValue());
		}
		return flag;
	}
	
	public boolean processRoutesWithWayPoints(UserDTO user) {
		boolean done = true;
		String routeGuid = null;
		String lastRouteGuid = null;
		int packageSize = 100;
		String databaseGuid = null;
		boolean work = true;
		ArrayList<OperatorDTO> operators =  this.dao.getOperatorGuids(user.getProvider());
		int totalProcessed = 0;
		int errorAttempt = 0;
		do {
			try {
				if(this.login(user)) {
					RouteApi api = new RouteApi();
					api.setApiClient(this.defaultApiCleint);
					List<Route>  routes =  api.routeGetAll(routeGuid, lastRouteGuid, packageSize, databaseGuid);
					for (Route route : routes) {
						sysout(route.toString(),user.getProvider());
						lastRouteGuid = route.getRouteGuid();
						boolean saveFlag = false;
						for (OperatorDTO operator : operators) {
							if(route.getOperatorID().equals(operator.getOperatorId()))
								saveFlag = true;
						}
						if(saveFlag ) {
							int id = this.dao.populateRoute(route);
							if (id>0) {
								this.processRouteWayPoints(lastRouteGuid,route.getRouteID(),id,user);
							}
						}
					}
					totalProcessed = totalProcessed + routes.size();
					if(routes.size()==packageSize) 
						work = true;
					else
						work = false;
					errorAttempt = 0;
				} else {
					work = true;
					errorAttempt = errorAttempt + 1;
				}
			} catch (ApiException e) {
				if(e.getCode() != 500) {
					this.generatTime = null;
				}
				errorAttempt = errorAttempt + 1;
				System.out.println(user.getProvider());
				sysoutError(e);
				if(errorAttempt == 1) {
					this.dao.logProcess(totalProcessed, true, "ROUTES import ERROR:" + e.getResponseBody() , user.getId(),OPERATION.ROUTE.getValue());
				}
				done = false;
			}
		} while(work && errorAttempt < 25);
		System.out.println("Routes: " + totalProcessed);
		System.out.println("Error attempts: " + errorAttempt);
		this.dao.logProcess(totalProcessed, true, "ROUTES import done Error:" +errorAttempt, user.getId(),OPERATION.ROUTE.getValue());
		return done;
	}
	
	public boolean processRouteWayPoints(String routeGuid,String routeID, int id,UserDTO user) {
		boolean done = true;
		WayPointApi api = new WayPointApi();
		String databaseGuid = null;
		try {
			if(this.login(user)) {
				api.setApiClient(this.defaultApiCleint);
				api.getApiClient().setConnectTimeout(180*1000);
				api.getApiClient().setReadTimeout(180*1000);
				api.getApiClient().setWriteTimeout(180*1000);
				List<WayPoint> points = api.wayPointGet(routeGuid, routeID, databaseGuid);
				if(points.size() > 0)
					this.dao.processRouteWayPoints(points,routeID, id);
				else
					System.out.println(user.getProvider()+ " No Data");
			}
		} catch (ApiException e) {
			System.out.println(user.getProvider());
			sysoutError(e);
			done = false;
		} 
		return done;
	}
	
	public boolean processRunPoints(UserDTO user,boolean isAll) {
		boolean done = true;
		String databaseGuid = null;
		String runGuid = null;
		String lastRunGuid = null;
		int packageSize = 100;
		boolean includeLatLongs = false;
		boolean includeRunPoints = false;
		boolean allDone = true;
		String supportedOperators = "";
		ArrayList<OperatorDTO> operators =  this.dao.getOperatorGuids(user.getProvider());
		for (OperatorDTO operatorDTO : operators) {
			if(StringUtils.isNotEmpty(supportedOperators )) {
				supportedOperators = supportedOperators + ",";
			}
			supportedOperators = supportedOperators + "'"+operatorDTO.getOperatorId() + "'";
		}
//		runGuid = "09a4aaf8-ed45-4898-95b4-b765c1aaf64b";
		String runidsUpdated = "";
		int totalProcessed = 0;
		int errorAttempt = 0;
//		System.out.println("One Time: " + isAll);
		do {
			try {
				if(this.login(user)) {
					RunApi api = new RunApi();
					api.setApiClient(this.defaultApiCleint);
					List<Run> runs =  api.runGetAll(runGuid, lastRunGuid, packageSize, includeLatLongs, includeRunPoints, databaseGuid);
//					System.out.println(runs.toString());
					for (Run run : runs) {
						sysout(run.toString(),user.getProvider());
						RunUpdateStatusDTO runStatus = this.dao.populateRunPoint(run,user.getProvider(),supportedOperators);	
						if (runStatus != null) {
							if(runStatus.isNewStatus() || runStatus.isOldUpdate() || runStatus.isStopsUpdated()) {
								if(StringUtils.isNotEmpty(runidsUpdated)) {
									runidsUpdated = runStatus.getRunid()+"," + runidsUpdated;
								} else {
									runidsUpdated = runStatus.getRunid() + "";
								}
								String runGuidUpdated = run.getRunGuid(); 
								boolean latLong = runStatus.isNewStatus()||runStatus.isStopsUpdated();
								List<Run> runLocal =  api.runGetAll(runGuidUpdated, null, 1, latLong, true, databaseGuid);
								if(runLocal.size()>0) {
									for (Run runL : runLocal) {
										if(runL.getComponentVariation().equalsIgnoreCase(runStatus.getComponentVariation())) {
											this.dao.populateOnlyRunPoint(runL,user.getProvider(),supportedOperators,runStatus);	
										}
									}
								}
							} else {
								if(StringUtils.isNotEmpty(runidsUpdated)) {
									runidsUpdated = runStatus.getRunid()+"," + runidsUpdated;
								} else {
									runidsUpdated = runStatus.getRunid() + "";
								}
							}
						}
					}
					totalProcessed = totalProcessed + runs.size();

					if(runs.size()<packageSize) 
						allDone = false;
					else
						lastRunGuid = runs.get(runs.size()-1).getRunGuid();
					done = true;
					errorAttempt = 0;
				} else {
					allDone = true;
					errorAttempt = errorAttempt + 1;
				}
			} catch (ApiException e) {
				if(e.getCode() != 500) {
					this.generatTime = null;
				}
				errorAttempt = errorAttempt + 1;
				sysoutError(e);
				if(errorAttempt == 1) {
					this.dao.logProcess(totalProcessed, true, "RUNS import Error:" +e.getResponseBody(), user.getId(),OPERATION.RUN.getValue());
				}
				done = false;
			}		
		} while(allDone && errorAttempt < 25);
		System.out.println("RUN: " + done + " : " + isAll);
		if(done && isAll) {
			if(StringUtils.isNotEmpty(runidsUpdated) && totalProcessed > 0) {
//				String sql = "update tdsb_i_run_points set Active = 0, RetireDate = getdate() where route_fk_id in (select id from tdsb_i_route where operator_id in ("+ supportedOperators +") and localroute = 0) and Active = 1 and id not in ("+ runidsUpdated +")";
//				System.out.println(sql);
				this.dao.deleteOldRuns(supportedOperators, runidsUpdated);
			}
			this.dao.logProcess(totalProcessed, true, "RUNS import DONE:" +errorAttempt, user.getId(),OPERATION.RUN.getValue());
		} else {
			this.dao.logProcess(totalProcessed, true, "RUNS import Error:" +errorAttempt, user.getId(),OPERATION.RUN.getValue());
		}
		System.out.println("RUNS: " + totalProcessed);
		return done;
	}
	
	public boolean processBusStops(UserDTO user,OPERATION stopType) {
		boolean done = true;
		String lastStopGuid = null;
		String databaseGuid = null;
		int packageSize = 200;
			List<BusStopDetails> stops = null;
			boolean ispending = true;
			ArrayList<OperatorDTO> operators =  this.dao.getOperatorGuids(user.getProvider());
			int totalProcessed = 0;
			int errorAttempt = 0;
			boolean syslog = false;
			do {
				try {
					if(this.login(user)) {
						BusStopApi api = new BusStopApi();
						api.setApiClient(this.defaultApiCleint);
						if(stopType == OPERATION.HOME_STOP) {
							stops = api.busStopGetHomeStops(packageSize, lastStopGuid, databaseGuid);
						} else if (stopType == OPERATION.SCHOOL_STOP) {
							stops = api.busStopGetSchoolStops(packageSize, lastStopGuid, databaseGuid);
						}
						if(stops != null && stops.size()>0) {
							sysout(stops.toString(),user.getProvider());
							int size =  stops.size();
							lastStopGuid = stops.get(size-1).getBasicInformation().getBusStopGuid();
							this.dao.populateBusStops(stops,user.getUserName(),operators,syslog);
							if(packageSize > size) {
								ispending = false;
//								System.out.println(totalProcessed + " : " + lastStopGuid);
							}
							totalProcessed = totalProcessed + stops.size();
						} else {
							ispending = false;
						}
						done = true;
						errorAttempt = 0;
					} else {
						ispending = true;
						errorAttempt = errorAttempt + 1;
					}
				} catch (ApiException e) {
					errorAttempt = errorAttempt + 1;
					System.out.println(user.getProvider());
					sysoutError(e);
					if(errorAttempt == 1) {
						this.dao.logProcess(totalProcessed, true, "BUSSTOP import Error:" +e.getResponseBody(), user.getId(),stopType.getValue());
					}
					done = false;
					ispending = true;
					if(e.getCode() != 500) {
						this.generatTime = null;
					}
				} 	
			} while(ispending && errorAttempt < 25);
			System.out.println(user.getProvider() + " : Stops Processed " + totalProcessed);
			this.dao.logProcess(totalProcessed, true, "BUSSTOP import ERROR ATTEMPTS:" +errorAttempt, user.getId(),stopType.getValue());	
		return done;
	}
	
	public boolean processRouteVehical(UserDTO user) {
		boolean done = true;
		GpsRouteVehicleApi api = new GpsRouteVehicleApi();
		try {
			ArrayList<OperatorDTO> operators =  this.dao.getOperatorGuids(user.getProvider());
			for (OperatorDTO operatorGuid : operators) {
				if(this.login(user)) {
					api.setApiClient(this.defaultApiCleint);
					List<GPSRouteVehicle>  vehicle =  api.gPSRouteVehicleGet(null, null, null, operatorGuid.getOperatorGuid(), null, null, null, null, null, null, null);
					 for (GPSRouteVehicle vehicle2 : vehicle) {
						 this.dao.saveGPSRouteVehicle(vehicle2,user.getProvider());
					}
				}
			}
			this.dao.logProcess(0, true, "ASSIGNMENT import DONE:", user.getId(),OPERATION.ASSIGNMENTS.getValue());	
		} catch (ApiException e) {
			System.out.println(user.getProvider());
			this.dao.logProcess(0, true, "ASSIGNMENT import Error:" +e.getResponseBody() , user.getId(),OPERATION.ASSIGNMENTS.getValue());	
			sysoutError(e);
			done = false;
		}
		return done;
	}
	
	public boolean processStudent(UserDTO user) {
		boolean done = true;
		String lastStudentGuid = null;
		String databaseGuid = null;
		int packageSize = 2;
		List<StudentComplete> students = null;
		boolean ispending = true;
		int totalProcessed = 0;
		int errorAttempt = 0;
		//New defaults
		String  routeID = "CWA1114";
		String studentGuid = null;
		String studentIDs = null;
		Boolean byAlternateIDs = true;
		String grade = null;
		String schoolID = null;
		Boolean onlyTransportedStudents = true;
		Boolean includeExtendedProperties = true;
		Boolean includeContacts = true;
		Boolean includeEnrollments = true;
		Boolean includeTransportationDetails = true;
		do {
			try {
				if(this.login(user)) {
					StudentCompleteApi api = new StudentCompleteApi();
					api.setApiClient(this.defaultApiCleint);
					students = api.studentCompleteGet(studentGuid, studentIDs, byAlternateIDs, grade, schoolID, routeID, 
							onlyTransportedStudents, lastStudentGuid, packageSize, includeExtendedProperties, includeContacts, 
							includeEnrollments, includeTransportationDetails, databaseGuid);
					if(students != null && students.size()>0) {
						for (StudentComplete studentComplete : students) {
							System.out.println(studentComplete.toString());
						}
						
						int size =  students.size();
						lastStudentGuid = students.get(size-1).getStudentGuid();
						if(packageSize > size)
							ispending = false;
						totalProcessed = totalProcessed + students.size();
					} else {
						ispending = false;
					}
					done = true;
				}
				errorAttempt = 0;
			} catch (ApiException e) {
				errorAttempt = errorAttempt + 1;
				System.out.println(user.getProvider());
				sysoutError(e);
				done = false;
				ispending = true;
				if(e.getCode() != 500) {
					this.generatTime = null;
				}
			} 	
		} while(ispending && errorAttempt < 25);
		System.out.println(user.getProvider() + " : Stops Processed " + totalProcessed);
		return done;
	}
	
	public static void sysoutError(ApiException e) {
		System.out.println(e.getCode());
		System.out.println(e.getResponseHeaders());
		System.out.println(e.getResponseBody());
		e.printStackTrace();
	}
	
	public boolean login(UserDTO user)  { //LOGIN
		boolean getNewTokan = true;
		if(this.generatTime != null) {
			Date now = new Date();
			long differenceInMillis = now.getTime() - generatTime.getTime();
		    long differenceInHours = (differenceInMillis) / 1000L;
		    if(differenceInHours>=this.expiresin) {
		    	this.generatTime = now;
		    } else {
		    	getNewTokan = false;
		    }
		} else {
			this.generatTime = new Date();
		}
	    if(getNewTokan) {
			OkHttpClient httpClient = new OkHttpClient();
			String input = "grant_type=password&username="+ user.getUserName() +"&password="+ user.getPassword();
//			RequestBody body = RequestBody.create(ProcessMain.JSON, input);
//			Request request = new Request.Builder().url(user.getUrl() + "/token").post(body).build();
			Response response1;
			try {
//				response1 = httpClient.newCall(request).execute();
//				ResponseBody body2 = response1.body();
//				String resp = body2.string();
//			    JSONObject obj = new JSONObject(resp);
//			    if(obj.has("access_token")) {
//			    	this.process_tokan = obj.getString("access_token");
//			    	this.expiresin = obj.getInt("expires_in");
//			    	this.defaultApiCleint.addDefaultHeader("Authorization", "Bearer "+this.process_tokan);
//			    	System.out.println(user.getProvider() + " Login");
//			    } else {
//			    	System.out.println(obj.toString());
//			    	getNewTokan = false;
//			    	this.dao.logProcess(0, false, "Login failed", user.getId(),OPERATION.LOGIN.getValue());
//			    }
//			    body2.close();
			} catch (Exception e1) {
				e1.printStackTrace();
				getNewTokan = false;
				this.dao.logProcess(0, false, "Login failed", user.getId(),OPERATION.LOGIN.getValue());
			}
	    } else {
	    	getNewTokan = true;
	    }
	    return getNewTokan;
	}
	
	public void sysout(String msg,String provider) {
			System.out.println(provider+ ": " + msg);
	}
	
	public static enum OPERATION {
		LOGIN(0),
		OPERAOTR(1),
		ROUTE(2),
		HOME_STOP(3),
		SCHOOL_STOP(3),
		ASSIGNMENTS(4),
		STUDENT(5),
		RUN(6);
		private final int value;
		OPERATION(final int newValue) {
            value = newValue;
        }
        public int getValue() { return value; }
	}
	private Date generatTime = null;
	private String process_tokan = null;
	private int expiresin = 0;
	private ArrayList<UserDTO>  userList = new ArrayList<UserDTO>();
	private ProcessDAO dao;
	ApiClient defaultApiCleint;
}