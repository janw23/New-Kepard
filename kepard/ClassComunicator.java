package kepard;

public class ClassComunicator extends Thread
{
	public void run()
	{
		try
		{
			Main.Start();
		}
		catch (Exception e)
		{
			Main.window.PaintText("ALERT: FATAL EXCEPTION OCCURED:\nProgram may not work properly\nError message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
}
