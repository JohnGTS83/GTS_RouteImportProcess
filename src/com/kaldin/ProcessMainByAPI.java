package com.kaldin;

import java.util.Date;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

//import com.kaldin.dto.UserDTO;
import com.s5.common.db.QueryHelper;
import com.squareup.okhttp.MediaType;


public class ProcessMainByAPI {

	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	public static final int HOME_STOP = 1;
	public static final int SCHOOL_STOP = 2;
	public  static final int TIMEOUT_IN_SEC = 210;
	private static Scheduler scheduler;
	
	public static void main(String [] arg) {

		try {
			QueryHelper helper = new QueryHelper();
			helper.closeConnection();
			JobDataMap jobDataMap =  new JobDataMap();

			/*
			//For Wheelchair/McCluskey ST- Working
			UserDTO user = new UserDTO();
			user.setUrl("https://busplannerweb.torontoschoolbus.org/rest");
		    user.setUserName("genuineTSRest");
		    user.setPassword("5h728fGeD4rbWHpGv7IxXpSE");
		    user.setProvider("Wheelchair");
			jobDataMap.putIfAbsent("user-TDSB", user);
		
			//For STFrancobus/McCluskey - ST- Working
			user = new UserDTO();
			user.setUrl("https://infobus.francobus.ca/rest/");
			user.setUserName("GTSFrancobus");
			user.setPassword("VkRmHxTglHTWdVu");
			user.setProvider("McCluskey");
			jobDataMap.putIfAbsent("user-STFrancobus", user);
			
		
			//For STWDSTS/Cook
			user = new UserDTO();
			user.setUrl("https://www.findmyschool.ca/rest/");
			user.setUserName("GTSSTWDSTS");
			user.setPassword("Welcome123!");
			user.setProvider("Cook");
			jobDataMap.putIfAbsent("user-STWDSTS", user);

			//For WRBP - Waterloo Region
			user = new UserDTO();
			user.setUrl("https://bpweb.stswr.ca/rest/");
			user.setUserName("gts_stswr"); //GTSSTSWR
			user.setPassword("GTSWaterloo2020"); //1UOx4kOUZ2uCdUxEM1
			user.setProvider("WRBP");
			jobDataMap.putIfAbsent("user-WRBP", user);

		    //For YCDSB
			user = new UserDTO();
			user.setUrl("https://bp.schoolbuscity.com/rest/");
			user.setUserName("WAT_STSYR");
			user.setPassword("!CKcn6YK43");
			user.setProvider("YCDSB");
			jobDataMap.putIfAbsent("user-YCDSB", user);
			
		    //For STSBHN
			user = new UserDTO();
			user.setUrl("https://transinfobhn.ca/rest/");
			user.setUserName("gts");
			user.setPassword("koiOArD2lcHC");
			user.setProvider("STSBHN");
			jobDataMap.putIfAbsent("user-STSBHN", user);

		    //For GTS_OSTA - Voyago - Ottawa -  213
			user = new UserDTO();
			user.setUrl("https://ostabusplanner.ottawaschoolbus.ca/rest/");
			user.setUserName("GTS_OSTA");
			user.setPassword("G4YNTi3toH68jQpcg");
			user.setProvider("GTS_OSTA");
			jobDataMap.putIfAbsent("user-GTS_OSTA", user);
		
			
		    //For PVSD_GTS - Prairie Valley -  324
			user = new UserDTO();
			user.setUrl("https://busplanner-studentspvsd.msappproxy.net/rest/");
			user.setUserName("PVSD_GTS");
			user.setPassword("b58KvnnO!46RL5"); //b58Kv#&nnO!4$6RL5
			user.setProvider("PVSD_GTS");
			jobDataMap.putIfAbsent("user-PVSD_GTS", user);
			*/
			 
			/**/
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start(); //Now start schedule.
			schedule(true,jobDataMap,"ALL");

//			schedule(true,jobDataMap,"ALL");
//			schedule(true,jobDataMap,"STUDENT");
//			schedule(true,jobDataMap,"OPERATOR");
//			Thread.sleep(1000*10);
//			schedule(true,jobDataMap,"ROUTE");
//			Thread.sleep(1000*10);
//			schedule(true,jobDataMap,"RUN");
//			Thread.sleep(1000*10);
//			schedule(true,jobDataMap,"STOP");
//			schedule(true,jobDataMap,"ASSIGNMENTS");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	 
	static boolean schedule(boolean oneTime,JobDataMap jobDataMap,String operation) {
		try {
			jobDataMap.putIfAbsent("oneTime", oneTime);
			JobDetail job = JobBuilder.newJob(ImportJobByAPI.class).withIdentity("TDSB-"+operation, operation).setJobData(jobDataMap).build();
			
			if(oneTime) {
				Trigger runOnceTrigger = TriggerBuilder.newTrigger().build();
				scheduler.scheduleJob(job, runOnceTrigger);
			} else {
				CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity("TDSB-"+operation, operation).startAt(new Date()).withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(1,10)).build();
				scheduler.scheduleJob(job, trigger);
				System.out.println(trigger.getNextFireTime());
				System.out.println(trigger.getKey());
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
		return true;
	}
}