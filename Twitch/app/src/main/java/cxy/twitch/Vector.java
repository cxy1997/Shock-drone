package cxy.twitch;

public class Vector {
    private double x;
    private double y;
    private double z;
    private double r;
    private double l;

    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        this.l = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    public double getSinTheta() {
        return y / r;
    }

    public double getCosTheta() {
        return x / r;
    }

    public double getTanTheta() {
        return y / x;
    }

    public double getSinPhi() {
        return r / l;
    }

    public double getCosPhi() {
        return z / l;
    }

    public double getTanPhi() {
        return r / z;
    }

    public static Vector minus(Vector a, Vector b) {
        return new Vector(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Vector plus(Vector a, Vector b) {
        return new Vector(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static double getAngleCos(Vector a, Vector b) {
        return (a.x * b.x + a.y * b.y + a.z * b.z)/(a.l * b.l);
    }

    public double getangle(){
        return Math.toDegrees(Math.atan2(-x, y));
    }

    public String gravity_instruction(){
        return new Degreetoinstruction(getangle()).getInstruction();
    }
}
