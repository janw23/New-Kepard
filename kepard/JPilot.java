package kepard;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import krpc.client.RPCException;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.AutoPilot;
import krpc.client.services.SpaceCenter.Control;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.Vessel;
import krpc.client.services.SpaceCenter.VesselSituation;

import org.javatuples.Triplet;

public class JPilot extends Thread
{
	public JWindow window;
	
	public boolean isActive;
	public SpaceCenter spaceCenter;
	
	public Vessel booster;
	public int booster_program = 0;
	public static int BOOSTER_PROGRAM_ASCENT = 1;
	public static int BOOSTER_PROGRAM_HOVER = 2;
	public static int BOOSTER_PROGRAM_LAND = 3;
	public static int BOOSTER_PROGRAM_ABORT = 4;
	public boolean booster_updateStarted = false;
	public boolean booster_update = true;
	public Flight booster_flight;
	public Flight booster_flight_surface;
	public Control booster_control;
	public AutoPilot booster_autopilot;
	public double booster_alt_radar = 0;
	public double booster_apoapsis = 0;
	public double booster_speed_vertical = 0;
	public double booster_TWR = 0;
	public boolean booster_landed = false;
	
	public Vessel capsule;
	public boolean capsule_updateStarted = false;
	public boolean capsule_update = false;
	public Flight capsule_flight;
	public Control capsule_control;
	public AutoPilot capsule_autopilot;
	public boolean capsule_landing_engines_fired = false;
	public boolean capsule_abort_activated = false;
	
	public boolean capsuleSet = false;
	public boolean capsule_landed = false;
	
	public double capsule_alt = 0;
	public double capsule_apoapsis = 0;
	public double capsule_velocity = 0;
	public double capsule_speed_vertical = 0;
	
	public double hover_alt = 7000d;
	public double hover_landsite_meanAlt = 83;
	public double hover_speed_vertical_max = 100d;
	public double hover_gforce_max = 0.15d;
	public double hover_gforce_brake = 0.4d;
	public double hover_deadband = 5d;
	public boolean hover_alt_reached = false;
	
	public double land_speed_vertical_max = 1.4d;
	public Triplet<Double, Double, Double> land_target_position = null;
	
	public boolean abort_activated = false;
	public boolean abort_chute_drag_deployed = false;
	public boolean abort_chute_main_deployed = false;
	public float abort_chute_drag_safeVel = 460;
	public float abort_chute_main_safeVel = 230;
	public float abort_chute_safeAlt = 7000;
	public float abort_pitch_min = 50;
	public float abort_booster_loss_pitch = 10;
	public boolean abort_chute_alert_displayed = false;
	public boolean abort_booster_catchup = false;
	public long abort_booster_ignition_delay = 1000000000l;
	private long abort_booster_ignition_timer = 0;
	public double abort_chute_alert_time = 4;
	
	public double align_distance_max_factor = 0.06666667;
	public boolean align_toLaunchpad = true;
	public double align_distance = 0;
	public dVector2 align_distance_horizontal;
	public dVector2 align_velocity_target;
	public PitchDir align_pdir;
	public dVector2 align_finalDistance;
	public boolean align_enabled = false;
	public boolean align_aligned = false;
	public int align_pitch_min = 70;
	public double align_pitch_factor = 2d;
	public double align_vel_max = 3;
	public dVector2 align_vel_current = new dVector2(0,0);
	public double align_vel_factor= 0.3d;
	public double align_vel_pow = 1.65d;
	public double align_vel_deadband = 0.4d;
	public double align_deadband_size = 8;
	public double align_vel_deadbandDecrease_distance = 20;
	public boolean align_airbrakes_enabled = false;
	public boolean align_reverse = false;
	public boolean[] align_airbrakes_state = new boolean[4];
	
	public boolean[] airbrake_state = new boolean[4];
	
	public boolean ascent_done = false;
	public double ascent_gforce = 1.1d;
	public double ascent_apoapsis = 75000;
	public double ascent_speed_horizontal = 0;
	
	public boolean booster_rcs_enabled = false;
	
	public void run()
	{
		window.telemetry_booster = new String[7][2];
		window.telemetry_booster[0][0] = "Program: ";
		window.telemetry_booster[1][0] = "Apo: ";
		window.telemetry_booster[2][0] = "Alt: ";
		window.telemetry_booster[3][0] = "Spd_vert: ";
		window.telemetry_booster[4][0] = "Spd_hor: ";
		window.telemetry_booster[5][0] = "Thr: ";
		window.telemetry_booster[6][0] = "Pitch: ";
		
		window.telemetry_capsule = new String[3][2];
		window.telemetry_capsule[0][0] = "Apo: ";
		window.telemetry_capsule[1][0] = "Alt: ";
		window.telemetry_capsule[2][0] = "Spd_vert: ";
		
		window.PaintText("JPilot thread started");
		
		while(true)
		{
			try {
				Refresh();
			} catch (RPCException | IOException e1) {
				e1.printStackTrace();
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void Initialize(Vessel ves, boolean enabled) throws InterruptedException
	{
		booster = ves;
		isActive = enabled;
	}
	
	public void setBoosterProgram(int num)
	{
		if(!(num == BOOSTER_PROGRAM_ABORT && (abort_activated || capsule != null)))
		{
			booster_program = num;
			window.PaintText("Booster program changed to: " + getBoosterProgram(num));
		}
	}
	
	String getBoosterProgram(int num)
	{
		switch(num)
		{
			case 1 : return "ASCENT";
			case 2 : return "HOVER";
			case 3 : return "LAND";
			case 4 : return "ABORT";
		}
		return "NoSuchProgram";
	}
	
	public void setAlign(boolean state)
	{
		align_enabled = state;
		window.PaintText("Align changed to: " + state);
	}
	
	public void setAlignToLaunchpad(boolean state)
	{
		align_toLaunchpad = state;
		
		if(state)window.PaintText("Retrying aligning to landsite!");
		else window.PaintText("ALERT: Aborted aligning to landsite!");
	}
	
	public void setActive(boolean state)
	{
		isActive = state;
		System.out.println("Active set to " + state);
	}
	
	public void setBooster(Vessel ves) throws RPCException, IOException
	{
		booster = ves;
		window.PaintText("Booster set to " + booster.getName());
	}
	
	public void setCapsule(Vessel ves) throws RPCException, IOException
	{
		capsule = ves;
		window.PaintText("Capsule set to " + capsule.getName());
		capsuleSet = true;
		capsule_flight = ves.flight(capsule.getOrbit().getBody().getReferenceFrame());
		capsule.getAutoPilot().disengage();
	}
	
	public void setLandingPosition(Triplet<Double, Double, Double> pos)
	{
		land_target_position = pos;
	}
	
	dVector2 getTargetAlignVel(dVector2 vec)
	{
		dVector2 toRet = new dVector2(0,0);
		
		if(align_toLaunchpad)
		{
			double length = vec.Magnitude();
			align_distance = length;
			
			if(align_reverse)
			{
				toRet.x = vec.x*align_vel_factor;
				toRet.y = vec.y*align_vel_factor;
			}
			else
			{
				if(length > align_deadband_size/2d)
				{
					align_aligned = false;
					if(length < align_vel_deadbandDecrease_distance && !align_airbrakes_enabled)
					{
						toRet.x = (align_vel_deadband + (align_vel_max - align_vel_deadband)*((Math.abs(vec.x)-align_deadband_size/2d))/align_vel_deadbandDecrease_distance)*MathJ.sign(-vec.x)*align_vel_factor;
						toRet.y = (align_vel_deadband + (align_vel_max - align_vel_deadband)*((Math.abs(vec.y)-align_deadband_size/2d))/align_vel_deadbandDecrease_distance)*MathJ.sign(-vec.y)*align_vel_factor;
					}
					else
					{
						toRet.x = -vec.x*align_vel_factor;
						toRet.y = -vec.y*align_vel_factor;
					}
				
					double velLength = toRet.Magnitude();
					if(velLength == velLength)
					{
						toRet = toRet.Normalized(velLength);
						velLength = Math.min(align_vel_max, velLength);
						toRet = toRet.Multiply(velLength);
					}
					else toRet = new dVector2(0, 0);
					
				}
				else
				{
					align_aligned = true;
					toRet = new dVector2(0,0);
				}
			}
		}
		else
		{
			align_aligned = true;
			toRet = new dVector2(0, 0);
		}
		////
		return toRet;
	}
	
	PitchDir getPitchDirFromVector(dVector2 vec) throws RPCException, IOException
	{
		PitchDir toRet = new PitchDir(0,0);
		Triplet<Double, Double, Double> relVel = spaceCenter.transformPosition(booster_flight.getVelocity(), booster.getOrbit().getBody().getReferenceFrame(), booster.getSurfaceReferenceFrame());
		align_vel_current.x = ((Double) relVel.getValue1()).doubleValue();
		align_vel_current.y = ((Double) relVel.getValue2()).doubleValue();
		
		align_vel_current = align_vel_current.Swap();
		
		vec = vec.Subtract(align_vel_current);
		toRet.HDG = (float) ((Math.atan2(vec.x, vec.y)/Math.PI)*180d);
		if(toRet.HDG < 0) toRet.HDG = 360f + toRet.HDG;
		
		double length = Math.pow(1+vec.Magnitude(), align_vel_pow)-1;
		
		toRet.pitch = Math.max(align_pitch_min,(float)(90d - length*align_pitch_factor) );
		
		return toRet;
	}
	
	float getTargetRoll(float hdg)
	{
		if(hdg < 180) return hdg;
		else return hdg - 360f;
	}
	
	void Refresh() throws RPCException, IOException
	{
		if(isActive)
		{
			try
			{
				UpdateBooster();
			}
			catch(Exception e)
			{
				booster_update = setVesselsAfterSeparation();
				if(booster_update) BoosterInitialUpdate();
				
				window.PaintText("ALERT: Booster's program encountered an error and\nmay not work properly. Error message:\n" + e.getMessage());
				e.printStackTrace();
			}
			
			try
			{
				UpdateCapsule();
			}
			catch(Exception e)
			{
				capsule_update = false;
				window.PaintText("ALERT: Capsule's program encountered an error and\nmay not work properly. Error message:\n" + e.getMessage());
				e.printStackTrace();
			}
			
			window.PaintTelemetry(booster_update, capsule_update, align_toLaunchpad && align_enabled && booster_update);
		}
	}
	
	void checkForAbort() throws RPCException, IOException
	{
		if(booster_flight_surface.getPitch() < abort_pitch_min || booster.getMaxVacuumThrust() < 10 && booster_alt_radar > 30)
			setBoosterProgram(BOOSTER_PROGRAM_ABORT);
	}
	
	void makeAbort() throws RPCException, IOException
	{
		abort_activated = true;
		setAlign(false);
		ascent_gforce = 0.3;
		capsule_landing_engines_fired = true;
		abort_booster_ignition_timer = System.nanoTime();
	}
	
	boolean setVesselsAfterSeparation() throws RPCException, IOException
	{
		boolean toRet = false;
		List<Vessel> vesList = spaceCenter.getVessels();
        for(int i = 0; i < vesList.size(); i++)
        {
        	if(vesList.get(i).getName().equals("New Kepard")) setCapsule(vesList.get(i));
        	else if(vesList.get(i).getName().equals("New Kepard Probe"))
        	{
        		setBooster(vesList.get(i));
        		toRet = true;
        	}
        }
        return toRet;
	}
	
	double getBetterAltitude(double alt_mean, double alt_bed)
	{
		if(alt_bed > alt_mean) return alt_mean;
		if(alt_bed < 150 || !align_toLaunchpad) return alt_bed;
		return alt_mean - hover_landsite_meanAlt;
	}
	
	void CapsuleInitialUpdate() throws RPCException, IOException
	{
		capsule_flight = capsule.flight(capsule.getOrbit().getBody().getReferenceFrame());
		
		capsule_control = capsule.getControl();
		capsule_autopilot = capsule.getAutoPilot();
		capsule_updateStarted = true;
	}
	
	int UpdateCapsule() throws RPCException, IOException
	{
		if(capsule == null || !capsule_update) return 0;
		
		if(!capsule_updateStarted)
			CapsuleInitialUpdate();
		
		//ABORT
		if(abort_activated)
		{	
			if(!capsule_abort_activated)
			{
				capsule_control.setSAS(false);
				capsule_autopilot.engage();
				capsule_autopilot.setTargetPitch(88);
				capsule_abort_activated = true;
			}
			else if(capsule.getAvailableThrust() == 0)
				capsule_autopilot.disengage();
		}
		//ABORT END
		
		//CHUTE
		if(!capsule_landed)
		{
			capsule_alt = capsule_flight.getSurfaceAltitude();
			capsule_velocity = capsule_flight.getTrueAirSpeed();
			capsule_speed_vertical = capsule_flight.getVerticalSpeed();
			capsule_apoapsis = capsule.getOrbit().getApoapsis();
			
			if(capsule_speed_vertical < 0)
			{
				if(capsule_alt <= abort_chute_safeAlt)
				{
					if(capsule_speed_vertical < -600) capsule_control.setAbort(true);
					
					if(capsule_alt < 24.5 && !capsule_landing_engines_fired)
					{
						capsule_control.setActionGroup(9, true);
						capsule_landing_engines_fired = true;
						window.PaintText("Capsule firing landing engines");
					}
					
					if(capsule_velocity < abort_chute_drag_safeVel&& !abort_chute_drag_deployed)
					{
						spaceCenter.setActiveVessel(capsule);
						capsule_control.activateNextStage();
						abort_chute_drag_deployed = true;
						window.PaintText("Capsule: deploying drag chute");
					}
					if(capsule_velocity < abort_chute_main_safeVel&& !abort_chute_main_deployed)
					{
						spaceCenter.setActiveVessel(capsule);
						capsule_control.activateNextStage();
						abort_chute_main_deployed = true;
						window.PaintText("Capsule: deploying main chute");
					}
				}
				else
				{
					if(!abort_chute_alert_displayed)
					{
						double time = -(capsule_alt - abort_chute_safeAlt)/capsule_speed_vertical;
						if(time < abort_chute_alert_time)
						{
							if(!Main.spaceCenter.getActiveVessel().equals(capsule))
								window.PaintText("ALERT: Set camera to Capsule!");
							
							abort_chute_alert_displayed = true;
						}
					}
				}
			}
			if(capsule.getSituation() == VesselSituation.LANDED || capsule.getSituation() == VesselSituation.SPLASHED)
			{
				capsule_landed = true;
				capsule_update = false;
			}
		}
		//CHUTE END
		
		window.telemetry_capsule[0][1] = new DecimalFormat("#0").format(capsule_apoapsis - 600000);
		window.telemetry_capsule[1][1] = new DecimalFormat("#0").format(capsule_alt);
		window.telemetry_capsule[2][1] = new DecimalFormat("#0.0").format(capsule_speed_vertical);
		
		return 1;
	}
	
	void BoosterInitialUpdate() throws RPCException, IOException
	{
		booster_flight = booster.flight(booster.getOrbit().getBody().getReferenceFrame());
		booster_flight_surface = booster.flight(booster.getSurfaceReferenceFrame());
		
		booster_control = booster.getControl();
		booster_autopilot = booster.getAutoPilot();
		booster_updateStarted = true;
	}
	
	int UpdateBooster() throws RPCException, IOException
	{
		if(booster == null || !booster_update) return 0;
		
		if(!booster_updateStarted)
			BoosterInitialUpdate();
		
		booster_alt_radar = getBetterAltitude(booster_flight.getMeanAltitude(), booster_flight.getBedrockAltitude());
		booster_speed_vertical = booster_flight.getVerticalSpeed();
		booster_apoapsis = booster.getOrbit().getApoapsis();
		
		booster_TWR = booster.getMaxThrust()/booster.getMass()/booster.getOrbit().getBody().getSurfaceGravity();
		
		//ALIGN
		if(align_enabled)
		{
			align_distance_horizontal = dVector2.tripletDistance(spaceCenter.transformPosition(land_target_position, booster.getOrbit().getBody().getReferenceFrame(), booster.getSurfaceReferenceFrame()), booster.position(booster.getSurfaceReferenceFrame()));
			align_velocity_target = getTargetAlignVel(align_distance_horizontal);
			align_pdir = getPitchDirFromVector(align_velocity_target);
			booster_autopilot.targetPitchAndHeading(align_pdir.pitch, align_pdir.HDG);
			booster_autopilot.setTargetRoll(getTargetRoll(align_pdir.HDG));
					
			if(booster_speed_vertical < -0.1) align_finalDistance = align_distance_horizontal.Add(align_vel_current.Multiply(booster_alt_radar/(Math.abs(booster_speed_vertical))));
			else align_finalDistance.Set(0,0);
			
			window.telemetry_booster_finalDistance = align_finalDistance;
					
			if(align_finalDistance.Magnitude() > booster_alt_radar * align_distance_max_factor + 100)
			{
				if(align_toLaunchpad) setAlignToLaunchpad(false);
			}
			else if(!align_toLaunchpad) setAlignToLaunchpad(true);
					
			if(align_airbrakes_enabled)
			{
				if(booster_flight.getTrueAirSpeed() > hover_speed_vertical_max - 50d) align_reverse = false;
				else align_reverse = true;
						
				if(align_toLaunchpad)
				{
					double distanceMax = 400*(Math.tanh((booster_alt_radar-38300)/15000)+1);
					double distanceMaxY = 0.00095 * booster_alt_radar + 1;
					
					if(align_finalDistance.x > distanceMax || (align_vel_current.x > 0 && align_finalDistance.x > 10))
					{
						align_airbrakes_state[1] = true;
						align_airbrakes_state[3] = false;
					}
					else if(-align_finalDistance.x > distanceMax || (align_vel_current.x < 0 && -align_finalDistance.x > 10))
					{
						align_airbrakes_state[1] = false;
						align_airbrakes_state[3] = true;
					}
					else
					{
						align_airbrakes_state[1] = true;
						align_airbrakes_state[3] = true;
					}
							
					if(align_finalDistance.y > distanceMaxY || (align_vel_current.y > 0 && align_finalDistance.y > 10))
					{
						align_airbrakes_state[0] = false;
						align_airbrakes_state[2] = true;
					}
					else if(-align_finalDistance.y > distanceMaxY || (align_vel_current.y < 0 && -align_finalDistance.y > 10))
					{
						align_airbrakes_state[0] = true;
						align_airbrakes_state[2] = false;
					}
					else
					{
						align_airbrakes_state[0] = true;
						align_airbrakes_state[2] = true;
					}
				}
				else
				{
					align_airbrakes_state[0] = true;
					align_airbrakes_state[1] = true;
					align_airbrakes_state[2] = true;
					align_airbrakes_state[3] = true;
				}
						
				for(int i = 0; i < 4; i++)
				{
					if(align_airbrakes_state[i] != airbrake_state[i])
					{
						airbrake_state[i] = align_airbrakes_state[i];
						if(align_airbrakes_state[i])
						{
							booster_control.setActionGroup(i+1, true);
							booster_control.setActionGroup(i+1, false);
						}
						else
						{
							booster_control.setActionGroup(i+5, true);
							booster_control.setActionGroup(i+5, false);
						}
					}
				}
				
				window.telemetry_booster_brakes = align_airbrakes_state;
			}
			else align_reverse = false;
						
		}
		
		if(booster_control.getBrakes())
		{
			window.telemetry_booster_brakes[0] = true;
			window.telemetry_booster_brakes[1] = true;
			window.telemetry_booster_brakes[2] = true;
			window.telemetry_booster_brakes[3] = true;
		}
		//ALIGN END
		
		//ABORT
		if(!abort_activated)
		{
			if(capsule == null)
			{
				if(booster_control.getAbort() || booster_program == BOOSTER_PROGRAM_ABORT)
				{
					if(booster_program == BOOSTER_PROGRAM_ABORT) booster_control.setAbort(true);
					else setBoosterProgram(BOOSTER_PROGRAM_ABORT);
					
					makeAbort();
					
					if(setVesselsAfterSeparation())
					{
						BoosterInitialUpdate();
						CapsuleInitialUpdate();
						capsule_update = true;
						booster_autopilot.engage();
						booster_autopilot.targetPitchAndHeading(90, 0);
						booster_control.setThrottle((float) (1/booster_TWR));
						booster_control.setRCS(true);
						booster_rcs_enabled = true;
					}
				}
				checkForAbort();
			}
		}
		else if(!abort_booster_catchup)
		{
			if(System.nanoTime() - abort_booster_ignition_timer >= abort_booster_ignition_delay)
			{
				if(booster_apoapsis < capsule_apoapsis && (booster_apoapsis < capsule_alt || capsule_speed_vertical > 0) && capsule_update)
				{
					booster_control.setThrottle((float) ((1+ascent_gforce)/booster_TWR));
					
					align_pdir = getPitchDirFromVector(new dVector2(0,0));
					booster_autopilot.targetPitchAndHeading(align_pdir.pitch, align_pdir.HDG);
					booster_autopilot.setTargetRoll(getTargetRoll(align_pdir.HDG));
				}
				else
				{
					booster_control.setThrottle(0);
					abort_booster_catchup = true;
				}
			}
		}
		
		if(booster_flight_surface.getPitch() < abort_booster_loss_pitch)
		{
			booster_autopilot.disengage();
			booster_control.setThrottle(0);
			booster_update = false;
			booster = null;
			window.PaintText("ALERT: Booster exceeded minimum pitch value\nand became abandoned");
		}
		//ABORT END
		
		//ASCENT
		if(booster_program == BOOSTER_PROGRAM_ASCENT)
		{
			if(!ascent_done)
			{
				align_velocity_target = new dVector2(Math.min(Math.abs(ascent_speed_horizontal), Math.abs(ascent_speed_horizontal*booster_alt_radar/3000d))*MathJ.sign(ascent_speed_horizontal), 0);
				align_pdir = getPitchDirFromVector(align_velocity_target);
				
				booster_autopilot.targetPitchAndHeading(align_pdir.pitch, align_pdir.HDG);
				booster_autopilot.setTargetRoll(getTargetRoll(align_pdir.HDG));
				
				booster_control.setThrottle((float) ((1+ascent_gforce)/booster_TWR));
				if(booster.getOrbit().getApoapsisAltitude() >= ascent_apoapsis)
				{
					booster_control.setThrottle(0);
					ascent_done = true;
				}
			}
			else
			{
				if(Math.abs(booster_flight_surface.getPitch() - booster_autopilot.getTargetPitch()) >= 2.5)
				{
					if(!booster_rcs_enabled)
					{
						booster_rcs_enabled = true;
						booster_control.setRCS(true);
					}
				}
				else
				{
					if(booster_rcs_enabled && ascent_apoapsis >= 50000)
					{
						booster_rcs_enabled = false;
						booster_control.setRCS(false);
					}
				}
			}
		}
		//ASCENT END
		
		//HOVER
		else if(booster_program == BOOSTER_PROGRAM_HOVER)
		{
			double altDif = booster_alt_radar - hover_alt;
			
			if(-altDif >= hover_deadband/2d)
			{
				hover_alt_reached = false;
				double newVertSpeed = hover_speed_vertical_max;
				
				if(-altDif <= Math.pow(hover_speed_vertical_max, 1.5))
					newVertSpeed = Math.min(hover_speed_vertical_max, -0.2*(altDif-hover_speed_vertical_max/2));
				
				double dif = booster_speed_vertical-newVertSpeed;
				booster_control.setThrottle( (float) ((1+0.08*Math.pow(dif, 2)*MathJ.sign(-dif)) /booster_TWR ));
			}
			else if(altDif >= hover_deadband/2d)
			{
				hover_alt_reached = false;
				double newVertSpeed = hover_speed_vertical_max;
				
				if(altDif <= Math.pow(hover_speed_vertical_max, 1.5))
					newVertSpeed = Math.min(hover_speed_vertical_max, 0.2*(altDif+hover_speed_vertical_max/2));
				
				double dif = booster_speed_vertical+newVertSpeed;
				booster_control.setThrottle( (float)  ((1+0.08*Math.pow(dif, 2)*MathJ.sign(-dif) )/booster_TWR ));
			}
			else
			{
				hover_alt_reached = true;
				booster_control.setThrottle( (float) ((1-0.08*booster_speed_vertical)/booster_TWR ) );
			}
		}
		//HOVER END
		
		//LAND
		else if(booster_program == BOOSTER_PROGRAM_LAND)
		{
			double dif = booster_speed_vertical+land_speed_vertical_max;
			booster_control.setThrottle( (float) ((1+0.08*Math.pow(dif, 2)*MathJ.sign(-dif)) /booster_TWR ));
			
			if(booster.getSituation() == VesselSituation.LANDED || booster.getSituation() == VesselSituation.SPLASHED)
			{
				booster_control.setThrottle(0);
				booster_control.setRCS(false);
				booster_rcs_enabled = false;
				booster_autopilot.disengage();
				booster_landed = true;
				booster_update = false;
			}
		}
		//LAND END
		
		window.telemetry_booster[0][1] = getBoosterProgram(booster_program);
		window.telemetry_booster[1][1] = new DecimalFormat("#0").format(booster_apoapsis - 600000);
		window.telemetry_booster[2][1] = new DecimalFormat("#0.0").format(booster_alt_radar);
		window.telemetry_booster[3][1] = new DecimalFormat("#0.0").format(booster_speed_vertical);
		window.telemetry_booster[4][1] = new DecimalFormat("#0.00").format(booster_flight.getHorizontalSpeed());
		window.telemetry_booster[5][1] = new DecimalFormat("#0.0%").format(booster_control.getThrottle());
		window.telemetry_booster[6][1] = new DecimalFormat("#0.0").format(booster_flight_surface.getPitch());
		
		return 1;
	}
}

class PitchDir
{
	public float HDG;
	public float pitch;
	
	public PitchDir(int h, int p)
	{
		HDG = h;
		pitch = p;
	}
}