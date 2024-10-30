package utils;

public class RGB {
    public int r;
    public int g;
    public int b;
    public RGB (int r, int g, int b) {
    	this.r=r;
    	this.g=g;
    	this.b=b;
    }
    public int sqrDistance(RGB rgb) {
    	return (r-rgb.r)*(r-rgb.r)+(g-rgb.g)*(g-rgb.g)+(b-rgb.b)*(b-rgb.b);
    }

    public String toString() {
    	return ""+r+","+g+","+b;
    }
}
