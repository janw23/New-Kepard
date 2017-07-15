package kepard;

import org.javatuples.Triplet;

public class dVector2
{
	public double x = 0;
	public double y = 0;
	
	public dVector2(double arg0, double arg1)
	{
		x = arg0;
		y = arg1;
	}

	public static dVector2 tripletDistance(Triplet<Double, Double, Double> t0, Triplet<Double, Double, Double> t1)
	{
		return new dVector2(-((Double)t0.getValue2()).doubleValue() + ((Double)t1.getValue2()).doubleValue(),
						-((Double)t0.getValue1()).doubleValue() + ((Double)t1.getValue1()).doubleValue());
	}

	public double Magnitude()
	{
		return Math.sqrt((x*x) + (y*y));
	}
	
	public String ToString()
	{
		return "("+x+"; " + y+")";
	}
	
	public dVector2 Swap()
	{
		return new dVector2(y, x);
	}
	
	public dVector2 Add(dVector2 vec)
	{
		return new dVector2(x + vec.x, y + vec.y);
	}
	
	public dVector2 Subtract(dVector2 vec)
	{
		return new dVector2(x - vec.x, y - vec.y);
	}
	
	public dVector2 Multiply(double fac)
	{
		return new dVector2(x*fac, y*fac);
	}
	
	public dVector2 Normalized()
	{
		double l = Magnitude();
		return new dVector2(x/l, y/l);
	}
	public dVector2 Normalized(double l)
	{
		return new dVector2(x/l, y/l);
	}
}
