package kepard;

import java.io.IOException;
import java.util.List;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Engine;
import krpc.client.services.SpaceCenter.Vessel;

import org.javatuples.Triplet;

public class Main
{
	public static Connection connection;
	public static SpaceCenter spaceCenter;
	
	public static JWindow window;
	public static JPilot jpilot = new JPilot();
	
    public static void main(String[] args)
    {
    	window = new JWindow("New Kepard Controller");
    	window.pilot = jpilot;
    }
    
    public static void Start() throws IOException, InterruptedException, RPCException
    {
    	spaceCenter = SpaceCenter.newInstance(connection);
    	LaunchVessel();
    }

    public static void LaunchVessel() throws InterruptedException, RPCException, IOException
    {
        Vessel booster = spaceCenter.getActiveVessel();
        
        Triplet<Double, Double, Double> landSite = booster.position(booster.getOrbit().getBody().getReferenceFrame());
        
        jpilot.window = window;
        jpilot.start();
        jpilot.Initialize(booster, true);
        jpilot.setLandingPosition(landSite);
        jpilot.spaceCenter = spaceCenter;
        
        jpilot.booster.getAutoPilot().engage();
        
        jpilot.align_pitch_min = 70;
        jpilot.align_pitch_factor = 1d;
        jpilot.align_vel_pow = 1d;
        
        if(jpilot.ascent_apoapsis >= 25000)
        {
        	jpilot.ascent_speed_horizontal = -1.9944608793218117
        	        							+ 1.9518595102808859e-004 * jpilot.ascent_apoapsis
        	        							- 5.0246650413714171e-010 * Math.pow(jpilot.ascent_apoapsis,2)
        	        							+ 2.5025257031322003e-015 * Math.pow(jpilot.ascent_apoapsis,3);
        }
        
        jpilot.setBoosterProgram(JPilot.BOOSTER_PROGRAM_ASCENT);
        
        Thread.sleep(3000);
        jpilot.booster_control.activateNextStage();
        jpilot.booster_control.setGear(false);
        
        while(!jpilot.ascent_done && !jpilot.abort_activated){Thread.sleep(30);}
        jpilot.hover_alt = 18;
        
        if(!jpilot.abort_activated)
        {
	        Thread.sleep(3000);
	        jpilot.booster_control.activateNextStage();
        
		    jpilot.setActive(false);
			jpilot.setVesselsAfterSeparation();
		    jpilot.booster_updateStarted = false;
		    jpilot.setActive(true);
		    
		    jpilot.BoosterInitialUpdate();
		    jpilot.CapsuleInitialUpdate();
		    jpilot.capsule_update = true;
		    
		    jpilot.booster_autopilot.engage();
		    jpilot.booster_autopilot.setTargetPitch(90);
		    
		    if(jpilot.ascent_apoapsis < 50000)
		    {
		        jpilot.booster_control.setRCS(true);
		        jpilot.booster_rcs_enabled = true;
		    }
        }
        
        if(jpilot.booster != null)
        {
	        while(jpilot.booster_alt_radar > 60000 || jpilot.booster_speed_vertical > 0){Thread.sleep(30);}
	        while(jpilot.booster_speed_vertical > -20){Thread.sleep(30);}
	        
	        List<Engine> enList = jpilot.booster.getParts().getEngines();
	        for(int i = 0; i < enList.size(); i++)
	        {
	        	if(enList.get(i).getPart().getName().equals("SSME"))
	        	{
	        		enList.get(i).setGimbalLimit(0.33f);
	        		break;
	        	}
	        }
	        
	        jpilot.hover_speed_vertical_max = 1400;
	        jpilot.setAlign(true);
	        
	        jpilot.align_airbrakes_enabled = true;
	        
	        jpilot.align_pitch_min = 68;
	        jpilot.align_pitch_factor = 1d;
	        jpilot.align_vel_max = 50;
	        jpilot.align_vel_factor= 1.25d;
	        jpilot.align_vel_pow = 1.1d;//1.7d;
	        jpilot.align_vel_deadband = 1;
	        jpilot.align_deadband_size = 8;
	        jpilot.align_vel_deadbandDecrease_distance = 600;
	        
	        jpilot.setBoosterProgram(JPilot.BOOSTER_PROGRAM_HOVER);
	        
	        double hoverAlt = jpilot.hover_alt;
	        double minHoverAlt = 100000;
	        
	        jpilot.booster_control.setRCS(false);
        	jpilot.booster_rcs_enabled = false;
	        
	        while(!jpilot.hover_alt_reached || !jpilot.align_aligned)
	        {
	        	if(jpilot.booster_alt_radar < 1000 && jpilot.align_airbrakes_enabled)
	        	{
	        		jpilot.align_airbrakes_enabled = false;
	        		jpilot.booster_control.setBrakes(true);
	        	}
	        	
	        	if(!jpilot.booster_rcs_enabled && jpilot.booster_alt_radar < 8000)
	        	{
	        		jpilot.booster_control.setRCS(true);
        			jpilot.booster_rcs_enabled = true;
        		}
	        	else if(!jpilot.align_toLaunchpad) jpilot.booster_control.setRCS(false);
	        		
	        	if(jpilot.booster_alt_radar < 4500 && jpilot.booster_alt_radar > 100)
	        	{
	        		double suicideAlt = 0.5 * Math.pow(jpilot.booster_speed_vertical, 2) / ((jpilot.booster_TWR -1) * jpilot.booster.getOrbit().getBody().getSurfaceGravity());
	        		
		        	if(jpilot.booster_alt_radar <= suicideAlt) jpilot.hover_speed_vertical_max = 0;
		        	else jpilot.hover_speed_vertical_max = Math.pow(jpilot.booster_alt_radar+375,2)/7000;
	        		
	        		if(jpilot.align_toLaunchpad && jpilot.align_distance > 20)
	        		{
		        		minHoverAlt = Math.min(hoverAlt + 2*Math.pow(Math.abs(jpilot.align_distance), 0.7) + 0.0001*Math.pow(Math.abs(jpilot.align_distance), 2), minHoverAlt);
		            	jpilot.hover_alt = minHoverAlt;
	        		}
	        		else jpilot.hover_alt = hoverAlt;
	        	}
	        	else jpilot.hover_alt = hoverAlt;
	        	Thread.sleep(30);
	        }
	        
	        /*jpilot.align_pitch_min = 75;
	        jpilot.align_pitch_factor = 1.35d;
	        jpilot.align_vel_max = 10;
	        jpilot.align_vel_factor= 1;
	        jpilot.align_vel_pow = 1.2d;
	        jpilot.align_vel_deadband = 2;
	        jpilot.align_deadband_size = 6;
	        jpilot.align_vel_deadbandDecrease_distance = 450;*/
	        
	        while(!jpilot.align_aligned){Thread.sleep(30);}
	        
	        jpilot.align_pitch_min = 83;
	        jpilot.align_pitch_factor = 0.8d;
	        jpilot.align_vel_max = 3;
	        jpilot.align_vel_factor= 1.1d;
	        jpilot.align_vel_pow = 1.1d;
	        jpilot.align_vel_deadband = 0.9d;
	        jpilot.align_deadband_size = 4;
	        jpilot.align_vel_deadbandDecrease_distance = 200;
	        
	        jpilot.setBoosterProgram(JPilot.BOOSTER_PROGRAM_LAND);
	        jpilot.booster_control.setGear(true);
	        
	        while(!(jpilot.booster_landed && jpilot.capsule_landed)){Thread.sleep(100);}
	        jpilot.setActive(false);
        
        }
        
    }
}