package cxy.twitch;

public class Degreetoinstruction {
    private Double angle;
    public Degreetoinstruction(Double a){
        angle = a;
    }
    public String getInstruction(){
        if (angle < -22.5*7 || angle >= 22.5*7)
            return "A";
        else if (angle < -22.5*5 && angle >= -22.5*7)
            return "Z";
        else if (angle < -22.5*3 && angle >= -22.5*5)
            return "X";
        else if (angle < -22.5 && angle >= -22.5*3)
            return "C";
        else if (angle < 22.5 && angle >= -22.5)
            return "D";
        else if (angle < 22.5*3 && angle >= 22.5)
            return "E";
        else if (angle < 22.5*5 && angle >= 22.5*3)
            return "W";
        else
            return "Q";
    }
}
